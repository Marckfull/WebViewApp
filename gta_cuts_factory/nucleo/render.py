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


def renderizar_corte_com_broll(
    entrada: str | Path,
    video_broll: str | Path,
    saida: str | Path,
    cfg,
    arquivo_ass: str | Path | None = None,
    inicio: float | str = 0.0,
    duracao: float | str | None = None,
    inicio_broll: float = 0.0,
    layout: str = "",
) -> Path:
    """
    MODO A com gameplay junto: corte original + b-roll na mesma tela.

    O áudio é sempre o do CORTE ORIGINAL — é a fala e a reação que seguram o
    espectador; o gameplay entra mudo, só para dar movimento à tela.

    Se o gameplay for mais curto que o corte, ele roda em LOOP.
    """
    from . import broll as modulo_broll

    ffmpeg = achar_ffmpeg(str(cfg.pegar("ffmpeg_caminho", "ffmpeg")))
    entrada, video_broll, saida = Path(entrada), Path(video_broll), Path(saida)
    saida.parent.mkdir(parents=True, exist_ok=True)

    inicio_seg = para_segundos(inicio)
    duracao_seg = para_segundos(duracao) if duracao is not None else None

    ffprobe = str(cfg.pegar("ffprobe_caminho", "ffprobe"))
    caminho_ffmpeg = str(cfg.pegar("ffmpeg_caminho", "ffmpeg"))
    info_principal = info_video(entrada, ffprobe, caminho_ffmpeg)
    info_broll = info_video(video_broll, ffprobe, caminho_ffmpeg)

    log(f"Corte: {info_principal.largura}x{info_principal.altura} | "
        f"gameplay: {info_broll.largura}x{info_broll.altura} "
        f"({modulo_broll.descrever_layout(cfg, layout)})", "info")

    filtro = modulo_broll.construir_filtro(info_principal, info_broll, cfg,
                                           "base", layout)
    rotulo_final = "base"
    if arquivo_ass is not None:
        opcoes = [f"filename={caminho_para_filtro(arquivo_ass)}"]
        pasta_fontes = cfg.raiz / "fontes"
        if pasta_fontes.exists():
            opcoes.append(f"fontsdir={caminho_para_filtro(pasta_fontes)}")
        filtro += f";[base]subtitles={':'.join(opcoes)}[final]"
        rotulo_final = "final"

    comando = [ffmpeg, "-y", "-hide_banner", "-loglevel", "error", "-stats"]
    comando += ["-ss", str(inicio_seg), "-i", str(entrada)]
    # o gameplay repete para sempre; o -t corta tudo no tamanho do corte
    comando += ["-stream_loop", "-1", "-ss", str(float(inicio_broll)),
                "-i", str(video_broll)]
    if duracao_seg is not None:
        comando += ["-t", str(duracao_seg)]

    comando += ["-filter_complex", filtro, "-map", f"[{rotulo_final}]"]
    if info_principal.tem_audio:
        comando += ["-map", "0:a:0"]

    comando += [
        "-c:v", "libx264",
        "-preset", str(cfg.pegar("video.preset", "veryfast")),
        "-crf", str(cfg.pegar("video.crf", 20)),
        "-pix_fmt", "yuv420p", "-profile:v", "high", "-level", "4.1",
        "-r", str(cfg.pegar("video.fps", 30)),
        "-g", str(int(cfg.pegar("video.fps", 30)) * 2),
    ]
    if info_principal.tem_audio:
        comando += ["-c:a", "aac", "-b:a", str(cfg.pegar("video.bitrate_audio", "160k")),
                    "-ar", "48000", "-ac", "2"]
    comando += ["-movflags", "+faststart", str(saida)]

    log("Renderizando com gameplay junto...", "etapa")
    rodar(comando, descricao="renderizar corte com b-roll")

    if not saida.exists() or saida.stat().st_size == 0:
        raise RuntimeError("O ffmpeg terminou mas o arquivo final não foi criado.")

    log(f"Vídeo pronto: {saida} ({saida.stat().st_size / (1024 * 1024):.1f} MB)", "ok")
    return saida


def duracao_do_audio(caminho: str | Path, cfg) -> float:
    """Descobre quantos segundos tem um arquivo de áudio."""
    informacoes = info_video(
        caminho,
        str(cfg.pegar("ffprobe_caminho", "ffprobe")),
        str(cfg.pegar("ffmpeg_caminho", "ffmpeg")),
    )
    return float(informacoes.duracao)


