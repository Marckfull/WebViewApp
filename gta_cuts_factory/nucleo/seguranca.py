"""
seguranca.py — 🛡️ a camada que protege o seu canal.

Roda SEMPRE, antes de qualquer vídeo entrar na fila de aprovação. Nenhum vídeo
chega até você sem passar por aqui.

As seis checagens:

    1. REPETIÇÃO      → esse trecho ou esse assunto já viraram vídeo?
    2. FONTE          → o canal está bloqueado? é "em teste"? é oficial?
    3. NARRAÇÃO       → é cinemática isolada sem comentário? (regra da Rockstar)
    4. VIOLÊNCIA      → tem cena gráfica demais? (política do YouTube)
    5. SPOILER        → está entregando final/revelação de história?
    6. ASSUNTO PROIBIDO → está na sua lista de temas banidos?

O resultado é um SELO:

    🟢 verde    → fonte oficial/própria, sem violência forte → pode publicar
    🟡 amarelo  → fonte em teste ou violência moderada → publique com atenção
    🔴 vermelho → algum risco real → NÃO entra na fila, vai para "reprovados"

Filosofia: na dúvida, reprovar. É muito mais barato perder um corte do que
perder o canal.
"""

from __future__ import annotations

import re
import unicodedata
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from . import banco
from .utils import log


# Gravidade de cada motivo encontrado
BLOQUEIO = "bloqueio"   # reprova o vídeo (selo vermelho)
AVISO = "aviso"         # deixa passar, mas marca em amarelo
INFO = "info"           # só informativo


@dataclass
class Motivo:
    """Uma observação da checagem de segurança."""

    codigo: str
    gravidade: str
    mensagem: str

    def para_dict(self) -> dict:
        return {"codigo": self.codigo, "gravidade": self.gravidade,
                "mensagem": self.mensagem}


@dataclass
class Avaliacao:
    """O resultado completo da checagem de um vídeo."""

    aprovado: bool = True
    selo: str = "verde"
    motivos: list[dict] = field(default_factory=list)
    detalhes: dict = field(default_factory=dict)

    @property
    def emoji(self) -> str:
        return {"verde": "🟢", "amarelo": "🟡", "vermelho": "🔴"}[self.selo]

    @property
    def bloqueios(self) -> list[dict]:
        return [m for m in self.motivos if m["gravidade"] == BLOQUEIO]

    @property
    def avisos(self) -> list[dict]:
        return [m for m in self.motivos if m["gravidade"] == AVISO]

    def resumo_texto(self) -> str:
        """Texto curto para mostrar no terminal e no painel."""
        if self.bloqueios:
            return "REPROVADO: " + "; ".join(m["mensagem"] for m in self.bloqueios)
        if self.avisos:
            return "Aprovado com ressalvas: " + "; ".join(
                m["mensagem"] for m in self.avisos)
        return "Aprovado sem ressalvas."


def _normalizar(texto: str) -> str:
    """Minúsculas, sem acento — para comparar textos sem escorregar em acento."""
    sem_acento = unicodedata.normalize("NFKD", str(texto))
    sem_acento = "".join(c for c in sem_acento if not unicodedata.combining(c))
    return re.sub(r"\s+", " ", sem_acento.lower()).strip()


# ============================================================================
#  CHECAGEM 1 — REPETIÇÃO
# ============================================================================

def checar_repeticao(conexao, video_id: str, inicio: float, fim: float,
                     assunto: str, idioma: str) -> list[Motivo]:
    """O trecho ou o assunto já foram usados? (log anti-repetição)"""
    motivos: list[Motivo] = []

    repetido, explicacao = banco.trecho_ja_usado(conexao, video_id, inicio, fim)
    if repetido:
        motivos.append(Motivo("trecho_repetido", BLOQUEIO, explicacao))

    if assunto:
        usado, explicacao = banco.assunto_ja_usado(conexao, assunto, idioma)
        if usado:
            motivos.append(Motivo("assunto_repetido", BLOQUEIO, explicacao))

    return motivos


# ============================================================================
#  CHECAGEM 2 — REPUTAÇÃO DA FONTE
# ============================================================================

