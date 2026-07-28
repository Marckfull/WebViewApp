"""
banco.py — a memória do sistema (SQLite).

É aqui que o sistema guarda TUDO que já fez, para nunca repetir e para saber em
quais fontes ele pode confiar. Um único arquivo (dados/banco.sqlite), sem
instalar nada — o SQLite já vem dentro do Python.

Cinco coisas são guardadas:

    fontes         → cada canal e sua REPUTAÇÃO (claims, restrições, status)
    videos_fonte   → vídeos que já foram baixados
    trechos        → os pedaços exatos (início/fim) que já viraram vídeo
    assuntos       → temas que já foram publicados
    videos_gerados → a fila de aprovação e a lista de reprovados
    ocorrencias    → histórico de problemas (claim, restrição de idade, remoção)

A regra de ouro: NUNCA repetir um trecho e NUNCA republicar o mesmo assunto.
"""

from __future__ import annotations

import json
import re
import sqlite3
import unicodedata
from dataclasses import dataclass
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any, Iterable

from .utils import log


# Status possíveis de uma fonte, do mais seguro para o mais arriscado
STATUS_VALIDOS = ("oficial", "proprio", "em_teste", "bloqueado")

# Quanto o sistema prefere cada tipo de fonte (usado na Etapa 3, na escolha)
PRIORIDADE_STATUS = {"oficial": 3, "proprio": 3, "em_teste": 1, "bloqueado": -1}


ESQUEMA = """
CREATE TABLE IF NOT EXISTS fontes (
    canal_id          TEXT PRIMARY KEY,
    nome              TEXT NOT NULL,
    url               TEXT,
    status            TEXT NOT NULL DEFAULT 'em_teste',
    peso              INTEGER NOT NULL DEFAULT 5,
    idioma            TEXT,
    videos_gerados    INTEGER NOT NULL DEFAULT 0,
    claims            INTEGER NOT NULL DEFAULT 0,
    restricoes_idade  INTEGER NOT NULL DEFAULT 0,
    remocoes          INTEGER NOT NULL DEFAULT 0,
    observacao        TEXT,
    criado_em         TEXT NOT NULL,
    atualizado_em     TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS videos_fonte (
    id          TEXT PRIMARY KEY,
    titulo      TEXT,
    canal_id    TEXT,
    url         TEXT,
    duracao     REAL,
    baixado_em  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS trechos (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    video_id  TEXT NOT NULL,
    inicio    REAL NOT NULL,
    fim       REAL NOT NULL,
    idioma    TEXT,
    usado_em  TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_trechos_video ON trechos(video_id);

CREATE TABLE IF NOT EXISTS assuntos (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    assunto   TEXT NOT NULL,
    chave     TEXT NOT NULL,
    idioma    TEXT,
    usado_em  TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_assuntos_chave ON assuntos(chave);

CREATE TABLE IF NOT EXISTS videos_gerados (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    arquivo      TEXT,
    titulo       TEXT,
    idioma       TEXT,
    modo         TEXT,
    assunto      TEXT,
    video_id     TEXT,
    canal_id     TEXT,
    inicio       REAL,
    fim          REAL,
    selo         TEXT,
    aprovado     INTEGER NOT NULL DEFAULT 0,
    motivos      TEXT,
    status       TEXT NOT NULL DEFAULT 'fila',
    criado_em    TEXT NOT NULL,
    ficha        TEXT
);

CREATE TABLE IF NOT EXISTS ocorrencias (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    canal_id   TEXT NOT NULL,
    video_id   TEXT,
    tipo       TEXT NOT NULL,
    descricao  TEXT,
    data       TEXT NOT NULL
);
"""


# ============================================================================
#  ABERTURA DO BANCO
# ============================================================================

def conectar(cfg) -> sqlite3.Connection:
    """
    Abre (e cria, na primeira vez) o banco de dados.

    O 'row_factory' faz cada resultado vir como dicionário — fica muito mais
    fácil de ler no código: linha["status"] em vez de linha[3].
    """
    caminho = cfg.caminho("banco")
    conexao = sqlite3.connect(str(caminho))
    conexao.row_factory = sqlite3.Row
    conexao.executescript(ESQUEMA)
    conexao.commit()
    return conexao


def _agora() -> str:
    return datetime.now().isoformat(timespec="seconds")


# ============================================================================
#  FONTES E REPUTAÇÃO
# ============================================================================

