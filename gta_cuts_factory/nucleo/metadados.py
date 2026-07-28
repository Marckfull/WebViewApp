"""
metadados.py — título, descrição e hashtags de cada vídeo.

O que é gerado, sempre no idioma do vídeo:

    TÍTULO     → chamativo, mas honesto (se não há confirmação, vira pergunta)
    DESCRIÇÃO  → resumo + CRÉDITO DA FONTE + aviso de rumor + hashtags
    HASHTAGS   → mistura de amplas (#gta6, #shorts) com específicas do tópico
    TAGS       → palavras-chave do YouTube (campo separado do upload)

Duas regras que o módulo aplica sozinho e que protegem o canal:

    1. CRÉDITO OBRIGATÓRIO
       O link do vídeo-fonte entra na descrição. Não é licença, mas é boa-fé —
       e é o que muitos canais pedem para liberar cortes.

    2. CLASSIFICAÇÃO HONESTA
       O título recebe uma etiqueta do tipo de conteúdo ("Notícia",
       "Cutscene Narrada", "Gameplay"). Isso ajuda o YouTube a classificar o
       vídeo direito e evita restrição de idade por engano.

E uma regra de conteúdo: nada de prometer o que o vídeo não mostra. Clickbait
mentiroso derruba a retenção, gera dislike e é caminho para "conteúdo enganoso".
"""

from __future__ import annotations

import re
import unicodedata
from dataclasses import dataclass, field

from .utils import log


# Limites das plataformas (2026). Ficam aqui em um lugar só, para você ajustar
# se a plataforma mudar.
LIMITE_TITULO_YOUTUBE = 100
LIMITE_TITULO_CONFORTAVEL = 65     # acima disso o celular corta o título
LIMITE_DESCRICAO_YOUTUBE = 5000
LIMITE_LEGENDA_TIKTOK = 2200


@dataclass
class Metadados:
    """Tudo que acompanha o vídeo na hora de publicar."""

    titulo: str
    descricao: str
    hashtags: list[str] = field(default_factory=list)
    tags: list[str] = field(default_factory=list)
    idioma: str = "pt"
    avisos: list[str] = field(default_factory=list)
    origem: str = "modelo"

    @property
    def legenda_tiktok(self) -> str:
        """No TikTok tudo é uma coisa só: texto curto + hashtags."""
        texto = f"{self.titulo} {' '.join(self.hashtags)}"
        return texto[:LIMITE_LEGENDA_TIKTOK].strip()

    def para_dict(self) -> dict:
        return {"titulo": self.titulo, "descricao": self.descricao,
                "hashtags": self.hashtags, "tags": self.tags,
                "idioma": self.idioma, "avisos": self.avisos,
                "origem": self.origem, "legenda_tiktok": self.legenda_tiktok}


# ============================================================================
#  TEXTO
# ============================================================================

def _limpar(texto: str) -> str:
    return re.sub(r"\s+", " ", str(texto)).strip()


def _sem_acento(texto: str) -> str:
    normalizado = unicodedata.normalize("NFKD", str(texto))
    return "".join(c for c in normalizado if not unicodedata.combining(c))


def _titulo_bonito(texto: str) -> str:
    """
    Deixa o título apresentável: primeira letra maiúscula, sem gritar.

    TÍTULO TODO EM MAIÚSCULO parece spam e é penalizado. Palavra solta em
    maiúscula (VAZOU, CONFIRMADO) é permitida e ajuda — o exagero é o problema.
    """
    texto = _limpar(texto)
    if not texto:
        return texto

    palavras = texto.split()
    gritadas = [p for p in palavras if len(p) > 3 and p.isupper()]
    if len(gritadas) > 2:
        # mantém só as duas primeiras palavras gritadas, o resto vira normal
        manter = set(gritadas[:2])
        palavras = [p if (p in manter or not p.isupper() or len(p) <= 3)
                    else p.capitalize() for p in palavras]
        texto = " ".join(palavras)

    return texto[0].upper() + texto[1:]


def etiqueta_do_modo(modo: str, idioma: str) -> str:
    """
    A etiqueta honesta do tipo de conteúdo, que ajuda a classificação.

        A_corte + fala   → "Análise" / "Discussion"
        B_narrado        → "Notícia" / "News"
        gameplay         → "Gameplay"
    """
    tabela = {
        "pt": {"A_corte": "Análise", "B_narrado": "Notícia",
               "gameplay": "Gameplay", "cutscene": "Cutscene Narrada"},
        "en": {"A_corte": "Discussion", "B_narrado": "News",
               "gameplay": "Gameplay", "cutscene": "Narrated Cutscene"},
    }
    lingua = "en" if str(idioma).startswith("en") else "pt"
    return tabela[lingua].get(modo, tabela[lingua]["A_corte"])


