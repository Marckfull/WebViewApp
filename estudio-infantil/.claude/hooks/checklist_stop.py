#!/usr/bin/env python3
"""Stop: imprime o checklist curto de compliance ao fim da sessão.

Além do checklist fixo, mostra o estado de QA dos episódios que têm roteiro,
para ninguém terminar a semana achando que produziu algo que está travado.
"""

import json
import os
import re
import sys

VEREDITO = re.compile(r"VEREDITO\s+FINAL\s*:\s*\**\s*([A-ZÇÃÕÁÉÍÓÚÂÊÔ ]+)", re.I)

CHECKLIST = """
── Checklist de compliance (docs/compliance.md) ──────────────
  [ ] Marcado como "feito para crianças" no Studio
  [ ] Divulgação de uso de IA preenchida onde aplicável
  [ ] Zero IP de terceiros (personagem, trilha, formato, thumbnail)
  [ ] Licença comercial documentada de vozes, imagens e trilhas
  [ ] Aprovação humana do roteiro E do corte final registradas
  [ ] Nenhuma coleta de dado de criança em nenhum canal
  [ ] Legendas revisadas por humano
"""


def project_dir(payload):
    return (
        os.environ.get("CLAUDE_PROJECT_DIR")
        or payload.get("cwd")
        or os.getcwd()
    )


def estado_dos_episodios(raiz):
    base = os.path.join(raiz, "episodios")
    if not os.path.isdir(base):
        return []
    linhas = []
    for nome in sorted(os.listdir(base)):
        pasta = os.path.join(base, nome)
        if nome.startswith("_") or not os.path.isdir(pasta):
            continue
        if not os.path.isfile(os.path.join(pasta, "01-roteiro.md")):
            continue
        qa = os.path.join(pasta, "04-qa.md")
        if not os.path.isfile(qa):
            linhas.append("  %-32s QA pendente" % nome)
            continue
        try:
            with open(qa, encoding="utf-8") as fh:
                achados = VEREDITO.findall(fh.read())
        except OSError:
            achados = []
        estado = " ".join(achados[-1].split()).upper() if achados else "SEM VEREDITO"
        linhas.append("  %-32s %s" % (nome, estado))
    return linhas


def main():
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        payload = {}

    saida = [CHECKLIST.rstrip()]
    estados = estado_dos_episodios(project_dir(payload))
    if estados:
        saida.append("")
        saida.append("── Estado de QA ──────────────────────────────────────────────")
        saida.extend(estados)
    saida.append("──────────────────────────────────────────────────────────────")

    sys.stderr.write("\n".join(saida) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
