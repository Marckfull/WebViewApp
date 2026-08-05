#!/usr/bin/env python3
"""Testa as travas sem tocar no repositório real.

Uso:  python3 .claude/hooks/selftest.py
"""

import json
import os
import shutil
import subprocess
import sys
import tempfile

AQUI = os.path.dirname(os.path.abspath(__file__))
GUARD = os.path.join(AQUI, "guard_qa.py")
LINT = os.path.join(AQUI, "lint_vocabulario.py")

QA_APROVADO = "# QA\n\nVEREDITO FINAL: APROVADO\n"
QA_AJUSTES = "# QA\n\nVEREDITO FINAL: APROVADO COM AJUSTES\n"
QA_REPROVADO = "# QA\n\nVEREDITO FINAL: REPROVADO\n"


def rodar(script, payload, raiz):
    ambiente = dict(os.environ, CLAUDE_PROJECT_DIR=raiz)
    proc = subprocess.run(
        [sys.executable, script],
        input=json.dumps(payload),
        capture_output=True,
        text=True,
        env=ambiente,
    )
    return proc.returncode, proc.stderr


def montar(raiz, episodio, qa=None):
    pasta = os.path.join(raiz, "episodios", episodio)
    os.makedirs(os.path.join(pasta, "locales", "es"), exist_ok=True)
    with open(os.path.join(pasta, "01-roteiro.md"), "w", encoding="utf-8") as fh:
        fh.write("roteiro\n")
    if qa is not None:
        with open(os.path.join(pasta, "04-qa.md"), "w", encoding="utf-8") as fh:
            fh.write(qa)
    return pasta


def escrita(caminho):
    return {"tool_name": "Write", "tool_input": {"file_path": caminho}}


def bash(comando):
    return {"tool_name": "Bash", "tool_input": {"command": comando}}


def main():
    falhas = []

    def checar(nome, obtido, esperado):
        marca = "ok  " if obtido == esperado else "FALHA"
        print("  %s %s (exit %s, esperado %s)" % (marca, nome, obtido, esperado))
        if obtido != esperado:
            falhas.append(nome)

    raiz = tempfile.mkdtemp(prefix="estudio-selftest-")
    try:
        montar(raiz, "EP001-sem-qa")
        montar(raiz, "EP002-reprovado", QA_REPROVADO)
        montar(raiz, "EP003-ajustes", QA_AJUSTES)
        montar(raiz, "EP004-aprovado", QA_APROVADO)

        print("guard_qa.py — bloqueio de produção")
        casos = [
            ("sem QA → storyboard", escrita("episodios/EP001-sem-qa/02-storyboard.md"), 2),
            ("reprovado → metadados", escrita("episodios/EP002-reprovado/05-metadados.md"), 2),
            ("com ajustes → letra", escrita("episodios/EP003-ajustes/03-letra.md"), 2),
            ("aprovado → storyboard", escrita("episodios/EP004-aprovado/02-storyboard.md"), 0),
            ("aprovado → locale", escrita("episodios/EP004-aprovado/locales/es/01-roteiro.md"), 0),
            ("sem QA → locale", escrita("episodios/EP001-sem-qa/locales/es/01-roteiro.md"), 2),
            ("roteiro sempre livre", escrita("episodios/EP001-sem-qa/01-roteiro.md"), 0),
            ("04-qa sempre livre", escrita("episodios/EP001-sem-qa/04-qa.md"), 0),
            ("pauta sempre livre", escrita("episodios/EP001-sem-qa/00-pauta.md"), 0),
            ("fora de episodios", escrita("biblia/universo.md"), 0),
            (
                "render sem QA",
                bash("python scripts/render.py --episodio EP001-sem-qa"),
                2,
            ),
            (
                "render aprovado",
                bash("python scripts/render.py --episodio EP004-aprovado"),
                0,
            ),
            ("render sem episódio", bash("python scripts/render.py"), 2),
            (
                "echo em arquivo protegido sem QA",
                bash("echo x > episodios/EP001-sem-qa/05-metadados.md"),
                2,
            ),
            ("bash inofensivo", bash("ls episodios/"), 0),
        ]
        for nome, payload, esperado in casos:
            codigo, _ = rodar(GUARD, payload, raiz)
            checar(nome, codigo, esperado)

        print("\nlint_vocabulario.py — aviso, nunca bloqueio")
        os.makedirs(os.path.join(raiz, "biblia"), exist_ok=True)
        with open(
            os.path.join(raiz, "biblia", "voz-e-vocabulario.md"), "w", encoding="utf-8"
        ) as fh:
            fh.write("# Voz\n\n## Lista negra\n\n- monstro\n- sozinho\n")
        with open(
            os.path.join(raiz, "episodios", "EP001-sem-qa", "01-roteiro.md"),
            "w",
            encoding="utf-8",
        ) as fh:
            fh.write("Tuca viu um monstro embaixo da cama e ficou muito quieta.\n")
        codigo, saida = rodar(
            LINT, escrita("episodios/EP001-sem-qa/01-roteiro.md"), raiz
        )
        checar("lint não bloqueia", codigo, 0)
        checar("lint achou lista negra", "monstro" in saida, True)
    finally:
        shutil.rmtree(raiz, ignore_errors=True)

    print()
    if falhas:
        print("%d falha(s): %s" % (len(falhas), ", ".join(map(str, falhas))))
        return 1
    print("Todas as travas passaram.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
