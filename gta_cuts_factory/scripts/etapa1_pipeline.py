"""
etapa1_pipeline.py — TESTE 2: o núcleo do sistema rodando de ponta a ponta.

Fluxo:
    baixar (ou usar arquivo seu) → transcrever com tempo por palavra →
    escolher o gancho → gerar legenda karaokê → cortar em 9:16 → renderizar

COMO RODAR (PowerShell, dentro da pasta gta_cuts_factory):

    # a partir de um link do YouTube
    python scripts\\etapa1_pipeline.py --url "https://youtu.be/XXXXXXXX" ^
        --inicio 00:01:20 --duracao 75 --idioma pt

    # a partir de um vídeo que já está no seu PC (sua gravação = risco zero)
    python scripts\\etapa1_pipeline.py --arquivo "C:\\Videos\\meu_gameplay.mp4" ^
        --inicio 30 --duracao 60 --idioma pt

Opções úteis:
    --assunto "GTA 6 data de lançamento"   define o tema usado no gancho
    --reframe blur                         força o modo de enquadramento
    --sem-transcricao                      pula o Whisper (testa só o corte 9:16)
"""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import downloader, gancho, legendas, render, transcricao  # noqa: E402
from nucleo.config import carregar_config  # noqa: E402
from nucleo.utils import (  # noqa: E402
    log,
    nome_seguro,
    para_segundos,
    titulo,
)


def montar_argumentos() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="GTA Cuts Factory — Etapa 1 (núcleo: corte 9:16 + legenda)"
    )
    origem = parser.add_mutually_exclusive_group(required=True)
    origem.add_argument("--url", help="link do vídeo-fonte (YouTube etc.)")
    origem.add_argument("--arquivo", help="vídeo que já está no seu computador")

    parser.add_argument("--inicio", default="0",
                        help='onde o corte começa: "90" ou "00:01:30"')
    parser.add_argument("--duracao", default="60",
                        help="duração do corte em segundos (60 a 180)")
    parser.add_argument("--idioma", default="pt", choices=["pt", "en"],
                        help="idioma do vídeo (define legenda e gancho)")
    parser.add_argument("--assunto", default="",
                        help='tema do vídeo, usado no gancho (ex.: "GTA 6")')
    parser.add_argument("--reframe", choices=["auto", "centro", "blur"],
                        help="força o modo de enquadramento 9:16")
    parser.add_argument("--sem-transcricao", action="store_true",
                        help="pula o Whisper (gera vídeo sem legenda de fala)")
    return parser.parse_args()


def main() -> int:
    args = montar_argumentos()
    cfg = carregar_config()

    if args.reframe:
        cfg["video"]["modo_reframe"] = args.reframe

    inicio = para_segundos(args.inicio)
    duracao = para_segundos(args.duracao)

    minimo = float(cfg.pegar("video.duracao_minima_corte", 60))
    maximo = float(cfg.pegar("video.duracao_maxima_corte", 180))
    if not (minimo <= duracao <= maximo):
        log(f"Duração {duracao:.0f}s está fora da faixa recomendada "
            f"({minimo:.0f}s a {maximo:.0f}s). Seguindo mesmo assim.", "aviso")

    # ------------------------------------------------------------------ 1) fonte
    titulo("1/5 — OBTENDO O VÍDEO-FONTE")
    if args.url:
        fonte = downloader.baixar(args.url, cfg)
    else:
        fonte = downloader.usar_arquivo_local(args.arquivo)
        log(f"Usando arquivo local: {fonte.arquivo}", "ok")

    # ------------------------------------------------- 2) transcrição por palavra
    palavras: list[legendas.Palavra] = []
    resultado_transcricao: dict = {}
    if not args.sem_transcricao:
        resultado_transcricao = transcricao.transcrever(
            fonte.arquivo, cfg, inicio=inicio, duracao=duracao,
            idioma=args.idioma,
        )
        palavras = transcricao.palavras_de(resultado_transcricao)
    else:
        titulo("2/5 — TRANSCRIÇÃO (pulada por opção sua)")

    # ------------------------------------------------------------------ 3) gancho
    titulo("3/5 — GANCHO DE ABERTURA")
    assunto = args.assunto or gancho.assunto_a_partir_do_texto(
        resultado_transcricao.get("texto", ""), args.idioma
    )
    frase = gancho.criar_frase(
        assunto, cfg, idioma=args.idioma,
        primeira_fala=transcricao.texto_inicial(resultado_transcricao)
        if resultado_transcricao else "",
    )
    log(f'Assunto: {assunto}', "info")
    log(f'Gancho : "{frase}"', "ok")

    # ---------------------------------------------------------------- 4) legenda
    titulo("4/5 — LEGENDA KARAOKÊ")
    carimbo = datetime.now().strftime("%Y-%m-%d")
    pasta_saida = cfg.caminho("saida") / carimbo
    pasta_saida.mkdir(parents=True, exist_ok=True)
    base = f"{nome_seguro(fonte.titulo or 'corte')}_{int(inicio)}_{args.idioma}"

    eventos = gancho.eventos_gancho(frase, cfg, idioma=args.idioma)
    arquivo_ass = legendas.salvar_ass(
        palavras, cfg, pasta_saida / f"{base}.ass",
        idioma=args.idioma, eventos_extra=eventos,
    )
    log(f"{len(palavras)} palavras na legenda → {arquivo_ass.name}", "ok")

    # ----------------------------------------------------------------- 5) render
    titulo("5/5 — RENDERIZANDO 1080x1920")
    final = render.renderizar_corte(
        entrada=fonte.arquivo,
        saida=pasta_saida / f"{base}.mp4",
        cfg=cfg,
        arquivo_ass=arquivo_ass,
        inicio=inicio,
        duracao=duracao,
    )
    render.gerar_miniatura(final, pasta_saida / f"{base}.jpg", cfg, segundo=1.0)

    # ---------------------------------------------- ficha do vídeo (rastreio)
    # Este JSON é o que as próximas etapas (banco anti-repetição, módulo de
    # segurança e painel de aprovação) vão consumir.
    ficha = {
        "arquivo": str(final),
        "miniatura": str(pasta_saida / f"{base}.jpg"),
        "idioma": args.idioma,
        "modo": "A_corte",
        "assunto": assunto,
        "gancho": frase,
        "trecho": {"inicio": inicio, "fim": inicio + duracao, "duracao": duracao},
        "fonte": fonte.para_dict(),
        "credito": fonte.credito,
        "texto_transcrito": resultado_transcricao.get("texto", ""),
        "gerado_em": datetime.now().isoformat(timespec="seconds"),
    }
    caminho_ficha = pasta_saida / f"{base}.json"
    caminho_ficha.write_text(
        json.dumps(ficha, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    titulo("PRONTO ✅")
    print(f"Vídeo   : {final}")
    print(f"Ficha   : {caminho_ficha}")
    if fonte.url:
        print(f"Crédito : {fonte.credito}")
    print("\nPróximo passo: assista e me diga o que ajustar (tamanho da legenda,")
    print("posição, cores, duração do gancho). Depois seguimos para a Etapa 2")
    print("(banco anti-repetição + módulo de segurança).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
