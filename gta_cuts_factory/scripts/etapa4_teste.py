"""
etapa4_teste.py — TESTE 5: escolher os melhores momentos dentro de um vídeo.

Três jeitos de rodar:

    # 1) DEMONSTRAÇÃO — usa um vídeo de mentirinha com roteiro conhecido
    #    (intro chata, patrocínio, parte morna, vazamento quente, despedida).
    #    Serve para você VER o sistema fugindo do lixo e achando o ouro.
    python scripts\\etapa4_teste.py --demo

    # 2) COM UMA TRANSCRIÇÃO QUE VOCÊ JÁ TEM (dados/transcricoes/*.json)
    python scripts\\etapa4_teste.py --transcricao dados\\transcricoes\\ARQUIVO.json

    # 3) COM UM VÍDEO DE VERDADE (transcreve com o Whisper e escolhe)
    python scripts\\etapa4_teste.py --video "C:\\Videos\\gameplay.mp4"

Nenhum vídeo é gerado aqui — este teste só mostra ONDE cortar.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import banco, selecao, transcricao as modulo_transcricao  # noqa: E402
from nucleo.config import carregar_config  # noqa: E402
from nucleo.selecao import formatar_tempo  # noqa: E402
from nucleo.utils import info_video, log, titulo  # noqa: E402


# ---------------------------------------------------------------------------
#  Vídeo de mentirinha, com roteiro que a gente conhece de antemão
# ---------------------------------------------------------------------------
#  (texto da frase, segundo em que começa, segundo em que termina)
ROTEIRO_DEMO = [
    # --- introdução: o sistema TEM que fugir daqui ------------------------
    ("Fala galera, tudo bem com vocês? Aqui é o canal de sempre.", 0, 6),
    ("Antes de começar, se inscreve no canal e deixa o like, por favor.", 6, 13),
    ("Ativa o sininho para não perder nenhum vídeo, comenta aí embaixo.", 13, 20),
    ("Hoje a gente vai falar de uma coisa muito importante.", 20, 26),
    ("Mas antes, um recado rápido dos nossos parceiros.", 26, 32),
    # --- patrocínio: também é lixo para corte -----------------------------
    ("Esse vídeo é patrocinado pela loja parceira do canal.", 32, 39),
    ("O link na descrição tem o cupom com desconto para vocês.", 39, 46),
    ("Use meu cupom e ajuda demais o canal, de verdade.", 46, 52),
    ("Beleza, agora vamos ao que interessa de fato.", 52, 58),
    # --- parte morna: fala genérica, sem gancho ---------------------------
    ("Então, como eu estava dizendo no vídeo passado, a gente esperava novidade.", 58, 68),
    ("A comunidade estava conversando sobre isso nos fóruns esses dias.", 68, 78),
    ("Muita gente comentou comigo sobre as expectativas do jogo.", 78, 88),
    ("É um assunto que aparece de vez em quando por aí.", 88, 98),
    ("Nada muito diferente do que já foi dito antes, na real.", 98, 110),
    ("A gente vai acompanhando e vendo no que dá.", 110, 122),
    # --- OURO 1: vazamento com gancho e emoção ----------------------------
    ("Mas olha isso que vazou agora, presta atenção nessa parte.", 122, 130),
    ("A Rockstar confirmou oficialmente a data de lançamento do GTA 6.", 130, 139),
    ("Caramba, eu não acredito que finalmente saiu essa confirmação!", 139, 147),
    ("O comunicado oficial fala do trailer novo que vem aí também.", 147, 156),
    ("Isso muda tudo o que a gente imaginava sobre o cronograma.", 156, 165),
    ("O GTA 6 vai chegar antes do que muita gente apostava.", 165, 174),
    ("É impressionante como a Rockstar segurou essa informação até agora.", 174, 184),
    ("Eu revi o comunicado três vezes para ter certeza do que estava lendo.", 184, 194),
    ("E tem mais detalhe importante nessa história toda.", 194, 202),
    ("O vazamento também mostrou a data do segundo trailer oficial.", 202, 212),
    # --- transição morna --------------------------------------------------
    ("Bom, isso é o que temos sobre essa parte por enquanto.", 212, 222),
    ("Vamos ver o resto das informações com calma agora.", 222, 232),
    ("Peguei um café aqui rapidinho antes de continuar.", 232, 240),
    # --- OURO 2: outro assunto, também forte ------------------------------
    ("Agora olha o que eu descobri no mapa do GTA 6, ninguém percebeu isso.", 240, 250),
    ("Tem uma região inteira que não apareceu em nenhum trailer até hoje.", 250, 260),
    ("Meu Deus, o mapa é muito maior do que a gente calculava.", 260, 269),
    ("Comparando com o mapa do GTA 5, dá quase o dobro do tamanho.", 269, 279),
    ("Isso confirma aquele vazamento antigo que todo mundo duvidou.", 279, 289),
    ("A cidade nova tem detalhe que ninguém tinha reparado ainda.", 289, 299),
    ("Eu fiquei chocado quando vi a comparação lado a lado.", 299, 308),
    ("Vou deixar a imagem aqui na tela para vocês verem comigo.", 308, 318),
    ("É surreal o tamanho do trabalho que a Rockstar fez nesse mapa.", 318, 328),
    # --- despedida: o sistema TEM que fugir daqui -------------------------
    ("Então é isso pessoal, era isso que eu queria mostrar hoje.", 328, 337),
    ("Se inscreve no canal, deixa o like e até o próximo vídeo!", 337, 345),
    ("Um abraço para todo mundo e até a próxima, valeu!", 345, 352),
]


def transcricao_de_demonstracao() -> dict:
    """Monta uma transcrição completa (frases + palavras) a partir do roteiro."""
    frases, palavras = [], []
    for texto, inicio, fim in ROTEIRO_DEMO:
        frases.append({"texto": texto, "inicio": float(inicio), "fim": float(fim)})

        # distribui as palavras da frase no tempo dela (como o Whisper faria)
        partes = texto.split()
        passo = (fim - inicio) / max(1, len(partes))
        for i, parte in enumerate(partes):
            comeco = inicio + i * passo
            palavras.append({"texto": parte, "inicio": round(comeco, 2),
                             "fim": round(comeco + passo * 0.9, 2)})

    return {
        "idioma": "pt",
        "texto": " ".join(f["texto"] for f in frases),
        "frases": frases,
        "palavras": palavras,
        "origem": "demonstração",
        "inicio_no_original": 0.0,
    }


def conferir_demonstracao(momentos, cfg) -> bool:
    """
    Confere automaticamente se o sistema acertou na demonstração:
    tem que fugir da intro/patrocínio/despedida e achar os dois trechos bons.
    """
    print("\n🔍 CONFERINDO O RESULTADO DA DEMONSTRAÇÃO")
    tudo_certo = True

    def checar(descricao: str, condicao: bool) -> None:
        nonlocal tudo_certo
        print(f"   {'✅' if condicao else '❌'} {descricao}")
        tudo_certo = tudo_certo and condicao

    checar("achou pelo menos 2 momentos", len(momentos) >= 2)
    if not momentos:
        return False

    textos = " ".join(m.texto.lower() for m in momentos)
    checar("nenhum corte pega o patrocínio ('cupom')", "cupom" not in textos)
    checar("nenhum corte pega 'se inscreve'", "se inscreve" not in textos)
    checar("nenhum corte começa antes dos 58s (intro)",
           all(m.inicio >= 58 for m in momentos))
    checar("achou o trecho do vazamento da data (~2:02)",
           any(120 <= m.inicio <= 135 for m in momentos))
    checar("achou o trecho do mapa (~4:00)",
           any(235 <= m.inicio <= 255 for m in momentos))

    for m in momentos:
        minimo = float(cfg.pegar("video.duracao_minima_corte", 60))
        maximo = float(cfg.pegar("video.duracao_maxima_corte", 180))
        checar(f"corte {formatar_tempo(m.inicio)} tem duração válida "
               f"({m.duracao:.0f}s entre {minimo:.0f} e {maximo:.0f})",
               minimo <= m.duracao <= maximo)

    folga = float(cfg.pegar("selecao.distancia_minima_entre_cortes", 30))
    for i, a in enumerate(momentos):
        for b in momentos[i + 1:]:
            checar(f"cortes {formatar_tempo(a.inicio)} e {formatar_tempo(b.inicio)} "
                   f"não se cruzam", not a.sobrepoe(b, folga))

    return tudo_certo


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Escolhe os melhores momentos de um vídeo (não gera vídeo)"
    )
    origem = parser.add_mutually_exclusive_group(required=True)
    origem.add_argument("--demo", action="store_true",
                        help="usa um roteiro de mentirinha para testar o sistema")
    origem.add_argument("--transcricao", help="arquivo .json de uma transcrição")
    origem.add_argument("--video", help="vídeo de verdade (usa o Whisper)")

    parser.add_argument("--quantidade", type=int, default=3)
    parser.add_argument("--idioma", default="pt", choices=["pt", "en"])
    parser.add_argument("--video-id", default="",
                        help="id do vídeo no banco (para o anti-repetição)")
    args = parser.parse_args()

    cfg = carregar_config()
    duracao_video = 0.0

    # ------------------------------------------------------- de onde vem o texto
    if args.demo:
        titulo("TESTE 5 (DEMONSTRAÇÃO) — ESCOLHA DOS MELHORES MOMENTOS")
        print("Vídeo de mentirinha com 5:52, contendo de propósito:")
        print("   0:00–0:58  introdução + 'se inscreve' + patrocínio  (lixo)")
        print("   0:58–2:02  papo morno, sem gancho                   (fraco)")
        print("   2:02–3:32  VAZAMENTO da data do GTA 6               (ouro)")
        print("   3:32–4:00  transição morna                          (fraco)")
        print("   4:00–5:28  DESCOBERTA do mapa                       (ouro)")
        print("   5:28–5:52  despedida                                (lixo)")
        transcricao = transcricao_de_demonstracao()
        duracao_video = ROTEIRO_DEMO[-1][2]

    elif args.transcricao:
        titulo("TESTE 5 — ESCOLHA DOS MELHORES MOMENTOS")
        caminho = Path(args.transcricao)
        if not caminho.exists():
            log(f"Arquivo não encontrado: {caminho}", "erro")
            return 1
        transcricao = json.loads(caminho.read_text(encoding="utf-8"))
        frases = transcricao.get("frases") or []
        duracao_video = float(frases[-1]["fim"]) if frases else 0.0

    else:
        titulo("TESTE 5 — ESCOLHA DOS MELHORES MOMENTOS")
        caminho = Path(args.video)
        if not caminho.exists():
            log(f"Vídeo não encontrado: {caminho}", "erro")
            return 1
        informacoes = info_video(caminho, str(cfg.pegar("ffprobe_caminho", "ffprobe")),
                                 str(cfg.pegar("ffmpeg_caminho", "ffmpeg")))
        duracao_video = informacoes.duracao
        log(f"Vídeo de {duracao_video / 60:.1f} minutos. Transcrevendo...", "etapa")
        transcricao = modulo_transcricao.transcrever(caminho, cfg, idioma=args.idioma)

    # ------------------------------------------------------------- a escolha
    conexao = banco.conectar(cfg)
    try:
        titulo(f"MOMENTOS ESCOLHIDOS (de {duracao_video / 60:.1f} min de vídeo)")
        momentos = selecao.escolher_momentos(
            transcricao, cfg, quantidade=args.quantidade, idioma=args.idioma,
            duracao_video=duracao_video, conexao=conexao, video_id=args.video_id,
        )
        print(selecao.resumo_de_momentos(momentos))

        if not momentos:
            log("Nenhum momento aproveitável. Tente um vídeo mais longo ou com "
                "mais fala.", "aviso")
            return 1

        # mostra os candidatos que o sistema RECUSOU (ajuda a entender o critério)
        frases = selecao.frases_da_transcricao(transcricao)
        todos = selecao.construir_candidatos(frases, cfg)
        for candidato in todos:
            nota, motivos, _ = selecao.pontuar_momento(
                candidato, cfg, args.idioma, duracao_video)
            candidato.pontuacao, candidato.motivos = nota, motivos
        piores = sorted(todos, key=lambda m: m.pontuacao)[:3]

        titulo("OS PIORES TRECHOS (para você ver o que ele evitou)")
        print(selecao.resumo_de_momentos(piores))

        if args.demo:
            ok = conferir_demonstracao(momentos, cfg)
            titulo("ETAPA 4 FUNCIONANDO ✅" if ok else "ATENÇÃO: algo saiu diferente")
            if not ok:
                return 1

        print("\nPara gerar o primeiro corte, use o comando da Etapa 1:")
        primeiro = momentos[0]
        print(f'   python scripts\\etapa1_pipeline.py --arquivo "SEU_VIDEO.mp4" '
              f'--inicio {primeiro.inicio:.0f} --duracao {primeiro.duracao:.0f} '
              f'--idioma {args.idioma}')
        print("\nOu deixe o sistema escolher sozinho, com --auto:")
        print('   python scripts\\etapa1_pipeline.py --arquivo "SEU_VIDEO.mp4" --auto')
        return 0
    finally:
        conexao.close()


if __name__ == "__main__":
    raise SystemExit(main())
