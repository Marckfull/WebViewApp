"""
transcricao.py — transcreve a fala com timestamp POR PALAVRA.

Por que "por palavra" e não "por frase"?
    Porque a legenda karaokê precisa saber o milissegundo em que CADA palavra
    é falada. Legenda por frase não permite acender a palavra certa na hora
    certa — e é justamente esse detalhe que dá a cara de "corte profissional".

Usamos o faster-whisper: é o Whisper da OpenAI reescrito para rodar rápido na
CPU. É gratuito, roda offline (nada é enviado para a internet) e já entrega os
tempos de cada palavra.

Na primeira execução ele baixa o modelo (~500 MB no tamanho "small").
Isso acontece UMA vez; depois fica salvo no seu computador.
"""

from __future__ import annotations

import json
from pathlib import Path

from .legendas import Palavra
from .render import extrair_audio
from .utils import log, titulo


_modelo_em_memoria = {}  # cache: evita recarregar o modelo a cada vídeo


def _carregar_modelo(cfg):
    """Carrega (uma vez só) o modelo do Whisper conforme o config."""
    try:
        from faster_whisper import WhisperModel  # type: ignore
    except ImportError as erro:
        raise RuntimeError(
            "O faster-whisper não está instalado.\n"
            "   Rode:  pip install -r requirements.txt"
        ) from erro

    nome = str(cfg.pegar("transcricao.modelo", "small"))
    dispositivo = str(cfg.pegar("transcricao.dispositivo", "auto"))
    computacao = str(cfg.pegar("transcricao.tipo_computacao", "int8"))

    chave = (nome, dispositivo, computacao)
    if chave not in _modelo_em_memoria:
        log(f"Carregando modelo Whisper '{nome}' "
            f"(na primeira vez ele é baixado, ~500 MB)...", "etapa")
        try:
            _modelo_em_memoria[chave] = WhisperModel(
                nome, device=dispositivo, compute_type=computacao
            )
        except Exception as erro:
            # Erro típico na PRIMEIRA execução: sem internet, firewall/antivírus
            # bloqueando, ou proxy da empresa. O modelo só precisa ser baixado
            # uma vez; depois disso funciona offline para sempre.
            raise RuntimeError(
                f"Não consegui carregar o modelo Whisper '{nome}'.\n"
                f"   Motivo técnico: {erro}\n\n"
                "   O que fazer:\n"
                "   1. Confira sua conexão com a internet (o modelo é baixado "
                "só na primeira vez).\n"
                "   2. Se estiver em rede com firewall/proxy, tente em outra rede.\n"
                "   3. Enquanto isso, dá para testar o resto do sistema com a "
                "opção --sem-transcricao.\n"
                "   4. Modelo menor baixa mais rápido: em config/config.yaml "
                "troque transcricao.modelo para \"base\"."
            ) from erro
        log("Modelo carregado.", "ok")
    return _modelo_em_memoria[chave]