# ============================================================================
#  HASHTAGS
# ============================================================================

def _virar_hashtag(termo: str) -> str:
    """"data de lançamento" → "#datadelancamento" """
    limpo = re.sub(r"[^\w\s]", "", _sem_acento(termo)).lower()
    return "#" + "".join(limpo.split())


def montar_hashtags(assunto: str, cfg, idioma: str, limite: int | None = None) -> list[str]:
    """
    Mistura hashtags AMPLAS (alcance) com ESPECÍFICAS do tópico (público certo).

    Só amplas = você compete com o mundo inteiro. Só específicas = ninguém
    procura. A mistura é o que funciona.
    """
    lingua = "en" if str(idioma).startswith("en") else "pt"
    limite = limite or int(cfg.pegar("metadados.max_hashtags", 6))

    amplas = list(cfg.pegar(f"metadados.hashtags_base_{lingua}", []) or [])
    hashtags: list[str] = []

    for tag in amplas:
        tag = tag if str(tag).startswith("#") else f"#{tag}"
        if tag.lower() not in [h.lower() for h in hashtags]:
            hashtags.append(tag)

    # específicas: tiradas das palavras do próprio assunto
    palavras = [p for p in re.findall(r"\w+", _sem_acento(assunto).lower())
                if len(p) > 2 and p not in {"gta", "the", "que", "com", "dos", "das"}]
    if palavras:
        especifica = _virar_hashtag(" ".join(palavras[:3]))
        if len(especifica) > 3 and especifica.lower() not in [h.lower() for h in hashtags]:
            hashtags.append(especifica)
    for palavra in palavras[:2]:
        tag = _virar_hashtag(palavra)
        if len(tag) > 4 and tag.lower() not in [h.lower() for h in hashtags]:
            hashtags.append(tag)

    return hashtags[:limite]


def montar_tags(assunto: str, cfg, idioma: str, modo: str) -> list[str]:
    """
    Tags do YouTube (campo separado, não aparece para o público).

    Aqui vale ser descritivo e honesto: é isso que ajuda o YouTube a entender
    do que o vídeo trata e a não classificar errado.
    """
    lingua = "en" if str(idioma).startswith("en") else "pt"
    tags = ["GTA 6", "GTA VI", "GTA 5", "Rockstar Games",
            etiqueta_do_modo(modo, idioma)]
    tags += [t for t in (cfg.pegar(f"metadados.tags_base_{lingua}", []) or [])]

    palavras = _limpar(assunto)
    if palavras:
        tags.append(palavras)

    # sem repetidos, mantendo a ordem
    vistos, resultado = set(), []
    for tag in tags:
        chave = _sem_acento(str(tag)).lower().strip()
        if chave and chave not in vistos:
            vistos.add(chave)
            resultado.append(str(tag).strip())
    return resultado[:15]


# ============================================================================
#  TÍTULO
# ============================================================================

def montar_titulo(assunto: str, cfg, idioma: str, modo: str,
                  confirmado: bool = False, gancho: str = "") -> str:
    """
    Monta o título.

    'confirmado' é a chave da honestidade: sem confirmação oficial, o título
    sai em forma de PERGUNTA ("A Rockstar confirmou X?") em vez de afirmação.
    Prometer no título o que o vídeo não entrega é o jeito mais rápido de
    perder a audiência que você acabou de conquistar.
    """
    lingua = "en" if str(idioma).startswith("en") else "pt"
    chave = "afirmativos" if confirmado else "perguntas"
    modelos = list(cfg.pegar(f"metadados.titulos_{chave}_{lingua}", []) or [])

    if not modelos:
        modelos = ["{assunto}"] if confirmado else ["{assunto}?"]

    # escolhe um modelo de forma estável (o mesmo assunto gera o mesmo título)
    modelo = modelos[abs(hash(_sem_acento(assunto).lower())) % len(modelos)]
    titulo = modelo.replace("{assunto}", _limpar(assunto))

    # A etiqueta de classificação ("Análise", "Notícia") ajuda o YouTube — mas
    # não a ponto de estourar o título. Tentamos a versão completa, depois a
    # curta, e se nem assim couber, deixamos de fora: as TAGS já classificam.
    etiqueta = etiqueta_do_modo(modo, idioma)
    if etiqueta.lower() not in titulo.lower():
        # se o título já fala em GTA 6, repetir o nome só gasta caractere
        completa = (f"{titulo} | {etiqueta}" if "gta 6" in titulo.lower()
                    else f"{titulo} | {etiqueta} GTA 6")
        for tentativa in (completa, f"{titulo} | {etiqueta}"):
            if len(tentativa) <= LIMITE_TITULO_CONFORTAVEL:
                titulo = tentativa
                break

    return _titulo_bonito(titulo)