@dataclass
class Fonte:
    """Um canal-fonte e sua ficha de reputação."""

    canal_id: str
    nome: str
    status: str
    url: str = ""
    peso: int = 5
    idioma: str = ""
    videos_gerados: int = 0
    claims: int = 0
    restricoes_idade: int = 0
    remocoes: int = 0
    observacao: str = ""

    @property
    def confiavel(self) -> bool:
        """Fonte oficial ou própria: a base mais segura para publicar."""
        return self.status in ("oficial", "proprio")

    @property
    def bloqueada(self) -> bool:
        return self.status == "bloqueado"

    @property
    def problemas(self) -> int:
        return self.claims + self.restricoes_idade + self.remocoes


def _linha_para_fonte(linha: sqlite3.Row) -> Fonte:
    return Fonte(
        canal_id=linha["canal_id"], nome=linha["nome"], status=linha["status"],
        url=linha["url"] or "", peso=linha["peso"], idioma=linha["idioma"] or "",
        videos_gerados=linha["videos_gerados"], claims=linha["claims"],
        restricoes_idade=linha["restricoes_idade"], remocoes=linha["remocoes"],
        observacao=linha["observacao"] or "",
    )


def obter_fonte(conexao: sqlite3.Connection, canal_id: str) -> Fonte | None:
    linha = conexao.execute(
        "SELECT * FROM fontes WHERE canal_id = ?", (canal_id,)
    ).fetchone()
    return _linha_para_fonte(linha) if linha else None


def registrar_fonte(
    conexao: sqlite3.Connection,
    canal_id: str,
    nome: str,
    url: str = "",
    status: str = "em_teste",
    peso: int = 5,
    idioma: str = "",
    observacao: str = "",
) -> Fonte:
    """
    Cadastra um canal (ou atualiza os dados dele, se já existir).

    IMPORTANTE: um canal já cadastrado NUNCA é promovido automaticamente.
    Se ele foi bloqueado por causa de um claim, o status bloqueado é mantido
    mesmo que o canal ainda esteja escrito no seu canais_fontes.yaml.
    Só você pode desbloquear (com desbloquear_fonte()).
    """
    if status not in STATUS_VALIDOS:
        raise ValueError(f"Status inválido: {status} (use um de {STATUS_VALIDOS})")

    existente = obter_fonte(conexao, canal_id)
    if existente is None:
        conexao.execute(
            "INSERT INTO fontes (canal_id, nome, url, status, peso, idioma, "
            "observacao, criado_em, atualizado_em) VALUES (?,?,?,?,?,?,?,?,?)",
            (canal_id, nome, url, status, peso, idioma, observacao, _agora(), _agora()),
        )
        conexao.commit()
        return obter_fonte(conexao, canal_id)  # type: ignore[return-value]

    # canal bloqueado continua bloqueado, aconteça o que acontecer
    novo_status = existente.status if existente.bloqueada else status
    conexao.execute(
        "UPDATE fontes SET nome=?, url=?, status=?, peso=?, idioma=?, "
        "observacao=?, atualizado_em=? WHERE canal_id=?",
        (nome, url, novo_status, peso, idioma, observacao, _agora(), canal_id),
    )
    conexao.commit()
    return obter_fonte(conexao, canal_id)  # type: ignore[return-value]


def sincronizar_canais(conexao: sqlite3.Connection, dados_canais: dict) -> int:
    """
    Lê o seu config/canais_fontes.yaml e joga para dentro do banco.

    Assim você edita canais num arquivo de texto simples, e o sistema mantém a
    reputação de cada um no banco.
    """
    total = 0
    for canal in dados_canais.get("canais", []) or []:
        nome = str(canal.get("nome", "")).strip()
        if not nome:
            continue
        # canal do YouTube usa a URL como identificador; pasta local usa o caminho
        identificador = str(
            canal.get("canal_id") or canal.get("url") or canal.get("pasta_local") or nome
        )
        registrar_fonte(
            conexao,
            canal_id=identificador,
            nome=nome,
            url=str(canal.get("url", "")),
            status=str(canal.get("status", "em_teste")),
            peso=int(canal.get("peso", 5)),
            idioma=str(canal.get("idioma", "")),
            observacao=str(canal.get("observacao", "")),
        )
        total += 1
    return total


def listar_fontes(conexao: sqlite3.Connection, status: str | None = None) -> list[Fonte]:
    if status:
        linhas = conexao.execute(
            "SELECT * FROM fontes WHERE status = ? ORDER BY peso DESC, nome", (status,)
        ).fetchall()
    else:
        linhas = conexao.execute(
            "SELECT * FROM fontes ORDER BY "
            "CASE status WHEN 'oficial' THEN 0 WHEN 'proprio' THEN 1 "
            "WHEN 'em_teste' THEN 2 ELSE 3 END, peso DESC, nome"
        ).fetchall()
    return [_linha_para_fonte(l) for l in linhas]


