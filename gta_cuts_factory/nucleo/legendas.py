"""
legendas.py — o coração visual do projeto.

Gera um arquivo .ass (Advanced SubStation Alpha) com legenda estilo
CapCut/TikTok:

    • palavra por palavra, sincronizada pelo timestamp de cada palavra
    • a palavra que está sendo falada AGORA muda de cor e dá um "pop" (cresce)
    • palavras de impacto (GTA 6, VAZOU, CONFIRMADO, LEAK...) saem em amarelo
    • texto grande, contorno grosso e sombra → legível em qualquer fundo
    • posicionada um pouco abaixo do centro da tela

Por que .ass e não legenda desenhada pelo Python?
    Porque o ffmpeg (com a biblioteca libass) desenha isso em altíssima
    qualidade e MUITO rápido — sem precisar processar frame a frame no Python.
    É o mesmo formato usado por fansubs de anime há 20 anos: aguenta cor por
    palavra, animação, escala, rotação e posicionamento exato.
"""

from __future__ import annotations

import json
import re
import unicodedata
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence

from .utils import cor_para_ass, para_tempo_ass


# ============================================================================
#  ESTRUTURA DE DADOS
# ============================================================================

@dataclass
class Palavra:
    """Uma palavra falada, com o momento exato em que começa e termina."""

    texto: str
    inicio: float  # em segundos, contando do começo do CORTE
    fim: float

    @property
    def duracao(self) -> float:
        return max(0.0, self.fim - self.inicio)

    @staticmethod
    def de_dict(dados: dict) -> "Palavra":
        """Aceita as chaves em português (nossas) ou em inglês (do Whisper)."""
        return Palavra(
            texto=str(dados.get("texto") or dados.get("word") or "").strip(),
            inicio=float(dados.get("inicio", dados.get("start", 0.0))),
            fim=float(dados.get("fim", dados.get("end", 0.0))),
        )

    def para_dict(self) -> dict:
        return {"texto": self.texto, "inicio": self.inicio, "fim": self.fim}


def carregar_palavras(caminho: str | Path) -> list[Palavra]:
    """Lê um JSON de transcrição e devolve a lista de palavras."""
    with open(caminho, "r", encoding="utf-8") as arquivo:
        dados = json.load(arquivo)
    lista = dados["palavras"] if isinstance(dados, dict) else dados
    return [Palavra.de_dict(item) for item in lista]


# ============================================================================
#  PALAVRAS-CHAVE (as que ganham cor de destaque)
# ============================================================================

def _normalizar(texto: str) -> str:
    """
    Deixa a palavra "crua" para comparação: sem pontuação, sem acento, maiúscula.

        "lançamento!"  ->  "LANCAMENTO"
    """
    sem_acento = unicodedata.normalize("NFKD", texto)
    sem_acento = "".join(c for c in sem_acento if not unicodedata.combining(c))
    return re.sub(r"[^\w\s]", "", sem_acento, flags=re.UNICODE).upper().strip()


def marcar_palavras_chave(
    palavras: Sequence[Palavra],
    lista_chaves: Iterable[str],
) -> list[bool]:
    """
    Descobre quais palavras devem sair na cor de destaque.

    Funciona também com expressões de 2 ou 3 palavras ("GTA 6", "GTA VI"):
    nesse caso TODAS as palavras da expressão são marcadas.

    Devolve uma lista de True/False do mesmo tamanho da lista de palavras.
    """
    chaves = {_normalizar(chave) for chave in lista_chaves if str(chave).strip()}
    marcadas = [False] * len(palavras)
    cruas = [_normalizar(p.texto) for p in palavras]

    for i, crua in enumerate(cruas):
        if not crua:
            continue
        # tenta expressões de 3, depois 2, depois 1 palavra (a mais longa vence)
        for tamanho in (3, 2, 1):
            if i + tamanho > len(cruas):
                continue
            expressao = " ".join(cruas[i : i + tamanho]).strip()
            if expressao and expressao in chaves:
                for j in range(i, i + tamanho):
                    marcadas[j] = True
                break
    return marcadas