def checar_fonte(fonte: banco.Fonte | None) -> list[Motivo]:
    """
    Fonte bloqueada reprova na hora. Fonte "em teste" passa com aviso amarelo.
    Fonte oficial/própria passa limpo.
    """
    if fonte is None:
        return [Motivo(
            "fonte_desconhecida", AVISO,
            "fonte ainda não cadastrada no seu banco de canais — tratada como "
            "'em teste'"
        )]

    if fonte.bloqueada:
        return [Motivo(
            "fonte_bloqueada", BLOQUEIO,
            f"a fonte '{fonte.nome}' está BLOQUEADA "
            f"({fonte.claims} claim(s), {fonte.restricoes_idade} restrição(ões), "
            f"{fonte.remocoes} remoção(ões))"
        )]

    if fonte.status == "em_teste":
        return [Motivo(
            "fonte_em_teste", AVISO,
            f"a fonte '{fonte.nome}' ainda está em teste "
            f"({fonte.videos_gerados} vídeo(s) gerado(s) até agora) — "
            f"acompanhe o resultado antes de confiar"
        )]

    return [Motivo("fonte_confiavel", INFO,
                   f"fonte '{fonte.nome}' ({fonte.status})")]


# ============================================================================
#  CHECAGEM 3 — NARRAÇÃO / CINEMÁTICA ISOLADA (regra da Rockstar)
# ============================================================================

def medir_fala(transcricao: dict, duracao: float) -> dict:
    """
    Mede quanto do corte tem alguém falando.

    Devolve:
        {"palavras": 120, "segundos_falados": 42.0, "cobertura": 0.70}

    Cobertura alta = tem comentário/narração (conteúdo transformativo).
    Cobertura baixa = é só imagem rodando (gameplay bruto ou cinemática).
    """
    palavras = transcricao.get("palavras", []) if transcricao else []
    segundos = sum(
        max(0.0, float(p.get("fim", 0)) - float(p.get("inicio", 0))) for p in palavras
    )
    duracao = max(1.0, float(duracao))
    return {
        "palavras": len(palavras),
        "segundos_falados": round(segundos, 1),
        "cobertura": round(min(1.0, segundos / duracao), 3),
    }


def checar_narracao(
    fala: dict,
    cfg,
    status_fonte: str,
    modo: str = "A_corte",
    cobertura_minima: float = 0.20,
    palavras_minimas: int = 20,
) -> list[Motivo]:
    """
    Aplica a regra da Rockstar: cutscene/cinemática só pode aparecer DENTRO de
    conteúdo narrado ou de um vídeo maior — nunca como clipe isolado.

    Na prática: se o corte quase não tem fala, ele é "só imagem". Se além disso
    a fonte é material oficial, o sistema BLOQUEIA — é exatamente o caso que a
    Rockstar não permite (cinemática isolada, sem comentário).
    """
    if not cfg.pegar("seguranca.bloquear_cinematica_sem_narracao", True):
        return []

    # No Modo B a narração é sua (TTS sobre roteiro original): sempre tem voz.
    if modo == "B_narrado":
        return [Motivo("narracao_propria", INFO,
                       "vídeo narrado por você — uso transformativo")]

    tem_pouca_fala = (fala["cobertura"] < cobertura_minima
                      or fala["palavras"] < palavras_minimas)
    if not tem_pouca_fala:
        return []

    if status_fonte == "oficial":
        return [Motivo(
            "cinematica_isolada", BLOQUEIO,
            f"material oficial da Rockstar praticamente sem fala "
            f"({fala['palavras']} palavras, {fala['cobertura'] * 100:.0f}% do "
            f"tempo) — a Rockstar só permite cinemática dentro de conteúdo "
            f"narrado. Use o MODO B (narrado) para este assunto."
        )]

    if status_fonte == "proprio":
        return [Motivo(
            "gameplay_sem_fala", AVISO,
            "sua gravação quase não tem fala — considere adicionar narração "
            "para aumentar a retenção (risco de strike segue zero)"
        )]

    return [Motivo(
        "clipe_sem_comentario", AVISO,
        f"o trecho quase não tem fala ({fala['palavras']} palavras) — sem "
        f"comentário, o corte é pouco transformativo e aumenta o risco de "
        f"Content ID"
    )]


# ============================================================================
#  CHECAGEM 4 — VIOLÊNCIA (política de conteúdo do YouTube)
# ============================================================================

