"""
test_fontes.py — testes automáticos das tendências e da escolha de fontes.

Nenhum teste aqui acessa a internet: tudo roda com dados de mentirinha e com
um banco temporário na memória. Rápido e sem surpresa.

COMO RODAR (dentro da pasta gta_cuts_factory):

    python testes\\test_fontes.py
"""

from __future__ import annotations

import sqlite3
import sys
import tempfile
from datetime import datetime, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import banco, fontes, tendencias  # noqa: E402
from nucleo.config import carregar_config  # noqa: E402
from nucleo.tendencias import Assunto  # noqa: E402


CFG = carregar_config()


def banco_temporario() -> sqlite3.Connection:
    conexao = sqlite3.connect(":memory:")
    conexao.row_factory = sqlite3.Row
    conexao.executescript(banco.ESQUEMA)
    return conexao


def data_de(dias_atras: int) -> str:
    """Data no formato do yt-dlp ("20260115")."""
    return (datetime.now() - timedelta(days=dias_atras)).strftime("%Y%m%d")


def assuntos_falsos() -> list[Assunto]:
    return [
        Assunto("GTA 6 data de lançamento", 5.0, ["base"]),
        Assunto("GTA 5 segredos", 2.0, ["base"]),
    ]


def video_falso(**campos) -> dict:
    base = {"id": "v1", "titulo": "Um vídeo qualquer sobre GTA",
            "url": "https://youtu.be/v1", "duracao": 900.0,
            "data_envio": data_de(3), "views": 1000}
    base.update(campos)
    return base


# ============================================================================
#  TENDÊNCIAS — EXTRAÇÃO DE EXPRESSÕES
# ============================================================================

def test_expressao_repetida_e_detectada():
    titulos = [
        "GTA 6 data de lançamento CONFIRMADA pela Rockstar",
        "A data de lançamento do GTA 6 mudou de novo",
        "GTA 6: data de lançamento e o que sabemos",
    ]
    contagem = tendencias.extrair_expressoes(titulos)
    assert contagem["data lancamento"] >= 3


def test_expressoes_ignoram_palavras_vazias():
    contagem = tendencias.extrair_expressoes(["O trailer do GTA 6 é incrível"])
    assert not any(" de " in exp or exp.startswith("o ") for exp in contagem)


def test_titulo_sem_gta_nao_vira_tendencia():
    contagem = tendencias.extrair_expressoes(["Receita de bolo de cenoura fácil"])
    assert len(contagem) == 0


def test_acento_nao_atrapalha():
    contagem = tendencias.extrair_expressoes([
        "GTA 6 lançamento adiado", "GTA 6 lancamento adiado",
    ])
    assert contagem["lancamento adiado"] == 2


def test_data_recente():
    assert tendencias.data_recente(data_de(3), dias=7)
    assert not tendencias.data_recente(data_de(60), dias=30)
    assert tendencias.data_recente("", dias=7)          # desconhecida = aceita
    assert tendencias.data_recente("data-ruim", dias=7)


def test_assuntos_base_funcionam_sem_internet():
    canais = {"assuntos_base": {"pt": ["GTA 6 trailer", "GTA 5 segredos"]},
              "assuntos_proibidos": []}
    assuntos = tendencias.descobrir_assuntos(CFG, canais, "pt", usar_rede=False)
    assert [a.termo for a in assuntos] == ["GTA 6 trailer", "GTA 5 segredos"]
    assert assuntos[0].pontuacao > assuntos[1].pontuacao   # ordem da sua lista


def test_assunto_proibido_nao_entra_na_lista():
    canais = {"assuntos_base": {"pt": ["GTA 6 trailer", "GTA 6 APK download"]},
              "assuntos_proibidos": ["GTA 6 APK"]}
    assuntos = tendencias.descobrir_assuntos(CFG, canais, "pt", usar_rede=False)
    assert [a.termo for a in assuntos] == ["GTA 6 trailer"]


def test_assunto_ja_publicado_nao_volta():
    with banco_temporario() as conexao:
        banco.registrar_assunto(conexao, "GTA 6 trailer", "pt")
        canais = {"assuntos_base": {"pt": ["GTA 6 trailer", "GTA 5 segredos"]},
                  "assuntos_proibidos": []}
        assuntos = tendencias.descobrir_assuntos(
            CFG, canais, "pt", usar_rede=False, conexao=conexao)
        assert [a.termo for a in assuntos] == ["GTA 5 segredos"]


# ============================================================================
#  FONTES — NOTA DE CADA CANDIDATO
# ============================================================================

