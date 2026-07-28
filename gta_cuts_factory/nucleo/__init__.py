"""
GTA Cuts Factory — pacote "nucleo".

Aqui ficam os módulos que fazem o trabalho pesado. Cada arquivo tem UMA
responsabilidade, para você conseguir ler e entender um de cada vez:

    config.py      → lê as configurações do config.yaml
    utils.py       → funções de apoio (ffmpeg, tempo, pastas, log)
    downloader.py  → baixa o vídeo-fonte com yt-dlp
    transcricao.py → transcreve o áudio com timestamp por palavra
    reframe.py     → transforma vídeo horizontal em 9:16
    legendas.py    → gera a legenda karaokê (.ass) com destaque de palavras
    gancho.py      → cria a frase de impacto dos 2 primeiros segundos
    render.py      → junta tudo e chama o ffmpeg
"""

__version__ = "0.1.0"  # Etapa 1 — núcleo
