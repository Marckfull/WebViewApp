"""
fontes.py — escolhe DE ONDE cortar.

Este módulo pega os assuntos em alta (tendencias.py) e procura, **apenas nos
canais que você aprovou**, quais vídeos servem de matéria-prima.

Por que só nos seus canais?
    Porque varrer o YouTube inteiro é a receita para tomar claim. Um banco de
    fontes pequeno e conhecido é a sua maior proteção — você sabe de quem está
    cortando, e o sistema aprende quais canais dão problema.

O que ele faz, em ordem:
    1. lista os vídeos recentes de cada canal aprovado (yt-dlp, sem chave de API)
    2. joga fora o que não serve: canal bloqueado, vídeo curto demais, assunto
       proibido, vídeo já muito cortado
    3. dá uma nota para cada candidato (fonte confiável + assunto quente +
       vídeo recente + duração boa)
    4. DIVERSIFICA: escolhe os melhores garantindo canais e assuntos DIFERENTES
"""

from __future__ import annotations

import re
import unicodedata
from dataclasses import dataclass, field
from pathlib import Path

from . import banco
from .tendencias import PALAVRAS_VAZIAS, Assunto, data_recente
from .utils import LoggerSilencioso, log


EXTENSOES_VIDEO = {".mp4", ".mkv", ".mov", ".avi", ".webm", ".m4v"}


@dataclass
class Candidato:
    """Um vídeo-fonte que pode virar corte."""

    video_id: str
    titulo: str
    url: str
    canal: str
    canal_id: str
    status_fonte: str
    duracao: float = 0.0
    data_envio: str = ""
    assunto: str = ""
    pontuacao: float = 0.0
    motivos: list[str] = field(default_factory=list)
    arquivo_local: str = ""

    @property
    def eh_local(self) -> bool:
        return bool(self.arquivo_local)

    @property
    def selo(self) -> str:
        return {"oficial": "🟢", "proprio": "🟢",
                "em_teste": "🟡", "bloqueado": "🔴"}.get(self.status_fonte, "⚪")

    def para_dict(self) -> dict:
        return {
            "video_id": self.video_id, "titulo": self.titulo, "url": self.url,
            "canal": self.canal, "canal_id": self.canal_id,
            "status_fonte": self.status_fonte, "duracao": self.duracao,
            "data_envio": self.data_envio, "assunto": self.assunto,
            "pontuacao": round(self.pontuacao, 2), "motivos": self.motivos,
            "arquivo_local": self.arquivo_local,
        }


def _limpar(texto: str) -> str:
    sem_acento = unicodedata.normalize("NFKD", str(texto))
    sem_acento = "".join(c for c in sem_acento if not unicodedata.combining(c))
    return re.sub(r"[^\w\s]", " ", sem_acento.lower())


# ============================================================================
#  LISTAR VÍDEOS DE UM CANAL
# ============================================================================

def listar_videos_do_canal(url_canal: str, limite: int = 10) -> list[dict]:
    """
    Lista os vídeos mais recentes de um canal do YouTube.

    Usa o yt-dlp em modo raso (só a listagem, não baixa nada). Se falhar,
    devolve lista vazia — o sistema continua com os outros canais.
    """
    try:
        from yt_dlp import YoutubeDL  # type: ignore
    except ImportError:
        log("yt-dlp não instalado: rode pip install -r requirements.txt", "aviso")
        return []

    endereco = url_canal.rstrip("/")
    if "/@" in endereco and not endereco.endswith(("/videos", "/shorts")):
        endereco += "/videos"

    opcoes = {"quiet": True, "no_warnings": True, "skip_download": True,
              "extract_flat": True, "playlistend": limite,
              "logger": LoggerSilencioso()}
    try:
        with YoutubeDL(opcoes) as ydl:
            dados = ydl.extract_info(endereco, download=False)
        entradas = dados.get("entries") or []
        return [
            {"id": e.get("id", ""), "titulo": e.get("title", ""),
             "url": e.get("url") or f"https://youtu.be/{e.get('id', '')}",
             "duracao": float(e.get("duration") or 0),
             "data_envio": str(e.get("upload_date") or ""),
             "views": int(e.get("view_count") or 0)}
            for e in entradas if e and e.get("id")
        ][:limite]
    except Exception as erro:
        log(f"Não consegui listar '{url_canal}' ({type(erro).__name__}).", "aviso")
        return []


def listar_videos_locais(pasta: str | Path, limite: int = 20) -> list[dict]:
    """
    Lista os vídeos de uma pasta do seu computador (suas gravações).

    Essa é a fonte de risco ZERO: material seu, sem Content ID possível.
    """
    caminho = Path(pasta)
    if not caminho.exists():
        log(f"Pasta de gravações não encontrada: {caminho}", "aviso")
        return []

    arquivos = [a for a in sorted(caminho.iterdir(), reverse=True)
                if a.is_file() and a.suffix.lower() in EXTENSOES_VIDEO]
    return [
        {"id": a.stem, "titulo": a.stem, "url": "", "duracao": 0.0,
         "data_envio": "", "views": 0, "arquivo_local": str(a)}
        for a in arquivos[:limite]
    ]


