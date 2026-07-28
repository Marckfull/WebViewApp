"""
test_seguranca.py — testes automáticos do banco e do módulo de segurança.

Estes testes usam um banco de dados temporário na memória do computador: eles
NÃO mexem no seu banco real (dados/banco.sqlite).

COMO RODAR (dentro da pasta gta_cuts_factory):

    python testes\\test_seguranca.py
"""

from __future__ import annotations

import sqlite3
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import banco, seguranca  # noqa: E402
from nucleo.config import carregar_config  # noqa: E402
from nucleo.downloader import VideoFonte  # noqa: E402


CFG = carregar_config()


def banco_temporario() -> sqlite3.Connection:
    """Cria um banco vazio só na memória (some quando o teste termina)."""
    conexao = sqlite3.connect(":memory:")
    conexao.row_factory = sqlite3.Row
    conexao.executescript(banco.ESQUEMA)
    return conexao


def video_falso(id_video: str = "abc123", canal_id: str = "canal_x",
                arquivo: str = "") -> VideoFonte:
    return VideoFonte(
        id=id_video, titulo="Vídeo de teste", canal="Canal X", canal_id=canal_id,
        canal_url="https://youtube.com/@canalx",
        url=f"https://youtu.be/{id_video}", duracao=600.0, data_envio="20260101",
        arquivo=arquivo,
    )


def transcricao_falsa(palavras: int = 150, duracao: float = 60.0,
                      texto: str = "") -> dict:
    """Simula uma transcrição com fala suficiente (corte comentado)."""
    passo = duracao / max(1, palavras)
    # cada palavra dura no máximo 0,5s — é o tamanho real de uma palavra
    # falada; sem esse limite, 2 palavras em 60s pareceriam cobrir o vídeo todo
    duracao_palavra = min(0.5, passo * 0.9)
    lista = [{"texto": f"palavra{i}", "inicio": i * passo,
              "fim": i * passo + duracao_palavra} for i in range(palavras)]
    return {"idioma": "pt", "texto": texto or " ".join(p["texto"] for p in lista),
            "palavras": lista, "frases": []}


# ============================================================================
#  BANCO — FONTES E REPUTAÇÃO
# ============================================================================

def test_fonte_nova_entra_como_em_teste():
    with banco_temporario() as conexao:
        fonte = banco.registrar_fonte(conexao, "canal_1", "Canal Novo")
        assert fonte.status == "em_teste"
        assert not fonte.confiavel and not fonte.bloqueada


def test_status_invalido_e_recusado():
    with banco_temporario() as conexao:
        try:
            banco.registrar_fonte(conexao, "canal_1", "X", status="qualquer_coisa")
        except ValueError:
            return
        raise AssertionError("deveria ter recusado um status inválido")


def test_claim_bloqueia_a_fonte_na_hora():
    with banco_temporario() as conexao:
        banco.registrar_fonte(conexao, "canal_1", "Canal Novo")
        fonte = banco.registrar_ocorrencia(conexao, "canal_1", "claim",
                                           "Content ID de trilha sonora")
        assert fonte is not None
        assert fonte.status == "bloqueado"
        assert fonte.claims == 1


def test_fonte_bloqueada_nao_volta_sozinha():
    """Mesmo que o canal continue no YAML, o bloqueio é mantido."""
    with banco_temporario() as conexao:
        banco.registrar_fonte(conexao, "canal_1", "Canal Novo")
        banco.registrar_ocorrencia(conexao, "canal_1", "remocao")
        banco.sincronizar_canais(conexao, {"canais": [
            {"nome": "Canal Novo", "canal_id": "canal_1", "status": "em_teste"}
        ]})
        assert banco.obter_fonte(conexao, "canal_1").status == "bloqueado"


def test_desbloqueio_e_decisao_sua():
    with banco_temporario() as conexao:
        banco.registrar_fonte(conexao, "canal_1", "Canal Novo")
        banco.registrar_ocorrencia(conexao, "canal_1", "claim")
        fonte = banco.desbloquear_fonte(conexao, "canal_1")
        assert fonte.status == "em_teste"