def transcrever(
    entrada: str | Path,
    cfg,
    inicio: float = 0.0,
    duracao: float | None = None,
    idioma: str | None = None,
    cache: bool = True,
) -> dict:
    """
    Transcreve um vídeo (ou apenas um trecho dele).

    Devolve um dicionário:
        {
          "idioma": "pt",
          "texto": "texto corrido completo",
          "palavras": [{"texto": "GTA", "inicio": 0.32, "fim": 0.61}, ...],
          "frases":   [{"texto": "...", "inicio": 0.0, "fim": 4.2}, ...]
        }

    Os tempos são contados a partir do INÍCIO DO TRECHO (não do vídeo original)
    — é exatamente o que a legenda precisa.
    """
    entrada = Path(entrada)
    pasta_cache = cfg.caminho("transcricoes")
    marca = f"{entrada.stem}_{int(inicio)}_{int(duracao or 0)}"
    arquivo_cache = pasta_cache / f"{marca}.json"

    if cache and arquivo_cache.exists():
        log(f"Reaproveitando transcrição salva: {arquivo_cache.name}", "ok")
        return json.loads(arquivo_cache.read_text(encoding="utf-8"))

    titulo("TRANSCRIÇÃO (Whisper)")

    # 1) tira só o áudio do trecho — bem mais rápido que mandar o vídeo inteiro
    wav = cfg.caminho("temporario") / f"{marca}.wav"
    log("Extraindo áudio...", "etapa")
    extrair_audio(entrada, wav, cfg, inicio=inicio, duracao=duracao)

    # 2) transcreve
    modelo = _carregar_modelo(cfg)
    idioma_config = idioma or cfg.pegar("transcricao.idioma")
    log("Transcrevendo (pode demorar alguns minutos)...", "etapa")

    segmentos, info = modelo.transcribe(
        str(wav),
        language=idioma_config,
        word_timestamps=True,                       # ← o que dá o tempo por palavra
        vad_filter=bool(cfg.pegar("transcricao.usar_vad", True)),
        vad_parameters={"min_silence_duration_ms": 400},
        beam_size=5,
        condition_on_previous_text=False,           # evita repetir texto em loop
    )

    palavras: list[dict] = []
    frases: list[dict] = []
    for segmento in segmentos:
        frases.append({
            "texto": segmento.text.strip(),
            "inicio": float(segmento.start),
            "fim": float(segmento.end),
        })
        for palavra in (segmento.words or []):
            texto = (palavra.word or "").strip()
            if texto:
                palavras.append({
                    "texto": texto,
                    "inicio": float(palavra.start),
                    "fim": float(palavra.end),
                })

    resultado = {
        "idioma": getattr(info, "language", idioma_config or "pt"),
        "texto": " ".join(f["texto"] for f in frases).strip(),
        "palavras": palavras,
        "frases": frases,
        "origem": str(entrada),
        "inicio_no_original": float(inicio),
    }

    arquivo_cache.parent.mkdir(parents=True, exist_ok=True)
    arquivo_cache.write_text(
        json.dumps(resultado, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    try:
        wav.unlink()  # o WAV é grande e não serve mais
    except OSError:
        pass

    log(f"{len(palavras)} palavras transcritas (idioma detectado: "
        f"{resultado['idioma']}).", "ok")
    return resultado


def recortar(resultado: dict, inicio: float, fim: float) -> dict:
    """
    Pega um PEDAÇO de uma transcrição já feita e reescreve os tempos como se
    ele começasse do zero.

    Para que serve: no modo automático, o sistema transcreve o vídeo inteiro
    uma vez e depois escolhe o melhor trecho. Sem esta função, seria preciso
    rodar o Whisper de novo só para pegar os tempos do corte — desperdício de
    vários minutos.

        recortar(transcricao_completa, 122, 212)
        → só o que foi falado entre 2:02 e 3:32, com os tempos começando em 0
    """
    inicio, fim = float(inicio), float(fim)

    def dentro(item: dict) -> bool:
        # o item conta se o MEIO dele estiver dentro do trecho — assim uma
        # palavra que começa 0,1s antes do corte não é perdida nem duplicada
        meio = (float(item.get("inicio", 0)) + float(item.get("fim", 0))) / 2
        return inicio <= meio <= fim

    def deslocar(itens: list[dict]) -> list[dict]:
        return [
            {**item,
             "inicio": round(float(item.get("inicio", 0)) - inicio, 3),
             "fim": round(float(item.get("fim", 0)) - inicio, 3)}
            for item in itens if dentro(item)
        ]

    frases = deslocar(resultado.get("frases", []) or [])
    palavras = deslocar(resultado.get("palavras", []) or [])

    return {
        "idioma": resultado.get("idioma", "pt"),
        "texto": " ".join(f.get("texto", "") for f in frases).strip()
                 or " ".join(p.get("texto", "") for p in palavras).strip(),
        "palavras": palavras,
        "frases": frases,
        "origem": resultado.get("origem", ""),
        "inicio_no_original": float(resultado.get("inicio_no_original", 0.0)) + inicio,
    }


def palavras_de(resultado: dict) -> list[Palavra]:
    """Converte o dicionário da transcrição na lista de Palavra usada pela legenda."""
    return [Palavra.de_dict(item) for item in resultado.get("palavras", [])]


def texto_inicial(resultado: dict, segundos: float = 20.0) -> str:
    """
    Devolve o texto falado nos primeiros segundos do corte.
    Serve de contexto para a IA escrever o gancho.
    """
    return " ".join(
        p["texto"] for p in resultado.get("palavras", [])
        if float(p.get("inicio", 0)) <= segundos
    ).strip()