# ============================================================================
#  DESCRIÇÃO
# ============================================================================

def montar_descricao(assunto: str, cfg, idioma: str, modo: str,
                     credito: str = "", resumo: str = "",
                     hashtags: list[str] | None = None,
                     confirmado: bool = False) -> str:
    """
    Monta a descrição, com o crédito da fonte no lugar de destaque.

    Ordem pensada para o YouTube: as duas primeiras linhas são as que aparecem
    antes do "mostrar mais", então o resumo vem primeiro; crédito logo depois.
    """
    lingua = "en" if str(idioma).startswith("en") else "pt"
    partes: list[str] = []

    if resumo:
        partes.append(_limpar(resumo)[:300])
    else:
        modelo = str(cfg.pegar(f"metadados.resumo_padrao_{lingua}",
                               "{assunto}")).replace("{assunto}", assunto)
        partes.append(_limpar(modelo))

    if not confirmado and cfg.pegar("metadados.incluir_aviso_rumor", True):
        partes.append(str(cfg.pegar(f"metadados.aviso_rumor_{lingua}", "")).strip())

    if credito and cfg.pegar("metadados.incluir_credito", True):
        rotulo = "CRÉDITO DA FONTE" if lingua == "pt" else "SOURCE CREDIT"
        partes.append(f"— {rotulo} —\n{_limpar_multilinha(credito)}")

    assinatura = str(cfg.pegar(f"metadados.assinatura_{lingua}", "")).strip()
    if assinatura:
        partes.append(assinatura)

    aviso_legal = str(cfg.pegar(f"metadados.aviso_legal_{lingua}", "")).strip()
    if aviso_legal:
        partes.append(aviso_legal)

    if hashtags:
        partes.append(" ".join(hashtags))

    descricao = "\n\n".join(p for p in partes if p).strip()
    return descricao[:LIMITE_DESCRICAO_YOUTUBE]


def _limpar_multilinha(texto: str) -> str:
    """Mantém as quebras de linha, mas tira espaços sobrando."""
    return "\n".join(linha.strip() for linha in str(texto).splitlines() if linha.strip())


# ============================================================================
#  GERAÇÃO COMPLETA
# ============================================================================

def gerar(
    assunto: str,
    cfg,
    idioma: str = "pt",
    modo: str = "A_corte",
    credito: str = "",
    texto_falado: str = "",
    gancho: str = "",
    confirmado: bool | None = None,
) -> Metadados:
    """
    Gera título, descrição, hashtags e tags de um vídeo.

    confirmado: se None, o sistema tenta descobrir sozinho lendo o texto falado
    (se a fala diz "confirmou oficialmente", trata como confirmado; se fala de
    rumor/vazamento, trata como não confirmado).
    """
    idioma = "en" if str(idioma).lower().startswith("en") else "pt"
    assunto = _limpar(assunto) or "GTA 6"
    avisos: list[str] = []

    if confirmado is None:
        confirmado = _parece_confirmado(texto_falado, idioma)

    if cfg.pegar("ia.ativa", False):
        gerado = _titulo_com_ia(assunto, cfg, idioma, modo, texto_falado, confirmado)
    else:
        gerado = ""

    titulo = gerado or montar_titulo(assunto, cfg, idioma, modo, confirmado, gancho)
    origem = "ia" if gerado else "modelo"

    if len(titulo) > LIMITE_TITULO_YOUTUBE:
        titulo = titulo[:LIMITE_TITULO_YOUTUBE - 1].rstrip() + "…"
        avisos.append("O título passou de 100 caracteres e foi cortado.")
    if len(titulo) > LIMITE_TITULO_CONFORTAVEL:
        avisos.append(
            f"Título com {len(titulo)} caracteres: no celular ele aparece "
            f"cortado por volta de {LIMITE_TITULO_CONFORTAVEL}. "
            f"Considere encurtar antes de publicar."
        )

    hashtags = montar_hashtags(assunto, cfg, idioma)
    tags = montar_tags(assunto, cfg, idioma, modo)

    if not credito and cfg.pegar("seguranca.exigir_credito_fonte", True):
        avisos.append(
            "Sem crédito da fonte! Se o vídeo veio do canal de outra pessoa, "
            "coloque o link antes de publicar."
        )

    # A primeira linha da descrição é a que aparece antes do "mostrar mais":
    # tem que ser uma frase inteira, e não um pedaço solto da fala.
    descricao = montar_descricao(assunto, cfg, idioma, modo, credito,
                                 resumo="", hashtags=hashtags, confirmado=confirmado)

    return Metadados(titulo=titulo, descricao=descricao, hashtags=hashtags,
                     tags=tags, idioma=idioma, avisos=avisos, origem=origem)


