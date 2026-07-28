"""
narracao.py — MODO B: vídeo narrado por você (voz sintética).

Por que este é o modo mais seguro do sistema:
    o áudio é 100% original (voz sintética lendo um roteiro seu) e o vídeo de
    fundo entra como material de APOIO de um conteúdo comentado. É exatamente
    o uso que a Rockstar permite para trailer e cinemática — e o Content ID
    não tem o que reclamar do áudio, porque ele não é de ninguém.

Duas partes:
    1. criar_roteiro() → o TEXTO que vai ser falado
    2. narrar()        → o ÁUDIO, com o tempo de cada palavra (para a legenda)

⚠️ SOBRE INVENTAR NOTÍCIA (leia, é importante):
    Este módulo NÃO inventa fatos. Ele monta o roteiro em cima dos fatos que
    VOCÊ passar (--fatos). Sem fatos, ele gera um roteiro de pergunta aberta,
    deixando claro que nada foi confirmado. Publicar "a Rockstar confirmou"
    quando ninguém confirmou é desinformação: derruba a confiança do canal e
    ainda esbarra na política de conteúdo enganoso do YouTube.
"""

from __future__ import annotations

import asyncio
import random
import re
from dataclasses import dataclass, field
from pathlib import Path

from .legendas import Palavra
from .utils import log


# Velocidade média de leitura da voz sintética (palavras por minuto).
# Serve para estimar quantas palavras cabem na duração desejada.
PALAVRAS_POR_MINUTO = {"pt": 155, "en": 165}


@dataclass
class Roteiro:
    """O texto que a voz vai ler."""

    assunto: str
    idioma: str
    blocos: list[str] = field(default_factory=list)
    fatos_usados: list[str] = field(default_factory=list)
    avisos: list[str] = field(default_factory=list)
    origem: str = "modelo"   # "modelo" ou "ia"

    @property
    def texto(self) -> str:
        return " ".join(b.strip() for b in self.blocos if b.strip())

    @property
    def palavras(self) -> int:
        return len(self.texto.split())

    def duracao_estimada(self) -> float:
        ppm = PALAVRAS_POR_MINUTO.get(self.idioma[:2], 155)
        return self.palavras / ppm * 60.0

    def para_dict(self) -> dict:
        return {"assunto": self.assunto, "idioma": self.idioma,
                "texto": self.texto, "blocos": self.blocos,
                "fatos_usados": self.fatos_usados, "avisos": self.avisos,
                "origem": self.origem, "palavras": self.palavras}


@dataclass
class Narracao:
    """O áudio pronto, com o tempo de cada palavra."""

    arquivo: Path
    palavras: list[Palavra]
    duracao: float
    voz: str
    provedor: str
    tempos_reais: bool = True   # False = tempos estimados, não medidos


# ============================================================================
#  1) O ROTEIRO
# ============================================================================

MODELOS_ABERTURA = {
    "pt": ["Olha o que apareceu sobre {assunto}.",
           "Tem novidade sobre {assunto}, e eu vou direto ao ponto.",
           "Sobre {assunto}: presta atenção nisso aqui."],
    "en": ["Here's what just showed up about {assunto}.",
           "There's news about {assunto}, and I'll get straight to the point.",
           "About {assunto}: pay attention to this."],
}

CONECTORES = {
    "pt": ["Primeiro:", "Além disso:", "E tem mais:", "Outro ponto:", "Por fim:"],
    "en": ["First:", "On top of that:", "There's more:", "Another point:", "Finally:"],
}

RESSALVA = {
    "pt": ("Vale lembrar: enquanto a Rockstar não confirmar oficialmente, "
           "trate isso como rumor."),
    "en": ("Keep in mind: until Rockstar confirms it officially, "
           "treat this as a rumor."),
}

SEM_FATOS = {
    "pt": ("Até agora, nada foi confirmado oficialmente sobre {assunto}. "
           "O que existe são conversas da comunidade, e é bom separar as duas coisas."),
    "en": ("So far, nothing has been officially confirmed about {assunto}. "
           "What we have is community talk, and it's worth keeping those separate."),
}