def palavras_chave_do_idioma(cfg, idioma: str) -> list[str]:
    """Pega a lista de palavras-chave do config conforme o idioma do vídeo."""
    if str(idioma).lower().startswith("en"):
        return list(cfg.pegar("legendas.palavras_chave_en", []) or [])
    return list(cfg.pegar("legendas.palavras_chave_pt", []) or [])


# ============================================================================
#  AGRUPAMENTO (quantas palavras aparecem juntas na tela)
# ============================================================================

def agrupar_palavras(
    palavras: Sequence[Palavra],
    max_palavras: int = 3,
    max_caracteres: int = 20,
    pausa_maxima: float = 0.7,
) -> list[list[Palavra]]:
    """
    Divide a fala em "blocos" que aparecem juntos na tela.

    Regras de quebra (nesta ordem):
        1. atingiu o número máximo de palavras
        2. o texto ficou comprido demais
        3. houve uma pausa longa na fala (respiração / mudança de assunto)
        4. a palavra terminou com . ? ! (fim de frase)

    Isso é o que faz a legenda "respirar" junto com a pessoa falando, em vez
    de ficar trocando de bloco no meio de uma ideia.
    """
    grupos: list[list[Palavra]] = []
    atual: list[Palavra] = []
    caracteres = 0

    for i, palavra in enumerate(palavras):
        if not palavra.texto.strip():
            continue

        pausa = 0.0
        if atual:
            pausa = palavra.inicio - atual[-1].fim

        estourou = (
            len(atual) >= max_palavras
            or (atual and caracteres + len(palavra.texto) + 1 > max_caracteres)
            or (atual and pausa > pausa_maxima)
        )
        if estourou:
            grupos.append(atual)
            atual, caracteres = [], 0

        atual.append(palavra)
        caracteres += len(palavra.texto) + 1

        # fim de frase → fecha o bloco (a não ser que seja só uma palavrinha)
        if palavra.texto.rstrip().endswith((".", "?", "!", "…")) and len(atual) >= 2:
            grupos.append(atual)
            atual, caracteres = [], 0

    if atual:
        grupos.append(atual)
    return grupos


def _escapar_ass(texto: str) -> str:
    """
    Protege caracteres que têm significado especial no formato ASS.
    ({ e } abrem comandos; \\ é escape; quebras de linha viram espaço)
    """
    return (
        texto.replace("\\", "\\\\")
        .replace("{", "(")
        .replace("}", ")")
        .replace("\n", " ")
        .replace("\r", " ")
        .strip()
    )


def _largura_estimada(texto: str, tamanho_fonte: int) -> float:
    """
    Estima quantos pixels o texto vai ocupar.

    É um cálculo aproximado: fontes pesadas (Arial Black, Impact, Montserrat
    ExtraBold) ocupam cerca de 0,72 da altura da fonte por caractere — valor
    medido rodando o render de teste. Serve só para decidir se precisamos
    quebrar o bloco em duas linhas.
    """
    return len(texto) * tamanho_fonte * 0.72


def _quebrar_em_duas_linhas(
    grupo: Sequence[Palavra], tamanho_fonte: int, largura_util: float
) -> list[list[int]]:
    """
    Se o bloco não couber na largura da tela, divide em 2 linhas.
    Devolve os índices das palavras de cada linha, ex: [[0,1],[2]]
    """
    texto_todo = " ".join(p.texto for p in grupo)
    if _largura_estimada(texto_todo, tamanho_fonte) <= largura_util or len(grupo) < 2:
        return [list(range(len(grupo)))]

    # procura o ponto de corte que deixa as duas linhas mais equilibradas
    melhor_corte, melhor_diferenca = 1, float("inf")
    for corte in range(1, len(grupo)):
        esquerda = " ".join(p.texto for p in grupo[:corte])
        direita = " ".join(p.texto for p in grupo[corte:])
        diferenca = abs(_largura_estimada(esquerda, tamanho_fonte)
                        - _largura_estimada(direita, tamanho_fonte))
        if diferenca < melhor_diferenca:
            melhor_corte, melhor_diferenca = corte, diferenca

    return [list(range(melhor_corte)), list(range(melhor_corte, len(grupo)))]


