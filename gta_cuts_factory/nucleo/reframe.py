"""
reframe.py — transforma vídeo horizontal (16:9) em vertical (9:16).

O problema: um vídeo de YouTube tem 1920x1080. Se você simplesmente encolher
para caber em 1080x1920, o vídeo fica minúsculo no meio da tela. Se cortar o
meio "no chute", a pessoa que está falando pode ficar de fora do quadro.

As três estratégias deste módulo:

    "auto"    → procura ROSTOS (OpenCV) e, se não achar, procura ONDE ESTÁ O
                MOVIMENTO. Corta a faixa vertical em volta desse ponto.
                É o modo recomendado para gameplay e para "cabeça falando".

    "centro"  → corta a faixa do meio. Rápido, previsível, sem dependências.

    "blur"    → não corta nada: coloca o vídeo inteiro no centro e preenche o
                fundo com uma versão ampliada e desfocada dele mesmo.
                Bom para trechos com HUD/minimapa importante nas bordas.
"""

from __future__ import annotations

from pathlib import Path

from .utils import InfoVideo, log


# ============================================================================
#  DETECÇÃO DO PONTO DE INTERESSE
# ============================================================================

def _criar_detector_rosto(cv2):
    """
    Cria o detector de rostos (classificador Haar), se ele existir nesta
    versão do OpenCV.

    Por que o "se existir": o OpenCV 5.0 tirou o CascadeClassifier do pacote
    padrão do pip. O requirements.txt fixa a versão 4.x (que tem o detector),
    mas se você já tiver a 5.x instalada o sistema não quebra: ele apenas
    passa a enquadrar pelo MOVIMENTO da cena, que funciona muito bem em
    gameplay de GTA.
    """
    try:
        caminho_modelo = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
        detector = cv2.CascadeClassifier(caminho_modelo)
        if detector.empty():
            return None
        return detector
    except AttributeError:
        log("Este OpenCV não tem detector de rosto — enquadrando pelo "
            "movimento da cena.", "info")
        return None
    except Exception:
        return None


def detectar_foco_x(
    caminho: str | Path,
    inicio: float = 0.0,
    duracao: float = 60.0,
    amostras: int = 12,
) -> float:
    """
    Descobre em qual posição horizontal está a "ação" do vídeo.

    Devolve um número de 0.0 (extrema esquerda) a 1.0 (extrema direita);
    0.5 significa "centro". Se o OpenCV não estiver instalado ou nada for
    detectado, devolve 0.5 — o sistema continua funcionando normalmente.

    Como funciona:
        1. tira ~12 "fotos" espalhadas pelo trecho
        2. procura rostos em cada foto (classificador Haar, que é leve e roda
           em qualquer notebook, sem placa de vídeo)
        3. se não achar rosto nenhum, mede ONDE a imagem mais muda entre uma
           foto e outra — que é onde está o movimento/ação
    """
    try:
        import cv2  # type: ignore
        import numpy as np  # type: ignore
    except ImportError:
        log("OpenCV não instalado — usando corte central.", "aviso")
        return 0.5

    captura = cv2.VideoCapture(str(caminho))
    if not captura.isOpened():
        log("Não consegui abrir o vídeo para análise — usando corte central.", "aviso")
        return 0.5

    try:
        detector = _criar_detector_rosto(cv2)
        largura = captura.get(cv2.CAP_PROP_FRAME_WIDTH) or 1920.0

        posicoes_rosto: list[tuple[float, float]] = []  # (posição x, peso=área)
        cinzas_anteriores = None
        movimento_acumulado = None

        for i in range(max(2, amostras)):
            instante = inicio + duracao * (i / max(1, amostras - 1))
            captura.set(cv2.CAP_PROP_POS_MSEC, instante * 1000.0)
            ok, frame = captura.read()
            if not ok or frame is None:
                continue

            cinza = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            pequeno = cv2.resize(cinza, (320, 180))

            # --- 1) rostos (só se o detector estiver disponível) ---
            if detector is not None:
                rostos = detector.detectMultiScale(cinza, scaleFactor=1.15,
                                                   minNeighbors=6, minSize=(60, 60))
                for (x, y, w, h) in rostos:
                    posicoes_rosto.append(((x + w / 2) / largura, float(w * h)))

            # --- 2) movimento (só usado se não houver rosto) ---
            if cinzas_anteriores is not None:
                diferenca = cv2.absdiff(pequeno, cinzas_anteriores)
                coluna = diferenca.sum(axis=0).astype("float64")
                movimento_acumulado = (
                    coluna if movimento_acumulado is None
                    else movimento_acumulado + coluna
                )
            cinzas_anteriores = pequeno

        # Rosto encontrado → média ponderada pela área (rosto maior = mais perto
        # da câmera = provavelmente o assunto principal)
        if posicoes_rosto:
            peso_total = sum(peso for _, peso in posicoes_rosto)
            foco = sum(x * peso for x, peso in posicoes_rosto) / peso_total
            log(f"Reframe: rosto detectado em x={foco:.2f}", "ok")
            return float(min(1.0, max(0.0, foco)))

        # Sem rosto → centro de massa do movimento
        if movimento_acumulado is not None and movimento_acumulado.sum() > 0:
            indices = np.arange(len(movimento_acumulado))
            foco = float((indices * movimento_acumulado).sum()
                         / movimento_acumulado.sum() / len(movimento_acumulado))
            # puxa 40% em direção ao centro: evita cortes exagerados por causa
            # de um efeito de explosão numa quina da tela
            foco = 0.6 * foco + 0.4 * 0.5
            log(f"Reframe: movimento concentrado em x={foco:.2f}", "ok")
            return float(min(1.0, max(0.0, foco)))
    finally:
        captura.release()

    log("Nada detectado — usando corte central.", "aviso")
    return 0.5


