"""
test_painel.py — testes do painel e do orquestrador (o botão CRIAR).

Os testes do painel usam o "cliente de teste" do Flask: ele bate nas rotas de
verdade, sem precisar abrir navegador nem servidor. O banco usado é um arquivo
temporário — o seu banco de verdade não é tocado.

COMO RODAR (dentro da pasta gta_cuts_factory):

    python testes\\test_painel.py
"""

from __future__ import annotations

import json
import sqlite3
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import banco, producao, seguranca  # noqa: E402
from nucleo.config import carregar_config  # noqa: E402
from nucleo.fontes import Candidato  # noqa: E402
from nucleo.tendencias import Assunto  # noqa: E402


CFG = carregar_config()


# ============================================================================
#  PLANEJAMENTO DO LOTE (a parte que decide o que produzir)
# ============================================================================

def _candidatos(quantidade: int = 3) -> list[Candidato]:
    return [
        Candidato(video_id=f"v{i}", titulo=f"Vídeo {i}", url=f"https://y/{i}",
                  canal=f"Canal {i}", canal_id=f"canal_{i}",
                  status_fonte="em_teste", duracao=900,
                  assunto=f"assunto {i}", pontuacao=9.0 - i)
        for i in range(quantidade)
    ]


def _assuntos() -> list[Assunto]:
    return [Assunto("GTA 6 data de lançamento", 5.0, ["base"]),
            Assunto("GTA 6 mapa", 4.0, ["base"]),
            Assunto("GTA 5 segredos", 3.0, ["base"])]


def test_plano_respeita_a_quantidade_configurada():
    planos = producao.planejar(CFG, _candidatos(5), _assuntos())
    assert len(planos) == int(CFG.pegar("producao.quantidade", 3))


def test_plano_respeita_a_mistura_de_modos():
    planos = producao.planejar(CFG, _candidatos(5), _assuntos())
    cortes = [p for p in planos if p.modo == "A_corte"]
    narrados = [p for p in planos if p.modo == "B_narrado"]
    assert len(cortes) == int(CFG.pegar("producao.modo_a_cortes", 2))
    assert len(narrados) == int(CFG.pegar("producao.modo_b_narrados", 1))


def test_plano_usa_os_idiomas_na_ordem():
    planos = producao.planejar(CFG, _candidatos(5), _assuntos())
    esperado = list(CFG.pegar("producao.idiomas", ["pt", "pt", "en"]))
    assert [p.idioma for p in planos] == esperado[:len(planos)]


def test_plano_sem_candidatos_vira_tudo_narrado():
    """Sem vídeo-fonte disponível, o sistema ainda entrega vídeos narrados."""
    planos = producao.planejar(CFG, [], _assuntos())
    assert planos and all(p.modo == "B_narrado" for p in planos)
    assert all(p.assunto for p in planos)


def test_plano_nao_repete_assunto_entre_corte_e_narrado():
    candidatos = _candidatos(2)
    candidatos[0].assunto = "GTA 6 mapa"
    planos = producao.planejar(CFG, candidatos, _assuntos())
    narrados = [p.assunto for p in planos if p.modo == "B_narrado"]
    assert "GTA 6 mapa" not in narrados


def test_plano_usa_candidatos_de_canais_diferentes():
    planos = producao.planejar(CFG, _candidatos(5), _assuntos())
    canais = [p.candidato.canal_id for p in planos if p.modo == "A_corte"]
    assert len(canais) == len(set(canais))


def test_descricao_do_plano_e_legivel():
    plano = producao.planejar(CFG, _candidatos(3), _assuntos())[0]
    texto = plano.descrever()
    assert "vídeo 1" in texto and ("corte" in texto or "narrado" in texto)


# ============================================================================
#  PAINEL (rotas de verdade, com o cliente de teste do Flask)
# ============================================================================