def test_sincronizar_canais_do_yaml():
    with banco_temporario() as conexao:
        total = banco.sincronizar_canais(conexao, {"canais": [
            {"nome": "Rockstar Games", "url": "https://youtube.com/@RockstarGames",
             "status": "oficial", "peso": 10},
            {"nome": "Minhas gravações", "pasta_local": "C:/Videos/GTA",
             "status": "proprio", "peso": 10},
        ]})
        assert total == 2
        assert len(banco.listar_fontes(conexao, "oficial")) == 1
        assert len(banco.listar_fontes(conexao, "proprio")) == 1


# ============================================================================
#  BANCO — ANTI-REPETIÇÃO
# ============================================================================

def test_trecho_igual_e_repeticao():
    with banco_temporario() as conexao:
        banco.registrar_trecho(conexao, "video_1", 60, 120)
        repetido, motivo = banco.trecho_ja_usado(conexao, "video_1", 60, 120)
        assert repetido and "repete" in motivo


def test_trecho_deslocado_ainda_e_repeticao():
    """Cortar 5 segundos antes não engana o sistema."""
    with banco_temporario() as conexao:
        banco.registrar_trecho(conexao, "video_1", 60, 120)
        repetido, _ = banco.trecho_ja_usado(conexao, "video_1", 55, 115)
        assert repetido


def test_trecho_distante_e_permitido():
    with banco_temporario() as conexao:
        banco.registrar_trecho(conexao, "video_1", 60, 120)
        repetido, _ = banco.trecho_ja_usado(conexao, "video_1", 300, 360)
        assert not repetido


def test_trecho_de_outro_video_e_permitido():
    with banco_temporario() as conexao:
        banco.registrar_trecho(conexao, "video_1", 60, 120)
        repetido, _ = banco.trecho_ja_usado(conexao, "video_2", 60, 120)
        assert not repetido


def test_encostar_no_trecho_anterior_e_permitido():
    """Terminar onde o outro começou não é repetição (0 s em comum)."""
    with banco_temporario() as conexao:
        banco.registrar_trecho(conexao, "video_1", 60, 120)
        repetido, _ = banco.trecho_ja_usado(conexao, "video_1", 120, 180)
        assert not repetido


def test_chave_do_assunto_ignora_ordem_e_acento():
    assert (banco.chave_do_assunto("A data de lançamento do GTA 6!")
            == banco.chave_do_assunto("GTA 6: lancamento data"))


def test_assunto_parecido_e_repeticao():
    with banco_temporario() as conexao:
        banco.registrar_assunto(conexao, "GTA 6 data de lançamento", "pt")
        usado, motivo = banco.assunto_ja_usado(
            conexao, "a data de lançamento do GTA 6", "pt")
        assert usado and "já foi usado" in motivo


def test_assunto_em_outro_idioma_e_permitido():
    """O vídeo em inglês pode falar do mesmo tema dos vídeos em português."""
    with banco_temporario() as conexao:
        banco.registrar_assunto(conexao, "GTA 6 data de lançamento", "pt")
        usado, _ = banco.assunto_ja_usado(conexao, "GTA 6 release date", "en")
        assert not usado


def test_assunto_diferente_e_permitido():
    with banco_temporario() as conexao:
        banco.registrar_assunto(conexao, "GTA 6 data de lançamento", "pt")
        usado, _ = banco.assunto_ja_usado(conexao, "GTA 5 segredos do mapa", "pt")
        assert not usado


# ============================================================================
#  SEGURANÇA — CHECAGENS
# ============================================================================

def test_fonte_oficial_com_fala_recebe_selo_verde():
    with banco_temporario() as conexao:
        banco.registrar_fonte(conexao, "canal_oficial", "Rockstar Games",
                              status="oficial")
        avaliacao = seguranca.avaliar(
            conexao, CFG, video_falso(canal_id="canal_oficial"),
            inicio=0, duracao=60, transcricao=transcricao_falsa(),
            assunto="GTA 6 trailer", idioma="pt", analisar_imagens=False,
        )
        # sem análise de imagem entra um aviso; o que importa é não ter bloqueio
        assert avaliacao.aprovado
        assert not avaliacao.bloqueios