def registrar_ocorrencia(
    conexao: sqlite3.Connection,
    canal_id: str,
    tipo: str,
    descricao: str = "",
    video_id: str = "",
    rebaixar: bool = True,
) -> Fonte | None:
    """
    Registra um problema com uma fonte e, por padrão, BLOQUEIA o canal na hora.

    tipo: "claim" | "restricao_idade" | "remocao" | "aviso"

    Essa é a trava mais importante do sistema: bastou UM problema, aquele canal
    para de ser usado. É melhor perder uma fonte do que perder o canal.
    """
    coluna = {
        "claim": "claims",
        "restricao_idade": "restricoes_idade",
        "remocao": "remocoes",
    }.get(tipo)

    conexao.execute(
        "INSERT INTO ocorrencias (canal_id, video_id, tipo, descricao, data) "
        "VALUES (?,?,?,?,?)",
        (canal_id, video_id, tipo, descricao, _agora()),
    )
    if coluna:
        conexao.execute(
            f"UPDATE fontes SET {coluna} = {coluna} + 1, atualizado_em = ? "
            "WHERE canal_id = ?",
            (_agora(), canal_id),
        )
        if rebaixar:
            conexao.execute(
                "UPDATE fontes SET status = 'bloqueado', atualizado_em = ? "
                "WHERE canal_id = ? AND status != 'bloqueado'",
                (_agora(), canal_id),
            )
    conexao.commit()

    fonte = obter_fonte(conexao, canal_id)
    if fonte and fonte.bloqueada:
        log(f"Fonte BLOQUEADA por '{tipo}': {fonte.nome}. "
            f"O sistema não vai mais usar esse canal.", "aviso")
    return fonte


def desbloquear_fonte(conexao: sqlite3.Connection, canal_id: str,
                      novo_status: str = "em_teste") -> Fonte | None:
    """Desbloqueia um canal manualmente (decisão sua, nunca automática)."""
    conexao.execute(
        "UPDATE fontes SET status = ?, atualizado_em = ? WHERE canal_id = ?",
        (novo_status, _agora(), canal_id),
    )
    conexao.commit()
    return obter_fonte(conexao, canal_id)


# ============================================================================
#  ANTI-REPETIÇÃO: VÍDEOS E TRECHOS
# ============================================================================

def registrar_video_fonte(conexao: sqlite3.Connection, video: Any) -> None:
    """Guarda que este vídeo-fonte já foi baixado (aceita um VideoFonte ou dict)."""
    dados = video.para_dict() if hasattr(video, "para_dict") else dict(video)
    conexao.execute(
        "INSERT OR REPLACE INTO videos_fonte (id, titulo, canal_id, url, duracao, "
        "baixado_em) VALUES (?,?,?,?,?,?)",
        (dados.get("id"), dados.get("titulo"), dados.get("canal_id"),
         dados.get("url"), float(dados.get("duracao") or 0.0), _agora()),
    )
    conexao.commit()


def trechos_do_video(conexao: sqlite3.Connection, video_id: str) -> list[tuple[float, float]]:
    linhas = conexao.execute(
        "SELECT inicio, fim FROM trechos WHERE video_id = ? ORDER BY inicio",
        (video_id,),
    ).fetchall()
    return [(float(l["inicio"]), float(l["fim"])) for l in linhas]


def sobreposicao(a: tuple[float, float], b: tuple[float, float]) -> float:
    """Quantos segundos dois trechos têm em comum (0 se não se cruzam)."""
    return max(0.0, min(a[1], b[1]) - max(a[0], b[0]))


def trecho_ja_usado(
    conexao: sqlite3.Connection,
    video_id: str,
    inicio: float,
    fim: float,
    tolerancia_segundos: float = 10.0,
    tolerancia_proporcao: float = 0.25,
) -> tuple[bool, str]:
    """
    Verifica se este pedaço do vídeo já virou corte antes.

    Não exige que os tempos sejam idênticos: se o novo trecho tem mais de 10 s
    (ou mais de 25% da sua duração) em comum com algo já usado, é considerado
    REPETIÇÃO. Isso impede o truque involuntário de "cortar 2 segundos antes"
    e publicar praticamente o mesmo vídeo de novo.

    Devolve (já_usado, explicação).
    """
    novo = (float(inicio), float(fim))
    duracao_nova = max(0.001, novo[1] - novo[0])

    for antigo in trechos_do_video(conexao, video_id):
        comum = sobreposicao(novo, antigo)
        if comum >= tolerancia_segundos or comum / duracao_nova >= tolerancia_proporcao:
            return True, (
                f"o trecho {novo[0]:.0f}s–{novo[1]:.0f}s repete "
                f"{comum:.0f}s de um corte já feito ({antigo[0]:.0f}s–{antigo[1]:.0f}s)"
            )
    return False, ""