def preparar_painel(tmp: Path):
    """
    Cria um banco temporário com 1 vídeo na fila e 1 reprovado, e devolve o
    cliente de teste do painel apontando para esse banco.
    """
    import painel.app as modulo

    caminho_banco = tmp / "banco_teste.sqlite"
    video_falso = tmp / "video.mp4"
    video_falso.write_bytes(b"\x00" * 64)
    miniatura_falsa = tmp / "video.jpg"
    miniatura_falsa.write_bytes(b"\xff\xd8\xff")

    cfg = carregar_config()
    cfg["pastas"]["banco"] = str(caminho_banco)

    conexao = sqlite3.connect(str(caminho_banco))
    conexao.row_factory = sqlite3.Row
    conexao.executescript(banco.ESQUEMA)

    banco.registrar_fonte(conexao, "canal_x", "Canal X", status="em_teste")

    ficha_ok = {
        "arquivo": str(video_falso), "miniatura": str(miniatura_falsa),
        "idioma": "pt", "modo": "A_corte", "assunto": "GTA 6 mapa",
        "gancho": "VAZOU AGORA: GTA 6",
        "trecho": {"inicio": 60, "fim": 150, "duracao": 90},
        "fonte": {"id": "abc", "canal": "Canal X", "canal_id": "canal_x",
                  "url": "https://youtu.be/abc"},
        "credito": "Fonte: Canal X\nhttps://youtu.be/abc",
        "metadados": {"titulo": "Título original", "descricao": "Descrição original",
                      "hashtags": ["#gta6", "#gta"]},
        "tiktok_txt": str(tmp / "video_tiktok.txt"),
    }
    seguranca.registrar_aprovacao(
        conexao, ficha_ok, seguranca.Avaliacao(aprovado=True, selo="amarelo",
                                               motivos=[]))

    ficha_reprovada = {
        "arquivo": "", "idioma": "pt", "modo": "A_corte",
        "assunto": "GTA 6 final do jogo",
        "trecho": {"inicio": 0, "fim": 90, "duracao": 90},
        "fonte": {"id": "def", "canal": "Canal X", "canal_id": "canal_x"},
    }
    seguranca.registrar_aprovacao(
        conexao, ficha_reprovada,
        seguranca.Avaliacao(aprovado=False, selo="vermelho", motivos=[
            {"codigo": "spoiler", "gravidade": "bloqueio",
             "mensagem": "o trecho parece conter spoiler de história"}]))
    conexao.close()

    # faz o painel usar o banco temporário
    modulo.carregar_config = lambda: cfg
    modulo.aplicacao.config["TESTING"] = True
    return modulo.aplicacao.test_client(), cfg


def test_pagina_inicial_abre_e_mostra_a_fila():
    with tempfile.TemporaryDirectory() as pasta:
        cliente, _ = preparar_painel(Path(pasta))
        resposta = cliente.get("/")
        assert resposta.status_code == 200
        pagina = resposta.get_data(as_text=True)
        assert "GTA Cuts Factory" in pagina
        assert "Fila de aprovação (1)" in pagina
        assert "Título original" in pagina


def test_pagina_mostra_os_reprovados_com_o_motivo():
    with tempfile.TemporaryDirectory() as pasta:
        cliente, _ = preparar_painel(Path(pasta))
        pagina = cliente.get("/").get_data(as_text=True)
        assert "Reprovados pelo escudo" in pagina
        assert "spoiler de história" in pagina


def test_pagina_mostra_a_reputacao_das_fontes():
    with tempfile.TemporaryDirectory() as pasta:
        cliente, _ = preparar_painel(Path(pasta))
        pagina = cliente.get("/").get_data(as_text=True)
        assert "Canal X" in pagina and "em_teste" in pagina


def test_previa_do_video_e_servida():
    with tempfile.TemporaryDirectory() as pasta:
        cliente, _ = preparar_painel(Path(pasta))
        resposta = cliente.get("/video/1")
        assert resposta.status_code == 200
        assert resposta.mimetype == "video/mp4"


