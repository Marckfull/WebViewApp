"""
painel.py — abre o painel local no navegador.

COMO RODAR (PowerShell, dentro da pasta gta_cuts_factory):

    python scripts\\painel.py

O navegador abre sozinho. Para fechar, volte a esta janela e aperte Ctrl + C.

Opções:
    --porta 5001      usa outra porta (se a 5000 estiver ocupada)
    --sem-navegador   não abre o navegador sozinho
"""

from __future__ import annotations

import argparse
import sys
import threading
import webbrowser
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo.config import carregar_config  # noqa: E402
from nucleo.utils import log, titulo  # noqa: E402
from painel.app import criar_aplicacao  # noqa: E402


def main() -> int:
    parser = argparse.ArgumentParser(description="Painel local do GTA Cuts Factory")
    parser.add_argument("--porta", type=int, default=5000)
    parser.add_argument("--sem-navegador", action="store_true")
    args = parser.parse_args()

    carregar_config()          # cria o config.yaml na primeira vez
    endereco = f"http://localhost:{args.porta}"

    titulo("PAINEL DO GTA CUTS FACTORY")
    print(f"Abra no navegador:  {endereco}")
    print("\nO que você faz aqui:")
    print("   • clicar em CRIAR para produzir os vídeos do dia")
    print("   • assistir cada vídeo e conferir o selo 🟢🟡🔴")
    print("   • editar título, descrição e hashtags")
    print("   • aprovar ou descartar — nada é publicado sozinho")
    print("\nPara fechar o painel: aperte Ctrl + C aqui nesta janela.\n")

    if not args.sem_navegador:
        threading.Timer(1.2, lambda: webbrowser.open(endereco)).start()

    try:
        criar_aplicacao().run(host="127.0.0.1", port=args.porta, debug=False,
                              use_reloader=False)
    except OSError as erro:
        log(f"Não consegui abrir a porta {args.porta} ({erro}).\n"
            f"   → provavelmente o painel já está aberto em outra janela\n"
            f"   → ou use outra porta: python scripts\\painel.py --porta 5001",
            "erro")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