def registrar_trecho(conexao: sqlite3.Connection, video_id: str, inicio: float,
                     fim: float, idioma: str = "") -> None:
    conexao.execute(
        "INSERT INTO trechos (video_id, inicio, fim, idioma, usado_em) VALUES (?,?,?,?,?)",
        (video_id, float(inicio), float(fim), idioma, _agora()),
    )
    conexao.commit()


# ============================================================================
#  ANTI-REPETIÇÃO: ASSUNTOS
# ============================================================================

_PALAVRAS_VAZIAS = {
    "a", "o", "os", "as", "de", "do", "da", "dos", "das", "em", "no", "na", "e",
    "que", "para", "com", "um", "uma", "the", "of", "in", "on", "and", "to",
    "is", "new", "novo", "sobre", "about",
}


def chave_do_assunto(assunto: str) -> str:
    """
    Reduz um assunto às suas palavras essenciais, para comparar temas.

        "A data de lançamento do GTA 6!"  ->  "6 data gta lancamento"
        "GTA 6 release date"              ->  "6 date gta release"

    Assim "GTA 6 data de lançamento" e "data de lançamento do GTA 6" são
    reconhecidos como o MESMO assunto.
    """
    sem_acento = unicodedata.normalize("NFKD", str(assunto))
    sem_acento = "".join(c for c in sem_acento if not unicodedata.combining(c))
    palavras = re.findall(r"\w+", sem_acento.lower())
    essenciais = sorted({p for p in palavras if p not in _PALAVRAS_VAZIAS and len(p) > 1})
    return " ".join(essenciais)


def _semelhanca(chave_a: str, chave_b: str) -> float:
    """Proporção de palavras em comum entre dois assuntos (0.0 a 1.0)."""
    a, b = set(chave_a.split()), set(chave_b.split())
    if not a or not b:
        return 0.0
    return len(a & b) / len(a | b)


def assunto_ja_usado(
    conexao: sqlite3.Connection,
    assunto: str,
    idioma: str = "",
    dias: int = 45,
    limite_semelhanca: float = 0.7,
) -> tuple[bool, str]:
    """
    Verifica se um assunto muito parecido já foi publicado nos últimos dias.

    Só compara dentro do MESMO idioma: o vídeo 3 é em inglês e pode falar do
    mesmo tema dos vídeos em português sem problema (públicos diferentes).
    """
    chave_nova = chave_do_assunto(assunto)
    if not chave_nova:
        return False, ""

    corte = (datetime.now() - timedelta(days=dias)).isoformat(timespec="seconds")
    consulta = "SELECT assunto, chave FROM assuntos WHERE usado_em >= ?"
    parametros: list[Any] = [corte]
    if idioma:
        consulta += " AND idioma = ?"
        parametros.append(idioma)

    for linha in conexao.execute(consulta, parametros).fetchall():
        parecido = _semelhanca(chave_nova, linha["chave"])
        if parecido >= limite_semelhanca:
            return True, (
                f'o assunto já foi usado nos últimos {dias} dias: '
                f'"{linha["assunto"]}" ({parecido * 100:.0f}% parecido)'
            )
    return False, ""


def registrar_assunto(conexao: sqlite3.Connection, assunto: str, idioma: str = "") -> None:
    conexao.execute(
        "INSERT INTO assuntos (assunto, chave, idioma, usado_em) VALUES (?,?,?,?)",
        (assunto, chave_do_assunto(assunto), idioma, _agora()),
    )
    conexao.commit()


# ============================================================================
#  FILA DE APROVAÇÃO E REPROVADOS
# ============================================================================

