"""
tendencias.py — descobre o que está EM ALTA sobre GTA 6 e GTA 5.

De onde vêm os sinais (tudo grátis, sem chave de API):

    1. ASSUNTOS BASE   → a sua lista fixa em config/canais_fontes.yaml
                         (é a rede de segurança: funciona até sem internet)
    2. YOUTUBE         → títulos dos vídeos recentes e mais vistos sobre GTA,
                         lidos pelo yt-dlp (o mesmo programa que baixa vídeo)
    3. REDDIT          → posts quentes de r/GTA6 e r/GTA, pelo JSON público

Como o sistema decide o que está em alta:
    ele junta os títulos/posts, quebra em expressões de 2 e 3 palavras
    ("data de lançamento", "release date", "mapa vazado"), joga fora palavras
    sem valor ("o", "de", "the") e conta quem mais aparece. Expressão repetida
    em muitos títulos recentes = assunto quente.

Nada aqui é obrigatório: se a internet cair ou o YouTube mudar algo, o sistema
volta para a sua lista base e segue funcionando.
"""

from __future__ import annotations

import json
import re
import unicodedata
import urllib.request
from collections import Counter
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from typing import Iterable

from .utils import LoggerSilencioso, log


# Palavras que não carregam assunto (não podem virar "tendência" sozinhas)
PALAVRAS_VAZIAS = {
    # português
    "a", "o", "os", "as", "um", "uma", "de", "do", "da", "dos", "das", "em",
    "no", "na", "nos", "nas", "e", "ou", "que", "para", "por", "com", "sem",
    "se", "ao", "aos", "é", "foi", "vai", "tem", "ter", "isso", "esse", "essa",
    "este", "esta", "mais", "muito", "todos", "todo", "toda", "sobre", "como",
    "quando", "onde", "qual", "quais", "eu", "voce", "você", "meu", "minha",
    # inglês
    "the", "of", "in", "on", "and", "or", "to", "for", "with", "is", "are",
    "was", "will", "this", "that", "these", "those", "you", "your", "we",
    "it", "its", "at", "by", "from", "about", "what", "when", "where", "how",
    "all", "just", "new",
    # ruído de título de vídeo
    "video", "vídeo", "shorts", "short", "gameplay", "oficial", "official",
    "hd", "4k", "pt", "br", "ep", "parte", "part",
}

# Uma expressão só entra se falar de GTA de alguma forma
ANCORAS = {"gta", "gta6", "gta5", "rockstar", "vice", "leonida", "lucia", "jason"}


@dataclass
class Assunto:
    """Um tema em alta, com de onde veio e o quanto está quente."""

    termo: str
    pontuacao: float = 0.0
    sinais: list[str] = field(default_factory=list)   # "base", "youtube", "reddit"
    exemplos: list[str] = field(default_factory=list)  # títulos que originaram
    idioma: str = "pt"

    def para_dict(self) -> dict:
        return {"termo": self.termo, "pontuacao": round(self.pontuacao, 2),
                "sinais": self.sinais, "exemplos": self.exemplos[:3],
                "idioma": self.idioma}


# ============================================================================
#  TEXTO
# ============================================================================

def _limpar(texto: str) -> str:
    """Minúsculas, sem acento e sem pontuação — para comparar sem escorregar."""
    sem_acento = unicodedata.normalize("NFKD", str(texto))
    sem_acento = "".join(c for c in sem_acento if not unicodedata.combining(c))
    return re.sub(r"[^\w\s]", " ", sem_acento.lower())


def extrair_expressoes(titulos: Iterable[str], tamanhos: tuple[int, ...] = (2, 3)) -> Counter:
    """
    Quebra os títulos em expressões de 2 e 3 palavras e conta quantas vezes
    cada uma aparece.

        ["GTA 6 data de lançamento confirmada", "A data de lançamento do GTA 6"]
        → {"gta 6": 2, "data lancamento": 2, ...}

    Só ficam as expressões que mencionam GTA/Rockstar ou que aparecem junto
    delas — o resto é ruído de título de YouTube.
    """
    contagem: Counter = Counter()

    for titulo in titulos:
        palavras = [p for p in _limpar(titulo).split() if p]
        # guarda quais palavras "âncora" existem neste título
        tem_ancora = any(p in ANCORAS for p in palavras)
        uteis = [p for p in palavras if p not in PALAVRAS_VAZIAS and len(p) > 1]

        for tamanho in tamanhos:
            for i in range(len(uteis) - tamanho + 1):
                expressao = " ".join(uteis[i:i + tamanho])
                # a expressão vale se ela própria cita GTA, ou se o título cita
                if any(p in ANCORAS for p in uteis[i:i + tamanho]) or tem_ancora:
                    contagem[expressao] += 1
    return contagem


