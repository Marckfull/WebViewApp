"""
selecao.py — escolhe QUAIS MOMENTOS do vídeo viram corte.

Este é o módulo que separa um canal de cortes bom de um ruim. Baixar e legendar
qualquer pedaço de 60 segundos é fácil; achar o pedaço que prende o espectador
é o trabalho de verdade.

Como ele pensa (na mesma ordem que um editor humano pensaria):

    1. NUNCA CORTAR NO MEIO DA FRASE
       Os cortes começam e terminam em fronteira de frase, usando os tempos que
       o Whisper devolveu. Um corte que começa em "...e é por isso que" perde o
       espectador em 1 segundo.

    2. PULAR A INTRO E A DESPEDIDA
       Os primeiros ~45s do vídeo-fonte quase sempre são vinheta, "fala galera"
       e patrocínio. Os últimos ~30s são "se inscreve, até a próxima".

    3. PROCURAR GANCHO, PALAVRA-CHAVE E EMOÇÃO
       Trecho que começa com "olha o que vazou" vale muito mais que trecho que
       começa com "então, como eu estava dizendo".

    4. FUGIR DE AUTOPROMOÇÃO
       "Link na descrição", "deixa o like", "cupom" — isso não faz sentido
       nenhum dentro do SEU corte, e ainda manda o espectador para outro canal.

    5. ENTREGAR MOMENTOS DIFERENTES ENTRE SI
       Os 3 cortes do dia não podem ser 3 recortes da mesma fala. O sistema
       compara o texto de cada candidato e descarta os parecidos.
"""

from __future__ import annotations

import re
import unicodedata
from dataclasses import dataclass, field
from typing import Sequence

from . import banco
from .utils import log


@dataclass
class Frase:
    """Uma frase falada, com início e fim (vem da transcrição do Whisper)."""

    texto: str
    inicio: float
    fim: float

    @property
    def duracao(self) -> float:
        return max(0.0, self.fim - self.inicio)


@dataclass
class Momento:
    """Um trecho candidato a virar corte."""

    inicio: float
    fim: float
    texto: str = ""
    pontuacao: float = 0.0
    motivos: list[str] = field(default_factory=list)
    palavras_chave: list[str] = field(default_factory=list)
    primeira_frase: str = ""

    @property
    def duracao(self) -> float:
        return max(0.0, self.fim - self.inicio)

    def sobrepoe(self, outro: "Momento", folga: float = 0.0) -> bool:
        """Diz se dois momentos se cruzam (com uma folga de segurança)."""
        return not (self.fim + folga <= outro.inicio or outro.fim + folga <= self.inicio)

    def para_dict(self) -> dict:
        return {
            "inicio": round(self.inicio, 2), "fim": round(self.fim, 2),
            "duracao": round(self.duracao, 1), "pontuacao": round(self.pontuacao, 2),
            "motivos": self.motivos, "palavras_chave": self.palavras_chave,
            "primeira_frase": self.primeira_frase, "texto": self.texto[:500],
        }


# ============================================================================
#  TEXTO
# ============================================================================

def _limpar(texto: str) -> str:
    sem_acento = unicodedata.normalize("NFKD", str(texto))
    sem_acento = "".join(c for c in sem_acento if not unicodedata.combining(c))
    return re.sub(r"\s+", " ", re.sub(r"[^\w\s]", " ", sem_acento.lower())).strip()


def _contar_expressoes(texto_limpo: str, expressoes: Sequence[str]) -> list[str]:
    """Quais expressões da lista aparecem no texto (já normalizado)."""
    achadas = []
    for expressao in expressoes or []:
        alvo = _limpar(expressao)
        if alvo and alvo in texto_limpo:
            achadas.append(str(expressao))
    return achadas


def _por_idioma(cfg, base: str, idioma: str) -> list[str]:
    """Pega a lista certa do config conforme o idioma ('..._pt' ou '..._en')."""
    sufixo = "en" if str(idioma).lower().startswith("en") else "pt"
    return list(cfg.pegar(f"selecao.{base}_{sufixo}", []) or [])


# ============================================================================
#  FRASES
# ============================================================================