# ============================================================================
#  GERAÇÃO DO ARQUIVO .ASS
# ============================================================================

def _cabecalho_ass(cfg) -> str:
    """Monta o [Script Info] e os estilos (aparência padrão do texto)."""
    largura = int(cfg.pegar("video.largura", 1080))
    altura = int(cfg.pegar("video.altura", 1920))

    leg = cfg.pegar("legendas", {})
    gan = cfg.pegar("gancho", {})

    def estilo(nome: str, fonte: str, tamanho: int, cor: str, contorno_cor: str,
               contorno: float, sombra: float, borda_caixa: bool = False) -> str:
        # Ordem dos campos definida pelo formato ASS (não mude a ordem!)
        return (
            f"Style: {nome},{fonte},{tamanho},"
            f"{cor_para_ass(cor)},{cor_para_ass(cor)},"
            f"{cor_para_ass(contorno_cor)},{cor_para_ass('#000000', 60)},"
            f"-1,0,0,0,"          # Bold=-1 (negrito), Italic, Underline, StrikeOut
            f"100,100,0,0,"       # ScaleX, ScaleY, Spacing, Angle
            f"{3 if borda_caixa else 1},{contorno},{sombra},"  # BorderStyle, Outline, Shadow
            f"5,60,60,60,1"       # Alignment=5 (centro), margens, Encoding
        )

    linhas_estilo = [
        estilo(
            "Legenda",
            leg.get("fonte", "Arial Black"),
            int(leg.get("tamanho_fonte", 96)),
            leg.get("cor_texto", "#FFFFFF"),
            leg.get("cor_contorno", "#000000"),
            float(leg.get("espessura_contorno", 7)),
            float(leg.get("sombra", 4)),
        ),
        estilo(
            "Gancho",
            gan.get("fonte", "Arial Black"),
            int(gan.get("tamanho_fonte", 110)),
            gan.get("cor_texto", "#FFFFFF"),
            gan.get("cor_contorno", "#000000"),
            float(gan.get("espessura_contorno", 9)),
            float(gan.get("sombra", 4)),
            borda_caixa=bool(gan.get("caixa_fundo", True)),
        ),
    ]

    return (
        "[Script Info]\n"
        "; Gerado automaticamente pelo GTA Cuts Factory\n"
        "ScriptType: v4.00+\n"
        f"PlayResX: {largura}\n"
        f"PlayResY: {altura}\n"
        "WrapStyle: 2\n"                 # não quebra linha sozinho (nós controlamos)
        "ScaledBorderAndShadow: yes\n"
        "YCbCr Matrix: TV.709\n"
        "\n"
        "[V4+ Styles]\n"
        "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, "
        "OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, "
        "ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, "
        "MarginL, MarginR, MarginV, Encoding\n"
        + "\n".join(linhas_estilo)
        + "\n\n[Events]\n"
        "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, "
        "Effect, Text\n"
    )