# ============================================================================
#  SINAL 2 — YOUTUBE (via yt-dlp, sem chave de API)
# ============================================================================

def buscar_titulos_youtube(termo: str, limite: int = 15) -> list[dict]:
    """
    Procura no YouTube e devolve os vídeos recentes encontrados.

    Usa o "ytsearch" do yt-dlp em modo raso (só a lista, sem baixar nada),
    então é rápido. Se der qualquer erro (sem internet, YouTube mudou algo),
    devolve lista vazia e o sistema segue com os outros sinais.
    """
    try:
        from yt_dlp import YoutubeDL  # type: ignore
    except ImportError:
        return []

    opcoes = {
        "quiet": True, "no_warnings": True, "skip_download": True,
        "extract_flat": True, "playlist_items": f"1-{limite}",
        "logger": LoggerSilencioso(),   # erro de rede não vira susto na tela
    }
    try:
        with YoutubeDL(opcoes) as ydl:
            dados = ydl.extract_info(f"ytsearch{limite}:{termo}", download=False)
        return [
            {"titulo": e.get("title", ""), "url": e.get("url", ""),
             "id": e.get("id", ""), "canal": e.get("uploader") or e.get("channel") or "",
             "views": int(e.get("view_count") or 0),
             "duracao": float(e.get("duration") or 0)}
            for e in (dados.get("entries") or []) if e
        ]
    except Exception as erro:
        log(f"Busca no YouTube falhou para '{termo}' ({type(erro).__name__}). "
            f"Seguindo com os outros sinais.", "aviso")
        return []


# ============================================================================
#  SINAL 3 — REDDIT (JSON público, sem chave de API)
# ============================================================================

def buscar_titulos_reddit(subreddit: str = "GTA6", limite: int = 25,
                          tempo_limite: int = 12) -> list[dict]:
    """
    Lê os posts "quentes" de um subreddit pelo JSON público.

    O Reddit costuma reagir antes do YouTube: vazamento e notícia aparecem lá
    primeiro. Se falhar, devolve lista vazia (é só mais um sinal, não é crítico).
    """
    url = f"https://www.reddit.com/r/{subreddit}/hot.json?limit={limite}"
    pedido = urllib.request.Request(
        url, headers={"User-Agent": "GTACutsFactory/0.1 (uso pessoal)"}
    )
    try:
        with urllib.request.urlopen(pedido, timeout=tempo_limite) as resposta:
            dados = json.loads(resposta.read().decode("utf-8"))
        return [
            {"titulo": filho["data"].get("title", ""),
             "pontos": int(filho["data"].get("score") or 0),
             "url": "https://reddit.com" + filho["data"].get("permalink", "")}
            for filho in dados.get("data", {}).get("children", [])
        ]
    except Exception as erro:
        log(f"Reddit indisponível ({type(erro).__name__}). "
            f"Seguindo com os outros sinais.", "aviso")
        return []


# ============================================================================
#  JUNTANDO TUDO
# ============================================================================