def frases_da_transcricao(transcricao: dict) -> list[Frase]:
    """
    Tira a lista de frases da transcrição.

    O Whisper já devolve frases prontas. Se por algum motivo não vierem
    (transcrição antiga, arquivo editado), montamos as frases a partir das
    palavras, cortando na pontuação e nas pausas longas.
    """
    frases = [
        Frase(str(f.get("texto", "")).strip(), float(f.get("inicio", 0)),
              float(f.get("fim", 0)))
        for f in (transcricao.get("frases") or [])
        if str(f.get("texto", "")).strip()
    ]
    if frases:
        return frases

    # plano B: reconstruir as frases a partir das palavras
    palavras = transcricao.get("palavras") or []
    atual: list[dict] = []
    for i, palavra in enumerate(palavras):
        atual.append(palavra)
        texto = str(palavra.get("texto", "")).strip()
        proxima = palavras[i + 1] if i + 1 < len(palavras) else None
        pausa = (float(proxima["inicio"]) - float(palavra["fim"])) if proxima else 99.0

        if texto.endswith((".", "?", "!", "…")) or pausa > 0.8 or proxima is None:
            frases.append(Frase(
                " ".join(str(p.get("texto", "")) for p in atual).strip(),
                float(atual[0]["inicio"]), float(atual[-1]["fim"]),
            ))
            atual = []
    return frases


# ============================================================================
#  NOTA DE UM MOMENTO
# ============================================================================

def pontuar_momento(momento: Momento, cfg, idioma: str = "pt",
                    duracao_video: float = 0.0) -> tuple[float, list[str], list[str]]:
    """
    Dá a nota de um trecho candidato.

    Devolve (nota, motivos legíveis, palavras-chave encontradas).
    Os pesos de cada critério ficam em config.yaml → seção 'selecao'.
    """
    motivos: list[str] = []
    nota = 0.0

    texto_limpo = _limpar(momento.texto)
    # os primeiros 15 segundos valem mais: é onde o espectador decide ficar
    abertura = _limpar(momento.primeira_frase)

    peso_gancho = float(cfg.pegar("selecao.peso_gancho", 3.0))
    peso_chave = float(cfg.pegar("selecao.peso_palavras_chave", 2.0))
    peso_emocao = float(cfg.pegar("selecao.peso_emocao", 1.5))
    peso_fala = float(cfg.pegar("selecao.peso_densidade_fala", 1.0))
    penalidade_promo = float(cfg.pegar("selecao.penalidade_autopromocao", 3.0))

    # --- 1) gancho: o trecho promete algo logo no começo? -------------------
    ganchos = _por_idioma(cfg, "palavras_gancho", idioma)
    ganchos_na_abertura = _contar_expressoes(abertura, ganchos)
    ganchos_no_corpo = _contar_expressoes(texto_limpo, ganchos)
    if ganchos_na_abertura:
        nota += peso_gancho
        motivos.append(f"abre com gancho ('{ganchos_na_abertura[0]}')")
    elif ganchos_no_corpo:
        nota += peso_gancho * 0.4
        motivos.append(f"tem gancho no meio ('{ganchos_no_corpo[0]}')")

    # --- 2) palavras-chave do assunto ---------------------------------------
    chaves_config = (cfg.pegar("legendas.palavras_chave_en", [])
                     if str(idioma).startswith("en")
                     else cfg.pegar("legendas.palavras_chave_pt", []))
    encontradas = _contar_expressoes(texto_limpo, chaves_config)
    if encontradas:
        # densidade: quantas por minuto (trecho curto e cheio vale mais)
        minutos = max(0.5, momento.duracao / 60.0)
        densidade = len(encontradas) / minutos
        nota += min(peso_chave * 1.5, peso_chave * densidade * 0.5)
        motivos.append(f"{len(encontradas)} palavra(s)-chave: "
                       f"{', '.join(encontradas[:4])}")

    # --- 3) emoção -----------------------------------------------------------
    emocoes = _contar_expressoes(texto_limpo, _por_idioma(cfg, "palavras_emocao", idioma))
    if emocoes:
        nota += peso_emocao
        motivos.append(f"reação forte ('{emocoes[0]}')")
    if momento.texto.count("!") >= 2 or momento.texto.count("?") >= 2:
        nota += peso_emocao * 0.3
        motivos.append("fala exclamativa/perguntas")

    # --- 4) densidade de fala (silêncio longo mata a retenção) --------------
    palavras = len(texto_limpo.split())
    por_minuto = palavras / max(0.5, momento.duracao / 60.0)
    if por_minuto >= 110:
        nota += peso_fala
        motivos.append(f"fala constante ({por_minuto:.0f} palavras/min)")
    elif por_minuto < 60:
        nota -= peso_fala
        motivos.append(f"pouca fala ({por_minuto:.0f} palavras/min)")

    # --- 5) autopromoção (o que mais estraga um corte) ----------------------
    promocoes = _contar_expressoes(texto_limpo, _por_idioma(cfg, "autopromocao", idioma))
    if promocoes:
        nota -= penalidade_promo * len(promocoes)
        motivos.append(f"⚠️ autopromoção: {', '.join(promocoes[:3])}")

    # --- 6) posição no vídeo -------------------------------------------------
    ignorar_inicio = float(cfg.pegar("selecao.ignorar_inicio", 45))
    ignorar_fim = float(cfg.pegar("selecao.ignorar_fim", 30))
    if momento.inicio < ignorar_inicio:
        nota -= 2.0
        motivos.append("começa na introdução do vídeo")
    if duracao_video and momento.fim > duracao_video - ignorar_fim:
        nota -= 2.0
        motivos.append("pega a despedida do vídeo")

    # --- 7) duração perto do alvo -------------------------------------------
    alvo = float(cfg.pegar("selecao.duracao_alvo", 90))
    distancia = abs(momento.duracao - alvo) / max(1.0, alvo)
    nota += max(0.0, 1.0 - distancia)

    return nota, motivos, encontradas