def analisar_violencia_visual(
    caminho_video: str | Path,
    inicio: float,
    duracao: float,
    amostras: int = 24,
) -> dict:
    """
    Estima quanto do trecho tem cena gráfica pesada, olhando as imagens.

    Como funciona (simples e rápido, roda em qualquer notebook):
        tira ~24 "fotos" espalhadas pelo trecho e mede, em cada uma, quanto da
        tela é "vermelho de sangue".

        Dois cuidados evitam alarme falso (aprendidos testando o detector):
        1. sangue é vermelho ESCURO/médio, não vermelho puro de tinta — por
           isso ignoramos o vermelho estourado (tipo 255,0,0);
        2. sangue tem textura irregular; carro vermelho, parede pintada, tela
           de "WASTED" e menu do jogo são cor CHAPADA — por isso, se a região
           vermelha for lisa demais, ela não conta.

    Devolve:
        {"analisado": True, "frames_graficos": 3, "total_frames": 24,
         "proporcao": 0.125, "segundos_estimados": 7.5, "pico": 0.09}

    ⚠️ É uma estimativa, não um laudo. Serve para pegar os casos evidentes
    (banho de sangue) e deixar o resto com você, no selo amarelo.
    """
    resultado = {"analisado": False, "motivo": "", "frames_graficos": 0,
                 "total_frames": 0, "proporcao": 0.0, "segundos_estimados": 0.0,
                 "pico": 0.0}
    try:
        import cv2  # type: ignore
        import numpy as np  # type: ignore
    except ImportError:
        resultado["motivo"] = "opencv_ausente"
        return resultado

    captura = cv2.VideoCapture(str(caminho_video))
    if not captura.isOpened():
        resultado["motivo"] = "video_inacessivel"
        return resultado

    try:
        graficos = 0
        total = 0
        pico = 0.0
        for i in range(max(2, amostras)):
            instante = inicio + duracao * (i / max(1, amostras - 1))
            captura.set(cv2.CAP_PROP_POS_MSEC, instante * 1000.0)
            ok, frame = captura.read()
            if not ok or frame is None:
                continue

            pequeno = cv2.resize(frame, (240, 135))
            canais = pequeno.astype("int16")
            azul, verde, vermelho = canais[:, :, 0], canais[:, :, 1], canais[:, :, 2]

            # "sangue" = vermelho médio/escuro que domina claramente verde e azul
            # (o < 235 descarta vermelho de tinta, tipo carro e menu do jogo)
            mascara = (
                (vermelho > 60) & (vermelho < 235)
                & (vermelho > verde * 1.7) & (vermelho > azul * 1.7)
            )
            proporcao_frame = float(mascara.mean())

            # cor chapada não é sangue: exigimos textura na região vermelha
            if proporcao_frame > 0.02:
                cinza = cv2.cvtColor(pequeno, cv2.COLOR_BGR2GRAY)
                textura = float(cinza[mascara].std()) if mascara.any() else 0.0
                if textura < 6.0:
                    proporcao_frame = 0.0

            pico = max(pico, proporcao_frame)
            total += 1
            if proporcao_frame > 0.06:      # mais de 6% da tela = cena pesada
                graficos += 1
    finally:
        captura.release()

    if total == 0:
        resultado["motivo"] = "video_inacessivel"
        return resultado

    proporcao = graficos / total
    resultado.update({
        "analisado": True,
        "frames_graficos": graficos,
        "total_frames": total,
        "proporcao": round(proporcao, 3),
        "segundos_estimados": round(proporcao * float(duracao), 1),
        "pico": round(pico, 3),
    })
    return resultado