def test_fonte_em_teste_recebe_selo_amarelo():
    with banco_temporario() as conexao:
        banco.registrar_fonte(conexao, "canal_x", "Canal X", status="em_teste")
        avaliacao = seguranca.avaliar(
            conexao, CFG, video_falso(), inicio=0, duracao=60,
            transcricao=transcricao_falsa(), assunto="GTA 6 mapa",
            idioma="pt", analisar_imagens=False,
        )
        assert avaliacao.aprovado and avaliacao.selo == "amarelo"


def test_fonte_bloqueada_reprova_o_video():
    with banco_temporario() as conexao:
        banco.registrar_fonte(conexao, "canal_x", "Canal X")
        banco.registrar_ocorrencia(conexao, "canal_x", "claim")
        avaliacao = seguranca.avaliar(
            conexao, CFG, video_falso(), inicio=0, duracao=60,
            transcricao=transcricao_falsa(), assunto="GTA 6 mapa",
            idioma="pt", analisar_imagens=False,
        )
        assert not avaliacao.aprovado and avaliacao.selo == "vermelho"
        assert any(m["codigo"] == "fonte_bloqueada" for m in avaliacao.motivos)


def test_cinematica_oficial_sem_fala_e_bloqueada():
    """Regra da Rockstar: cinemática isolada, sem comentário, não pode."""
    with banco_temporario() as conexao:
        banco.registrar_fonte(conexao, "canal_oficial", "Rockstar Games",
                              status="oficial")
        avaliacao = seguranca.avaliar(
            conexao, CFG, video_falso(canal_id="canal_oficial"),
            inicio=0, duracao=60,
            transcricao=transcricao_falsa(palavras=3, duracao=60),
            assunto="GTA 6 trailer", idioma="pt", analisar_imagens=False,
        )
        assert not avaliacao.aprovado
        assert any(m["codigo"] == "cinematica_isolada" for m in avaliacao.motivos)


def test_modo_narrado_nao_cai_na_regra_da_cinematica():
    """No Modo B a narração é sua, então material oficial é permitido."""
    with banco_temporario() as conexao:
        banco.registrar_fonte(conexao, "canal_oficial", "Rockstar Games",
                              status="oficial")
        avaliacao = seguranca.avaliar(
            conexao, CFG, video_falso(canal_id="canal_oficial"),
            inicio=0, duracao=60,
            transcricao=transcricao_falsa(palavras=3, duracao=60),
            assunto="GTA 6 trailer", idioma="pt", modo="B_narrado",
            analisar_imagens=False,
        )
        assert avaliacao.aprovado


def test_spoiler_bloqueia():
    with banco_temporario() as conexao:
        banco.registrar_fonte(conexao, "canal_x", "Canal X")
        avaliacao = seguranca.avaliar(
            conexao, CFG, video_falso(), inicio=0, duracao=60,
            transcricao=transcricao_falsa(texto="hoje eu mostro o final do jogo"),
            assunto="GTA 5", idioma="pt", analisar_imagens=False,
        )
        assert not avaliacao.aprovado
        assert any(m["codigo"] == "spoiler" for m in avaliacao.motivos)


def test_assunto_proibido_bloqueia():
    with banco_temporario() as conexao:
        banco.registrar_fonte(conexao, "canal_x", "Canal X")
        avaliacao = seguranca.avaliar(
            conexao, CFG, video_falso(), inicio=0, duracao=60,
            transcricao=transcricao_falsa(), assunto="GTA 6 APK download",
            idioma="pt", assuntos_proibidos=["GTA 6 APK"], analisar_imagens=False,
        )
        assert not avaliacao.aprovado
        assert any(m["codigo"] == "assunto_proibido" for m in avaliacao.motivos)


def test_trecho_repetido_reprova_o_video():
    with banco_temporario() as conexao:
        banco.registrar_fonte(conexao, "canal_x", "Canal X")
        banco.registrar_trecho(conexao, "abc123", 60, 120)
        avaliacao = seguranca.avaliar(
            conexao, CFG, video_falso(), inicio=60, duracao=60,
            transcricao=transcricao_falsa(), assunto="GTA 6 mapa",
            idioma="pt", analisar_imagens=False,
        )
        assert not avaliacao.aprovado
        assert any(m["codigo"] == "trecho_repetido" for m in avaliacao.motivos)


# ============================================================================
#  SEGURANÇA — VIOLÊNCIA
# ============================================================================