def descobrir_assuntos(
    cfg,
    canais: dict,
    idioma: str = "pt",
    limite: int = 8,
    usar_rede: bool = True,
    conexao=None,
) -> list[Assunto]:
    """
    Devolve os assuntos em alta, do mais quente para o menos quente.

    conexao (opcional): se você passar o banco, os assuntos já publicados nos
    últimos dias são descartados aqui mesmo — assim a lista que chega na
    seleção já vem limpa.
    """
    base = list((canais.get("assuntos_base", {}) or {}).get(idioma, []) or [])
    proibidos = [_limpar(p) for p in (canais.get("assuntos_proibidos", []) or [])]

    encontrados: dict[str, Assunto] = {}

    # ---------------------------------------------------- 1) seus assuntos base
    for posicao, termo in enumerate(base):
        chave = _limpar(termo).strip()
        # os primeiros da sua lista valem um pouco mais
        encontrados[chave] = Assunto(
            termo=termo, pontuacao=3.0 - posicao * 0.1, sinais=["base"], idioma=idioma
        )

    if not usar_rede:
        log("Modo offline: usando só a sua lista de assuntos base.", "info")
        return _finalizar(encontrados, proibidos, limite, conexao, idioma)

    # -------------------------------------------------------------- 2) YouTube
    titulos_youtube: list[str] = []
    consultas = base[:3] or (["GTA 6 news"] if idioma == "en" else ["GTA 6 notícias"])
    for consulta in consultas:
        for video in buscar_titulos_youtube(consulta, limite=15):
            titulos_youtube.append(video["titulo"])

    if titulos_youtube:
        log(f"{len(titulos_youtube)} títulos lidos do YouTube.", "ok")
        for expressao, vezes in extrair_expressoes(titulos_youtube).items():
            if vezes < 2:      # aparecer uma vez só não é tendência
                continue
            exemplos = [t for t in titulos_youtube if expressao.split()[0] in _limpar(t)]
            _somar(encontrados, expressao, vezes * 0.8, "youtube", exemplos, idioma)

    # --------------------------------------------------------------- 3) Reddit
    subreddit = "GTA6"
    posts = buscar_titulos_reddit(subreddit)
    if posts:
        log(f"{len(posts)} posts lidos do r/{subreddit}.", "ok")
        titulos_reddit = [p["titulo"] for p in posts]
        for expressao, vezes in extrair_expressoes(titulos_reddit).items():
            if vezes < 2:
                continue
            exemplos = [t for t in titulos_reddit if expressao.split()[0] in _limpar(t)]
            _somar(encontrados, expressao, vezes * 0.6, "reddit", exemplos, idioma)

    return _finalizar(encontrados, proibidos, limite, conexao, idioma)


def _somar(encontrados: dict, termo: str, pontos: float, sinal: str,
           exemplos: list[str], idioma: str) -> None:
    """Soma pontos a um assunto (criando se for a primeira vez que aparece)."""
    chave = termo.strip()
    if chave in encontrados:
        assunto = encontrados[chave]
        assunto.pontuacao += pontos
        if sinal not in assunto.sinais:
            assunto.sinais.append(sinal)
        assunto.exemplos.extend(e for e in exemplos[:2] if e not in assunto.exemplos)
    else:
        encontrados[chave] = Assunto(termo=chave, pontuacao=pontos, sinais=[sinal],
                                     exemplos=exemplos[:2], idioma=idioma)


def _finalizar(encontrados: dict, proibidos: list[str], limite: int,
               conexao, idioma: str) -> list[Assunto]:
    """Tira os proibidos e os já publicados, ordena e corta no limite."""
    lista = []
    for assunto in encontrados.values():
        alvo = _limpar(assunto.termo)

        if any(p and p in alvo for p in proibidos):
            continue

        if conexao is not None:
            from . import banco  # importado aqui para evitar dependência circular

            usado, _ = banco.assunto_ja_usado(conexao, assunto.termo, idioma)
            if usado:
                continue

        lista.append(assunto)

    lista.sort(key=lambda a: a.pontuacao, reverse=True)
    return lista[:limite]


def resumo_de_assuntos(assuntos: list[Assunto]) -> str:
    """Texto para mostrar no terminal."""
    if not assuntos:
        return "(nenhum assunto disponível)"
    linhas = []
    for i, assunto in enumerate(assuntos, 1):
        sinais = "+".join(assunto.sinais)
        linhas.append(f"   {i}. {assunto.termo}  "
                      f"(pontos: {assunto.pontuacao:.1f} | sinais: {sinais})")
    return "\n".join(linhas)


def data_recente(data_envio: str, dias: int = 30) -> bool:
    """
    Diz se uma data no formato do yt-dlp ("20260115") está dentro do prazo.
    Data desconhecida conta como recente (não dá para punir o que não sabemos).
    """
    texto = str(data_envio or "").strip()
    if len(texto) != 8 or not texto.isdigit():
        return True
    try:
        data = datetime.strptime(texto, "%Y%m%d")
    except ValueError:
        return True
    return data >= datetime.now() - timedelta(days=dias)