FECHO = {
    "pt": ["E você, acredita nisso? Comenta aí.",
           "O que você acha que vem primeiro? Comenta aí.",
           "Se confirmar, muda tudo. Comenta o que você acha."],
    "en": ["Do you buy it? Let me know.",
           "What do you think comes first? Tell me below.",
           "If it's confirmed, everything changes. Let me know what you think."],
}


def _limpar_texto(texto: str) -> str:
    """Tira espaços duplos e deixa o texto pronto para a voz ler."""
    return re.sub(r"\s+", " ", str(texto)).strip()


def _pontuar(frase: str) -> str:
    """
    Garante que a frase termine com pontuação.

    Não é firula: a voz sintética usa o ponto final para dar a pausa. Sem ele,
    as frases saem coladas ("...segundo trailer Além disso") e a narração soa
    apressada e robótica.
    """
    texto = _limpar_texto(frase)
    if texto and texto[-1] not in ".!?…:":
        texto += "."
    return texto


def _cortar_para_duracao(blocos: list[str], idioma: str, segundos: float) -> list[str]:
    """
    Corta o roteiro para caber na duração desejada.

    Corta em BLOCO INTEIRO (nunca no meio de uma frase) — narração cortada na
    metade soa como erro e derruba a retenção.
    """
    ppm = PALAVRAS_POR_MINUTO.get(idioma[:2], 155)
    limite_palavras = int(segundos / 60.0 * ppm)

    resultado, total = [], 0
    for bloco in blocos:
        palavras = len(bloco.split())
        if total + palavras > limite_palavras and resultado:
            break
        resultado.append(bloco)
        total += palavras
    return resultado


def criar_roteiro(
    assunto: str,
    cfg,
    idioma: str = "pt",
    fatos: list[str] | None = None,
    contexto: str = "",
) -> Roteiro:
    """
    Monta o roteiro narrado.

    fatos: lista de informações que VOCÊ apurou (uma frase cada). É daqui que
    o conteúdo sai — o sistema não inventa nada.

    Se a IA estiver ligada no config, ela escreve o texto (mais natural), mas
    continua presa aos MESMOS fatos: a instrução proíbe inventar.
    """
    idioma = "en" if str(idioma).lower().startswith("en") else "pt"
    fatos = [_limpar_texto(f) for f in (fatos or []) if str(f).strip()]
    duracao_alvo = float(cfg.pegar("narracao.duracao_alvo", 75))

    if cfg.pegar("ia.ativa", False):
        roteiro = _roteiro_com_ia(assunto, cfg, idioma, fatos, contexto, duracao_alvo)
        if roteiro:
            return roteiro

    # ------------------------------------------------ roteiro por modelo (grátis)
    blocos = [random.choice(MODELOS_ABERTURA[idioma]).format(assunto=assunto)]
    avisos: list[str] = []

    if fatos:
        conectores = CONECTORES[idioma]
        for i, fato in enumerate(fatos):
            ligacao = conectores[min(i, len(conectores) - 1)]
            blocos.append(_pontuar(f"{ligacao} {fato}"))
    else:
        blocos.append(SEM_FATOS[idioma].format(assunto=assunto))
        avisos.append(
            "Roteiro genérico: você não passou nenhum fato (--fatos). O vídeo vai "
            "falar de forma vaga. Para um vídeo bom, passe 2 a 4 informações "
            "que você apurou."
        )

    blocos.append(RESSALVA[idioma])
    blocos.append(random.choice(FECHO[idioma]))

    blocos = [_pontuar(b) for b in _cortar_para_duracao(blocos, idioma, duracao_alvo)]

    roteiro = Roteiro(assunto=assunto, idioma=idioma, blocos=blocos,
                      fatos_usados=fatos, avisos=avisos, origem="modelo")
    _avisar_se_curto(roteiro, duracao_alvo)
    return roteiro


