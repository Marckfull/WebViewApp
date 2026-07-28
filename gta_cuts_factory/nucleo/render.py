"""
render.py — junta tudo e chama o ffmpeg para produzir o MP4 final.

Ordem das operações dentro de uma única passada do ffmpeg (rápido, sem gerar
arquivos intermediários):

    1. corta o trecho escolhido (início + duração)
    2. converte para 9:16 (reframe.py monta esse filtro)
    3. queima a legenda karaokê + o gancho por cima (arquivo .ass)
    4. codifica em H.264/AAC no padrão que YouTube e TikTok gostam

"Queimar" a legenda significa desenhar o texto nos pixels do vídeo — é assim
que ela aparece igual em qualquer app, sem depender de arquivo separado.
"""

from __future__ import annotations

from pathlib import Path

from . import reframe
from .utils import (
    achar_ffmpeg,
    caminho_para_filtro,
    info_video,
    log,
    para_segundos,
    rodar,
)


def extrair_audio(
    entrada: str | Path,
    destino: str | Path,
    cfg,
    inicio: float | str = 0.0,
    duracao: float | str | None = None,
) -> Path:
    """
    Extrai o áudio em WAV 16 kHz mono — o formato que o Whisper prefere.

    Usar WAV mono 16k deixa a transcrição bem mais rápida e não perde nada de
    qualidade para reconhecimento de fala.
    """
    ffmpeg = achar_ffmpeg(str(cfg.pegar("ffmpeg_caminho", "ffmpeg")))
    destino = Path(destino)
    destino.parent.mkdir(parents=True, exist_ok=True)

    comando = [ffmpeg, "-y", "-ss", str(para_segundos(inicio)), "-i", str(entrada)]
    if duracao is not None:
        comando += ["-t", str(para_segundos(duracao))]
    comando += ["-vn", "-ac", "1", "-ar", "16000", "-c:a", "pcm_s16le", str(destino)]

    rodar(comando, descricao="extrair áudio para transcrição")
    return destino


def renderizar_corte(
    entrada: str | Path,
    saida: str | Path,
    cfg,
    arquivo_ass: str | Path | None = None,
    inicio: float | str = 0.0,
    duracao: float | str | None = None,
    foco_x: float | None = None,
) -> Path:
    """
    Produz o vídeo final 1080x1920 com legenda queimada.

    Parâmetros:
        entrada     : vídeo-fonte baixado
        saida       : caminho do .mp4 final
        arquivo_ass : legenda gerada por legendas.py (pode ser None para testar
                      só o reframe)
        inicio      : onde o corte começa ("00:01:30" ou 90)
        duracao     : quantos segundos cortar
        foco_x      : posição horizontal do interesse (0..1). Se None e o modo
                      for "auto", o próprio sistema detecta com o OpenCV.
    """
    ffmpeg = achar_ffmpeg(str(cfg.pegar("ffmpeg_caminho", "ffmpeg")))
    entrada, saida = Path(entrada), Path(saida)
    saida.parent.mkdir(parents=True, exist_ok=True)

    inicio_seg = para_segundos(inicio)
    duracao_seg = para_segundos(duracao) if duracao is not None else None

    info = info_video(
        entrada,
        str(cfg.pegar("ffprobe_caminho", "ffprobe")),
        str(cfg.pegar("ffmpeg_caminho", "ffmpeg")),
    )
    log(f"Fonte: {info.largura}x{info.altura}, {info.duracao:.1f}s, "
        f"áudio: {'sim' if info.tem_audio else 'não'}")

    # --- 1) descobrir onde está a ação (só no modo automático) --------------
    modo = str(cfg.pegar("video.modo_reframe", "auto")).lower()
    if foco_x is None and modo == "auto":
        foco_x = reframe.detectar_foco_x(
            entrada, inicio_seg, duracao_seg or min(60.0, info.duracao)
        )
    if foco_x is None:
        foco_x = 0.5

    # --- 2) montar a cadeia de filtros --------------------------------------
    filtro = reframe.construir_filtro(info, cfg, foco_x, rotulo_saida="base")
    rotulo_final = "base"

    if arquivo_ass is not None:
        opcoes_legenda = [f"filename={caminho_para_filtro(arquivo_ass)}"]
        # Se você colocar arquivos .ttf na pasta "fontes/", a legenda usa essas
        # fontes mesmo que não estejam instaladas no Windows.
        pasta_fontes = cfg.raiz / "fontes"
        if pasta_fontes.exists():
            opcoes_legenda.append(f"fontsdir={caminho_para_filtro(pasta_fontes)}")
        filtro += f";[base]subtitles={':'.join(opcoes_legenda)}[final]"
        rotulo_final = "final"

    # --- 3) montar o comando do ffmpeg --------------------------------------
    comando = [ffmpeg, "-y", "-hide_banner", "-loglevel", "error", "-stats"]
    comando += ["-ss", str(inicio_seg), "-i", str(entrada)]
    if duracao_seg is not None:
        comando += ["-t", str(duracao_seg)]

    comando += [
        "-filter_complex", filtro,
        "-map", f"[{rotulo_final}]",
    ]
    if info.tem_audio:
        comando += ["-map", "0:a:0"]

    comando += [
        "-c:v", "libx264",
        "-preset", str(cfg.pegar("video.preset", "veryfast")),
        "-crf", str(cfg.pegar("video.crf", 20)),
        "-pix_fmt", "yuv420p",             # obrigatório para tocar em celular
        "-profile:v", "high", "-level", "4.1",
        "-r", str(cfg.pegar("video.fps", 30)),
        "-g", str(int(cfg.pegar("video.fps", 30)) * 2),
    ]
    if info.tem_audio:
        comando += ["-c:a", "aac", "-b:a", str(cfg.pegar("video.bitrate_audio", "160k")),
                    "-ar", "48000", "-ac", "2"]

    comando += ["-movflags", "+faststart", str(saida)]

    log(f"Renderizando ({reframe.descrever_modo(cfg)})... isso pode demorar "
        f"alguns minutos", "etapa")
    rodar(comando, descricao="renderizar vídeo final")

    if not saida.exists() or saida.stat().st_size == 0:
        raise RuntimeError("O ffmpeg terminou mas o arquivo final não foi criado.")

    tamanho_mb = saida.stat().st_size / (1024 * 1024)
    log(f"Vídeo pronto: {saida}  ({tamanho_mb:.1f} MB)", "ok")
    return saida


def gerar_miniatura(video: str | Path, destino: str | Path, cfg,
                    segundo: float = 1.0) -> Path:
    """
    Salva um frame como imagem — usado depois pelo painel para mostrar a
    prévia de cada vídeo da fila de aprovação.
    """
    ffmpeg = achar_ffmpeg(str(cfg.pegar("ffmpeg_caminho", "ffmpeg")))
    destino = Path(destino)
    destino.parent.mkdir(parents=True, exist_ok=True)
    rodar(
        [ffmpeg, "-y", "-loglevel", "error", "-ss", str(segundo), "-i", str(video),
         "-frames:v", "1", "-q:v", "3", str(destino)],
        descricao="gerar miniatura",
    )
    return destino
