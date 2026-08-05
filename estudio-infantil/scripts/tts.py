#!/usr/bin/env python3
"""Narração (TTS) de um episódio.

Lê as falas de `01-roteiro.md` e a letra de `03-letra.md`, e gera um arquivo de
áudio por fala em `episodios/<EP>/audio/`.

O provedor de TTS é intencionalmente plugável: escolha um com **licença
comercial** para as vozes e guarde o contrato (docs/compliance.md). Implemente
a função `sintetizar()` do adaptador do seu provedor e nada mais muda.

Uso:
    python3 scripts/tts.py --episodio EP001-o-medo-do-escuro
    python3 scripts/tts.py --episodio EP001-o-medo-do-escuro --dry-run
    python3 scripts/tts.py --episodio EP001-... --locale es

Configuração por ambiente (nunca versionada):
    TTS_PROVIDER   nome do adaptador (ex.: "meu-provedor")
    TTS_API_KEY    credencial
"""

import argparse
import json
import os
import re
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# **NOME:** fala   ou   **NOME**: fala   ou   NOME: fala
FALA = re.compile(
    r"^\s*\**([A-ZÀ-Ÿ][A-ZÀ-Ÿ ÇÃÕÁÉÍÓÚÂÊÔ'-]{1,30}?)\**\s*:\**\s*(.+?)\s*$"
)
ACAO = re.compile(r"\[AÇÃO[^\]]*\]")
TEMPO = re.compile(r"^##\s*(\d+:\d{2})\s*[–-]\s*(\d+:\d{2})")


def caminho_episodio(episodio, locale=None):
    base = os.path.join(RAIZ, "episodios", episodio)
    if locale:
        base = os.path.join(base, "locales", locale)
    return base


def carregar_vozes():
    """Mapa personagem → parâmetros de voz, lido da bíblia.

    Mantido simples de propósito: o campo `Voz:` de cada arquivo de personagem
    é a fonte da verdade, para não haver dois lugares descrevendo a mesma voz.
    """
    vozes = {}
    pasta = os.path.join(RAIZ, "biblia", "personagens")
    if not os.path.isdir(pasta):
        return vozes
    for nome in sorted(os.listdir(pasta)):
        if not nome.endswith(".md") or nome.startswith("_"):
            continue
        caminho = os.path.join(pasta, nome)
        with open(caminho, encoding="utf-8") as fh:
            texto = fh.read()
        titulo = re.search(r"^#\s+(.+)$", texto, re.M)
        voz = re.search(r"^\s*-\s*\*\*Voz:\*\*\s*(.+)$", texto, re.M)
        if titulo:
            vozes[titulo.group(1).strip().upper()] = {
                "arquivo": nome,
                "descricao": voz.group(1).strip() if voz else "",
            }
    return vozes


def extrair_falas(caminho):
    """Lista de {bloco, personagem, texto} na ordem do roteiro."""
    with open(caminho, encoding="utf-8") as fh:
        linhas = fh.read().splitlines()

    falas, bloco = [], ""
    dentro_yaml = False
    for linha in linhas:
        if linha.strip() == "---":
            dentro_yaml = not dentro_yaml
            continue
        if dentro_yaml:
            continue
        marcador = TEMPO.match(linha)
        if marcador:
            bloco = "%s-%s" % marcador.groups()
            continue
        achado = FALA.match(ACAO.sub("", linha))
        if achado:
            texto = achado.group(2).strip()
            if texto:
                falas.append(
                    {
                        "bloco": bloco,
                        "personagem": achado.group(1).strip().upper(),
                        "texto": texto,
                    }
                )
    return falas


def sintetizar(texto, voz, destino):
    """ADAPTADOR DO PROVEDOR — implemente aqui.

    Deve gravar um arquivo de áudio em `destino` e devolver o caminho.
    Requisitos não negociáveis do provedor escolhido:
      - licença comercial explícita, incluindo obra derivada e monetização;
      - permissão para conteúdo infantil;
      - contrato arquivado (docs/compliance.md).
    """
    raise NotImplementedError(
        "Configure o adaptador de TTS em scripts/tts.py::sintetizar(). "
        "Rode com --dry-run para validar a extração de falas antes disso."
    )


def main():
    parser = argparse.ArgumentParser(description="Narração de um episódio")
    parser.add_argument("--episodio", required=True, help="ex.: EP001-o-medo-do-escuro")
    parser.add_argument("--locale", help="ex.: es (omita para PT-BR)")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="extrai e lista as falas sem chamar o provedor",
    )
    args = parser.parse_args()

    pasta = caminho_episodio(args.episodio, args.locale)
    roteiro = os.path.join(pasta, "01-roteiro.md")
    if not os.path.isfile(roteiro):
        print("não achei %s" % roteiro, file=sys.stderr)
        return 1

    falas = extrair_falas(roteiro)
    if not falas:
        print("nenhuma fala encontrada em %s" % roteiro, file=sys.stderr)
        print("formato esperado: **NOME:** texto da fala", file=sys.stderr)
        return 1

    vozes = carregar_vozes()
    desconhecidos = sorted({f["personagem"] for f in falas} - set(vozes))

    print("%d falas em %d blocos" % (falas and len(falas), len({f["bloco"] for f in falas})))
    if desconhecidos:
        print("AVISO: sem ficha de voz na bíblia: %s" % ", ".join(desconhecidos))

    if args.dry_run:
        for i, fala in enumerate(falas, 1):
            print("  %03d [%s] %s: %s" % (i, fala["bloco"] or "?", fala["personagem"], fala["texto"][:60]))
        return 0

    destino_dir = os.path.join(pasta, "audio")
    os.makedirs(destino_dir, exist_ok=True)
    manifesto = []
    for i, fala in enumerate(falas, 1):
        destino = os.path.join(destino_dir, "%03d-%s.wav" % (i, fala["personagem"].lower()))
        sintetizar(fala["texto"], vozes.get(fala["personagem"], {}), destino)
        manifesto.append(dict(fala, arquivo=os.path.relpath(destino, RAIZ)))

    with open(os.path.join(destino_dir, "manifesto.json"), "w", encoding="utf-8") as fh:
        json.dump(manifesto, fh, ensure_ascii=False, indent=2)
    print("ok: %d arquivos em %s" % (len(manifesto), destino_dir))
    return 0


if __name__ == "__main__":
    sys.exit(main())
