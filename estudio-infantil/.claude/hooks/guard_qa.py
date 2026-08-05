#!/usr/bin/env python3
"""PreToolUse: bloqueia produção de episódio sem QA aprovado.

Lê o payload do hook em stdin e recusa (exit 2) qualquer tentativa de:
  - escrever/editar 02-storyboard.md, 03-letra.md, 05-metadados.md ou locales/
    de um episódio;
  - executar scripts/render.py para um episódio;
quando o 04-qa.md daquele episódio não terminar com
`VEREDITO FINAL: APROVADO`.

Só a stdlib. Nunca falha aberto por erro de parsing de arquivo de QA: se o
veredito não puder ser lido, bloqueia.
"""

import json
import os
import re
import sys

# Arquivos de produção: só podem ser escritos depois do QA aprovado.
PROTEGIDOS = re.compile(
    r"(?:02-storyboard|03-letra|05-metadados)[^/]*\.md$|(?:^|/)locales(?:/|$)"
)

# Um diretório de episódio: episodios/EP001-slug/...
EPISODIO = re.compile(r"episodios[/\\]([^/\\\s'\";|&)]+)")

RENDER = re.compile(r"scripts[/\\]render\.py")

# Aceita **VEREDITO FINAL: APROVADO**, com ou sem markdown/pontuação.
VEREDITO = re.compile(r"VEREDITO\s+FINAL\s*:\s*\**\s*([A-ZÇÃÕÁÉÍÓÚÂÊÔ ]+)", re.I)


def project_dir(payload):
    return (
        os.environ.get("CLAUDE_PROJECT_DIR")
        or payload.get("cwd")
        or os.getcwd()
    )


def veredito_do_episodio(raiz, episodio):
    """Retorna (aprovado: bool, motivo: str)."""
    qa = os.path.join(raiz, "episodios", episodio, "04-qa.md")
    if not os.path.isfile(qa):
        return False, "não existe 04-qa.md — rode /qa %s" % episodio
    try:
        with open(qa, encoding="utf-8") as fh:
            texto = fh.read()
    except OSError as err:
        return False, "não consegui ler 04-qa.md (%s)" % err

    achados = VEREDITO.findall(texto)
    if not achados:
        return False, "04-qa.md não tem linha 'VEREDITO FINAL:'"

    final = " ".join(achados[-1].split()).upper()
    if final == "APROVADO":
        return True, final
    return False, "veredito atual: %s" % final


def alvos_de_escrita(tool_input):
    """Caminhos que a ferramenta vai escrever."""
    caminhos = []
    for chave in ("file_path", "path", "notebook_path"):
        valor = tool_input.get(chave)
        if isinstance(valor, str):
            caminhos.append(valor)
    for edicao in tool_input.get("edits") or []:
        if isinstance(edicao, dict) and isinstance(edicao.get("file_path"), str):
            caminhos.append(edicao["file_path"])
    return caminhos


def episodio_de(caminho):
    achado = EPISODIO.search(caminho.replace("\\", "/"))
    return achado.group(1) if achado else None


def checar_escrita(raiz, caminhos):
    """Lista de (episodio, motivo) bloqueados."""
    bloqueios = []
    for caminho in caminhos:
        normal = caminho.replace("\\", "/")
        if not PROTEGIDOS.search(normal):
            continue
        episodio = episodio_de(normal)
        if episodio is None:
            continue
        ok, motivo = veredito_do_episodio(raiz, episodio)
        if not ok:
            bloqueios.append((episodio, motivo))
    return bloqueios


def checar_bash(raiz, comando):
    """Bash: render.py exige QA; escrita direta em arquivo protegido também."""
    bloqueios = []
    normal = comando.replace("\\", "/")

    # Qualquer referência a arquivo protegido dentro do comando
    # (redirecionamento, cp, sed -i, tee...).
    for trecho in re.findall(r"[\w./\-]*episodios/[\w./\-]+", normal):
        if PROTEGIDOS.search(trecho):
            episodio = episodio_de(trecho)
            if episodio:
                ok, motivo = veredito_do_episodio(raiz, episodio)
                if not ok:
                    bloqueios.append((episodio, motivo))

    if RENDER.search(normal):
        episodios = set(re.findall(r"episodios/([^/\s'\";|&)]+)", normal))
        flag = re.search(r"--episodio[= ]+([^\s'\";|&)]+)", normal)
        if flag:
            episodios.add(flag.group(1))
        if not episodios:
            bloqueios.append(
                (
                    "?",
                    "render.py sem episódio identificável — use "
                    "--episodio EPXXX-slug para o hook poder verificar o QA",
                )
            )
        for episodio in sorted(episodios):
            ok, motivo = veredito_do_episodio(raiz, episodio)
            if not ok:
                bloqueios.append((episodio, motivo))

    # Deduplica preservando ordem.
    vistos, unicos = set(), []
    for item in bloqueios:
        if item not in vistos:
            vistos.add(item)
            unicos.append(item)
    return unicos


def main():
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        # Payload ilegível: não é papel do hook derrubar a sessão.
        return 0

    raiz = project_dir(payload)
    ferramenta = payload.get("tool_name", "")
    entrada = payload.get("tool_input") or {}

    if ferramenta == "Bash":
        comando = entrada.get("command")
        bloqueios = checar_bash(raiz, comando) if isinstance(comando, str) else []
    elif ferramenta in ("Write", "Edit", "MultiEdit", "NotebookEdit"):
        bloqueios = checar_escrita(raiz, alvos_de_escrita(entrada))
    else:
        bloqueios = []

    if not bloqueios:
        return 0

    linhas = [
        "BLOQUEADO pelo Guardião: produção sem QA aprovado.",
        "",
    ]
    for episodio, motivo in bloqueios:
        linhas.append("  - %s: %s" % (episodio, motivo))
    linhas += [
        "",
        "Arquivos de produção (02-storyboard, 03-letra, 05-metadados, locales/)",
        "e scripts/render.py só liberam com 'VEREDITO FINAL: APROVADO' limpo",
        "em 04-qa.md. 'APROVADO COM AJUSTES' não libera — os ajustes precisam",
        "ser aplicados e revisados primeiro.",
        "",
        "Próximo passo: rode /qa <EPISODIO>, aplique as correções obrigatórias",
        "e rode o QA de novo. Não edite este hook e não escreva o veredito à mão.",
    ]
    sys.stderr.write("\n".join(linhas) + "\n")
    return 2


if __name__ == "__main__":
    sys.exit(main())