def checar_violencia(analise: dict, texto: str, cfg) -> list[Motivo]:
    """
    Junta a análise das imagens com as palavras faladas e decide.

    Respeita duas configurações suas:
        seguranca.nivel_violencia_maximo       "nenhum" | "moderado" | "qualquer"
        seguranca.segundos_maximos_cena_grafica  duração ACUMULADA permitida

    O limite é de duração ACUMULADA porque a política do YouTube soma o total
    de cena gráfica do vídeo — não adianta ter várias cenas curtas.
    """
    motivos: list[Motivo] = []
    nivel = str(cfg.pegar("seguranca.nivel_violencia_maximo", "moderado")).lower()
    limite = float(cfg.pegar("seguranca.segundos_maximos_cena_grafica", 8))

    if not analise.get("analisado"):
        # A mensagem muda conforme o MOTIVO de não ter analisado: desligar a
        # análise de propósito não é um risco; não conseguir analisar é.
        explicacoes = {
            "desativado": (INFO, "análise de imagem desativada nesta execução"),
            "opencv_ausente": (AVISO, "OpenCV não instalado — não deu para "
                                      "analisar as imagens; confira o vídeo "
                                      "antes de publicar"),
            "video_inacessivel": (AVISO, "não consegui abrir o vídeo para "
                                         "analisar as imagens; confira antes "
                                         "de publicar"),
        }
        gravidade, mensagem = explicacoes.get(
            str(analise.get("motivo", "")),
            (AVISO, "não consegui analisar as imagens — confira o vídeo antes "
                    "de publicar"),
        )
        motivos.append(Motivo("violencia_nao_analisada", gravidade, mensagem))
    else:
        segundos = float(analise.get("segundos_estimados", 0.0))
        if nivel == "nenhum" and segundos > 0:
            motivos.append(Motivo(
                "violencia_detectada", BLOQUEIO,
                f"detectei ~{segundos:.0f}s de cena gráfica e sua configuração "
                f"não permite nenhuma"
            ))
        elif nivel != "qualquer" and segundos > limite:
            motivos.append(Motivo(
                "violencia_excessiva", BLOQUEIO,
                f"cena gráfica acumulada de ~{segundos:.0f}s passa do seu limite "
                f"de {limite:.0f}s — prefira corridas, NPCs engraçados ou "
                f"passeios pela cidade"
            ))
        elif segundos > 0:
            motivos.append(Motivo(
                "violencia_moderada", AVISO,
                f"~{segundos:.0f}s de cena possivelmente gráfica (dentro do seu "
                f"limite de {limite:.0f}s)"
            ))

    # palavras faladas também contam: "tortura", "massacre"...
    texto_normalizado = _normalizar(texto)
    termos = [t for t in (cfg.pegar("seguranca.termos_violencia", []) or [])
              if _normalizar(t) and _normalizar(t) in texto_normalizado]
    if termos:
        gravidade = BLOQUEIO if nivel == "nenhum" else AVISO
        motivos.append(Motivo(
            "violencia_na_fala", gravidade,
            f"a fala menciona: {', '.join(termos)}"
        ))

    return motivos


# ============================================================================
#  CHECAGEM 5 e 6 — SPOILERS E ASSUNTOS PROIBIDOS
# ============================================================================

def checar_spoiler(texto: str, assunto: str, cfg) -> list[Motivo]:
    """Bloqueia final do jogo, grandes revelações e spoilers de enredo."""
    if not cfg.pegar("seguranca.bloquear_spoilers", True):
        return []

    alvo = _normalizar(f"{assunto} {texto}")
    encontrados = [t for t in (cfg.pegar("seguranca.termos_spoiler", []) or [])
                   if _normalizar(t) and _normalizar(t) in alvo]
    if encontrados:
        return [Motivo(
            "spoiler", BLOQUEIO,
            f"o trecho parece conter spoiler de história ({', '.join(encontrados)}) "
            f"— a Rockstar não permite revelar enredo/final"
        )]
    return []


def checar_assunto_proibido(assunto: str, texto: str, proibidos: list[str]) -> list[Motivo]:
    """Sua lista pessoal de temas banidos (canais_fontes.yaml)."""
    alvo = _normalizar(f"{assunto} {texto[:2000]}")
    encontrados = [p for p in (proibidos or [])
                   if _normalizar(p) and _normalizar(p) in alvo]
    if encontrados:
        return [Motivo(
            "assunto_proibido", BLOQUEIO,
            f"assunto na sua lista de proibidos: {', '.join(encontrados)}"
        )]
    return []


# ============================================================================
#  AVALIAÇÃO COMPLETA (é esta que o pipeline chama)
# ============================================================================

