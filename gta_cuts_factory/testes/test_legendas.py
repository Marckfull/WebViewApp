"""
test_legendas.py — testes automáticos do núcleo.

Estes testes NÃO precisam de internet, nem de vídeo, nem do Whisper: eles
conferem a "matemática" da legenda (agrupamento, tempos, cores, destaque de
palavra-chave) e o filtro de reframe.

COMO RODAR (dentro da pasta gta_cuts_factory):

    python testes\\test_legendas.py

Se aparecer "TODOS OS TESTES PASSARAM", o núcleo está saudável.
(Também funciona com pytest, se você preferir: pytest testes/)
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import gancho, legendas, reframe  # noqa: E402
from nucleo.config import carregar_config  # noqa: E402
from nucleo.legendas import Palavra  # noqa: E402
from nucleo.utils import InfoVideo, cor_para_ass, para_segundos, para_tempo_ass  # noqa: E402


CFG = carregar_config()


def _falar(texto: str, inicio: float = 0.0, passo: float = 0.4) -> list[Palavra]:
    """Cria palavras com tempos regulares, como se alguém estivesse falando."""
    palavras, momento = [], inicio
    for parte in texto.split():
        palavras.append(Palavra(parte, momento, momento + passo * 0.9))
        momento += passo
    return palavras


# ============================================================================
#  UTILITÁRIOS
# ============================================================================

def test_conversao_de_cor():
    # ASS usa &HAABBGGRR: azul e vermelho trocam de lugar
    assert cor_para_ass("#FFE100") == "&H0000E1FF"
    assert cor_para_ass("#FFFFFF") == "&H00FFFFFF"
    assert cor_para_ass("#000000") == "&H00000000"
    assert cor_para_ass("#FA0") == cor_para_ass("#FFAA00")   # formato curto


def test_conversao_de_tempo():
    assert para_segundos("90") == 90.0
    assert para_segundos("01:30") == 90.0
    assert para_segundos("00:01:30") == 90.0
    assert para_segundos("00:01:30.5") == 90.5
    assert para_tempo_ass(90.5) == "0:01:30.50"
    assert para_tempo_ass(0) == "0:00:00.00"
    assert para_tempo_ass(3661.25) == "1:01:01.25"


# ============================================================================
#  PALAVRAS-CHAVE
# ============================================================================

def test_palavra_chave_simples():
    palavras = _falar("a rockstar confirmou tudo")
    marcadas = legendas.marcar_palavras_chave(palavras, ["ROCKSTAR", "CONFIRMOU"])
    assert marcadas == [False, True, True, False]


def test_palavra_chave_de_duas_palavras():
    # "GTA 6" precisa acender as DUAS palavras
    palavras = _falar("o novo GTA 6 chegou")
    marcadas = legendas.marcar_palavras_chave(palavras, ["GTA 6"])
    assert marcadas == [False, False, True, True, False]


def test_palavra_chave_ignora_acento_e_pontuacao():
    palavras = _falar("data de lançamento!")
    marcadas = legendas.marcar_palavras_chave(palavras, ["LANCAMENTO", "DATA"])
    assert marcadas == [True, False, True]


# ============================================================================
#  AGRUPAMENTO
# ============================================================================

def test_agrupamento_respeita_maximo_de_palavras():
    grupos = legendas.agrupar_palavras(_falar("um dois tres quatro cinco seis"),
                                       max_palavras=3, max_caracteres=100)
    assert all(len(g) <= 3 for g in grupos)
    assert sum(len(g) for g in grupos) == 6   # nenhuma palavra some


def test_agrupamento_quebra_em_pausa_longa():
    palavras = [
        Palavra("antes", 0.0, 0.4),
        Palavra("da", 0.4, 0.7),
        Palavra("pausa", 5.0, 5.4),   # 4,3 s de silêncio no meio
    ]
    grupos = legendas.agrupar_palavras(palavras, max_palavras=5, pausa_maxima=0.7)
    assert len(grupos) == 2
    assert grupos[1][0].texto == "pausa"


def test_agrupamento_quebra_no_fim_da_frase():
    palavras = _falar("isso é incrível. agora vem o resto")
    grupos = legendas.agrupar_palavras(palavras, max_palavras=8, max_caracteres=200)
    assert grupos[0][-1].texto.endswith(".")


# ============================================================================
#  ARQUIVO ASS
# ============================================================================

def test_ass_tem_estrutura_valida():
    conteudo = legendas.gerar_ass(_falar("a rockstar confirmou o GTA 6"), CFG, "pt")
    assert "[Script Info]" in conteudo
    assert "[V4+ Styles]" in conteudo
    assert "[Events]" in conteudo
    assert "Style: Legenda," in conteudo
    assert f"PlayResX: {CFG.pegar('video.largura')}" in conteudo
    assert f"PlayResY: {CFG.pegar('video.altura')}" in conteudo


def test_ass_cria_um_evento_por_palavra():
    palavras = _falar("a rockstar confirmou o GTA 6")
    conteudo = legendas.gerar_ass(palavras, CFG, "pt")
    eventos = [l for l in conteudo.splitlines() if l.startswith("Dialogue:")]
    assert len(eventos) == len(palavras)


def test_ass_nao_tem_buraco_entre_palavras():
    """A legenda não pode piscar: cada evento termina onde o próximo começa."""
    palavras = _falar("uma duas tres quatro cinco seis sete oito")
    eventos = legendas.eventos_legenda(palavras, CFG, "pt")
    tempos = [(l.split(",")[1], l.split(",")[2]) for l in eventos]
    for (_, fim), (inicio, _) in zip(tempos, tempos[1:]):
        assert fim <= inicio, f"buraco/sobreposição entre {fim} e {inicio}"


def test_ass_destaca_palavra_ativa_e_palavra_chave():
    palavras = _falar("o GTA 6 vazou")
    conteudo = legendas.gerar_ass(palavras, CFG, "pt")
    cor_ativa = cor_para_ass(CFG.pegar("legendas.cor_palavra_ativa"))
    cor_chave = cor_para_ass(CFG.pegar("legendas.cor_palavra_chave"))
    assert cor_ativa in conteudo          # palavra sendo falada
    assert cor_chave in conteudo          # "GTA 6" / "VAZOU"
    assert "\\t(0,90," in conteudo        # animação de "pop"


def test_ass_escapa_caracteres_perigosos():
    """Chaves { } são comandos no ASS: precisam ser neutralizadas."""
    palavras = [Palavra("{teste}", 0.0, 0.5), Palavra("normal", 0.5, 1.0)]
    conteudo = legendas.gerar_ass(palavras, CFG, "pt")
    linha = [l for l in conteudo.splitlines() if l.startswith("Dialogue:")][0]
    corpo = linha.split(",,", 1)[1]
    assert "{teste}" not in corpo
    assert "(TESTE)" in corpo or "(teste)" in corpo


def test_bloco_largo_demais_vira_duas_linhas():
    """
    Quando o bloco não cabe na largura da tela, ele tem que virar duas linhas
    em vez de vazar para fora do vídeo.
    """
    cfg = carregar_config()
    cfg["legendas"]["max_caracteres_linha"] = 60   # força um bloco bem largo
    cfg["legendas"]["palavras_por_linha"] = 3
    palavras = [
        Palavra("CONFIRMADISSIMO", 0.0, 0.5),
        Palavra("ABSOLUTAMENTE", 0.5, 1.0),
        Palavra("IMPRESSIONANTE", 1.0, 1.5),
    ]
    conteudo = legendas.gerar_ass(palavras, cfg, "pt")
    assert "\\N" in conteudo   # \N = quebra de linha do ASS


def test_bloco_curto_fica_em_uma_linha_so():
    conteudo = legendas.gerar_ass(_falar("vai ter"), CFG, "pt")
    assert "\\N" not in conteudo


def test_ass_lista_vazia_nao_quebra():
    """Corte sem fala (só gameplay) tem que gerar arquivo válido mesmo assim."""
    conteudo = legendas.gerar_ass([], CFG, "pt")
    assert "[Events]" in conteudo


# ============================================================================
#  GANCHO
# ============================================================================

def test_gancho_gera_frase_no_idioma_certo():
    frase_pt = gancho.criar_frase("GTA 6", CFG, "pt")
    frase_en = gancho.criar_frase("GTA 6", CFG, "en")
    assert "GTA 6" in frase_pt and len(frase_pt) <= 70
    assert "GTA 6" in frase_en
    assert frase_pt == frase_pt.upper()   # maiúsculas ligadas no config


def test_gancho_aparece_no_primeiro_segundo():
    eventos = gancho.eventos_gancho("VAZOU AGORA: GTA 6", CFG, "pt")
    assert len(eventos) == 1
    assert eventos[0].split(",")[1] == "0:00:00.00"     # começa no frame 0
    assert "\\an5" in eventos[0] and "\\pos(" in eventos[0]


def test_gancho_pinta_palavra_chave():
    eventos = gancho.eventos_gancho("A ROCKSTAR CONFIRMOU O GTA 6", CFG, "pt")
    assert cor_para_ass(CFG.pegar("gancho.cor_destaque")) in eventos[0]


# ============================================================================
#  REFRAME 9:16
# ============================================================================

def test_reframe_corta_faixa_correta_de_video_16x9():
    info = InfoVideo(1920, 1080, 120.0, 30.0, True)
    filtro = reframe.construir_filtro(info, CFG, foco_x=0.5)
    # 1080 de altura * 9/16 = 607,5 → 608 (largura par, exigência do codec).
    # Centralizado: x = (1920 - 608) / 2 = 656
    assert "crop=608:1080:656:0" in filtro
    assert "scale=1080:1920" in filtro


def test_reframe_segue_o_foco_sem_sair_da_tela():
    info = InfoVideo(1920, 1080, 120.0, 30.0, True)
    esquerda = reframe.construir_filtro(info, CFG, foco_x=0.0)
    direita = reframe.construir_filtro(info, CFG, foco_x=1.0)
    assert "crop=608:1080:0:0" in esquerda            # colado na borda esquerda
    assert "crop=608:1080:1312:0" in direita          # colado na borda direita


def test_reframe_video_ja_vertical_usa_fundo_desfocado():
    info = InfoVideo(1080, 1920, 60.0, 30.0, True)
    filtro = reframe.construir_filtro(info, CFG, foco_x=0.5)
    assert "gblur" in filtro and "overlay" in filtro


def test_reframe_modo_centro_ignora_o_foco():
    cfg = carregar_config()
    cfg["video"]["modo_reframe"] = "centro"
    info = InfoVideo(1920, 1080, 60.0, 30.0, True)
    assert (reframe.construir_filtro(info, cfg, foco_x=0.9)
            == reframe.construir_filtro(info, cfg, foco_x=0.1))


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
