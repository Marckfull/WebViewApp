"""
gancho.py — a frase de impacto dos 2 primeiros segundos.

Por que isso é a parte mais importante para a retenção?
    No Shorts/TikTok, a decisão de continuar assistindo acontece em ~1,5 s.
    Se nesse tempo a pessoa não entendeu o que ela vai ganhar assistindo, ela
    desliza para o próximo vídeo. Então o gancho precisa:
        1. aparecer IMEDIATAMENTE (frame 0), sem esperar a fala;
        2. ser enorme e legível de relance;
        3. prometer algo específico ("A ROCKSTAR CONFIRMOU A DATA?!") em vez
           de algo genérico ("veja esse vídeo sobre GTA").

Este módulo tem duas partes:
    • criar_frase()      → decide O QUE escrever (modelos prontos ou IA)
    • eventos_gancho()   → desenha na tela (linhas ASS com animação)
"""

from __future__ import annotations

import random
import re

from .legendas import _escapar_ass, _normalizar, palavras_chave_do_idioma
from .utils import cor_para_ass, para_tempo_ass


# ============================================================================
#  1) O TEXTO DO GANCHO
# ============================================================================

def criar_frase(assunto: str, cfg, idioma: str = "pt", primeira_fala: str = "") -> str:
    """
    Devolve a frase de gancho.

    Ordem de preferência:
        1. IA (se ligada no config) — melhor qualidade, ~US$ 0,01/vídeo
        2. modelos prontos do config.yaml — grátis, funciona bem

    'primeira_fala' é o começo da transcrição do corte; usamos para o modelo
    de IA entender o contexto do trecho. Sem IA, ele é ignorado.
    """
    if cfg.pegar("ia.ativa", False):
        frase = _frase_com_ia(assunto, cfg, idioma, primeira_fala)
        if frase:
            return _limpar(frase, cfg)

    chave = "gancho.modelos_en" if str(idioma).startswith("en") else "gancho.modelos_pt"
    modelos = cfg.pegar(chave, []) or ["{assunto}"]
    frase = random.choice(list(modelos)).replace("{assunto}", assunto.strip())
    return _limpar(frase, cfg)


def _limpar(frase: str, cfg) -> str:
    """Tira espaços sobrando, corta se ficou comprida demais e aplica MAIÚSCULAS."""
    texto = re.sub(r"\s+", " ", str(frase)).strip().strip('"')
    if len(texto) > 70:  # gancho comprido demais não é lido em 2 segundos
        texto = texto[:67].rstrip() + "..."
    if cfg.pegar("gancho.maiusculas", True):
        texto = texto.upper()
    return texto


def _frase_com_ia(assunto: str, cfg, idioma: str, primeira_fala: str) -> str:
    """
    Gera o gancho com IA (opcional). Se algo falhar, devolve "" e o sistema
    cai automaticamente nos modelos prontos — nunca trava o pipeline por isso.

    Custo aproximado: US$ 0,003 a 0,01 por chamada (é um texto minúsculo).
    """
    provedor = str(cfg.pegar("ia.provedor", "anthropic")).lower()
    chave = str(cfg.pegar("ia.chave_api", "") or "")
    if not chave:
        return ""

    lingua = "inglês" if str(idioma).startswith("en") else "português do Brasil"
    instrucao = (
        f"Você escreve ganchos de vídeos curtos sobre GTA. Escreva UMA frase em "
        f"{lingua}, no máximo 8 palavras, TODA EM MAIÚSCULAS, que provoque "
        f"curiosidade imediata sobre: {assunto}. "
        f"Trecho da fala do vídeo: \"{primeira_fala[:400]}\". "
        f"Não use aspas. Não invente fatos: se não houver confirmação, use "
        f"pergunta. Responda só a frase."
    )

    try:
        if provedor == "anthropic":
            import anthropic  # type: ignore

            cliente = anthropic.Anthropic(api_key=chave)
            resposta = cliente.messages.create(
                model=str(cfg.pegar("ia.modelo", "claude-sonnet-4-5")),
                max_tokens=60,
                messages=[{"role": "user", "content": instrucao}],
            )
            return resposta.content[0].text.strip()

        if provedor == "openai":
            from openai import OpenAI  # type: ignore

            cliente = OpenAI(api_key=chave)
            resposta = cliente.chat.completions.create(
                model=str(cfg.pegar("ia.modelo", "gpt-4o-mini")),
                max_tokens=60,
                messages=[{"role": "user", "content": instrucao}],
            )
            return (resposta.choices[0].message.content or "").strip()
    except Exception as erro:  # sem internet, chave errada, cota... tanto faz
        print(f"⚠️  IA indisponível ({erro}). Usando modelo pronto de gancho.")
    return ""