def test_video_inexistente_devolve_404():
    with tempfile.TemporaryDirectory() as pasta:
        cliente, _ = preparar_painel(Path(pasta))
        assert cliente.get("/video/999").status_code == 404


def test_salvar_altera_titulo_e_hashtags():
    with tempfile.TemporaryDirectory() as pasta:
        cliente, cfg = preparar_painel(Path(pasta))
        resposta = cliente.post("/salvar/1", data={
            "titulo": "Título editado por mim",
            "descricao": "Minha descrição",
            "hashtags": "gta6 mapa #vazou",
        })
        assert resposta.status_code in (301, 302)

        conexao = banco.conectar(cfg)
        item = banco.listar_fila(conexao, "fila")[0]
        conexao.close()

        assert item["titulo"] == "Título editado por mim"
        meta = item["ficha"]["metadados"]
        assert meta["descricao"] == "Minha descrição"
        # hashtags sem "#" ganham o "#" sozinhas
        assert meta["hashtags"] == ["#gta6", "#mapa", "#vazou"]


def test_salvar_atualiza_o_texto_do_tiktok():
    with tempfile.TemporaryDirectory() as pasta:
        cliente, _ = preparar_painel(Path(pasta))
        cliente.post("/salvar/1", data={"titulo": "Novo título",
                                        "descricao": "x", "hashtags": "#gta6"})
        texto = (Path(pasta) / "video_tiktok.txt").read_text(encoding="utf-8")
        assert "Novo título" in texto and "#gta6" in texto


def test_publicar_tira_da_fila():
    with tempfile.TemporaryDirectory() as pasta:
        cliente, cfg = preparar_painel(Path(pasta))
        cliente.post("/publicar/1")
        conexao = banco.conectar(cfg)
        assert banco.listar_fila(conexao, "fila") == []
        assert len(banco.listar_fila(conexao, "publicado")) == 1
        conexao.close()


def test_descartar_tira_da_fila():
    with tempfile.TemporaryDirectory() as pasta:
        cliente, cfg = preparar_painel(Path(pasta))
        cliente.post("/descartar/1")
        conexao = banco.conectar(cfg)
        assert banco.listar_fila(conexao, "fila") == []
        assert len(banco.listar_fila(conexao, "descartado")) == 1
        conexao.close()


def test_claim_pelo_painel_bloqueia_a_fonte():
    with tempfile.TemporaryDirectory() as pasta:
        cliente, cfg = preparar_painel(Path(pasta))
        cliente.post("/fonte/claim", data={"canal_id": "canal_x"})
        conexao = banco.conectar(cfg)
        fonte = banco.obter_fonte(conexao, "canal_x")
        conexao.close()
        assert fonte.status == "bloqueado" and fonte.claims == 1


def test_desbloquear_pelo_painel():
    with tempfile.TemporaryDirectory() as pasta:
        cliente, cfg = preparar_painel(Path(pasta))
        cliente.post("/fonte/claim", data={"canal_id": "canal_x"})
        cliente.post("/fonte/desbloquear", data={"canal_id": "canal_x"})
        conexao = banco.conectar(cfg)
        assert banco.obter_fonte(conexao, "canal_x").status == "em_teste"
        conexao.close()


def test_status_devolve_json():
    with tempfile.TemporaryDirectory() as pasta:
        cliente, _ = preparar_painel(Path(pasta))
        resposta = cliente.get("/status")
        assert resposta.status_code == 200
        dados = json.loads(resposta.get_data(as_text=True))
        assert "rodando" in dados and "historico" in dados


def test_reprovado_sem_arquivo_nao_volta_para_a_fila():
    """Reprovado não é renderizado, então não há o que mandar para a fila."""
    with tempfile.TemporaryDirectory() as pasta:
        cliente, cfg = preparar_painel(Path(pasta))
        cliente.post("/reavaliar/2")
        conexao = banco.conectar(cfg)
        assert len(banco.listar_fila(conexao, "reprovado")) == 1
        conexao.close()


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