# ============================================================================
#  MONTAGEM DO FILTRO FFMPEG
# ============================================================================

def construir_filtro(
    info: InfoVideo,
    cfg,
    foco_x: float = 0.5,
    rotulo_saida: str = "base",
) -> str:
    """
    Monta o trecho de filtro do ffmpeg que converte o vídeo para 9:16.

    Devolve algo como:
        "[0:v]crop=1080:1080:420:0,scale=1080:1920,setsar=1[base]"

    O rótulo [base] é usado depois pelo render.py para grudar a legenda por cima.
    """
    largura_alvo = int(cfg.pegar("video.largura", 1080))
    altura_alvo = int(cfg.pegar("video.altura", 1920))
    modo = str(cfg.pegar("video.modo_reframe", "auto")).lower()

    proporcao_alvo = largura_alvo / altura_alvo  # 0.5625 para 9:16
    proporcao_fonte = (info.largura / info.altura) if info.altura else 16 / 9

    # Se o vídeo já é vertical (ou mais estreito que 9:16), cortar tiraria
    # conteúdo importante: trocamos automaticamente para o modo com fundo.
    if proporcao_fonte <= proporcao_alvo + 0.01 and modo != "blur":
        log("Fonte já é vertical — usando fundo desfocado em vez de cortar.", "info")
        modo = "blur"

    if modo == "blur":
        return (
            f"[0:v]split=2[bg][fg];"
            f"[bg]scale={largura_alvo}:{altura_alvo}:force_original_aspect_ratio=increase,"
            f"crop={largura_alvo}:{altura_alvo},gblur=sigma=28,eq=brightness=-0.10[bgd];"
            f"[fg]scale={largura_alvo}:-2:flags=lanczos[fgd];"
            f"[bgd][fgd]overlay=(W-w)/2:(H-h)/2,setsar=1[{rotulo_saida}]"
        )

    # --- modos "auto" e "centro": recorte de uma faixa vertical --------------
    largura_corte = int(round(info.altura * proporcao_alvo))
    largura_corte = min(largura_corte, info.largura)
    largura_corte -= largura_corte % 2  # o codec exige número par

    if modo == "centro":
        foco_x = 0.5

    centro = foco_x * info.largura
    x = int(round(centro - largura_corte / 2))
    x = max(0, min(info.largura - largura_corte, x))  # não deixa sair da tela
    x -= x % 2

    return (
        f"[0:v]crop={largura_corte}:{info.altura}:{x}:0,"
        f"scale={largura_alvo}:{altura_alvo}:flags=lanczos,setsar=1[{rotulo_saida}]"
    )


def descrever_modo(cfg) -> str:
    """Texto amigável para mostrar no terminal/painel."""
    return {
        "auto": "inteligente (rosto/ação)",
        "centro": "corte central",
        "blur": "fundo desfocado",
    }.get(str(cfg.pegar("video.modo_reframe", "auto")).lower(), "inteligente")
