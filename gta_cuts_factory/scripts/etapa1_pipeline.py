"""
etapa1_pipeline.py — o núcleo do sistema rodando de ponta a ponta.

Fluxo:
    baixar (ou usar arquivo seu) → transcrever com tempo por palavra →
    🛡️ CHECAGEM DE SEGURANÇA → gancho → legenda karaokê → 9:16 → renderizar

A checagem de segurança roda ANTES do render de propósito: não faz sentido
gastar minutos processando um vídeo que vai ser reprovado.

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
    --forcar                               renderiza mesmo se for reprovado
                                           (o vídeo continua marcado 🔴)
"""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import (  # noqa: E402
    banco,
    downloader,
    gancho,
    legendas,
    render,
    seguranca,
    selecao,
    transcricao,
)
from nucleo.config import carregar_canais, carregar_config  # noqa: E402
from nucleo.utils import info_video, log, nome_seguro, para_segundos, titulo  # noqa: E402


def montar_argumentos() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="GTA Cuts Factory — pipeline de 1 corte (núcleo + segurança)"
    )
    origem = parser.add_mutually_exclusive_group(required=True)
    origem.add_argument("--url", help="link do vídeo-fonte (YouTube etc.)")
    origem.add_argument("--arquivo", help="vídeo que já está no seu computador")

    parser.add_argument("--auto", action="store_true",
                        help="deixa o sistema escolher o melhor trecho sozinho "
                             "(transcreve o vídeo todo e usa a Etapa 4)")
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
    parser.add_argument("--forcar", action="store_true",
                        help="renderiza mesmo se a checagem de segurança reprovar")
    return parser.parse_args()


