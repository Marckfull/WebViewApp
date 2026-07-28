"""
etapa5_narrado.py — MODO B: gera um vídeo narrado do começo ao fim.

Este é o modo mais seguro do sistema: a voz é sintética lendo o SEU roteiro, e
o vídeo de fundo (trailer oficial ou gameplay) entra só como apoio.

COMO RODAR (PowerShell, dentro da pasta gta_cuts_factory):

    python scripts\\etapa5_narrado.py ^
        --assunto "GTA 6 data de lançamento" ^
        --fundo "C:\\Videos\\trailer_oficial.mp4" ^
        --fatos "a Rockstar publicou um comunicado no site oficial" ^
                "o comunicado fala do segundo trailer, sem data fechada" ^
                "a página do jogo foi atualizada no mesmo dia" ^
        --idioma pt

⚠️ Os FATOS são a alma do vídeo. O sistema não inventa notícia: ele narra o que
   você passar. Sem --fatos, ele gera um roteiro de pergunta aberta, avisando
   que nada foi confirmado.

Opções úteis:
    --so-roteiro       mostra o texto e para (não gera áudio nem vídeo)
    --fatos-arquivo    lê os fatos de um .txt (uma frase por linha)
    --inicio-fundo 30  começa o vídeo de fundo em outro ponto
    --voz              força uma voz específica do Edge-TTS
"""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import (  # noqa: E402
    banco, gancho, legendas, metadados, narracao, render, seguranca,
)
from nucleo.config import carregar_canais, carregar_config  # noqa: E402
from nucleo.downloader import usar_arquivo_local  # noqa: E402
from nucleo.utils import log, nome_seguro, titulo  # noqa: E402


def montar_argumentos() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="GTA Cuts Factory — MODO B (vídeo narrado, risco quase zero)"
    )
    parser.add_argument("--assunto", required=True,
                        help='tema do vídeo, ex.: "GTA 6 data de lançamento"')
    parser.add_argument("--fundo",
                        help="vídeo de fundo (trailer oficial ou gameplay seu)")
    parser.add_argument("--fatos", nargs="*", default=[],
                        help="as informações que VOCÊ apurou (uma frase cada)")
    parser.add_argument("--fatos-arquivo",
                        help="arquivo .txt com um fato por linha")
    parser.add_argument("--idioma", default="pt", choices=["pt", "en"])
    parser.add_argument("--voz", help="voz do Edge-TTS (ex.: pt-BR-FranciscaNeural)")
    parser.add_argument("--inicio-fundo", type=float, default=0.0,
                        help="segundo em que o vídeo de fundo começa")
    parser.add_argument("--so-roteiro", action="store_true",
                        help="só mostra o roteiro e para")
    parser.add_argument("--forcar", action="store_true",
                        help="gera mesmo se a checagem de segurança reprovar")
    return parser.parse_args()


def carregar_fatos(args) -> list[str]:
    """Junta os fatos da linha de comando com os do arquivo, se houver."""
    fatos = [f.strip() for f in (args.fatos or []) if f.strip()]
    if args.fatos_arquivo:
        caminho = Path(args.fatos_arquivo)
        if not caminho.exists():
            log(f"Arquivo de fatos não encontrado: {caminho}", "erro")
            raise SystemExit(1)
        fatos += [linha.strip() for linha in
                  caminho.read_text(encoding="utf-8").splitlines()
                  if linha.strip() and not linha.strip().startswith("#")]
    return fatos


