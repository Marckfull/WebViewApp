"""
utils.py — funções de apoio usadas por todos os outros módulos.

Nada aqui é "o produto"; são as ferramentas de bancada:
    - achar e rodar o ffmpeg/ffprobe
    - descobrir duração e resolução de um vídeo
    - converter tempo ("00:01:30" <-> 90.0 segundos)
    - converter cor do formato do Paint (#FFE100) para o formato do ASS
    - escrever mensagens bonitas no terminal
"""

from __future__ import annotations

import json
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence


# ============================================================================
#  MENSAGENS NO TERMINAL
# ============================================================================

def log(mensagem: str, nivel: str = "info") -> None:
    """
    Escreve uma mensagem no terminal com um ícone que indica o que é.

        log("Baixando vídeo...")            -> ℹ️  Baixando vídeo...
        log("Deu certo!", "ok")             -> ✅ Deu certo!
        log("Cuidado", "aviso")             -> ⚠️  Cuidado
        log("Falhou", "erro")               -> ❌ Falhou
    """
    icones = {"info": "ℹ️ ", "ok": "✅", "aviso": "⚠️ ", "erro": "❌", "etapa": "▶️ "}
    print(f"{icones.get(nivel, 'ℹ️ ')} {mensagem}", flush=True)


def titulo(texto: str) -> None:
    """Escreve um cabeçalho separando as etapas (só para ficar legível)."""
    print("\n" + "=" * 68)
    print(f"  {texto}")
    print("=" * 68, flush=True)


# ============================================================================
#  FFMPEG
# ============================================================================

class LoggerSilencioso:
    """
    "Tapa-boca" para o yt-dlp durante as BUSCAS (tendências e listagem de canal).

    Sem isso, quando a internet cai ou o YouTube muda algo, o yt-dlp imprime um
    erro enorme pedindo para você abrir um chamado no GitHub dele — o que
    assusta à toa, já que o nosso código trata a falha e segue com os outros
    sinais. Nos DOWNLOADS de verdade os erros continuam aparecendo.
    """

    def debug(self, mensagem: str) -> None:  # noqa: D102
        pass

    def info(self, mensagem: str) -> None:  # noqa: D102
        pass

    def warning(self, mensagem: str) -> None:  # noqa: D102
        pass

    def error(self, mensagem: str) -> None:  # noqa: D102
        pass


class FFmpegNaoEncontrado(RuntimeError):
    """Erro amigável quando o ffmpeg não está instalado."""


def achar_ffmpeg(caminho_config: str = "ffmpeg") -> str:
    """
    Descobre onde está o ffmpeg (ou o ffprobe).

    Ordem de busca:
        1. o caminho escrito no config.yaml
        2. o PATH do sistema (instalação via winget)
        3. caminhos comuns do Windows (C:/ffmpeg/bin/...)
        4. o ffmpeg embutido no pacote imageio-ffmpeg, se estiver instalado
    """
    # 1 e 2 — caminho direto ou no PATH
    if Path(caminho_config).exists():
        return str(Path(caminho_config))
    encontrado = shutil.which(caminho_config)
    if encontrado:
        return encontrado

    nome = Path(caminho_config).stem or "ffmpeg"

    # 3 — locais típicos de instalação manual no Windows
    candidatos = [
        Path(f"C:/ffmpeg/bin/{nome}.exe"),
        Path(f"C:/Program Files/ffmpeg/bin/{nome}.exe"),
        Path.home() / f"ffmpeg/bin/{nome}.exe",
    ]
    for candidato in candidatos:
        if candidato.exists():
            return str(candidato)

    # 4 — plano B: ffmpeg que vem dentro do pacote imageio-ffmpeg
    if nome == "ffmpeg":
        try:
            import imageio_ffmpeg  # type: ignore

            return imageio_ffmpeg.get_ffmpeg_exe()
        except Exception:
            pass

    raise FFmpegNaoEncontrado(
        f"Não encontrei o '{nome}'.\n"
        "   → No Windows, rode:  winget install --id Gyan.FFmpeg -e\n"
        "   → Ou escreva o caminho completo em config/config.yaml "
        "(campos ffmpeg_caminho / ffprobe_caminho).\n"
        "   → Veja o Passo 2 do arquivo INSTALACAO_WINDOWS.md"
    )