def main() -> int:
    args = montar_argumentos()
    cfg = carregar_config()
    canais = carregar_canais()

    if args.reframe:
        cfg["video"]["modo_reframe"] = args.reframe

    inicio = para_segundos(args.inicio)
    duracao = para_segundos(args.duracao)

    minimo = float(cfg.pegar("video.duracao_minima_corte", 60))
    maximo = float(cfg.pegar("video.duracao_maxima_corte", 180))
    if not args.auto and not (minimo <= duracao <= maximo):
        log(f"Duração {duracao:.0f}s está fora da faixa recomendada "
            f"({minimo:.0f}s a {maximo:.0f}s). Seguindo mesmo assim.", "aviso")

    conexao = banco.conectar(cfg)
    banco.sincronizar_canais(conexao, canais)

    try:
        # -------------------------------------------------------------- 1) fonte
        titulo("1/6 — OBTENDO O VÍDEO-FONTE")
        if args.url:
            fonte = downloader.baixar(args.url, cfg)
        else:
            fonte = downloader.usar_arquivo_local(args.arquivo)
            log(f"Usando arquivo local: {fonte.arquivo}", "ok")

        # cadastra o canal (se for novo, entra como "em teste") e o vídeo
        if fonte.canal_id:
            fonte_banco = banco.obter_fonte(conexao, fonte.canal_id)
            if fonte_banco is None:
                banco.registrar_fonte(
                    conexao, canal_id=fonte.canal_id, nome=fonte.canal or fonte.canal_id,
                    url=fonte.canal_url, status="proprio" if fonte.canal_id == "proprio"
                    else "em_teste", idioma=args.idioma,
                )
                log(f"Canal novo cadastrado como 'em teste': {fonte.canal}", "info")
        banco.registrar_video_fonte(conexao, fonte)

        # --------------------------------------- 2) transcrição por palavra
        palavras: list[legendas.Palavra] = []
        resultado_transcricao: dict = {}

        if args.auto:
            if args.sem_transcricao:
                log("--auto precisa da transcrição para escolher o trecho. "
                    "Ignorando --sem-transcricao.", "aviso")
            # transcreve o vídeo INTEIRO uma vez só, e depois recorta
            completa = transcricao.transcrever(
                fonte.arquivo, cfg, inicio=0, duracao=None, idioma=args.idioma,
            )
            titulo("2b/6 — ESCOLHENDO O MELHOR MOMENTO (modo automático)")
            informacoes = info_video(
                fonte.arquivo, str(cfg.pegar("ffprobe_caminho", "ffprobe")),
                str(cfg.pegar("ffmpeg_caminho", "ffmpeg")),
            )
            momento = selecao.melhor_momento(
                completa, cfg, idioma=args.idioma,
                duracao_video=informacoes.duracao,
                conexao=conexao, video_id=fonte.id,
            )
            if momento is None:
                log("Não achei nenhum trecho bom neste vídeo. Tente outro vídeo, "
                    "ou escolha o trecho na mão com --inicio e --duracao.", "erro")
                return 3

            inicio, duracao = momento.inicio, momento.duracao
            log(f"Trecho escolhido: {selecao.formatar_tempo(momento.inicio)} → "
                f"{selecao.formatar_tempo(momento.fim)} ({duracao:.0f}s), "
                f"nota {momento.pontuacao:.2f}", "ok")
            print(f'   abre com: "{momento.primeira_frase[:70]}"')
            print(f"   motivos : {', '.join(momento.motivos)}")

            # aproveita a transcrição que já temos (não roda o Whisper de novo)
            resultado_transcricao = transcricao.recortar(completa, inicio, inicio + duracao)
            palavras = transcricao.palavras_de(resultado_transcricao)

        elif not args.sem_transcricao:
            resultado_transcricao = transcricao.transcrever(
                fonte.arquivo, cfg, inicio=inicio, duracao=duracao,
                idioma=args.idioma,
            )
            palavras = transcricao.palavras_de(resultado_transcricao)
        else:
            titulo("2/6 — TRANSCRIÇÃO (pulada por opção sua)")

        assunto = args.assunto or gancho.assunto_a_partir_do_texto(
            resultado_transcricao.get("texto", ""), args.idioma
        )

        # ----------------------------------------- 3) 🛡️ CHECAGEM DE SEGURANÇA
        titulo("3/6 — 🛡️ CHECAGEM DE SEGURANÇA")
        avaliacao = seguranca.avaliar(
            conexao, cfg, fonte,
            inicio=inicio, duracao=duracao,
            transcricao=resultado_transcricao,
            assunto=assunto, idioma=args.idioma, modo="A_corte",
            assuntos_proibidos=canais.get("assuntos_proibidos", []),
        )
        seguranca.imprimir_avaliacao(avaliacao)

        ficha_base = {
            "idioma": args.idioma,
            "modo": "A_corte",
            "assunto": assunto,
            "trecho": {"inicio": inicio, "fim": inicio + duracao, "duracao": duracao},
            "fonte": fonte.para_dict(),
            "credito": fonte.credito,
            "texto_transcrito": resultado_transcricao.get("texto", ""),
            "selo": avaliacao.selo,
            "motivos": avaliacao.motivos,
            "detalhes_seguranca": avaliacao.detalhes,
            "gerado_em": datetime.now().isoformat(timespec="seconds"),
        }

        if not avaliacao.aprovado and not args.forcar:
            # Nada é renderizado: o vídeo vai direto para a lista de reprovados,
            # com o motivo, para você revisar no painel se quiser.
            ficha_base["arquivo"] = ""
            seguranca.registrar_aprovacao(conexao, ficha_base, avaliacao)
            titulo("REPROVADO PELA CHECAGEM DE SEGURANÇA 🔴")
            print(avaliacao.resumo_texto())
            print("\nEste corte NÃO foi renderizado e NÃO entrou na fila.")
            print("Ele fica na lista de reprovados (você vê no painel, na Etapa 7).")
            print("\nO que fazer:")
            print("  • escolher outro trecho do mesmo vídeo (--inicio diferente)")
            print("  • usar o MODO B (narrado) se o material for oficial")
            print("  • se discordar, rode de novo com --forcar")
            return 2

        if not avaliacao.aprovado and args.forcar:
            log("Você usou --forcar: o vídeo será gerado mesmo REPROVADO. "
                "Ele fica marcado com selo vermelho.", "aviso")

        # ------------------------------------------------------------ 4) gancho
        titulo("4/6 — GANCHO DE ABERTURA")
        frase = gancho.criar_frase(
            assunto, cfg, idioma=args.idioma,
            primeira_fala=transcricao.texto_inicial(resultado_transcricao)
            if resultado_transcricao else "",
        )
        log(f"Assunto: {assunto}", "info")
        log(f'Gancho : "{frase}"', "ok")

        # ----------------------------------------------------------- 5) legenda
        titulo("5/6 — LEGENDA KARAOKÊ")
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

        # ------------------------------------------------------------ 6) render
        titulo("6/6 — RENDERIZANDO 1080x1920")
        final = render.renderizar_corte(
            entrada=fonte.arquivo,
            saida=pasta_saida / f"{base}.mp4",
            cfg=cfg,
            arquivo_ass=arquivo_ass,
            inicio=inicio,
            duracao=duracao,
        )
        render.gerar_miniatura(final, pasta_saida / f"{base}.jpg", cfg, segundo=1.0)

        # ------------------------------------------- ficha + registro no banco
        ficha = dict(ficha_base)
        ficha.update({
            "arquivo": str(final),
            "miniatura": str(pasta_saida / f"{base}.jpg"),
            "gancho": frase,
        })
        id_registro = seguranca.registrar_aprovacao(conexao, ficha, avaliacao)

        caminho_ficha = pasta_saida / f"{base}.json"
        caminho_ficha.write_text(
            json.dumps(ficha, ensure_ascii=False, indent=2), encoding="utf-8"
        )

        titulo(f"PRONTO {avaliacao.emoji}")
        print(f"Vídeo   : {final}")
        print(f"Ficha   : {caminho_ficha}")
        print(f"Selo    : {avaliacao.emoji} {avaliacao.selo.upper()} "
              f"(registro #{id_registro} na "
              f"{'fila' if avaliacao.aprovado else 'lista de reprovados'})")
        if fonte.url:
            print(f"Crédito : {fonte.credito}")

        estado = banco.resumo(conexao)
        print(f"\nMemória do sistema: {estado['trechos_usados']} trecho(s) usado(s), "
              f"{estado['na_fila']} na fila, {estado['reprovados']} reprovado(s).")
        return 0
    finally:
        conexao.close()


if __name__ == "__main__":
    raise SystemExit(main())