# ============================================================================
#  GERAÇÃO DOS CANDIDATOS
# ============================================================================

def construir_candidatos(frases: Sequence[Frase], cfg) -> list[Momento]:
    """
    Monta todos os trechos possíveis que começam e terminam em frase completa.

    Para cada frase de início, tenta três tamanhos (curto, alvo e longo) dentro
    da faixa permitida (1 a 3 minutos, configurável). Isso dá bastante variedade
    sem explodir o número de combinações.
    """
    minimo = float(cfg.pegar("video.duracao_minima_corte", 60))
    maximo = float(cfg.pegar("video.duracao_maxima_corte", 180))
    alvo = float(cfg.pegar("selecao.duracao_alvo", 90))

    candidatos: list[Momento] = []
    vistos: set[tuple[float, float]] = set()

    for i, frase_inicial in enumerate(frases):
        for duracao_desejada in (minimo, alvo, maximo):
            fim_indice = i
            for j in range(i, len(frases)):
                if frases[j].fim - frase_inicial.inicio > maximo:
                    break
                fim_indice = j
                if frases[j].fim - frase_inicial.inicio >= duracao_desejada:
                    break

            inicio = frase_inicial.inicio
            fim = frases[fim_indice].fim
            duracao = fim - inicio
            if duracao < minimo or duracao > maximo:
                continue

            chave = (round(inicio, 1), round(fim, 1))
            if chave in vistos:
                continue
            vistos.add(chave)

            trecho = frases[i:fim_indice + 1]
            candidatos.append(Momento(
                inicio=inicio, fim=fim,
                texto=" ".join(f.texto for f in trecho).strip(),
                primeira_frase=trecho[0].texto if trecho else "",
            ))

    return candidatos


def _parecidos(a: Momento, b: Momento, limite: float = 0.5) -> bool:
    """
    Diz se dois trechos falam basicamente a mesma coisa.

    Compara as palavras dos dois (ignorando as muito curtas). Se mais da metade
    for igual, é a mesma ideia recortada de outro jeito — não serve como
    segundo vídeo.
    """
    palavras_a = {p for p in _limpar(a.texto).split() if len(p) > 3}
    palavras_b = {p for p in _limpar(b.texto).split() if len(p) > 3}
    if not palavras_a or not palavras_b:
        return False
    return len(palavras_a & palavras_b) / len(palavras_a | palavras_b) >= limite


# ============================================================================
#  ESCOLHA FINAL
# ============================================================================

