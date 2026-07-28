"""
downloader.py — baixa o vídeo-fonte com o yt-dlp.

Detalhes que importam:
    • baixamos no máximo 1080p: acima disso é desperdício, já que o vídeo final
      é 1080x1920 e vem de um RECORTE do meio da imagem;
    • guardamos o JSON com as informações do vídeo (título, canal, link, data)
      porque o crédito da fonte é obrigatório na descrição — e o link vira
      prova de origem se você precisar contestar um claim;
    • o arquivo é reaproveitado se já existir: não baixa a mesma coisa duas vezes.
"""

from __future__ import annotations

import json
from dataclasses import dataclass, asdict
from pathlib import Path

from .utils import log, nome_seguro


@dataclass
class VideoFonte:
    """Tudo que precisamos saber sobre o vídeo que baixamos."""

    id: str
    titulo: str
    canal: str
    canal_id: str
    canal_url: str
    url: str
    duracao: float
    data_envio: str
    arquivo: str

    @property
    def credito(self) -> str:
        """Linha de crédito que vai na descrição do vídeo publicado."""
        return f"Fonte: {self.titulo} — {self.canal}\n{self.url}"

    def para_dict(self) -> dict:
        return asdict(self)


def _opcoes_ytdlp(pasta: Path, cfg) -> dict:
    """Configurações do yt-dlp (formato, nome do arquivo, etc.)."""
    return {
        # melhor vídeo até 1080p + melhor áudio, juntando em MP4
        "format": "bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a]/"
                  "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best",
        "merge_output_format": "mp4",
        "outtmpl": str(pasta / "%(id)s.%(ext)s"),
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
        "retries": 5,
        "fragment_retries": 5,
        "concurrent_fragment_downloads": 4,
        "ffmpeg_location": str(cfg.pegar("ffmpeg_caminho", "ffmpeg")),
        "writeinfojson": False,   # gravamos o nosso próprio JSON, mais enxuto
    }


def baixar(url: str, cfg, pasta: Path | None = None) -> VideoFonte:
    """
    Baixa um vídeo do YouTube (ou de outro site suportado pelo yt-dlp).

    Devolve um VideoFonte com o caminho do arquivo e os dados de crédito.
    """
    try:
        from yt_dlp import YoutubeDL  # type: ignore
    except ImportError as erro:
        raise RuntimeError(
            "O yt-dlp não está instalado. Rode:  pip install -r requirements.txt"
        ) from erro

    pasta = Path(pasta) if pasta else cfg.caminho("downloads")
    pasta.mkdir(parents=True, exist_ok=True)

    log(f"Baixando: {url}", "etapa")
    with YoutubeDL(_opcoes_ytdlp(pasta, cfg)) as ydl:
        info = ydl.extract_info(url, download=True)
        caminho = Path(ydl.prepare_filename(info))
        # o yt-dlp pode ter juntado os arquivos em .mp4/.mkv com outro nome
        if not caminho.exists():
            candidatos = sorted(pasta.glob(f"{info['id']}.*"))
            candidatos = [c for c in candidatos if c.suffix != ".json"]
            if not candidatos:
                raise RuntimeError(f"O download terminou mas não achei o arquivo em {pasta}")
            caminho = candidatos[0]

    fonte = VideoFonte(
        id=str(info.get("id", "")),
        titulo=str(info.get("title", "")),
        canal=str(info.get("uploader") or info.get("channel") or ""),
        canal_id=str(info.get("channel_id") or info.get("uploader_id") or ""),
        canal_url=str(info.get("channel_url") or info.get("uploader_url") or ""),
        url=str(info.get("webpage_url") or url),
        duracao=float(info.get("duration") or 0.0),
        data_envio=str(info.get("upload_date") or ""),
        arquivo=str(caminho),
    )

    # guarda os metadados ao lado do vídeo (crédito + rastreabilidade)
    (pasta / f"{fonte.id}.info.json").write_text(
        json.dumps(fonte.para_dict(), ensure_ascii=False, indent=2), encoding="utf-8"
    )

    log(f"Baixado: {fonte.titulo} ({fonte.canal}) → {caminho.name}", "ok")
    return fonte


def consultar_sem_baixar(url: str) -> dict:
    """
    Lê as informações do vídeo SEM baixar (usado depois pelas etapas de
    tendências e de seleção de fontes, para filtrar candidatos rapidinho).
    """
    from yt_dlp import YoutubeDL  # type: ignore

    with YoutubeDL({"quiet": True, "no_warnings": True, "skip_download": True}) as ydl:
        return ydl.extract_info(url, download=False)


def usar_arquivo_local(caminho: str | Path) -> VideoFonte:
    """
    Cria um VideoFonte a partir de um arquivo que já está no seu computador
    (suas próprias gravações — a fonte mais segura de todas, risco zero).
    """
    caminho = Path(caminho)
    if not caminho.exists():
        raise FileNotFoundError(f"Arquivo não encontrado: {caminho}")
    return VideoFonte(
        id=nome_seguro(caminho.stem),
        titulo=caminho.stem,
        canal="Gravação própria",
        canal_id="proprio",
        canal_url="",
        url="",
        duracao=0.0,
        data_envio="",
        arquivo=str(caminho),
    )
