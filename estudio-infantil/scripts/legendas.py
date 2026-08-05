#!/usr/bin/env python3
"""Legendas (SRT/VTT) a partir do roteiro e da letra.

Gera legenda pelo tempo declarado no roteiro (que é a fonte da verdade da
série), sem depender de ASR. Se você quiser conferir com Whisper local, use
`--conferir <arquivo.srt>` para comparar.

**A legenda publicada sempre passa por revisão humana.** Saída bruta de ASR não
vai ao ar — está em docs/compliance.md.

Uso:
    python3 scripts/legendas.py --episodio EP001-o-medo-do-escuro
    python3 scripts/legendas.py --episodio EP001-... --locale es --formato vtt
"""

import argparse
import os
import re
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

TEMPO = re.compile(r"^##\s*(\d+:\d{2})\s*[–-]\s*(\d+:\d{2})")
FALA = re.compile(
    r"^\s*\**([A-ZÀ-Ÿ][A-ZÀ-Ÿ ÇÃÕÁÉÍÓÚÂÊÔ'-]{1,30}?)\**\s*:\**\s*(.+?)\s*$"
)
ACAO = re.compile(r"\[AÇÃO[^\]]*\]")
KARAOKE = re.compile(r"^\[(\d{2}):(\d{2}):(\d{2})\.(\d)\]\s*(.+)$")

MAX_CARACTERES = 40  # por linha, leitura infantil


def segundos(marca):
    minuto, segundo = marca.split(":")
    return int(minuto) * 60 + int(segundo)


def formatar(t, formato):
    horas, resto = divmod(t, 3600)
    minutos, segundos_ = divmod(resto, 60)
    inteiro = int(segundos_)
    milis = int(round((segundos_ - inteiro) * 1000))
    separador = "." if formato == "vtt" else ","
    return "%02d:%02d:%02d%s%03d" % (horas, minutos, inteiro, separador, milis)


def quebrar(texto):
    palavras, linhas, atual = texto.split(), [], ""
    for palavra in palavras:
        if len(atual) + len(palavra) + 1 > MAX_CARACTERES and atual:
            linhas.append(atual)
            atual = palavra
        else:
            atual = ("%s %s" % (atual, palavra)).strip()
    if atual:
        linhas.append(atual)
    return linhas[:2]  # nunca mais de 2 linhas na tela


def do_roteiro(caminho):
    """Distribui as falas de cada bloco uniformemente dentro do bloco."""
    with open(caminho, encoding="utf-8") as fh:
        linhas = fh.read().splitlines()

    blocos, atual = [], None
    dentro_yaml = False
    for linha in linhas:
        if linha.strip() == "---":
            dentro_yaml = not dentro_yaml
            continue
        if dentro_yaml:
            continue
        marcador = TEMPO.match(linha)
        if marcador:
            atual = {
                "inicio": segundos(marcador.group(1)),
                "fim": segundos(marcador.group(2)),
                "falas": [],
            }
            blocos.append(atual)
            continue
        achado = FALA.match(ACAO.sub("", linha))
        if achado and atual is not None:
            texto = achado.group(2).strip()
            if texto:
                atual["falas"].append(texto)

    entradas = []
    for bloco in blocos:
        if not bloco["falas"]:
            continue
        duracao = (bloco["fim"] - bloco["inicio"]) / float(len(bloco["falas"]))
        for i, texto in enumerate(bloco["falas"]):
            inicio = bloco["inicio"] + i * duracao
            entradas.append((inicio, inicio + duracao, texto))
    return entradas


def do_karaoke(caminho):
    entradas = []
    if not os.path.isfile(caminho):
        return entradas
    marcas = []
    with open(caminho, encoding="utf-8") as fh:
        for linha in fh:
            achado = KARAOKE.match(linha.strip())
            if achado:
                h, m, s, d, texto = achado.groups()
                marcas.append((int(h) * 3600 + int(m) * 60 + int(s) + int(d) / 10.0, texto))
    for i, (inicio, texto) in enumerate(marcas):
        fim = marcas[i + 1][0] if i + 1 < len(marcas) else inicio + 2.0
        entradas.append((inicio, fim, texto))
    return entradas


def escrever(entradas, destino, formato):
    with open(destino, "w", encoding="utf-8") as fh:
        if formato == "vtt":
            fh.write("WEBVTT\n\n")
        for i, (inicio, fim, texto) in enumerate(entradas, 1):
            if formato == "srt":
                fh.write("%d\n" % i)
            fh.write("%s --> %s\n" % (formatar(inicio, formato), formatar(fim, formato)))
            fh.write("\n".join(quebrar(texto)) + "\n\n")


def main():
    parser = argparse.ArgumentParser(description="Legendas do episódio")
    parser.add_argument("--episodio", required=True)
    parser.add_argument("--locale", help="ex.: es (omita para PT-BR)")
    parser.add_argument("--formato", choices=("srt", "vtt"), default="srt")
    args = parser.parse_args()

    pasta = os.path.join(RAIZ, "episodios", args.episodio)
    if args.locale:
        pasta = os.path.join(pasta, "locales", args.locale)

    roteiro = os.path.join(pasta, "01-roteiro.md")
    if not os.path.isfile(roteiro):
        print("não achei %s" % roteiro, file=sys.stderr)
        return 1

    entradas = do_roteiro(roteiro) + do_karaoke(os.path.join(pasta, "03-letra.md"))
    entradas.sort(key=lambda e: e[0])
    if not entradas:
        print("nada para legendar", file=sys.stderr)
        return 1

    sufixo = args.locale or "pt-BR"
    destino = os.path.join(pasta, "legendas-%s.%s" % (sufixo, args.formato))
    escrever(entradas, destino, args.formato)

    longas = [t for _, _, t in entradas if len(t) > MAX_CARACTERES * 2]
    print("%d entradas em %s" % (len(entradas), destino))
    if longas:
        print("AVISO: %d falas passam de 2 linhas na tela — encurte no roteiro" % len(longas))
    print("REVISÃO HUMANA OBRIGATÓRIA antes do upload (docs/compliance.md).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