def rodar(comando: Sequence[str], descricao: str = "", silencioso: bool = True) -> str:
    """
    Executa um programa externo (ffmpeg, ffprobe...) e devolve a saída de texto.

    Se der erro, mostra as últimas linhas do ffmpeg — que é onde fica a
    explicação do problema — em vez de um erro gigante e ilegível.
    """
    processo = subprocess.run(
        list(comando),
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if processo.returncode != 0:
        cauda = "\n".join((processo.stderr or "").strip().splitlines()[-15:])
        raise RuntimeError(
            f"Falhou: {descricao or comando[0]}\n"
            f"--- últimas linhas do erro ---\n{cauda}"
        )
    if not silencioso and processo.stderr:
        print(processo.stderr)
    return processo.stdout


@dataclass
class InfoVideo:
    """Informações básicas de um arquivo de vídeo."""

    largura: int
    altura: int
    duracao: float
    fps: float
    tem_audio: bool

    @property
    def horizontal(self) -> bool:
        return self.largura >= self.altura


def info_video(
    caminho: str | Path, ffprobe: str = "ffprobe", ffmpeg: str = "ffmpeg"
) -> InfoVideo:
    """
    Lê largura, altura, duração e fps de um vídeo.

    Usa o ffprobe (que vem junto com o ffmpeg). Se por algum motivo só o
    ffmpeg estiver disponível, cai para um plano B que lê as mesmas
    informações da saída do próprio ffmpeg.
    """
    try:
        binario = achar_ffmpeg(ffprobe)
    except FFmpegNaoEncontrado:
        return _info_via_ffmpeg(caminho, ffmpeg)
    saida = rodar(
        [
            binario, "-v", "error",
            "-show_entries", "stream=width,height,avg_frame_rate,codec_type",
            "-show_entries", "format=duration",
            "-of", "json", str(caminho),
        ],
        descricao="ffprobe (ler informações do vídeo)",
    )
    dados = json.loads(saida)

    largura = altura = 0
    fps = 30.0
    tem_audio = False
    for fluxo in dados.get("streams", []):
        if fluxo.get("codec_type") == "video" and not largura:
            largura = int(fluxo.get("width") or 0)
            altura = int(fluxo.get("height") or 0)
            bruto = fluxo.get("avg_frame_rate") or "30/1"
            if "/" in bruto:
                num, den = bruto.split("/")
                fps = float(num) / float(den) if float(den) else 30.0
        elif fluxo.get("codec_type") == "audio":
            tem_audio = True

    duracao = float(dados.get("format", {}).get("duration") or 0.0)
    return InfoVideo(largura, altura, duracao, fps, tem_audio)


def _info_via_ffmpeg(caminho: str | Path, ffmpeg: str = "ffmpeg") -> InfoVideo:
    """
    Plano B do info_video: descobre resolução/duração/fps lendo o relatório que
    o ffmpeg imprime quando abre um arquivo.
    """
    binario = achar_ffmpeg(ffmpeg)
    processo = subprocess.run(
        [binario, "-hide_banner", "-i", str(caminho)],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    texto = processo.stderr or ""

    # Arquivo só de áudio (a narração do Modo B, por exemplo) não tem
    # dimensões — e isso é normal, não é erro.
    dimensoes = re.search(r"Video:.*?(\d{2,5})x(\d{2,5})", texto)
    largura, altura = (int(dimensoes.group(1)), int(dimensoes.group(2))) if dimensoes else (0, 0)

    fps = 30.0
    achou_fps = re.search(r"(\d+(?:\.\d+)?)\s+fps", texto)
    if achou_fps:
        fps = float(achou_fps.group(1))

    duracao = 0.0
    achou_duracao = re.search(r"Duration:\s*(\d+):(\d+):(\d+(?:\.\d+)?)", texto)
    if achou_duracao:
        h, m, s = achou_duracao.groups()
        duracao = int(h) * 3600 + int(m) * 60 + float(s)

    tem_audio = "Audio:" in texto
    if not dimensoes and not tem_audio:
        raise RuntimeError(
            f"Não consegui ler o arquivo (não achei vídeo nem áudio nele): {caminho}"
        )

    return InfoVideo(largura, altura, duracao, fps, tem_audio)


def caminho_para_filtro(caminho: str | Path) -> str:
    """
    Prepara um caminho de arquivo para ser usado DENTRO de um filtro do ffmpeg
    (por exemplo o filtro subtitles=...).

    Isso é necessário porque no Windows o caminho "C:\\videos\\a.ass" quebra o
    ffmpeg: a barra invertida e os dois-pontos têm significado especial.
    Resultado: 'C\\:/videos/a.ass'
    """
    texto = str(Path(caminho).resolve()).replace("\\", "/")
    texto = texto.replace(":", r"\:").replace("'", r"\'")
    return f"'{texto}'"


# ============================================================================
#  TEMPO
# ============================================================================

def para_segundos(valor: str | float | int) -> float:
    """
    Aceita vários formatos de tempo e devolve segundos (float).

        "90"         -> 90.0
        "01:30"      -> 90.0
        "00:01:30"   -> 90.0
        "00:01:30.5" -> 90.5
    """
    if isinstance(valor, (int, float)):
        return float(valor)

    texto = str(valor).strip()
    if re.fullmatch(r"\d+(\.\d+)?", texto):
        return float(texto)

    partes = texto.split(":")
    if len(partes) > 3:
        raise ValueError(f"Formato de tempo inválido: {valor}")
    total = 0.0
    for parte in partes:
        total = total * 60 + float(parte or 0)
    return total


def para_tempo_ass(segundos: float) -> str:
    """
    Converte segundos para o formato de tempo do arquivo de legenda ASS:
    H:MM:SS.CC (centésimos de segundo).

        90.5  ->  "0:01:30.50"
    """
    segundos = max(0.0, float(segundos))
    horas = int(segundos // 3600)
    minutos = int((segundos % 3600) // 60)
    resto = segundos % 60
    centesimos = int(round((resto - int(resto)) * 100))
    inteiro = int(resto)
    if centesimos == 100:  # arredondamento para cima
        centesimos = 0
        inteiro += 1
    return f"{horas}:{minutos:02d}:{inteiro:02d}.{centesimos:02d}"


# ============================================================================
#  CORES
# ============================================================================

def cor_para_ass(cor_hex: str, transparencia: int = 0) -> str:
    """
    Converte cor do formato comum (#RRGGBB) para o formato do ASS (&HAABBGGRR).

    O ASS inverte a ordem das cores (azul-verde-vermelho) e coloca a
    transparência na frente — por isso a conversão é necessária.

        "#FFE100" -> "&H0000E1FF"   (amarelo, 100% opaco)
    """
    texto = str(cor_hex).strip().lstrip("#")
    if len(texto) == 3:  # formato curto: #FA0 -> #FFAA00
        texto = "".join(c * 2 for c in texto)
    if len(texto) != 6:
        raise ValueError(f"Cor inválida: {cor_hex} (use o formato #RRGGBB)")

    vermelho, verde, azul = texto[0:2], texto[2:4], texto[4:6]
    alfa = max(0, min(255, int(transparencia)))
    return f"&H{alfa:02X}{azul}{verde}{vermelho}".upper()


# ============================================================================
#  DIVERSOS
# ============================================================================

def nome_seguro(texto: str, tamanho_max: int = 60) -> str:
    """
    Transforma um texto qualquer em nome de arquivo válido no Windows.

        "GTA 6: VAZOU?!" -> "gta_6_vazou"
    """
    limpo = re.sub(r"[^\w\s-]", "", texto, flags=re.UNICODE).strip().lower()
    limpo = re.sub(r"[\s_-]+", "_", limpo)
    return (limpo[:tamanho_max] or "video").strip("_")


def garantir_python_moderno() -> None:
    """Avisa se a versão do Python for antiga demais para as bibliotecas."""
    if sys.version_info < (3, 9):
        log("Este projeto precisa de Python 3.9 ou mais novo.", "erro")
        sys.exit(1)