def _avisar_se_curto(roteiro: Roteiro, duracao_alvo: float) -> None:
    """
    Avisa quando o roteiro vai render bem menos que o alvo.

    Um vídeo narrado de 25 segundos não é um problema técnico, mas rende menos
    no algoritmo do que um de 60–90 segundos — e o jeito de alongar é passar
    mais fatos, não encher de enrolação.
    """
    estimada = roteiro.duracao_estimada()
    if estimada < duracao_alvo * 0.6:
        roteiro.avisos.append(
            f"Roteiro curto: ~{estimada:.0f}s contra os {duracao_alvo:.0f}s do "
            f"alvo. Passe mais 1 ou 2 fatos (--fatos) para o vídeo render melhor."
        )


def _roteiro_com_ia(assunto: str, cfg, idioma: str, fatos: list[str],
                    contexto: str, duracao_alvo: float) -> Roteiro | None:
    """
    Escreve o roteiro com IA (opcional, ~US$ 0,01 por vídeo).

    A instrução é rígida de propósito: usar SÓ os fatos fornecidos e marcar
    rumor como rumor. Se a IA falhar, devolve None e o sistema usa o modelo
    pronto — nunca trava por causa disso.
    """
    chave = str(cfg.pegar("ia.chave_api", "") or "")
    if not chave:
        return None

    ppm = PALAVRAS_POR_MINUTO.get(idioma, 155)
    limite = int(duracao_alvo / 60.0 * ppm)
    lingua = "inglês" if idioma == "en" else "português do Brasil"
    lista_fatos = "\n".join(f"- {f}" for f in fatos) or "(nenhum fato fornecido)"

    instrucao = (
        f"Escreva um roteiro de narração para um vídeo curto vertical sobre GTA, "
        f"em {lingua}, com no máximo {limite} palavras, sobre: {assunto}.\n\n"
        f"FATOS DISPONÍVEIS (use SOMENTE estes):\n{lista_fatos}\n\n"
        f"Contexto extra: {contexto[:500]}\n\n"
        f"REGRAS OBRIGATÓRIAS:\n"
        f"1. NÃO invente nenhum fato, data, número ou declaração.\n"
        f"2. Se algo não estiver confirmado, diga que é rumor.\n"
        f"3. Se não houver fatos, escreva um roteiro de pergunta aberta, deixando "
        f"claro que nada foi confirmado.\n"
        f"4. Comece com uma frase que prenda a atenção em 2 segundos.\n"
        f"5. Linguagem falada, frases curtas, sem emojis e sem marcações.\n"
        f"6. Termine com uma pergunta curta para o público.\n"
        f"Responda apenas com o texto da narração."
    )

    try:
        provedor = str(cfg.pegar("ia.provedor", "anthropic")).lower()
        if provedor == "anthropic":
            import anthropic  # type: ignore

            cliente = anthropic.Anthropic(api_key=chave)
            resposta = cliente.messages.create(
                model=str(cfg.pegar("ia.modelo", "claude-sonnet-4-5")),
                max_tokens=700,
                messages=[{"role": "user", "content": instrucao}],
            )
            texto = resposta.content[0].text.strip()
        elif provedor == "openai":
            from openai import OpenAI  # type: ignore

            cliente = OpenAI(api_key=chave)
            resposta = cliente.chat.completions.create(
                model=str(cfg.pegar("ia.modelo", "gpt-4o-mini")),
                max_tokens=700,
                messages=[{"role": "user", "content": instrucao}],
            )
            texto = (resposta.choices[0].message.content or "").strip()
        else:
            return None
    except Exception as erro:
        log(f"IA indisponível para o roteiro ({erro}). Usando modelo pronto.", "aviso")
        return None

    # separa em blocos por frase, para poder cortar sem quebrar no meio
    blocos = [b.strip() for b in re.split(r"(?<=[.!?])\s+", _limpar_texto(texto)) if b.strip()]
    blocos = _cortar_para_duracao(blocos, idioma, duracao_alvo)

    avisos = [] if fatos else [
        "Roteiro escrito pela IA sem fatos fornecidos: confira o texto antes de "
        "publicar."
    ]
    roteiro = Roteiro(assunto=assunto, idioma=idioma, blocos=blocos,
                      fatos_usados=fatos, avisos=avisos, origem="ia")
    _avisar_se_curto(roteiro, duracao_alvo)
    return roteiro


