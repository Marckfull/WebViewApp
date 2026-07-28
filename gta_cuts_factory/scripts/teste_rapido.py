"""
teste_rapido.py — TESTE 1 (30 segundos, sem internet, sem baixar nada).

O que ele faz:
    1. cria um vídeo de teste horizontal (1920x1080) aqui mesmo no seu PC
    2. inventa uma fala com tempos de palavra (como se o Whisper tivesse
       transcrito)
    3. gera o gancho de abertura + a legenda karaokê
    4. renderiza tudo em 1080x1920

Serve para provar que ffmpeg, fontes e legenda estão funcionando ANTES de você
gastar tempo baixando vídeo e rodando o Whisper.

COMO RODAR (no PowerShell, dentro da pasta gta_cuts_factory):

    python scripts\\teste_rapido.py

Depois abra o arquivo que ele indicar no fim.
"""

from __future__ import annotations

import sys
from pathlib import Path

# Permite rodar este arquivo direto, sem instalar o projeto como biblioteca
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import gancho, legendas, render  # noqa: E402
from nucleo.config import carregar_config  # noqa: E402
from nucleo.legendas import Palavra  # noqa: E402
from nucleo.utils import achar_ffmpeg, log, rodar, titulo  # noqa: E402


# ---------------------------------------------------------------------------
#  Fala de mentirinha (é o que o Whisper devolveria de um vídeo real)
# ---------------------------------------------------------------------------
FALA_TESTE = (
    "a Rockstar finalmente confirmou a data de lançamento do GTA 6 "
    "e um vazamento novo mostrou o mapa completo agora"
)


def criar_palavras_falsas(texto: str, inicio: float = 2.0,
                          duracao_por_palavra: float = 0.42) -> list[Palavra]:
    """Distribui as palavras no tempo, como se alguém estivesse falando."""
    palavras = []
    momento = inicio
    for texto_palavra in texto.split():
        palavras.append(
            Palavra(texto=texto_palavra, inicio=momento,
                    fim=momento + duracao_por_palavra * 0.92)
        )
        momento += duracao_por_palavra
    return palavras


def criar_video_de_teste(destino: Path, cfg, segundos: int = 12) -> Path:
    """
    Gera um vídeo horizontal colorido com áudio, usando o próprio ffmpeg.
    (É o "vídeo-fonte" falso deste teste.)
    """
    ffmpeg = achar_ffmpeg(str(cfg.pegar("ffmpeg_caminho", "ffmpeg")))
    destino.parent.mkdir(parents=True, exist_ok=True)
    rodar(
        [
            ffmpeg, "-y", "-hide_banner", "-loglevel", "error",
            "-f", "lavfi", "-i", f"testsrc2=size=1920x1080:rate=30:duration={segundos}",
            "-f", "lavfi", "-i", f"sine=frequency=180:duration={segundos}",
            "-c:v", "libx264", "-preset", "ultrafast", "-pix_fmt", "yuv420p",
            "-c:a", "aac", "-shortest", str(destino),
        ],
        descricao="criar vídeo de teste",
    )
    return destino


def main() -> int:
    titulo("TESTE 1 — LEGENDA KARAOKÊ + GANCHO + REFRAME 9:16")

    cfg = carregar_config()
    pasta = cfg.raiz / "saida" / "teste"
    pasta.mkdir(parents=True, exist_ok=True)

    # 1) vídeo-fonte falso -----------------------------------------------------
    log("Criando vídeo de teste (1920x1080)...", "etapa")
    fonte = criar_video_de_teste(pasta / "fonte_teste.mp4", cfg)

    # 2) fala falsa com tempo por palavra --------------------------------------
    palavras = criar_palavras_falsas(FALA_TESTE)
    log(f"{len(palavras)} palavras simuladas "
        f"({palavras[0].inicio:.1f}s → {palavras[-1].fim:.1f}s)", "ok")

    # 3) gancho de abertura ----------------------------------------------------
    frase = gancho.criar_frase("GTA 6", cfg, idioma="pt")
    log(f'Gancho: "{frase}"', "ok")
    eventos = gancho.eventos_gancho(frase, cfg, idioma="pt")

    # 4) arquivo de legenda ----------------------------------------------------
    arquivo_ass = legendas.salvar_ass(
        palavras, cfg, pasta / "teste_legenda.ass", idioma="pt", eventos_extra=eventos
    )
    log(f"Legenda gerada: {arquivo_ass.name}", "ok")

    # 5) render final ----------------------------------------------------------
    final = render.renderizar_corte(
        entrada=fonte,
        saida=pasta / "teste_legenda.mp4",
        cfg=cfg,
        arquivo_ass=arquivo_ass,
        inicio=0,
        duracao=12,
        foco_x=0.5,  # vídeo de teste é sintético: não faz sentido detectar rosto
    )

    titulo("DEU CERTO! 🎉")
    print(f"Abra o vídeo:\n    {final}\n")
    print("No Windows, para abrir pelo terminal:")
    print(f"    start {final.relative_to(cfg.raiz)}\n")
    print("👀 Confira nesta ordem:")
    print("   1. o vídeo está VERTICAL (1080x1920)")
    print("   2. o GANCHO aparece grande nos ~2 primeiros segundos")
    print("   3. a legenda aparece um pouco ABAIXO do centro")
    print("   4. cada palavra 'acende' e cresce na hora certa")
    print("   5. 'ROCKSTAR', 'GTA 6', 'VAZAMENTO' e 'DATA' saem em AMARELO")
    print("\nSe algo estiver diferente, ajuste em config/config.yaml "
          "(seção 'legendas') e rode de novo.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