# ============================================================================
#  NOTA DE CADA CANDIDATO
# ============================================================================

def combinar_com_assunto(titulo: str, assuntos: list[Assunto]) -> tuple[str, float]:
    """
    Vê com qual assunto em alta o título do vídeo tem mais a ver.

    Devolve (assunto, força), onde força vai de 0 (nada a ver) a 1 (todas as
    palavras do assunto aparecem no título).

    Só as palavras que CARREGAM sentido contam. Sem isso, "Receita de bolo"
    casaria 25% com "GTA 6 data de lançamento" — por causa do "de".
    """
    titulo_limpo = set(_limpar(titulo).split())
    melhor_termo, melhor_forca = "", 0.0

    for assunto in assuntos:
        palavras = [p for p in _limpar(assunto.termo).split()
                    if p not in PALAVRAS_VAZIAS and (len(p) > 1 or p.isdigit())]
        if not palavras:
            continue
        acertos = sum(1 for p in palavras if p in titulo_limpo)
        forca = acertos / len(palavras)
        # o assunto mais quente desempata quando a força é igual
        if forca > melhor_forca or (forca == melhor_forca and forca > 0
                                    and assunto.pontuacao > 0 and not melhor_termo):
            melhor_termo, melhor_forca = assunto.termo, forca

    return melhor_termo, melhor_forca


def pontuar(video: dict, fonte: banco.Fonte, assuntos: list[Assunto],
            cfg) -> tuple[float, str, list[str]]:
    """
    Dá a nota de um vídeo candidato.

    A nota soma quatro coisas (e a primeira é de longe a mais importante):

        confiança da fonte  → oficial/próprio valem muito mais que "em teste"
        assunto em alta     → o título fala do que está bombando?
        recência            → vídeo novo rende mais no algoritmo
        duração             → precisa caber um corte de 1 a 3 minutos

    Devolve (nota, assunto_casado, lista de motivos legíveis).
    """
    motivos: list[str] = []
    nota = 0.0

    # --- 1) confiança da fonte ---------------------------------------------
    pontos_status = {"oficial": 4.0, "proprio": 4.0, "em_teste": 1.0}
    nota += pontos_status.get(fonte.status, 0.0)
    nota += (fonte.peso or 5) / 10.0
    motivos.append(f"fonte {fonte.status}")

    # --- 2) assunto em alta -------------------------------------------------
    assunto, forca = combinar_com_assunto(video.get("titulo", ""), assuntos)
    if forca > 0:
        nota += forca * 3.0
        motivos.append(f"casa com '{assunto}' ({forca * 100:.0f}%)")

    # --- 3) recência --------------------------------------------------------
    dias = int(cfg.pegar("producao.dias_recentes", 30))
    if data_recente(video.get("data_envio", ""), dias=7):
        nota += 1.5
        motivos.append("publicado nos últimos 7 dias")
    elif data_recente(video.get("data_envio", ""), dias=dias):
        nota += 0.7
        motivos.append(f"publicado nos últimos {dias} dias")

    # --- 4) duração ---------------------------------------------------------
    duracao = float(video.get("duracao") or 0)
    minimo = float(cfg.pegar("video.duracao_minima_corte", 60))
    if duracao and duracao >= minimo * 3:
        nota += 0.8
        motivos.append("dá para tirar vários cortes")
    elif duracao and duracao < minimo + 20:
        nota -= 2.0
        motivos.append("curto demais para um corte")

    return nota, assunto, motivos


# ============================================================================
#  BUSCA DOS CANDIDATOS
# ============================================================================