# ============================================================================
#  2) A VOZ
# ============================================================================

def estimar_tempos_das_palavras(texto: str, duracao: float) -> list[Palavra]:
    """
    Distribui as palavras no tempo quando a voz não informa os tempos reais.

    Palavra maior demora mais para ser falada — por isso dividimos pelo número
    de CARACTERES e não pelo número de palavras. Não é perfeito como o tempo
    medido, mas fica muito perto para legenda.
    """
    partes = [p for p in _limpar_texto(texto).split() if p]
    if not partes or duracao <= 0:
        return []

    total_caracteres = sum(len(p) + 1 for p in partes)
    palavras: list[Palavra] = []
    momento = 0.0
    for parte in partes:
        fatia = (len(parte) + 1) / total_caracteres * duracao
        palavras.append(Palavra(parte, round(momento, 3), round(momento + fatia * 0.95, 3)))
        momento += fatia
    return palavras


async def _falar_com_edge(texto: str, voz: str, destino: Path) -> list[Palavra]:
    """
    Gera o áudio com o Edge-TTS (GRÁTIS) e captura o tempo REAL de cada palavra.

    O Edge-TTS avisa a posição exata de cada palavra enquanto sintetiza
    ("WordBoundary"). Isso é ouro para a legenda karaokê: os tempos vêm
    perfeitos, sem precisar transcrever o áudio depois com o Whisper.
    """
    import edge_tts  # type: ignore

    comunicacao = edge_tts.Communicate(texto, voz)
    palavras: list[Palavra] = []

    with open(destino, "wb") as arquivo:
        async for pedaco in comunicacao.stream():
            if pedaco["type"] == "audio":
                arquivo.write(pedaco["data"])
            elif pedaco["type"] == "WordBoundary":
                # os tempos vêm em unidades de 100 nanossegundos
                inicio = pedaco["offset"] / 10_000_000
                duracao = pedaco["duration"] / 10_000_000
                palavras.append(Palavra(
                    str(pedaco.get("text", "")).strip(),
                    round(inicio, 3), round(inicio + duracao, 3),
                ))
    return palavras


def _falar_com_elevenlabs(texto: str, cfg, destino: Path) -> None:
    """
    Gera o áudio com o ElevenLabs (pago, ~US$ 0,02–0,05 por vídeo).

    A voz é mais natural, mas a API não informa o tempo de cada palavra —
    então os tempos da legenda são estimados (veja estimar_tempos_das_palavras).
    """
    from elevenlabs.client import ElevenLabs  # type: ignore

    cliente = ElevenLabs(api_key=str(cfg.pegar("tts.chave_elevenlabs", "")))
    audio = cliente.text_to_speech.convert(
        voice_id=str(cfg.pegar("tts.voz_elevenlabs", "")),
        model_id="eleven_multilingual_v2",
        text=texto,
    )
    with open(destino, "wb") as arquivo:
        for pedaco in audio:
            arquivo.write(pedaco)


def _falar_de_mentira(roteiro: Roteiro, cfg, destino: Path) -> float:
    """
    Provedor "teste": gera um áudio MUDO com a duração que a narração teria.

    Para que serve: testar o Modo B inteiro (roteiro → legenda → render 9:16)
    sem internet e sem gastar nada, conferindo se o ffmpeg, a legenda e o vídeo
    de fundo estão certos. O vídeo sai SEM VOZ — é só para teste, nunca para
    publicar.
    """
    from .render import achar_ffmpeg as _achar, rodar as _rodar

    duracao = max(3.0, roteiro.duracao_estimada())
    ffmpeg = _achar(str(cfg.pegar("ffmpeg_caminho", "ffmpeg")))
    _rodar(
        [ffmpeg, "-y", "-loglevel", "error", "-f", "lavfi",
         "-i", f"anullsrc=channel_layout=stereo:sample_rate=48000",
         "-t", f"{duracao:.2f}", "-c:a", "libmp3lame", str(destino)],
        descricao="gerar áudio de teste (mudo)",
    )
    return duracao