def avaliar(
    conexao,
    cfg,
    fonte_video: Any,
    inicio: float,
    duracao: float,
    transcricao: dict | None = None,
    assunto: str = "",
    idioma: str = "pt",
    modo: str = "A_corte",
    assuntos_proibidos: list[str] | None = None,
    analisar_imagens: bool = True,
) -> Avaliacao:
    """
    Roda as SEIS checagens e devolve a decisão final.

    fonte_video: o VideoFonte que veio do downloader (tem id, canal_id, url...)
    """
    transcricao = transcricao or {}
    texto = str(transcricao.get("texto", ""))
    fim = float(inicio) + float(duracao)

    fonte_banco = banco.obter_fonte(conexao, str(getattr(fonte_video, "canal_id", "")))
    status_fonte = fonte_banco.status if fonte_banco else "em_teste"

    motivos: list[Motivo] = []
    motivos += checar_repeticao(conexao, str(getattr(fonte_video, "id", "")),
                                float(inicio), fim, assunto, idioma)
    motivos += checar_fonte(fonte_banco)

    fala = medir_fala(transcricao, duracao)
    motivos += checar_narracao(fala, cfg, status_fonte, modo)

    analise_visual: dict = {"analisado": False, "motivo": "desativado"}
    if analisar_imagens:
        analise_visual = analisar_violencia_visual(
            getattr(fonte_video, "arquivo", ""), float(inicio), float(duracao)
        )
    motivos += checar_violencia(analise_visual, texto, cfg)

    motivos += checar_spoiler(texto, assunto, cfg)
    motivos += checar_assunto_proibido(assunto, texto, assuntos_proibidos or [])

    # --- decisão final -------------------------------------------------------
    lista = [m.para_dict() for m in motivos]
    tem_bloqueio = any(m.gravidade == BLOQUEIO for m in motivos)
    tem_aviso = any(m.gravidade == AVISO for m in motivos)

    if tem_bloqueio:
        selo, aprovado = "vermelho", False
    elif tem_aviso or status_fonte == "em_teste":
        selo, aprovado = "amarelo", True
    else:
        selo, aprovado = "verde", True

    return Avaliacao(
        aprovado=aprovado,
        selo=selo,
        motivos=lista,
        detalhes={
            "status_fonte": status_fonte,
            "fala": fala,
            "violencia": analise_visual,
            "modo": modo,
            "trecho": {"inicio": float(inicio), "fim": fim, "duracao": float(duracao)},
        },
    )


def registrar_aprovacao(conexao, ficha: dict, avaliacao: Avaliacao) -> int:
    """
    Salva o resultado no banco:
        aprovado  → entra na FILA e o trecho/assunto ficam marcados como usados
        reprovado → vai para a lista de REPROVADOS com o motivo

    O trecho só é marcado como "usado" quando o vídeo é aprovado — assim um
    corte reprovado por violência pode ser reaproveitado depois com outros
    tempos, sem ficar preso no log anti-repetição.
    """
    id_registro = banco.registrar_video_gerado(conexao, ficha, avaliacao)

    if avaliacao.aprovado:
        fonte = ficha.get("fonte", {})
        trecho = ficha.get("trecho", {})
        if fonte.get("id"):
            banco.registrar_trecho(
                conexao, fonte["id"], float(trecho.get("inicio", 0)),
                float(trecho.get("fim", 0)), ficha.get("idioma", ""),
            )
        if ficha.get("assunto"):
            banco.registrar_assunto(conexao, ficha["assunto"], ficha.get("idioma", ""))

    return id_registro


def imprimir_avaliacao(avaliacao: Avaliacao) -> None:
    """Mostra o resultado da checagem no terminal, de forma legível."""
    log(f"Selo de segurança: {avaliacao.emoji} {avaliacao.selo.upper()}",
        "ok" if avaliacao.aprovado else "erro")
    for motivo in avaliacao.motivos:
        icone = {BLOQUEIO: "   🔴", AVISO: "   🟡", INFO: "   🟢"}[motivo["gravidade"]]
        print(f"{icone} {motivo['mensagem']}")

    detalhes = avaliacao.detalhes
    fala = detalhes.get("fala", {})
    violencia = detalhes.get("violencia", {})
    print(f"   ── fala: {fala.get('palavras', 0)} palavras "
          f"({fala.get('cobertura', 0) * 100:.0f}% do tempo) | "
          f"cena gráfica estimada: {violencia.get('segundos_estimados', 0)}s")
