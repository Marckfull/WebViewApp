"""
etapa3_teste.py — TESTE 4: o que está em alta e de onde vamos cortar.

Este script NÃO gera vídeo nenhum. Ele só mostra o que o sistema descobriu:

    1. os assuntos em alta sobre GTA 6 / GTA 5
    2. os vídeos candidatos dos SEUS canais aprovados
    3. quais 3 ele escolheria, e por quê (com canais e assuntos diferentes)

É o teste "olho no olho": você confere se as fontes e os temas fazem sentido
ANTES de deixar o sistema gastar tempo baixando e renderizando.

COMO RODAR (PowerShell, dentro da pasta gta_cuts_factory):

    python scripts\\etapa3_teste.py                 # usa internet
    python scripts\\etapa3_teste.py --offline       # só a sua lista base
    python scripts\\etapa3_teste.py --idioma en     # tendências em inglês
"""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import banco, fontes, tendencias  # noqa: E402
from nucleo.config import carregar_canais, carregar_config  # noqa: E402
from nucleo.utils import log, titulo  # noqa: E402


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Mostra assuntos em alta e candidatos a corte (não gera vídeo)"
    )
    parser.add_argument("--idioma", default="pt", choices=["pt", "en"])
    parser.add_argument("--offline", action="store_true",
                        help="não acessa a internet; usa só a sua lista base")
    parser.add_argument("--quantidade", type=int, default=3,
                        help="quantos vídeos o sistema escolheria (padrão: 3)")
    parser.add_argument("--salvar", action="store_true",
                        help="salva o resultado em dados/candidatos.json")
    args = parser.parse_args()

    cfg = carregar_config()
    canais = carregar_canais()
    usar_rede = not args.offline and bool(
        cfg.pegar("producao.buscar_tendencias_online", True))

    conexao = banco.conectar(cfg)
    banco.sincronizar_canais(conexao, canais)

    try:
        # ------------------------------------------------------- 1) tendências
        titulo(f"1/3 — ASSUNTOS EM ALTA ({args.idioma.upper()})")
        assuntos = tendencias.descobrir_assuntos(
            cfg, canais, idioma=args.idioma, limite=8,
            usar_rede=usar_rede, conexao=conexao,
        )
        print(tendencias.resumo_de_assuntos(assuntos))
        if not assuntos:
            log("Nenhum assunto disponível. Verifique 'assuntos_base' em "
                "config/canais_fontes.yaml.", "aviso")

        # -------------------------------------------------------- 2) candidatos
        titulo("2/3 — CANDIDATOS NOS SEUS CANAIS APROVADOS")
        candidatos = fontes.buscar_candidatos(
            conexao, cfg, canais, assuntos, idioma=args.idioma, usar_rede=usar_rede,
        )
        print(fontes.resumo_de_candidatos(candidatos[:10]))

        if not candidatos:
            log("Nenhum candidato. Causas comuns:", "aviso")
            print("   • config/canais_fontes.yaml ainda está com os canais de exemplo")
            print("   • você rodou com --offline (canais do YouTube precisam de rede)")
            print("   • os canais listados estão todos bloqueados")
            print("\n👉 Edite config/canais_fontes.yaml e coloque canais reais que "
                  "você aprova.")
            return 1

        # ------------------------------------------------------ 3) escolha final
        titulo(f"3/3 — OS {args.quantidade} QUE O SISTEMA ESCOLHERIA")
        escolhidos = fontes.diversificar(candidatos, args.quantidade)
        idiomas = list(cfg.pegar("producao.idiomas", ["pt", "pt", "en"]))

        for i, candidato in enumerate(escolhidos):
            idioma_video = idiomas[i] if i < len(idiomas) else args.idioma
            print(f"\n🎬 VÍDEO {i + 1} ({idioma_video.upper()})")
            print(f"   {candidato.selo} {candidato.titulo}")
            print(f"   canal  : {candidato.canal} ({candidato.status_fonte})")
            print(f"   assunto: {candidato.assunto or '(nenhum em alta)'}")
            print(f"   nota   : {candidato.pontuacao:.2f} — "
                  f"{', '.join(candidato.motivos)}")
            print(f"   fonte  : {candidato.arquivo_local or candidato.url}")

        canais_distintos = len({c.canal_id for c in escolhidos})
        assuntos_distintos = len({c.assunto or c.video_id for c in escolhidos})
        print(f"\n✅ Variedade: {canais_distintos} canal(is) diferente(s), "
              f"{assuntos_distintos} assunto(s) diferente(s).")
        if canais_distintos < len(escolhidos):
            log("Repetiu canal porque não havia candidatos suficientes em outros. "
                "Adicione mais canais em config/canais_fontes.yaml.", "aviso")

        if args.salvar:
            destino = cfg.caminho("downloads").parent / "candidatos.json"
            destino.write_text(json.dumps({
                "gerado_em": datetime.now().isoformat(timespec="seconds"),
                "idioma": args.idioma,
                "assuntos": [a.para_dict() for a in assuntos],
                "candidatos": [c.para_dict() for c in candidatos],
                "escolhidos": [c.para_dict() for c in escolhidos],
            }, ensure_ascii=False, indent=2), encoding="utf-8")
            log(f"Resultado salvo em {destino}", "ok")

        titulo("ETAPA 3 FUNCIONANDO ✅")
        print("Nenhum vídeo foi baixado ou gerado — isso aqui é só o planejamento.")
        print("\nPara transformar um destes em vídeo, use o comando da Etapa 1:")
        primeiro = escolhidos[0]
        origem = (f'--arquivo "{primeiro.arquivo_local}"' if primeiro.eh_local
                  else f'--url "{primeiro.url}"')
        print(f'   python scripts\\etapa1_pipeline.py {origem} '
              f'--inicio 00:01:00 --duracao 75 --idioma {idiomas[0]} '
              f'--assunto "{primeiro.assunto or "GTA 6"}"')
        return 0
    finally:
        conexao.close()


if __name__ == "__main__":
    raise SystemExit(main())