def narrar(roteiro: Roteiro, cfg, destino: str | Path) -> Narracao:
    """
    Transforma o roteiro em áudio narrado.

    Provedores (config.yaml → tts.provedor):
        "edge"       → GRÁTIS, vozes brasileiras boas, com tempo por palavra
        "elevenlabs" → pago, voz mais natural, tempos estimados
    """
    from .render import duracao_do_audio  # importado aqui para evitar ciclo

    destino = Path(destino)
    destino.parent.mkdir(parents=True, exist_ok=True)
    texto = roteiro.texto
    if not texto.strip():
        raise ValueError("Roteiro vazio: não há o que narrar.")

    provedor = str(cfg.pegar("tts.provedor", "edge")).lower()
    chave_voz = "tts.voz_en" if roteiro.idioma == "en" else "tts.voz_pt"
    voz = str(cfg.pegar(chave_voz, "pt-BR-AntonioNeural"))

    log(f"Gerando narração ({provedor}, voz {voz}, {roteiro.palavras} palavras)...",
        "etapa")

    palavras: list[Palavra] = []
    tempos_reais = True

    if provedor == "teste":
        log("Provedor 'teste': o áudio sai MUDO, só para conferir o resto do "
            "sistema. Não publique este vídeo.", "aviso")
        _falar_de_mentira(roteiro, cfg, destino)
        tempos_reais = False
        voz = "(sem voz — modo teste)"

    elif provedor == "elevenlabs":
        try:
            _falar_com_elevenlabs(texto, cfg, destino)
            tempos_reais = False
            voz = str(cfg.pegar("tts.voz_elevenlabs", "elevenlabs"))
        except Exception as erro:
            raise RuntimeError(
                f"Não consegui gerar a voz no ElevenLabs: {erro}\n"
                "   → confira 'tts.chave_elevenlabs' e 'tts.voz_elevenlabs' no "
                "config.yaml\n"
                "   → ou troque para o Edge-TTS (grátis): tts.provedor: \"edge\""
            ) from erro
    else:
        try:
            palavras = asyncio.run(_falar_com_edge(texto, voz, destino))
        except Exception as erro:
            raise RuntimeError(
                f"Não consegui gerar a voz com o Edge-TTS: {erro}\n"
                "   → o Edge-TTS precisa de internet (o serviço é da Microsoft)\n"
                "   → confira se a voz existe: python -m edge_tts --list-voices\n"
                "   → vozes PT-BR boas: pt-BR-AntonioNeural, pt-BR-FranciscaNeural"
            ) from erro

    if not destino.exists() or destino.stat().st_size == 0:
        raise RuntimeError("A narração não gerou áudio nenhum.")

    duracao = duracao_do_audio(destino, cfg)

    if not palavras:
        palavras = estimar_tempos_das_palavras(texto, duracao)
        tempos_reais = False

    log(f"Narração pronta: {duracao:.1f}s, {len(palavras)} palavras "
        f"({'tempos medidos' if tempos_reais else 'tempos estimados'}).", "ok")

    return Narracao(arquivo=destino, palavras=palavras, duracao=duracao,
                    voz=voz, provedor=provedor, tempos_reais=tempos_reais)


def transcricao_do_roteiro(roteiro: Roteiro, narracao: Narracao) -> dict:
    """
    Monta um "resultado de transcrição" a partir da narração.

    Assim o Modo B usa exatamente os mesmos módulos do Modo A (legenda,
    segurança, metadados) sem precisar de nenhum caso especial.
    """
    return {
        "idioma": roteiro.idioma,
        "texto": roteiro.texto,
        "palavras": [p.para_dict() for p in narracao.palavras],
        "frases": [{"texto": bloco, "inicio": 0.0, "fim": narracao.duracao}
                   for bloco in roteiro.blocos],
        "origem": "narracao_propria",
        "inicio_no_original": 0.0,
    }