def eventos_legenda(palavras: Sequence[Palavra], cfg, idioma: str = "pt") -> list[str]:
    """
    Transforma a lista de palavras nas linhas "Dialogue:" do arquivo ASS.

    A técnica: para CADA palavra criamos um evento que mostra o bloco inteiro,
    mas com aquela palavra colorida e ampliada. Como um evento começa exatamente
    quando o anterior termina, o resultado na tela é o efeito karaokê —
    a palavra "acende" no instante em que é falada.
    """
    leg = cfg.pegar("legendas", {})
    largura = int(cfg.pegar("video.largura", 1080))
    altura = int(cfg.pegar("video.altura", 1920))

    tamanho_fonte = int(leg.get("tamanho_fonte", 96))
    maiusculas = bool(leg.get("maiusculas", True))
    escala_pop = int(leg.get("escala_pop", 118))

    cor_normal = cor_para_ass(leg.get("cor_texto", "#FFFFFF"))
    cor_ativa = cor_para_ass(leg.get("cor_palavra_ativa", "#00E5FF"))
    cor_chave = cor_para_ass(leg.get("cor_palavra_chave", "#FFE100"))

    pos_x = largura // 2
    pos_y = int(altura * float(leg.get("posicao_vertical", 0.62)))
    largura_util = largura * 0.88  # deixa uma margem lateral de segurança

    grupos = agrupar_palavras(
        palavras,
        max_palavras=int(leg.get("palavras_por_linha", 3)),
        max_caracteres=int(leg.get("max_caracteres_linha", 20)),
    )
    chaves = palavras_chave_do_idioma(cfg, idioma)

    eventos: list[str] = []

    for grupo in grupos:
        if not grupo:
            continue
        marcadas = marcar_palavras_chave(grupo, chaves)
        linhas = _quebrar_em_duas_linhas(grupo, tamanho_fonte, largura_util)
        fim_do_grupo = grupo[-1].fim

        for indice, palavra in enumerate(grupo):
            inicio = palavra.inicio
            # o evento vai até o começo da próxima palavra: assim o bloco nunca
            # some entre uma palavra e outra (nada de legenda piscando)
            fim = grupo[indice + 1].inicio if indice + 1 < len(grupo) else fim_do_grupo
            if fim <= inicio:
                fim = inicio + 0.12

            partes_linhas = []
            for linha in linhas:
                pedacos = []
                for j in linha:
                    texto = grupo[j].texto.strip()
                    if maiusculas:
                        texto = texto.upper()
                    texto = _escapar_ass(texto)
                    if not texto:
                        continue

                    if j == indice:
                        # palavra sendo falada: cor de destaque + efeito "pop"
                        cor = cor_chave if marcadas[j] else cor_ativa
                        tags = (
                            f"\\c{cor}\\fscx100\\fscy100"
                            f"\\t(0,90,\\fscx{escala_pop}\\fscy{escala_pop})"
                            f"\\t(90,190,\\fscx100\\fscy100)"
                        )
                    elif marcadas[j]:
                        tags = f"\\c{cor_chave}"      # palavra-chave sempre amarela
                    else:
                        tags = f"\\c{cor_normal}"
                    pedacos.append(f"{{{tags}}}{texto}")
                partes_linhas.append(" ".join(pedacos))

            corpo = "\\N".join(partes_linhas)  # \N = quebra de linha no ASS
            aparicao = "\\fad(90,0)" if indice == 0 else ""
            texto_final = f"{{\\an5\\pos({pos_x},{pos_y}){aparicao}}}{corpo}"

            eventos.append(
                f"Dialogue: 0,{para_tempo_ass(inicio)},{para_tempo_ass(fim)},"
                f"Legenda,,0,0,0,,{texto_final}"
            )

    return eventos


def gerar_ass(
    palavras: Sequence[Palavra],
    cfg,
    idioma: str = "pt",
    eventos_extra: Sequence[str] | None = None,
) -> str:
    """
    Monta o conteúdo completo do arquivo .ass.

    eventos_extra: linhas "Dialogue:" de outros módulos (ex.: o gancho de
    abertura, gerado por gancho.py).
    """
    partes = [_cabecalho_ass(cfg)]
    partes.extend(eventos_legenda(palavras, cfg, idioma))
    if eventos_extra:
        partes.extend(eventos_extra)
    return "\n".join(partes) + "\n"


def salvar_ass(
    palavras: Sequence[Palavra],
    cfg,
    destino: str | Path,
    idioma: str = "pt",
    eventos_extra: Sequence[str] | None = None,
) -> Path:
    """Gera o .ass e grava no disco (UTF-8, que é o que a libass espera)."""
    destino = Path(destino)
    destino.parent.mkdir(parents=True, exist_ok=True)
    conteudo = gerar_ass(palavras, cfg, idioma, eventos_extra)
    destino.write_text(conteudo, encoding="utf-8")
    return destino
