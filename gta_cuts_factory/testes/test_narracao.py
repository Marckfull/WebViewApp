"""
test_narracao.py — testes automáticos do MODO B (roteiro + voz).

Nenhum teste aqui acessa a internet nem gera áudio de verdade: eles conferem a
montagem do roteiro, o corte por duração e a distribuição dos tempos das
palavras (que é o que a legenda usa).

COMO RODAR (dentro da pasta gta_cuts_factory):

    python testes\\test_narracao.py
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import narracao  # noqa: E402
from nucleo.config import carregar_config  # noqa: E402
from nucleo.narracao import Narracao, Roteiro  # noqa: E402


CFG = carregar_config()

FATOS = [
    "a Rockstar publicou um comunicado no site oficial",
    "o comunicado fala do segundo trailer, sem data fechada",
    "a página do jogo foi atualizada no mesmo dia",
]


# ============================================================================
#  ROTEIRO
# ============================================================================

def test_roteiro_usa_os_fatos_que_voce_passou():
    roteiro = narracao.criar_roteiro("GTA 6 trailer", CFG, "pt", FATOS)
    texto = roteiro.texto.lower()
    assert "comunicado no site oficial" in texto
    assert "segundo trailer" in texto
    assert roteiro.fatos_usados == FATOS


def test_roteiro_sempre_avisa_que_e_rumor():
    """Proteção contra desinformação: a ressalva é obrigatória."""
    roteiro = narracao.criar_roteiro("GTA 6 data", CFG, "pt", FATOS)
    assert "rumor" in roteiro.texto.lower()


def test_roteiro_sem_fatos_nao_inventa_nada():
    roteiro = narracao.criar_roteiro("GTA 6 mapa", CFG, "pt", fatos=[])
    texto = roteiro.texto.lower()
    assert "nada foi confirmado" in texto
    assert any("não passou nenhum fato" in a for a in roteiro.avisos)


def test_roteiro_curto_gera_aviso():
    roteiro = narracao.criar_roteiro("GTA 6 mapa", CFG, "pt", ["um fato curto"])
    assert any("Roteiro curto" in a for a in roteiro.avisos)


def test_roteiro_em_ingles_sai_em_ingles():
    roteiro = narracao.criar_roteiro("GTA 6 release date", CFG, "en",
                                     ["Rockstar posted a statement"])
    texto = roteiro.texto.lower()
    assert "rumor" in texto and "vale lembrar" not in texto
    assert roteiro.idioma == "en"


def test_todas_as_frases_terminam_com_pontuacao():
    """A voz sintética precisa da pontuação para dar as pausas."""
    roteiro = narracao.criar_roteiro("GTA 6 trailer", CFG, "pt", FATOS)
    for bloco in roteiro.blocos:
        assert bloco.rstrip()[-1] in ".!?…", f"bloco sem pontuação: {bloco}"


def test_roteiro_respeita_a_duracao_alvo():
    cfg = carregar_config()
    cfg["narracao"]["duracao_alvo"] = 20        # bem curto de propósito
    muitos_fatos = [f"este é o fato número {i} com bastante texto para ocupar "
                    f"tempo de narração" for i in range(12)]
    roteiro = narracao.criar_roteiro("GTA 6", cfg, "pt", muitos_fatos)
    # tolerância de um bloco: o corte é feito em frase inteira, nunca no meio
    assert roteiro.duracao_estimada() <= 20 * 1.6


def test_corte_por_duracao_nao_quebra_frase():
    blocos = ["Primeira frase completa aqui.", "Segunda frase completa aqui.",
              "Terceira frase completa aqui."]
    cortados = narracao._cortar_para_duracao(blocos, "pt", 2.0)
    assert all(bloco in blocos for bloco in cortados)
    assert cortados


def test_duracao_estimada_bate_com_o_tamanho():
    roteiro = Roteiro("x", "pt", blocos=[" ".join(["palavra"] * 155)])
    assert 55 <= roteiro.duracao_estimada() <= 65   # 155 palavras ≈ 60s


# ============================================================================
#  TEMPOS DAS PALAVRAS (o que alimenta a legenda karaokê)
# ============================================================================

def test_tempos_estimados_cobrem_a_duracao_toda():
    palavras = narracao.estimar_tempos_das_palavras(
        "uma frase de teste com algumas palavras aqui", 10.0)
    assert palavras
    assert palavras[0].inicio == 0.0
    assert 9.0 <= palavras[-1].fim <= 10.0


def test_tempos_estimados_ficam_em_ordem():
    palavras = narracao.estimar_tempos_das_palavras(
        "primeira segunda terceira quarta quinta sexta", 6.0)
    for anterior, seguinte in zip(palavras, palavras[1:]):
        assert anterior.inicio <= seguinte.inicio
        assert anterior.fim <= seguinte.fim + 0.001


def test_palavra_maior_dura_mais():
    """Dividir por caractere (e não por palavra) deixa a legenda mais certa."""
    palavras = narracao.estimar_tempos_das_palavras("oi extraordinariamente", 5.0)
    assert palavras[1].duracao > palavras[0].duracao


def test_texto_vazio_nao_quebra():
    assert narracao.estimar_tempos_das_palavras("", 10.0) == []
    assert narracao.estimar_tempos_das_palavras("texto", 0.0) == []


# ============================================================================
#  INTEGRAÇÃO COM O RESTO DO SISTEMA
# ============================================================================

def test_transcricao_do_roteiro_tem_o_formato_esperado():
    """
    O Modo B tem que devolver o MESMO formato do Whisper, para reaproveitar
    legenda, segurança e metadados sem nenhum caso especial.
    """
    roteiro = narracao.criar_roteiro("GTA 6 trailer", CFG, "pt", FATOS)
    palavras = narracao.estimar_tempos_das_palavras(roteiro.texto, 40.0)
    voz = Narracao(Path("fake.mp3"), palavras, 40.0, "voz", "teste", False)

    resultado = narracao.transcricao_do_roteiro(roteiro, voz)
    assert resultado["idioma"] == "pt"
    assert resultado["texto"] == roteiro.texto
    assert len(resultado["palavras"]) == len(palavras)
    assert set(resultado["palavras"][0]) == {"texto", "inicio", "fim"}
    assert resultado["origem"] == "narracao_propria"


def test_transcricao_do_roteiro_serve_para_a_legenda():
    from nucleo import legendas

    roteiro = narracao.criar_roteiro("GTA 6 trailer", CFG, "pt", FATOS)
    palavras = narracao.estimar_tempos_das_palavras(roteiro.texto, 40.0)
    conteudo = legendas.gerar_ass(palavras, CFG, "pt")
    eventos = [l for l in conteudo.splitlines() if l.startswith("Dialogue:")]
    assert len(eventos) == len(palavras)


def test_modo_b_passa_na_checagem_de_cinematica():
    """
    A regra da Rockstar bloqueia cinemática isolada — mas o Modo B tem
    narração sua, então tem que passar.
    """
    from nucleo import seguranca

    fala = {"palavras": 5, "segundos_falados": 2.0, "cobertura": 0.03}
    motivos = seguranca.checar_narracao(fala, CFG, "oficial", modo="B_narrado")
    assert all(m.gravidade != seguranca.BLOQUEIO for m in motivos)


def test_roteiro_vazio_e_recusado():
    from nucleo.config import carregar_config as _carregar

    roteiro = Roteiro("x", "pt", blocos=[])
    try:
        narracao.narrar(roteiro, _carregar(), "/tmp/nao_deve_existir.mp3")
    except ValueError:
        return
    raise AssertionError("deveria ter recusado um roteiro vazio")


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