# ============================================================================
#  2) O DESENHO NA TELA
# ============================================================================

def _quebrar_frase(frase: str, tamanho_fonte: int, largura_util: float) -> list[str]:
    """
    Divide a frase em até 3 linhas curtas, para o texto ficar grande e centrado
    em vez de virar uma linha fininha atravessando a tela.
    """
    palavras = frase.split()
    if not palavras:
        return [""]

    max_caracteres = max(8, int(largura_util / (tamanho_fonte * 0.72)))
    linhas: list[str] = []
    atual = ""
    for palavra in palavras:
        candidata = f"{atual} {palavra}".strip()
        if len(candidata) <= max_caracteres or not atual:
            atual = candidata
        else:
            linhas.append(atual)
            atual = palavra
    if atual:
        linhas.append(atual)

    # se passou de 3 linhas, junta as sobras na última (evita ocupar meia tela)
    if len(linhas) > 3:
        linhas = linhas[:2] + [" ".join(linhas[2:])]
    return linhas


def eventos_gancho(frase: str, cfg, idioma: str = "pt", inicio: float = 0.0) -> list[str]:
    """
    Cria as linhas "Dialogue:" do ASS que desenham o gancho.

    Efeito visual: o texto entra "estourando" (cresce de 55% para 105% e volta
    a 100%), com tarja escura atrás para garantir leitura, e some com um fade.
    As palavras de impacto (GTA 6, VAZOU, CONFIRMADO...) saem em amarelo.
    """
    gan = cfg.pegar("gancho", {})
    largura = int(cfg.pegar("video.largura", 1080))
    altura = int(cfg.pegar("video.altura", 1920))

    duracao = float(gan.get("duracao", 2.2))
    tamanho_fonte = int(gan.get("tamanho_fonte", 110))
    cor_texto = cor_para_ass(gan.get("cor_texto", "#FFFFFF"))
    cor_destaque = cor_para_ass(gan.get("cor_destaque", "#FFE100"))

    pos_x = largura // 2
    pos_y = int(altura * float(gan.get("posicao_vertical", 0.30)))
    largura_util = largura * 0.86

    chaves = {_normalizar(k) for k in palavras_chave_do_idioma(cfg, idioma)}

    linhas_texto = _quebrar_frase(frase, tamanho_fonte, largura_util)
    linhas_montadas = []
    for linha in linhas_texto:
        pedacos = []
        for palavra in linha.split():
            crua = _normalizar(palavra)
            # destaca palavra-chave OU palavra escrita em MAIÚSCULAS na frase
            destacar = crua in chaves or (len(crua) > 3 and palavra.isupper()
                                          and not gan.get("maiusculas", True))
            cor = cor_destaque if destacar else cor_texto
            pedacos.append(f"{{\\c{cor}}}{_escapar_ass(palavra)}")
        linhas_montadas.append(" ".join(pedacos))

    corpo = "\\N".join(linhas_montadas)

    animacao = (
        f"\\an5\\pos({pos_x},{pos_y})"
        f"\\fscx55\\fscy55"
        f"\\t(0,140,\\fscx106\\fscy106)"    # estoura
        f"\\t(140,260,\\fscx100\\fscy100)"  # assenta
        f"\\fad(0,180)"                      # some suavemente no fim
    )

    return [
        f"Dialogue: 1,{para_tempo_ass(inicio)},{para_tempo_ass(inicio + duracao)},"
        f"Gancho,,0,0,0,,{{{animacao}}}{corpo}"
    ]


def assunto_a_partir_do_texto(texto: str, idioma: str = "pt") -> str:
    """
    Plano B para descobrir o "assunto" do corte quando ele não veio de fora:
    procura menções a GTA 6 / GTA 5 no texto falado.
    """
    cru = _normalizar(texto)
    if "GTA 6" in cru or "GTA VI" in cru or "GTA6" in cru:
        return "GTA 6"
    if "GTA 5" in cru or "GTA V" in cru or "GTA5" in cru:
        return "GTA 5"
    return "GTA 6"
