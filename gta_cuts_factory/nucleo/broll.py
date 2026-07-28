"""
broll.py — gameplay de fundo para os cortes de "cabeça falando".

O problema que este módulo resolve:
    um corte de alguém falando na frente da câmera, por 90 segundos, com a tela
    quase parada, perde o espectador. Colocar gameplay rodando junto dá o que
    olhar enquanto a pessoa fala — é o truque mais usado nos cortes que retêm.

Dois formatos (escolha em config.yaml → broll.layout):

    "split"  →  tela dividida: em cima o corte original, embaixo o gameplay
                ┌──────────┐
                │  pessoa  │  55% da altura
                ├──────────┤
                │ gameplay │  45% da altura
                └──────────┘

    "fundo"  →  gameplay ocupa a tela toda e o corte aparece como uma janela
                em cima (estilo "reação")

⚠️ O ÁUDIO: o áudio que vale é sempre o do corte original — é a emoção da fala
   que segura o espectador. O gameplay entra mudo por padrão.
"""

from __future__ import annotations

import random
from dataclasses import dataclass
from pathlib import Path

from .utils import InfoVideo, log


EXTENSOES_VIDEO = {".mp4", ".mkv", ".mov", ".avi", ".webm", ".m4v"}


@dataclass
class AnaliseTela:
    """O quanto a tela do corte se mexe (e se ele precisa de b-roll)."""

    movimento: float = 0.0        # 0 = tela parada, 1 = muita coisa acontecendo
    analisado: bool = False
    motivo: str = ""

    @property
    def tela_parada(self) -> bool:
        return self.analisado and self.movimento < 0.08


def medir_movimento(caminho: str | Path, inicio: float = 0.0,
                    duracao: float = 60.0, amostras: int = 12) -> AnaliseTela:
    """
    Mede o quanto a imagem muda ao longo do trecho.

    Compara "fotos" tiradas de tempos em tempos: se elas são quase iguais, é
    alguém falando na frente de um fundo parado — o caso clássico que pede
    gameplay junto.
    """
    try:
        import cv2  # type: ignore
    except ImportError:
        return AnaliseTela(motivo="OpenCV não instalado")

    captura = cv2.VideoCapture(str(caminho))
    if not captura.isOpened():
        return AnaliseTela(motivo="não consegui abrir o vídeo")

    try:
        anterior = None
        diferencas: list[float] = []
        for i in range(max(2, amostras)):
            instante = inicio + duracao * (i / max(1, amostras - 1))
            captura.set(cv2.CAP_PROP_POS_MSEC, instante * 1000.0)
            ok, frame = captura.read()
            if not ok or frame is None:
                continue
            pequeno = cv2.cvtColor(cv2.resize(frame, (160, 90)), cv2.COLOR_BGR2GRAY)
            if anterior is not None:
                diferencas.append(float(cv2.absdiff(pequeno, anterior).mean()) / 255.0)
            anterior = pequeno
    finally:
        captura.release()

    if not diferencas:
        return AnaliseTela(motivo="não consegui ler os quadros")

    movimento = sum(diferencas) / len(diferencas)
    return AnaliseTela(movimento=round(movimento, 4), analisado=True)


def precisa_de_broll(caminho: str | Path, cfg, inicio: float = 0.0,
                     duracao: float = 60.0, cobertura_fala: float = 0.0
                     ) -> tuple[bool, str, AnaliseTela]:
    """
    Decide se vale colocar gameplay junto.

    Recomenda quando as duas coisas acontecem ao mesmo tempo:
        • a tela quase não muda (alguém falando, fundo parado); e
        • tem bastante fala (o valor do corte está no áudio).

    Devolve (recomendado, explicação, análise).
    """
    if not cfg.pegar("broll.ativo", True):
        return False, "b-roll desligado no config", AnaliseTela()

    analise = medir_movimento(caminho, inicio, duracao)
    limite = float(cfg.pegar("broll.limite_movimento", 0.08))

    if not analise.analisado:
        return False, f"não deu para analisar ({analise.motivo})", analise

    if analise.movimento >= limite:
        return False, (f"a tela já tem movimento ({analise.movimento:.3f}) — "
                       f"não precisa de gameplay junto"), analise

    if cobertura_fala and cobertura_fala < 0.35:
        return False, ("tela parada E pouca fala: esse trecho é fraco, melhor "
                       "trocar de trecho do que tapar com gameplay"), analise

    return True, (f"tela quase parada ({analise.movimento:.3f}) com bastante "
                  f"fala — gameplay junto ajuda a segurar o espectador"), analise