AFIRMACOES = {
    "pt": ["confirmou oficialmente", "confirmado oficialmente", "anunciou oficialmente",
           "comunicado oficial", "a rockstar confirmou", "anuncio oficial"],
    "en": ["officially confirmed", "confirmed officially", "official announcement",
           "rockstar confirmed", "official statement"],
}


def _parece_confirmado(texto: str, idioma: str) -> bool:
    """
    Tenta descobrir se o assunto é fato confirmado ou rumor.

    Na dúvida, devolve False — ou seja, título em forma de pergunta. É melhor
    ser conservador: um "?" a mais não custa nada, um "CONFIRMADO" errado custa
    a confiança do canal.
    """
    alvo = _sem_acento(str(texto)).lower()
    return any(_sem_acento(m) in alvo for m in AFIRMACOES.get(idioma, []))


def _titulo_com_ia(assunto: str, cfg, idioma: str, modo: str,
                   texto: str, confirmado: bool) -> str:
    """Título escrito por IA (opcional, ~US$ 0,005). Se falhar, devolve ""."""
    chave = str(cfg.pegar("ia.chave_api", "") or "")
    if not chave:
        return ""

    lingua = "inglês" if idioma == "en" else "português do Brasil"
    regra_confirmacao = (
        "O fato É confirmado oficialmente: pode afirmar."
        if confirmado else
        "O fato NÃO é confirmado: use pergunta ou deixe claro que é rumor. "
        "NUNCA escreva 'confirmado' nem invente data."
    )
    instrucao = (
        f"Escreva UM título para um vídeo curto vertical sobre GTA, em {lingua}, "
        f"sobre: {assunto}.\n"
        f"Trecho do vídeo: \"{texto[:400]}\"\n\n"
        f"REGRAS:\n"
        f"1. No máximo 60 caracteres.\n"
        f"2. {regra_confirmacao}\n"
        f"3. Pode usar UMA palavra em maiúsculas para dar impacto.\n"
        f"4. Sem emoji, sem aspas, sem hashtag.\n"
        f"5. Precisa entregar o que promete.\n"
        f"Responda só o título."
    )

    try:
        provedor = str(cfg.pegar("ia.provedor", "anthropic")).lower()
        if provedor == "anthropic":
            import anthropic  # type: ignore

            cliente = anthropic.Anthropic(api_key=chave)
            resposta = cliente.messages.create(
                model=str(cfg.pegar("ia.modelo", "claude-sonnet-4-5")),
                max_tokens=80,
                messages=[{"role": "user", "content": instrucao}],
            )
            return _limpar(resposta.content[0].text).strip('"')
        if provedor == "openai":
            from openai import OpenAI  # type: ignore

            cliente = OpenAI(api_key=chave)
            resposta = cliente.chat.completions.create(
                model=str(cfg.pegar("ia.modelo", "gpt-4o-mini")),
                max_tokens=80,
                messages=[{"role": "user", "content": instrucao}],
            )
            return _limpar(resposta.choices[0].message.content or "").strip('"')
    except Exception as erro:
        log(f"IA indisponível para o título ({erro}). Usando modelo pronto.", "aviso")
    return ""


def salvar_para_tiktok(metadados: Metadados, video: str, pasta) -> None:
    """
    Salva um .txt ao lado do vídeo com a legenda pronta para colar no TikTok.

    O TikTok não deixa publicar 100% automático sem aprovação de parceiro
    (veja o README), então o fluxo é: arrastar o vídeo e colar este texto.
    """
    from pathlib import Path

    pasta = Path(pasta)
    pasta.mkdir(parents=True, exist_ok=True)
    destino = pasta / (Path(video).stem + "_tiktok.txt")
    destino.write_text(
        f"{metadados.legenda_tiktok}\n\n"
        f"--- vídeo: {video}\n"
        f"--- gerado pelo GTA Cuts Factory\n",
        encoding="utf-8",
    )
    return destino


def resumo(metadados: Metadados) -> str:
    """Texto para conferir no terminal."""
    return (
        f"   TÍTULO ({len(metadados.titulo)} caracteres):\n      {metadados.titulo}\n\n"
        f"   HASHTAGS:\n      {' '.join(metadados.hashtags)}\n\n"
        f"   TAGS (YouTube):\n      {', '.join(metadados.tags[:8])}\n\n"
        f"   DESCRIÇÃO:\n" +
        "\n".join(f"      {linha}" for linha in metadados.descricao.splitlines())
    )