def escolher_momentos(
    transcricao: dict,
    cfg,
    quantidade: int = 3,
    idioma: str = "pt",
    duracao_video: float = 0.0,
    conexao=None,
    video_id: str = "",
    deslocamento: float = 0.0,
) -> list[Momento]:
    """
    Devolve os melhores momentos, já DIFERENTES entre si e sem repetir nada
    que o sistema já cortou antes.

    deslocamento: se a transcrição começou no meio do vídeo (você transcreveu
    só um pedaço), informe aqui quantos segundos somar para os tempos baterem
    com o arquivo original.

    conexao + video_id: se você passar os dois, os trechos já usados no banco
    são descartados aqui mesmo.
    """
    frases = frases_da_transcricao(transcricao)
    if not frases:
        log("Transcrição sem frases — não dá para escolher momentos.", "aviso")
        return []

    candidatos = construir_candidatos(frases, cfg)
    if not candidatos:
        log(f"Nenhum trecho cabe na faixa de "
            f"{cfg.pegar('video.duracao_minima_corte', 60):.0f}s a "
            f"{cfg.pegar('video.duracao_maxima_corte', 180):.0f}s. "
            f"O vídeo/transcrição pode ser curto demais.", "aviso")
        return []

    for candidato in candidatos:
        nota, motivos, chaves = pontuar_momento(candidato, cfg, idioma, duracao_video)
        candidato.pontuacao = nota
        candidato.motivos = motivos
        candidato.palavras_chave = chaves

    candidatos.sort(key=lambda m: m.pontuacao, reverse=True)

    folga = float(cfg.pegar("selecao.distancia_minima_entre_cortes", 30))
    nota_minima = float(cfg.pegar("selecao.nota_minima", 1.0))
    escolhidos: list[Momento] = []
    reprovados_por_nota = 0

    for candidato in candidatos:
        if len(escolhidos) >= quantidade:
            break

        # Trecho ruim NÃO entra só para fechar a conta. É melhor entregar
        # 2 cortes bons do que 3 com um pegando patrocínio ou papo morno.
        if candidato.pontuacao < nota_minima:
            reprovados_por_nota += 1
            continue

        # não pode encostar em outro corte já escolhido
        if any(candidato.sobrepoe(escolhido, folga) for escolhido in escolhidos):
            continue

        # não pode dizer a mesma coisa que outro corte já escolhido
        if any(_parecidos(candidato, escolhido) for escolhido in escolhidos):
            continue

        # não pode repetir algo que já virou vídeo no passado
        if conexao is not None and video_id:
            repetido, _ = banco.trecho_ja_usado(
                conexao, video_id,
                candidato.inicio + deslocamento, candidato.fim + deslocamento,
            )
            if repetido:
                continue

        escolhidos.append(candidato)

    # devolve na ordem em que aparecem no vídeo (fica mais fácil de conferir)
    escolhidos.sort(key=lambda m: m.inicio)

    if deslocamento:
        for momento in escolhidos:
            momento.inicio += deslocamento
            momento.fim += deslocamento

    if len(escolhidos) < quantidade:
        detalhe = ""
        if reprovados_por_nota:
            detalhe = (f" {reprovados_por_nota} trecho(s) ficaram abaixo da nota "
                       f"mínima ({nota_minima:.1f}) — geralmente é intro, "
                       f"patrocínio ou papo sem gancho.")
        log(f"Só consegui {len(escolhidos)} momento(s) bom(ns) de "
            f"{quantidade} pedido(s).{detalhe}", "aviso")

    return escolhidos


def melhor_momento(transcricao: dict, cfg, idioma: str = "pt", **extras) -> Momento | None:
    """Atalho: devolve só o melhor momento (usado pelo modo automático)."""
    momentos = escolher_momentos(transcricao, cfg, quantidade=1, idioma=idioma, **extras)
    return momentos[0] if momentos else None


def formatar_tempo(segundos: float) -> str:
    """90.5 → '01:30'  (fica mais fácil de conferir no vídeo)"""
    total = int(round(segundos))
    return f"{total // 60:02d}:{total % 60:02d}"


def resumo_de_momentos(momentos: Sequence[Momento]) -> str:
    """Texto para mostrar no terminal."""
    if not momentos:
        return "   (nenhum momento encontrado)"
    linhas = []
    for i, m in enumerate(momentos, 1):
        linhas.append(
            f"   {i}. [{m.pontuacao:5.2f}] {formatar_tempo(m.inicio)} → "
            f"{formatar_tempo(m.fim)}  ({m.duracao:.0f}s)\n"
            f"        abre com: \"{m.primeira_frase[:70]}\"\n"
            f"        motivos : {', '.join(m.motivos) or '—'}"
        )
    return "\n".join(linhas)
