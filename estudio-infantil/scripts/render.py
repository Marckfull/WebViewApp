#!/usr/bin/env python3
"""Montagem do episódio com ffmpeg.

Lê `02-storyboard.md`, casa cada plano com a imagem correspondente em
`episodios/<EP>/imagens/` e o áudio em `episodios/<EP>/audio/`, e monta o
vídeo final em `episodios/<EP>/render/`.

O hook `guard_qa.py` bloqueia a execução deste script se o episódio não tiver
`VEREDITO FINAL: APROVADO` em `04-qa.md`. Sempre passe `--episodio` — é por ele
que o hook identifica o que verificar.

Uso:
    python3 scripts/render.py --episodio EP001-o-medo-do-escuro --dry-run
    python3 scripts/render.py --episodio EP001-o-medo-do-escuro
"""

import argparse
import os
import re
import shutil
import subprocess
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MIN_PLANO = 3.0  # segundos — regra de biblia/estilo-visual.md
FPS = 24
RESOLUCAO = "1920x1080"


def segundos(marca):
    partes = [float(p) for p in marca.strip().split(":")]
    total = 0.0
    for parte in partes:
        total = total * 60 + parte
    return total


def ler_planos(caminho):
    """Extrai as linhas da tabela de planos do storyboard."""
    planos = []
    with open(caminho, encoding="utf-8") as fh:
        for linha in fh:
            if not linha.lstrip().startswith("|"):
                continue
            celulas = [c.strip() for c in linha.strip().strip("|").split("|")]
            if len(celulas) < 7 or not celulas[0].isdigit():
                continue
            tempo = celulas[1]
            achado = re.match(r"(\d+:\d{2}(?::\d+)?)\s*[–-]\s*(\d+:\d{2}(?::\d+)?)", tempo)
            if not achado:
                continue
            inicio, fim = segundos(achado.group(1)), segundos(achado.group(2))
            planos.append(
                {
                    "n": int(celulas[0]),
                    "inicio": inicio,
                    "fim": fim,
                    "duracao": fim - inicio,
                    "prompt": celulas[6],
                }
            )
    return planos


def validar(planos):
    problemas = []
    if not planos:
        problemas.append("nenhum plano encontrado na tabela do storyboard")
        return problemas
    for plano in planos:
        if plano["duracao"] < MIN_PLANO:
            problemas.append(
                "plano %d dura %.1fs — mínimo é %.1fs (biblia/estilo-visual.md)"
                % (plano["n"], plano["duracao"], MIN_PLANO)
            )
    for anterior, atual in zip(planos, planos[1:]):
        if abs(atual["inicio"] - anterior["fim"]) > 0.01:
            problemas.append(
                "buraco/sobreposição entre os planos %d e %d"
                % (anterior["n"], atual["n"])
            )
    return problemas


def imagem_do_plano(pasta, n):
    for extensao in (".png", ".jpg", ".jpeg", ".webp"):
        caminho = os.path.join(pasta, "imagens", "%03d%s" % (n, extensao))
        if os.path.isfile(caminho):
            return caminho
    return None


def main():
    parser = argparse.ArgumentParser(description="Montagem do episódio")
    parser.add_argument("--episodio", required=True, help="ex.: EP001-o-medo-do-escuro")
    parser.add_argument("--dry-run", action="store_true", help="valida sem renderizar")
    parser.add_argument("--saida", help="caminho do mp4 final")
    args = parser.parse_args()

    pasta = os.path.join(RAIZ, "episodios", args.episodio)
    storyboard = os.path.join(pasta, "02-storyboard.md")
    if not os.path.isfile(storyboard):
        print("não achei %s" % storyboard, file=sys.stderr)
        return 1

    planos = ler_planos(storyboard)
    problemas = validar(planos)
    print("%d planos, %.1fs somados" % (len(planos), sum(p["duracao"] for p in planos)))
    for problema in problemas:
        print("  ERRO: %s" % problema, file=sys.stderr)
    if problemas:
        print("\ncorrija o storyboard antes de renderizar", file=sys.stderr)
        return 1

    faltando = [p["n"] for p in planos if imagem_do_plano(pasta, p["n"]) is None]
    if faltando:
        print("  imagens faltando para os planos: %s" % faltando)
        print("  gere com os prompts visuais do storyboard (provedor com direitos comerciais)")

    if args.dry_run:
        print("\ndry-run ok — nada renderizado")
        return 0 if not faltando else 1
    if faltando:
        return 1

    if shutil.which("ffmpeg") is None:
        print("ffmpeg não está no PATH", file=sys.stderr)
        return 1

    render_dir = os.path.join(pasta, "render")
    os.makedirs(render_dir, exist_ok=True)
    lista = os.path.join(render_dir, "planos.txt")
    with open(lista, "w", encoding="utf-8") as fh:
        for plano in planos:
            imagem = imagem_do_plano(pasta, plano["n"])
            fh.write("file '%s'\n" % os.path.abspath(imagem))
            fh.write("duration %.3f\n" % plano["duracao"])
        # ffmpeg concat: repete o último frame para respeitar a duração final
        fh.write("file '%s'\n" % os.path.abspath(imagem_do_plano(pasta, planos[-1]["n"])))

    saida = args.saida or os.path.join(render_dir, "%s.mp4" % args.episodio)
    comando = [
        "ffmpeg", "-y",
        "-f", "concat", "-safe", "0", "-i", lista,
        "-vf", "scale=%s:force_original_aspect_ratio=decrease,pad=%s:(ow-iw)/2:(oh-ih)/2,fps=%d"
               % (RESOLUCAO.replace("x", ":"), RESOLUCAO.replace("x", ":"), FPS),
        "-c:v", "libx264", "-pix_fmt", "yuv420p", "-preset", "medium", "-crf", "18",
        saida,
    ]
    print("\n%s" % " ".join(comando))
    resultado = subprocess.run(comando)
    if resultado.returncode != 0:
        return resultado.returncode

    print("\nvídeo mudo em %s" % saida)
    print("Próximo: mixar áudio (audio/manifesto.json), depois CHECKPOINT HUMANO 2 —")
    print("uma pessoa assiste ao corte inteiro e assina 05-metadados.md.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
