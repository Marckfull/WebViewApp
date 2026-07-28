"""
etapa6_teste.py — TESTE 7: metadados (título/descrição/hashtags) e b-roll.

Mostra o que o sistema escreveria para publicar, em quatro situações
diferentes, para você conferir o tom antes de sair publicando. Nenhum vídeo é
gerado aqui.

COMO RODAR (PowerShell, dentro da pasta gta_cuts_factory):

    python scripts\\etapa6_teste.py

    # para testar também a decisão de b-roll num vídeo seu:
    python scripts\\etapa6_teste.py --video "C:\\Videos\\corte.mp4"
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import broll, metadados  # noqa: E402
from nucleo.config import carregar_config  # noqa: E402
from nucleo.utils import log, titulo  # noqa: E402


CREDITO_EXEMPLO = ("Fonte: GTA 6 — tudo sobre o novo trailer — Canal de Notícias GTA\n"
                   "https://youtu.be/EXEMPLO123")

CASOS = [
    {
        "nome": "1) RUMOR em português (o caso mais comum)",
        "assunto": "GTA 6 data de lançamento",
        "idioma": "pt", "modo": "A_corte",
        "texto": "olha o que vazou hoje, ainda não tem nada oficial da Rockstar",
        "credito": CREDITO_EXEMPLO,
    },
    {
        "nome": "2) FATO CONFIRMADO em português",
        "assunto": "GTA 6 segundo trailer",
        "idioma": "pt", "modo": "B_narrado",
        "texto": "a Rockstar confirmou oficialmente o segundo trailer no site",
        "credito": "Fonte: Rockstar Games (canal oficial)\nhttps://youtu.be/OFICIAL",
    },
    {
        "nome": "3) Vídeo em INGLÊS (o terceiro vídeo do dia)",
        "assunto": "GTA 6 map leak",
        "idioma": "en", "modo": "A_corte",
        "texto": "this leak showed the full map, nothing confirmed yet",
        "credito": CREDITO_EXEMPLO,
    },
    {
        "nome": "4) SEM CRÉDITO da fonte (o sistema tem que reclamar)",
        "assunto": "GTA 5 segredos do mapa",
        "idioma": "pt", "modo": "A_corte",
        "texto": "achei uma sala escondida embaixo do mapa",
        "credito": "",
    },
]


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Mostra os metadados que o sistema geraria (não gera vídeo)")
    parser.add_argument("--video", help="vídeo para testar a decisão de b-roll")
    args = parser.parse_args()

    cfg = carregar_config()

    titulo("TESTE 7 — METADADOS DE PUBLICAÇÃO")
    for caso in CASOS:
        print(f"\n{'─' * 68}\n{caso['nome']}\n{'─' * 68}")
        dados = metadados.gerar(
            caso["assunto"], cfg, idioma=caso["idioma"], modo=caso["modo"],
            credito=caso["credito"], texto_falado=caso["texto"],
        )
        print(metadados.resumo(dados))
        for aviso in dados.avisos:
            log(aviso, "aviso")

    # ---------------------------------------------------- conferência automática
    print(f"\n{'─' * 68}")
    print("🔍 CONFERINDO O COMPORTAMENTO")
    print(f"{'─' * 68}")
    tudo_certo = True

    def checar(descricao: str, condicao: bool) -> None:
        nonlocal tudo_certo
        print(f"   {'✅' if condicao else '❌'} {descricao}")
        tudo_certo = tudo_certo and condicao

    rumor = metadados.gerar(CASOS[0]["assunto"], cfg, "pt", "A_corte",
                            CASOS[0]["credito"], CASOS[0]["texto"])
    confirmado = metadados.gerar(CASOS[1]["assunto"], cfg, "pt", "B_narrado",
                                 CASOS[1]["credito"], CASOS[1]["texto"])
    sem_credito = metadados.gerar(CASOS[3]["assunto"], cfg, "pt", "A_corte", "",
                                  CASOS[3]["texto"])
    ingles = metadados.gerar(CASOS[2]["assunto"], cfg, "en", "A_corte",
                             CASOS[2]["credito"], CASOS[2]["texto"])

    checar("rumor NÃO vira 'CONFIRMADO' no título",
           "confirmado:" not in rumor.titulo.lower())
    checar("rumor leva o aviso de rumor na descrição",
           "rumor" in rumor.descricao.lower())
    checar("fato confirmado pode afirmar no título",
           "?" not in confirmado.titulo or "confirm" in confirmado.titulo.lower())
    checar("crédito da fonte aparece na descrição",
           "youtu.be" in rumor.descricao)
    checar("sem crédito, o sistema avisa",
           any("crédito" in a.lower() for a in sem_credito.avisos))
    checar("vídeo em inglês sai todo em inglês",
           "SOURCE CREDIT" in ingles.descricao)
    checar("todos os títulos cabem no limite do YouTube",
           all(len(d.titulo) <= metadados.LIMITE_TITULO_YOUTUBE
               for d in (rumor, confirmado, ingles, sem_credito)))
    checar("aviso legal (não afiliado à Rockstar) está presente",
           "não é afiliado" in rumor.descricao.lower())

    # ------------------------------------------------------------------ b-roll
    titulo("B-ROLL — GAMEPLAY JUNTO DO CORTE")
    print(f"Formato configurado: {broll.descrever_layout(cfg)}")
    pasta = str(cfg.pegar("broll.pasta", "") or "")
    print(f"Pasta de gameplay  : {pasta or '(não configurada)'}")

    if pasta:
        escolhido = broll.escolher_gameplay(cfg)
        print(f"Sorteado agora     : {Path(escolhido).name if escolhido else '(nenhum)'}")
    else:
        log("Configure 'broll.pasta' no config.yaml para o sistema poder usar "
            "gameplay de fundo.", "aviso")

    if args.video:
        caminho = Path(args.video)
        if not caminho.exists():
            log(f"Vídeo não encontrado: {caminho}", "erro")
        else:
            print(f"\nAnalisando {caminho.name}...")
            recomendado, explicacao, analise = broll.precisa_de_broll(
                caminho, cfg, 0, 60, cobertura_fala=0.7)
            print(f"   movimento medido: {analise.movimento:.4f} "
                  f"(limite: {cfg.pegar('broll.limite_movimento', 0.08)})")
            print(f"   decisão: {'USAR gameplay junto' if recomendado else 'não usar'}")
            print(f"   motivo : {explicacao}")

    titulo("ETAPA 6 FUNCIONANDO ✅" if tudo_certo else "ATENÇÃO: algo saiu diferente")
    print("Os textos acima são gerados de graça (sem IA). Se quiser títulos mais")
    print("criativos, ligue 'ia.ativa' no config.yaml (~US$ 0,005 por vídeo).")
    print("\nTudo isso já entra sozinho nos vídeos gerados pelas Etapas 1 e 5,")
    print("e o texto do TikTok é salvo em saida/tiktok/.")
    return 0 if tudo_certo else 1


if __name__ == "__main__":
    raise SystemExit(main())
