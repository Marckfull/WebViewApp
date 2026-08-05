#!/usr/bin/env python3
"""PostToolUse: lint de vocabulário em roteiro e letra.

Compara o texto escrito com a lista negra de biblia/voz-e-vocabulario.md e
avisa (não bloqueia — quem bloqueia é o Guardião, item 10). Também sinaliza
frases com mais de 12 palavras, que é a regra de escrita do roteirista.

Só a stdlib.
"""

import json
import os
import re
import sys

ALVOS = re.compile(r"(?:01-roteiro|03-letra)[^/]*\.md$")
MAX_PALAVRAS = 12

# Na lista negra, cada linha começa com "- " dentro da seção marcada.
SECAO_LISTA_NEGRA = re.compile(
    r"^#+\s*Lista negra.*?$(.*?)(?=^#|\Z)", re.I | re.M | re.S
)


def project_dir(payload):
    return (
        os.environ.get("CLAUDE_PROJECT_DIR")
        or payload.get("cwd")
        or os.getcwd()
    )


def carregar_lista_negra(raiz):
    caminho = os.path.join(raiz, "biblia", "voz-e-vocabulario.md")
    try:
        with open(caminho, encoding="utf-8") as fh:
            texto = fh.read()
    except OSError:
        return []
    achado = SECAO_LISTA_NEGRA.search(texto)
    if not achado:
        return []
    palavras = []
    for linha in achado.group(1).splitlines():
        linha = linha.strip()
        if not linha.startswith(("-", "*")):
            continue
        termo = linha.lstrip("-* ").split("—")[0].split("#")[0].strip()
        termo = termo.strip("`*_ ")
        if termo:
            palavras.append(termo.lower())
    return palavras


def frases_longas(texto):
    longas = []
    # Ignora marcações de ação, cabeçalho YAML e tabelas.
    corpo = re.sub(r"^---.*?^---", "", texto, flags=re.S | re.M)
    corpo = re.sub(r"\[AÇÃO[^\]]*\]", " ", corpo)
    corpo = re.sub(r"^\s*[|#>-].*$", "", corpo, flags=re.M)
    for frase in re.split(r"(?<=[.!?…])\s+|\n", corpo):
        palavras = [p for p in re.findall(r"[\wÀ-ÿ'-]+", frase) if p]
        if len(palavras) > MAX_PALAVRAS:
            longas.append((len(palavras), " ".join(frase.split())[:80]))
    return longas


def main():
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        return 0

    entrada = payload.get("tool_input") or {}
    caminho = entrada.get("file_path") or entrada.get("path")
    if not isinstance(caminho, str) or not ALVOS.search(caminho.replace("\\", "/")):
        return 0

    raiz = project_dir(payload)
    absoluto = caminho if os.path.isabs(caminho) else os.path.join(raiz, caminho)
    try:
        with open(absoluto, encoding="utf-8") as fh:
            texto = fh.read()
    except OSError:
        return 0

    minusculo = texto.lower()
    achadas = [
        palavra
        for palavra in carregar_lista_negra(raiz)
        if re.search(r"\b%s\b" % re.escape(palavra), minusculo)
    ]
    longas = frases_longas(texto)

    if not achadas and not longas:
        return 0

    linhas = ["Lint de vocabulário em %s:" % os.path.basename(caminho)]
    if achadas:
        linhas.append("")
        linhas.append("  LISTA NEGRA (corrija antes do QA): %s" % ", ".join(sorted(set(achadas))))
    if longas:
        linhas.append("")
        linhas.append("  Frases acima de %d palavras: %d" % (MAX_PALAVRAS, len(longas)))
        for tamanho, trecho in longas[:5]:
            linhas.append("    - %d palavras: %s..." % (tamanho, trecho))
        if len(longas) > 5:
            linhas.append("    - (+%d)" % (len(longas) - 5))
    linhas.append("")
    linhas.append("  Aviso, não bloqueio. O Guardião cobra isso no item 10.")

    sys.stderr.write("\n".join(linhas) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