def renderizar_narrado(
    fundo: str | Path,
    audio_narracao: str | Path,
    saida: str | Path,
    cfg,
    arquivo_ass: str | Path | None = None,
    duracao: float | None = None,
    inicio_fundo: float = 0.0,
    foco_x: float | None = None,
) -> Path:
    """
    MODO B: monta o vídeo narrado (voz sua por cima de gameplay/trailer).

    Diferenças para o Modo A:
        • o áudio principal é a SUA narração, não o áudio do vídeo original
          (é isso que praticamente zera o risco de Content ID);
        • o áudio do fundo entra baixinho, só de ambiente (dá para desligar);
        • se o vídeo de fundo for mais curto que a narração, ele roda em LOOP
          até a narração acabar — nada de tela preta no fim.
    """
    ffmpeg = achar_ffmpeg(str(cfg.pegar("ffmpeg_caminho", "ffmpeg")))
    fundo, audio_narracao, saida = Path(fundo), Path(audio_narracao), Path(saida)
    saida.parent.mkdir(parents=True, exist_ok=True)

    if duracao is None:
        duracao = duracao_do_audio(audio_narracao, cfg)
    duracao = float(duracao) + 0.35   # um respiro no fim, para não cortar a fala

    info = info_video(
        fundo,
        str(cfg.pegar("ffprobe_caminho", "ffprobe")),
        str(cfg.pegar("ffmpeg_caminho", "ffmpeg")),
    )
    log(f"Fundo: {info.largura}x{info.altura}, {info.duracao:.1f}s | "
        f"narração: {duracao:.1f}s", "info")

    # --- enquadramento 9:16 do fundo ----------------------------------------
    modo = str(cfg.pegar("video.modo_reframe", "auto")).lower()
    if foco_x is None and modo == "auto":
        foco_x = reframe.detectar_foco_x(
            fundo, inicio_fundo, min(duracao, max(1.0, info.duracao)))
    filtro = reframe.construir_filtro(info, cfg, foco_x if foco_x is not None else 0.5,
                                      rotulo_saida="base")
    rotulo_final = "base"

    if arquivo_ass is not None:
        opcoes = [f"filename={caminho_para_filtro(arquivo_ass)}"]
        pasta_fontes = cfg.raiz / "fontes"
        if pasta_fontes.exists():
            opcoes.append(f"fontsdir={caminho_para_filtro(pasta_fontes)}")
        filtro += f";[base]subtitles={':'.join(opcoes)}[final]"
        rotulo_final = "final"

    # --- áudio: narração (+ fundo baixinho, se você quiser) -----------------
    volume_fundo = float(cfg.pegar("narracao.volume_fundo", 0.12))
    usar_audio_do_fundo = (bool(cfg.pegar("narracao.manter_audio_do_fundo", True))
                           and info.tem_audio and volume_fundo > 0)
    if usar_audio_do_fundo:
        filtro += (f";[0:a]volume={volume_fundo}[fundo_baixo];"
                   f"[1:a]volume=1.0[voz];"
                   f"[fundo_baixo][voz]amix=inputs=2:duration=longest:"
                   f"dropout_transition=0[audio]")
        mapa_audio = ["-map", "[audio]"]
    else:
        mapa_audio = ["-map", "1:a:0"]

    comando = [ffmpeg, "-y", "-hide_banner", "-loglevel", "error", "-stats"]
    # -stream_loop -1 faz o fundo repetir para sempre; o -t corta na narração
    comando += ["-stream_loop", "-1", "-ss", str(float(inicio_fundo)), "-i", str(fundo)]
    comando += ["-i", str(audio_narracao)]
    comando += ["-t", f"{duracao:.3f}"]
    comando += ["-filter_complex", filtro, "-map", f"[{rotulo_final}]"] + mapa_audio
    comando += [
        "-c:v", "libx264",
        "-preset", str(cfg.pegar("video.preset", "veryfast")),
        "-crf", str(cfg.pegar("video.crf", 20)),
        "-pix_fmt", "yuv420p", "-profile:v", "high", "-level", "4.1",
        "-r", str(cfg.pegar("video.fps", 30)),
        "-g", str(int(cfg.pegar("video.fps", 30)) * 2),
        "-c:a", "aac", "-b:a", str(cfg.pegar("video.bitrate_audio", "160k")),
        "-ar", "48000", "-ac", "2",
        "-movflags", "+faststart", str(saida),
    ]

    log("Renderizando vídeo narrado (Modo B)...", "etapa")
    rodar(comando, descricao="renderizar vídeo narrado")

    if not saida.exists() or saida.stat().st_size == 0:
        raise RuntimeError("O ffmpeg terminou mas o arquivo final não foi criado.")

    log(f"Vídeo narrado pronto: {saida} "
        f"({saida.stat().st_size / (1024 * 1024):.1f} MB)", "ok")
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