def escolher_gameplay(cfg, evitar: str = "") -> Path | None:
    """
    Sorteia um vídeo de gameplay da sua pasta (config → broll.pasta).

    Sortear, e não pegar sempre o primeiro, evita que todos os seus vídeos
    fiquem com o mesmo fundo — o que fica visivelmente repetitivo no perfil.
    """
    pasta = str(cfg.pegar("broll.pasta", "") or "").strip()
    if not pasta:
        return None

    caminho = Path(pasta)
    if not caminho.exists():
        log(f"Pasta de gameplay não encontrada: {caminho}", "aviso")
        return None

    arquivos = [a for a in caminho.iterdir()
                if a.is_file() and a.suffix.lower() in EXTENSOES_VIDEO
                and str(a) != str(evitar)]
    if not arquivos:
        log(f"Nenhum vídeo de gameplay em {caminho}", "aviso")
        return None

    return random.choice(arquivos)


# ============================================================================
#  MONTAGEM DA TELA
# ============================================================================

def _corte_para_preencher(info: InfoVideo, largura: int, altura: int,
                          entrada: str, saida: str) -> str:
    """
    Monta o filtro que faz um vídeo PREENCHER uma área, sem distorcer.

    Ele aumenta até cobrir a área e corta o que sobra (igual ao "cover" do CSS).
    Distorcer a imagem para caber é o erro que mais denuncia corte amador.
    """
    return (f"[{entrada}]scale={largura}:{altura}:force_original_aspect_ratio=increase,"
            f"crop={largura}:{altura},setsar=1[{saida}]")


def construir_filtro_split(info_principal: InfoVideo, info_broll: InfoVideo,
                           cfg, rotulo_saida: str = "base") -> str:
    """
    Tela dividida: corte em cima, gameplay embaixo.

    A entrada 0 é o corte original e a 1 é o gameplay (o render cuida disso).
    """
    largura = int(cfg.pegar("video.largura", 1080))
    altura = int(cfg.pegar("video.altura", 1920))
    proporcao = float(cfg.pegar("broll.proporcao_principal", 0.55))

    altura_principal = int(altura * proporcao)
    altura_principal -= altura_principal % 2          # o codec exige par
    altura_broll = altura - altura_principal

    return (
        _corte_para_preencher(info_principal, largura, altura_principal, "0:v", "cima") + ";" +
        _corte_para_preencher(info_broll, largura, altura_broll, "1:v", "baixo") + ";" +
        f"[cima][baixo]vstack=inputs=2,setsar=1[{rotulo_saida}]"
    )


def construir_filtro_fundo(info_principal: InfoVideo, info_broll: InfoVideo,
                           cfg, rotulo_saida: str = "base") -> str:
    """
    Gameplay ocupando a tela toda, com o corte original como janela em cima.

    Bom quando o gameplay é o assunto e a pessoa está só comentando.
    """
    largura = int(cfg.pegar("video.largura", 1080))
    altura = int(cfg.pegar("video.altura", 1920))
    proporcao_janela = float(cfg.pegar("broll.largura_janela", 0.92))

    largura_janela = int(largura * proporcao_janela)
    largura_janela -= largura_janela % 2
    # a janela mantém a proporção do vídeo original
    proporcao_fonte = (info_principal.largura / info_principal.altura
                       if info_principal.altura else 16 / 9)
    altura_janela = int(largura_janela / max(0.1, proporcao_fonte))
    altura_janela -= altura_janela % 2

    topo = int(altura * float(cfg.pegar("broll.topo_janela", 0.12)))

    return (
        _corte_para_preencher(info_broll, largura, altura, "1:v", "fundo") + ";" +
        f"[0:v]scale={largura_janela}:{altura_janela}:flags=lanczos,setsar=1[janela];"
        f"[fundo][janela]overlay=(W-w)/2:{topo},setsar=1[{rotulo_saida}]"
    )


def construir_filtro(info_principal: InfoVideo, info_broll: InfoVideo, cfg,
                     rotulo_saida: str = "base", layout: str = "") -> str:
    """Escolhe o formato conforme o config (ou o que você passar aqui)."""
    escolhido = (layout or str(cfg.pegar("broll.layout", "split"))).lower()
    if escolhido == "fundo":
        return construir_filtro_fundo(info_principal, info_broll, cfg, rotulo_saida)
    return construir_filtro_split(info_principal, info_broll, cfg, rotulo_saida)


def descrever_layout(cfg, layout: str = "") -> str:
    escolhido = (layout or str(cfg.pegar("broll.layout", "split"))).lower()
    return {"split": "tela dividida (corte em cima, gameplay embaixo)",
            "fundo": "gameplay no fundo com janela do corte"}.get(
                escolhido, "tela dividida")