def test_fonte_oficial_vale_mais_que_em_teste():
    oficial = banco.Fonte("c1", "Rockstar", "oficial", peso=10)
    terceiro = banco.Fonte("c2", "Canal X", "em_teste", peso=5)
    video = video_falso()
    nota_oficial, _, _ = fontes.pontuar(video, oficial, assuntos_falsos(), CFG)
    nota_terceiro, _, _ = fontes.pontuar(video, terceiro, assuntos_falsos(), CFG)
    assert nota_oficial > nota_terceiro


def test_titulo_no_assunto_quente_ganha_pontos():
    fonte = banco.Fonte("c1", "Canal X", "em_teste")
    combina = video_falso(titulo="GTA 6: data de lançamento confirmada!")
    nao_combina = video_falso(titulo="Jogando um pouco hoje")
    nota_a, assunto, _ = fontes.pontuar(combina, fonte, assuntos_falsos(), CFG)
    nota_b, _, _ = fontes.pontuar(nao_combina, fonte, assuntos_falsos(), CFG)
    assert nota_a > nota_b
    assert assunto == "GTA 6 data de lançamento"


def test_video_recente_ganha_de_video_antigo():
    fonte = banco.Fonte("c1", "Canal X", "em_teste")
    novo = video_falso(data_envio=data_de(2))
    velho = video_falso(data_envio=data_de(300))
    nota_novo, _, _ = fontes.pontuar(novo, fonte, assuntos_falsos(), CFG)
    nota_velho, _, _ = fontes.pontuar(velho, fonte, assuntos_falsos(), CFG)
    assert nota_novo > nota_velho


def test_video_curto_demais_e_penalizado():
    fonte = banco.Fonte("c1", "Canal X", "em_teste")
    curto = video_falso(duracao=40)
    longo = video_falso(duracao=1200)
    nota_curto, _, motivos = fontes.pontuar(curto, fonte, assuntos_falsos(), CFG)
    nota_longo, _, _ = fontes.pontuar(longo, fonte, assuntos_falsos(), CFG)
    assert nota_curto < nota_longo
    assert any("curto demais" in m for m in motivos)


def test_combinar_com_assunto_sem_relacao():
    assunto, forca = fontes.combinar_com_assunto("Receita de bolo", assuntos_falsos())
    assert forca == 0.0 and assunto == ""


# ============================================================================
#  FONTES — BUSCA DE CANDIDATOS (com pasta local, sem internet)
# ============================================================================

def criar_gravacoes(pasta: Path, nomes: list[str]) -> None:
    pasta.mkdir(parents=True, exist_ok=True)
    for nome in nomes:
        (pasta / nome).write_bytes(b"video de mentirinha")


def test_gravacoes_locais_viram_candidatos():
    with tempfile.TemporaryDirectory() as temporaria:
        pasta = Path(temporaria) / "GTA"
        criar_gravacoes(pasta, ["GTA 6 data de lancamento.mp4",
                                "passeio pela cidade.mp4", "anotacoes.txt"])
        with banco_temporario() as conexao:
            canais = {"canais": [{"nome": "Minhas gravações",
                                  "pasta_local": str(pasta), "status": "proprio",
                                  "peso": 10}]}
            candidatos = fontes.buscar_candidatos(
                conexao, CFG, canais, assuntos_falsos(), usar_rede=False)
            # o .txt não pode virar candidato
            assert len(candidatos) == 2
            assert all(c.eh_local for c in candidatos)
            # o que casa com o assunto quente fica em primeiro
            assert "lancamento" in candidatos[0].titulo


def test_canal_bloqueado_nao_entra_na_busca():
    with tempfile.TemporaryDirectory() as temporaria:
        pasta = Path(temporaria) / "GTA"
        criar_gravacoes(pasta, ["corte1.mp4"])
        with banco_temporario() as conexao:
            banco.registrar_fonte(conexao, str(pasta), "Minhas gravações",
                                  status="proprio")
            banco.registrar_ocorrencia(conexao, str(pasta), "claim")
            canais = {"canais": [{"nome": "Minhas gravações",
                                  "pasta_local": str(pasta), "status": "proprio"}]}
            assert fontes.buscar_candidatos(
                conexao, CFG, canais, assuntos_falsos(), usar_rede=False) == []