def buscar_candidatos(
    conexao,
    cfg,
    canais: dict,
    assuntos: list[Assunto],
    idioma: str = "pt",
    por_canal: int | None = None,
    usar_rede: bool = True,
) -> list[Candidato]:
    """
    Percorre os canais aprovados e monta a lista de candidatos, já filtrada
    e ordenada pela nota.
    """
    por_canal = por_canal or int(cfg.pegar("producao.candidatos_por_canal", 8))
    max_cortes = int(cfg.pegar("producao.max_cortes_por_video", 3))
    proibidos = [_limpar(p) for p in (canais.get("assuntos_proibidos", []) or [])]
    minimo = float(cfg.pegar("video.duracao_minima_corte", 60))

    candidatos: list[Candidato] = []
    descartados = 0

    for entrada in (canais.get("canais", []) or []):
        nome = str(entrada.get("nome", "")).strip()
        if not nome:
            continue

        identificador = str(entrada.get("canal_id") or entrada.get("url")
                            or entrada.get("pasta_local") or nome)
        fonte = banco.obter_fonte(conexao, identificador)
        if fonte is None:
            fonte = banco.registrar_fonte(
                conexao, identificador, nome, url=str(entrada.get("url", "")),
                status=str(entrada.get("status", "em_teste")),
                peso=int(entrada.get("peso", 5)),
                idioma=str(entrada.get("idioma", "")),
            )

        # --- canal bloqueado: nem lista ------------------------------------
        if fonte.bloqueada:
            log(f"Pulando '{fonte.nome}': canal BLOQUEADO "
                f"({fonte.problemas} problema(s)).", "aviso")
            continue

        # --- lista os vídeos -------------------------------------------------
        if entrada.get("pasta_local"):
            videos = listar_videos_locais(entrada["pasta_local"], por_canal)
        elif usar_rede and entrada.get("url"):
            videos = listar_videos_do_canal(str(entrada["url"]), por_canal)
        else:
            videos = []

        for video in videos:
            titulo = video.get("titulo", "")

            # assunto proibido no título
            if any(p and p in _limpar(titulo) for p in proibidos):
                descartados += 1
                continue

            # vídeo curto demais para um corte de 1 minuto
            duracao = float(video.get("duracao") or 0)
            if duracao and duracao < minimo:
                descartados += 1
                continue

            # já foi cortado vezes demais (diversificar também dentro do canal)
            usados = len(banco.trechos_do_video(conexao, video.get("id", "")))
            if usados >= max_cortes:
                descartados += 1
                continue

            nota, assunto, motivos = pontuar(video, fonte, assuntos, cfg)
            if usados:
                motivos.append(f"{usados} corte(s) já feito(s) deste vídeo")

            candidatos.append(Candidato(
                video_id=video.get("id", ""), titulo=titulo,
                url=video.get("url", ""), canal=fonte.nome, canal_id=fonte.canal_id,
                status_fonte=fonte.status, duracao=duracao,
                data_envio=video.get("data_envio", ""), assunto=assunto,
                pontuacao=nota, motivos=motivos,
                arquivo_local=video.get("arquivo_local", ""),
            ))

    candidatos.sort(key=lambda c: c.pontuacao, reverse=True)
    log(f"{len(candidatos)} candidato(s) encontrado(s), {descartados} descartado(s).",
        "ok" if candidatos else "aviso")
    return candidatos


# ============================================================================
#  DIVERSIFICAÇÃO
# ============================================================================

def diversificar(candidatos: list[Candidato], quantidade: int = 3) -> list[Candidato]:
    """
    Escolhe os melhores garantindo VARIEDADE.

    A regra, em três passadas (só relaxa quando não tem jeito):
        1ª passada: canais diferentes E assuntos diferentes  ← o ideal
        2ª passada: aceita repetir o canal, mas não o assunto
        3ª passada: completa com o que sobrou

    Isso evita os dois erros clássicos: publicar 3 cortes do mesmo canal
    (chama atenção e concentra risco) e publicar 3 vídeos sobre a mesma
    notícia (o público vê como repetição).
    """
    # ordena aqui dentro: assim a função funciona mesmo que a lista chegue
    # fora de ordem (não depende de quem chamou ter ordenado antes)
    candidatos = sorted(candidatos, key=lambda c: c.pontuacao, reverse=True)

    escolhidos: list[Candidato] = []
    canais_usados: set[str] = set()
    assuntos_usados: set[str] = set()

    def chave_assunto(candidato: Candidato) -> str:
        # sem assunto casado, o próprio vídeo conta como assunto único
        return _limpar(candidato.assunto) or f"video::{candidato.video_id}"

    # 1ª passada — o ideal
    for candidato in candidatos:
        if len(escolhidos) >= quantidade:
            break
        if candidato.canal_id in canais_usados:
            continue
        if chave_assunto(candidato) in assuntos_usados:
            continue
        escolhidos.append(candidato)
        canais_usados.add(candidato.canal_id)
        assuntos_usados.add(chave_assunto(candidato))

    # 2ª passada — aceita repetir canal, mantém assunto diferente
    if len(escolhidos) < quantidade:
        for candidato in candidatos:
            if len(escolhidos) >= quantidade:
                break
            if candidato in escolhidos:
                continue
            if chave_assunto(candidato) in assuntos_usados:
                continue
            escolhidos.append(candidato)
            assuntos_usados.add(chave_assunto(candidato))

    # 3ª passada — completa com o que sobrou (vídeos diferentes, ao menos)
    if len(escolhidos) < quantidade:
        for candidato in candidatos:
            if len(escolhidos) >= quantidade:
                break
            if candidato in escolhidos:
                continue
            if any(c.video_id == candidato.video_id for c in escolhidos):
                continue
            escolhidos.append(candidato)

    return escolhidos


def resumo_de_candidatos(candidatos: list[Candidato]) -> str:
    """Texto para mostrar no terminal."""
    if not candidatos:
        return "   (nenhum candidato)"
    linhas = []
    for i, c in enumerate(candidatos, 1):
        duracao = f"{c.duracao / 60:.0f} min" if c.duracao else "duração ?"
        linhas.append(
            f"   {i}. {c.selo} [{c.pontuacao:5.2f}] {c.titulo[:58]}\n"
            f"        canal: {c.canal} ({c.status_fonte}) | {duracao}\n"
            f"        assunto: {c.assunto or '(nenhum em alta)'}\n"
            f"        motivos: {', '.join(c.motivos)}"
        )
    return "\n".join(linhas)