def test_violencia_acumulada_acima_do_limite_bloqueia():
    analise = {"analisado": True, "segundos_estimados": 20.0, "proporcao": 0.33}
    motivos = seguranca.checar_violencia(analise, "", CFG)
    assert any(m.gravidade == seguranca.BLOQUEIO for m in motivos)


def test_violencia_dentro_do_limite_vira_aviso():
    analise = {"analisado": True, "segundos_estimados": 3.0, "proporcao": 0.05}
    motivos = seguranca.checar_violencia(analise, "", CFG)
    assert motivos and all(m.gravidade == seguranca.AVISO for m in motivos)


def test_sem_violencia_nao_gera_motivo():
    analise = {"analisado": True, "segundos_estimados": 0.0, "proporcao": 0.0}
    assert seguranca.checar_violencia(analise, "", CFG) == []


def test_termo_violento_na_fala_gera_aviso():
    analise = {"analisado": True, "segundos_estimados": 0.0}
    motivos = seguranca.checar_violencia(analise, "cena de tortura pesada", CFG)
    assert any(m.codigo == "violencia_na_fala" for m in motivos)


def test_configuracao_sem_nenhuma_violencia_bloqueia_qualquer_cena():
    cfg = carregar_config()
    cfg["seguranca"]["nivel_violencia_maximo"] = "nenhum"
    analise = {"analisado": True, "segundos_estimados": 1.0}
    motivos = seguranca.checar_violencia(analise, "", cfg)
    assert any(m.gravidade == seguranca.BLOQUEIO for m in motivos)


# ============================================================================
#  SEGURANÇA — REGISTRO DO RESULTADO
# ============================================================================

def _ficha(inicio: float = 60, fim: float = 120) -> dict:
    return {
        "arquivo": "saida/teste.mp4", "idioma": "pt", "modo": "A_corte",
        "assunto": "GTA 6 mapa completo",
        "trecho": {"inicio": inicio, "fim": fim, "duracao": fim - inicio},
        "fonte": video_falso().para_dict(),
    }


def test_video_aprovado_entra_na_fila_e_marca_o_trecho():
    with banco_temporario() as conexao:
        aprovacao = seguranca.Avaliacao(aprovado=True, selo="verde")
        seguranca.registrar_aprovacao(conexao, _ficha(), aprovacao)

        assert len(banco.listar_fila(conexao, "fila")) == 1
        repetido, _ = banco.trecho_ja_usado(conexao, "abc123", 60, 120)
        assert repetido
        usado, _ = banco.assunto_ja_usado(conexao, "GTA 6 mapa completo", "pt")
        assert usado


def test_video_reprovado_nao_ocupa_o_trecho():
    """Corte reprovado pode ser reaproveitado depois — não fica preso no log."""
    with banco_temporario() as conexao:
        reprovacao = seguranca.Avaliacao(aprovado=False, selo="vermelho")
        seguranca.registrar_aprovacao(conexao, _ficha(), reprovacao)

        assert len(banco.listar_fila(conexao, "fila")) == 0
        assert len(banco.listar_fila(conexao, "reprovado")) == 1
        repetido, _ = banco.trecho_ja_usado(conexao, "abc123", 60, 120)
        assert not repetido


def test_video_aprovado_conta_para_a_reputacao_da_fonte():
    with banco_temporario() as conexao:
        banco.registrar_fonte(conexao, "canal_x", "Canal X")
        seguranca.registrar_aprovacao(
            conexao, _ficha(), seguranca.Avaliacao(aprovado=True, selo="amarelo"))
        assert banco.obter_fonte(conexao, "canal_x").videos_gerados == 1


def test_resumo_do_sistema():
    with banco_temporario() as conexao:
        banco.registrar_fonte(conexao, "canal_x", "Canal X")
        seguranca.registrar_aprovacao(
            conexao, _ficha(), seguranca.Avaliacao(aprovado=True, selo="verde"))
        seguranca.registrar_aprovacao(
            conexao, _ficha(300, 360), seguranca.Avaliacao(aprovado=False,
                                                           selo="vermelho"))
        estado = banco.resumo(conexao)
        assert estado["na_fila"] == 1 and estado["reprovados"] == 1


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