def test_titulo_com_assunto_proibido_e_descartado():
    with tempfile.TemporaryDirectory() as temporaria:
        pasta = Path(temporaria) / "GTA"
        criar_gravacoes(pasta, ["GTA 6 APK download gratis.mp4", "corrida.mp4"])
        with banco_temporario() as conexao:
            canais = {"canais": [{"nome": "Minhas gravações",
                                  "pasta_local": str(pasta), "status": "proprio"}],
                      "assuntos_proibidos": ["GTA 6 APK"]}
            candidatos = fontes.buscar_candidatos(
                conexao, CFG, canais, assuntos_falsos(), usar_rede=False)
            assert len(candidatos) == 1 and "corrida" in candidatos[0].titulo


def test_video_ja_muito_cortado_sai_da_lista():
    with tempfile.TemporaryDirectory() as temporaria:
        pasta = Path(temporaria) / "GTA"
        criar_gravacoes(pasta, ["gameplay_longo.mp4"])
        with banco_temporario() as conexao:
            # já tiramos 3 cortes deste vídeo (o limite padrão)
            for inicio in (0, 200, 400):
                banco.registrar_trecho(conexao, "gameplay_longo", inicio, inicio + 60)
            canais = {"canais": [{"nome": "Minhas gravações",
                                  "pasta_local": str(pasta), "status": "proprio"}]}
            assert fontes.buscar_candidatos(
                conexao, CFG, canais, assuntos_falsos(), usar_rede=False) == []


# ============================================================================
#  FONTES — DIVERSIFICAÇÃO
# ============================================================================

def candidato(video_id: str, canal_id: str, assunto: str, nota: float) -> fontes.Candidato:
    return fontes.Candidato(
        video_id=video_id, titulo=f"Vídeo {video_id}", url=f"https://y/{video_id}",
        canal=canal_id, canal_id=canal_id, status_fonte="em_teste",
        duracao=600, assunto=assunto, pontuacao=nota,
    )


def test_diversificar_prefere_canais_e_assuntos_diferentes():
    lista = [
        candidato("a1", "canal_1", "GTA 6 data", 9.0),
        candidato("a2", "canal_1", "GTA 6 data", 8.9),   # mesmo canal e assunto
        candidato("b1", "canal_2", "GTA 6 mapa", 8.0),
        candidato("c1", "canal_3", "GTA 5 segredos", 7.0),
    ]
    escolhidos = fontes.diversificar(lista, 3)
    assert [c.video_id for c in escolhidos] == ["a1", "b1", "c1"]
    assert len({c.canal_id for c in escolhidos}) == 3
    assert len({c.assunto for c in escolhidos}) == 3


def test_diversificar_relaxa_o_canal_quando_precisa():
    """Com poucos canais, aceita repetir canal — mas nunca repete o assunto."""
    lista = [
        candidato("a1", "canal_1", "GTA 6 data", 9.0),
        candidato("a2", "canal_1", "GTA 6 mapa", 8.5),
        candidato("a3", "canal_1", "GTA 5 segredos", 8.0),
    ]
    escolhidos = fontes.diversificar(lista, 3)
    assert len(escolhidos) == 3
    assert len({c.assunto for c in escolhidos}) == 3


def test_diversificar_nunca_repete_o_mesmo_video():
    lista = [candidato("a1", "canal_1", "", 9.0), candidato("a2", "canal_1", "", 8.0)]
    escolhidos = fontes.diversificar(lista, 5)
    assert len(escolhidos) == 2
    assert len({c.video_id for c in escolhidos}) == 2


def test_diversificar_com_lista_vazia():
    assert fontes.diversificar([], 3) == []


def test_diversificar_respeita_a_ordem_de_nota():
    lista = [
        candidato("baixo", "canal_1", "assunto A", 1.0),
        candidato("alto", "canal_2", "assunto B", 9.0),
    ]
    assert fontes.diversificar(lista, 1)[0].video_id == "alto"


# ============================================================================
#  EXECUÇÃO
# ============================================================================

def main() -> int:
    testes = [(nome, funcao) for nome, funcao in sorted(globals().items())
              if nome.startswith("test_") and callable(funcao)]
    falhas = 0
    for nome, funcao in testes:
        try:
            funcao()
            print(f"  ✅ {nome}")
        except AssertionError as erro:
            falhas += 1
            print(f"  ❌ {nome}  →  {erro or 'condição falhou'}")
        except Exception as erro:
            falhas += 1
            print(f"  ❌ {nome}  →  ERRO: {type(erro).__name__}: {erro}")

    print("\n" + "=" * 60)
    if falhas:
        print(f"  {falhas} de {len(testes)} testes FALHARAM")
    else:
        print(f"  TODOS OS TESTES PASSARAM ({len(testes)}/{len(testes)}) 🎉")
    print("=" * 60)
    return 1 if falhas else 0


if __name__ == "__main__":
    raise SystemExit(main())