def registrar_video_gerado(conexao: sqlite3.Connection, ficha: dict,
                           avaliacao: Any) -> int:
    """
    Guarda o vídeo produzido, já com o resultado da checagem de segurança.

    status:
        "fila"      → passou nas checagens, esperando sua aprovação
        "reprovado" → não passou; fica na lista separada com o motivo
    """
    trecho = ficha.get("trecho", {})
    fonte = ficha.get("fonte", {})
    aprovado = bool(getattr(avaliacao, "aprovado", False))

    cursor = conexao.execute(
        "INSERT INTO videos_gerados (arquivo, titulo, idioma, modo, assunto, "
        "video_id, canal_id, inicio, fim, selo, aprovado, motivos, status, "
        "criado_em, ficha) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        (
            ficha.get("arquivo", ""),
            ficha.get("titulo") or ficha.get("gancho", ""),
            ficha.get("idioma", ""),
            ficha.get("modo", ""),
            ficha.get("assunto", ""),
            fonte.get("id", ""),
            fonte.get("canal_id", ""),
            float(trecho.get("inicio", 0.0)),
            float(trecho.get("fim", 0.0)),
            getattr(avaliacao, "selo", "amarelo"),
            1 if aprovado else 0,
            json.dumps(getattr(avaliacao, "motivos", []), ensure_ascii=False),
            "fila" if aprovado else "reprovado",
            _agora(),
            json.dumps(ficha, ensure_ascii=False),
        ),
    )
    if aprovado and fonte.get("canal_id"):
        conexao.execute(
            "UPDATE fontes SET videos_gerados = videos_gerados + 1, "
            "atualizado_em = ? WHERE canal_id = ?",
            (_agora(), fonte["canal_id"]),
        )
    conexao.commit()
    return int(cursor.lastrowid or 0)


def listar_fila(conexao: sqlite3.Connection, status: str = "fila") -> list[dict]:
    """Devolve os vídeos da fila de aprovação (ou os reprovados)."""
    linhas = conexao.execute(
        "SELECT * FROM videos_gerados WHERE status = ? ORDER BY criado_em DESC",
        (status,),
    ).fetchall()
    resultado = []
    for linha in linhas:
        item = dict(linha)
        item["motivos"] = json.loads(item.get("motivos") or "[]")
        item["ficha"] = json.loads(item.get("ficha") or "{}")
        resultado.append(item)
    return resultado


def atualizar_status_video(conexao: sqlite3.Connection, id_video: int,
                           status: str) -> None:
    """Muda o estado de um vídeo: fila → publicado / descartado."""
    conexao.execute("UPDATE videos_gerados SET status = ? WHERE id = ?",
                    (status, id_video))
    conexao.commit()


# ============================================================================
#  RESUMO (usado pelo painel e pelos scripts de teste)
# ============================================================================

def resumo(conexao: sqlite3.Connection) -> dict:
    """Números gerais do sistema, para mostrar no painel."""
    def contar(consulta: str, parametros: Iterable = ()) -> int:
        return int(conexao.execute(consulta, tuple(parametros)).fetchone()[0])

    return {
        "fontes": contar("SELECT COUNT(*) FROM fontes"),
        "fontes_bloqueadas": contar(
            "SELECT COUNT(*) FROM fontes WHERE status = 'bloqueado'"),
        "videos_fonte": contar("SELECT COUNT(*) FROM videos_fonte"),
        "trechos_usados": contar("SELECT COUNT(*) FROM trechos"),
        "assuntos_usados": contar("SELECT COUNT(*) FROM assuntos"),
        "na_fila": contar("SELECT COUNT(*) FROM videos_gerados WHERE status='fila'"),
        "reprovados": contar(
            "SELECT COUNT(*) FROM videos_gerados WHERE status='reprovado'"),
        "publicados": contar(
            "SELECT COUNT(*) FROM videos_gerados WHERE status='publicado'"),
        "ocorrencias": contar("SELECT COUNT(*) FROM ocorrencias"),
    }


if __name__ == "__main__":
    # "python -m nucleo.banco" mostra a situação atual do sistema
    from .config import carregar_canais, carregar_config

    cfg = carregar_config()
    with conectar(cfg) as conexao:
        sincronizar_canais(conexao, carregar_canais())
        print("\n📊 RESUMO DO SISTEMA")
        for chave, valor in resumo(conexao).items():
            print(f"   {chave:20s}: {valor}")
        print("\n📺 FONTES CADASTRADAS")
        for fonte in listar_fontes(conexao):
            marca = {"oficial": "🟢", "proprio": "🟢",
                     "em_teste": "🟡", "bloqueado": "🔴"}[fonte.status]
            print(f"   {marca} [{fonte.status:9s}] {fonte.nome} "
                  f"(gerados: {fonte.videos_gerados}, problemas: {fonte.problemas})")