def main() -> int:
    args = montar_argumentos()
    cfg = carregar_config()
    canais = carregar_canais()

    if args.voz:
        cfg["tts"]["voz_en" if args.idioma == "en" else "voz_pt"] = args.voz

    fatos = carregar_fatos(args)

    # ------------------------------------------------------------ 1) roteiro
    titulo("1/5 — ROTEIRO")
    roteiro = narracao.criar_roteiro(args.assunto, cfg, args.idioma, fatos)

    print(f"Assunto : {roteiro.assunto}")
    print(f"Origem  : {'IA' if roteiro.origem == 'ia' else 'modelo pronto (grátis)'}")
    print(f"Tamanho : {roteiro.palavras} palavras "
          f"(~{roteiro.duracao_estimada():.0f}s de narração)")
    print(f"\n─── TEXTO QUE VAI SER FALADO ───\n{roteiro.texto}\n")
    for aviso in roteiro.avisos:
        log(aviso, "aviso")

    if args.so_roteiro:
        print("\n(--so-roteiro: parando por aqui, nada foi gerado)")
        return 0

    if not args.fundo:
        log("Falta o vídeo de fundo. Use --fundo \"C:\\Videos\\trailer.mp4\".", "erro")
        print("\n💡 Dica: o fundo mais seguro é um trailer OFICIAL da Rockstar ou "
              "uma gravação sua.")
        return 1

    fundo = Path(args.fundo)
    if not fundo.exists():
        log(f"Vídeo de fundo não encontrado: {fundo}", "erro")
        return 1

    # ------------------------------------------------------------ 2) narração
    titulo("2/5 — NARRAÇÃO (voz sintética)")
    pasta_temp = cfg.caminho("temporario")
    base = f"{nome_seguro(args.assunto)}_{args.idioma}"
    voz = narracao.narrar(roteiro, cfg, pasta_temp / f"{base}.mp3")

    if not voz.tempos_reais:
        log("Os tempos das palavras foram estimados (o provedor de voz não "
            "informa os tempos reais). A legenda pode ficar levemente fora de "
            "sincronia.", "aviso")

    # -------------------------------------------- 3) 🛡️ checagem de segurança
    titulo("3/5 — 🛡️ CHECAGEM DE SEGURANÇA")
    conexao = banco.conectar(cfg)
    banco.sincronizar_canais(conexao, canais)

    try:
        fonte = usar_arquivo_local(fundo)
        banco.registrar_video_fonte(conexao, fonte)
        transcricao_narrada = narracao.transcricao_do_roteiro(roteiro, voz)

        avaliacao = seguranca.avaliar(
            conexao, cfg, fonte,
            inicio=args.inicio_fundo, duracao=voz.duracao,
            transcricao=transcricao_narrada,
            assunto=args.assunto, idioma=args.idioma,
            modo="B_narrado",          # ← é isto que libera o material oficial
            assuntos_proibidos=canais.get("assuntos_proibidos", []),
        )
        seguranca.imprimir_avaliacao(avaliacao)

        if not avaliacao.aprovado and not args.forcar:
            ficha_reprovada = {
                "arquivo": "", "idioma": args.idioma, "modo": "B_narrado",
                "assunto": args.assunto,
                "trecho": {"inicio": args.inicio_fundo,
                           "fim": args.inicio_fundo + voz.duracao,
                           "duracao": voz.duracao},
                "fonte": fonte.para_dict(), "selo": avaliacao.selo,
                "motivos": avaliacao.motivos,
            }
            seguranca.registrar_aprovacao(conexao, ficha_reprovada, avaliacao)
            titulo("REPROVADO PELA CHECAGEM 🔴")
            print(avaliacao.resumo_texto())
            print("\nDica: no Modo B, o motivo mais comum é assunto repetido. "
                  "Tente outro tema ou rode com --forcar se discordar.")
            return 2

        # ------------------------------------------------ 4) gancho + legenda
        titulo("4/5 — GANCHO + LEGENDA KARAOKÊ")
        frase = gancho.criar_frase(args.assunto, cfg, idioma=args.idioma,
                                   primeira_fala=roteiro.texto[:400])
        log(f'Gancho: "{frase}"', "ok")

        carimbo = datetime.now().strftime("%Y-%m-%d")
        pasta_saida = cfg.caminho("saida") / carimbo
        pasta_saida.mkdir(parents=True, exist_ok=True)

        eventos = gancho.eventos_gancho(frase, cfg, idioma=args.idioma)
        arquivo_ass = legendas.salvar_ass(
            voz.palavras, cfg, pasta_saida / f"{base}.ass",
            idioma=args.idioma, eventos_extra=eventos,
        )
        log(f"{len(voz.palavras)} palavras na legenda → {arquivo_ass.name}", "ok")

        # ------------------------------------------------------- 5) render
        titulo("5/5 — RENDERIZANDO 1080x1920 (narrado)")
        final = render.renderizar_narrado(
            fundo=fundo, audio_narracao=voz.arquivo,
            saida=pasta_saida / f"{base}.mp4", cfg=cfg,
            arquivo_ass=arquivo_ass, duracao=voz.duracao,
            inicio_fundo=args.inicio_fundo,
        )
        render.gerar_miniatura(final, pasta_saida / f"{base}.jpg", cfg, segundo=1.0)

        dados = metadados.gerar(
            args.assunto, cfg, idioma=args.idioma, modo="B_narrado",
            credito=fonte.credito, texto_falado=roteiro.texto, gancho=frase,
        )
        for aviso in dados.avisos:
            log(aviso, "aviso")
        arquivo_tiktok = metadados.salvar_para_tiktok(
            dados, str(final), cfg.raiz / str(cfg.pegar("tiktok.pasta_saida",
                                                        "saida/tiktok")))

        ficha = {
            "arquivo": str(final),
            "titulo": dados.titulo,
            "metadados": dados.para_dict(),
            "tiktok_txt": str(arquivo_tiktok),
            "miniatura": str(pasta_saida / f"{base}.jpg"),
            "idioma": args.idioma, "modo": "B_narrado",
            "assunto": args.assunto, "gancho": frase,
            "trecho": {"inicio": args.inicio_fundo,
                       "fim": args.inicio_fundo + voz.duracao,
                       "duracao": voz.duracao},
            "fonte": fonte.para_dict(), "credito": fonte.credito,
            "roteiro": roteiro.para_dict(),
            "narracao": {"voz": voz.voz, "provedor": voz.provedor,
                         "tempos_reais": voz.tempos_reais},
            "texto_transcrito": roteiro.texto,
            "selo": avaliacao.selo, "motivos": avaliacao.motivos,
            "gerado_em": datetime.now().isoformat(timespec="seconds"),
        }
        id_registro = seguranca.registrar_aprovacao(conexao, ficha, avaliacao)
        (pasta_saida / f"{base}.json").write_text(
            json.dumps(ficha, ensure_ascii=False, indent=2), encoding="utf-8")

        titulo(f"VÍDEO NARRADO PRONTO {avaliacao.emoji}")
        print(f"Vídeo : {final}")
        print(f"Selo  : {avaliacao.emoji} {avaliacao.selo.upper()} "
              f"(registro #{id_registro} na fila)")
        print(f"Voz   : {voz.voz} ({voz.provedor}) — {voz.duracao:.0f}s")
        print(f"Título: {dados.titulo}")
        print(f"Tags  : {' '.join(dados.hashtags)}")
        print(f"TikTok: {arquivo_tiktok}")
        print("\nEste é o modo mais seguro contra Content ID: o áudio é seu.")
        return 0
    finally:
        conexao.close()


if __name__ == "__main__":
    raise SystemExit(main())
