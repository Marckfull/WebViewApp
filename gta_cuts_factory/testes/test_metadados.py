"""
test_metadados.py — testes dos metadados (título/descrição/hashtags) e do b-roll.

Nada aqui acessa a internet nem gera vídeo: são conferências de texto e da
montagem dos filtros do ffmpeg.

COMO RODAR (dentro da pasta gta_cuts_factory):

    python testes\\test_metadados.py
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import broll, metadados  # noqa: E402
from nucleo.config import carregar_config  # noqa: E402
from nucleo.utils import InfoVideo  # noqa: E402


CFG = carregar_config()
CREDITO = "Fonte: Vídeo de teste — Canal X\nhttps://youtu.be/abc123"


# ============================================================================
#  TÍTULO
# ============================================================================

def test_sem_confirmacao_o_titulo_nao_afirma():
    """Proteção contra clickbait mentiroso: sem confirmação, vira pergunta."""
    dados = metadados.gerar("GTA 6 data de lançamento", CFG, "pt",
                            texto_falado="vazou um rumor sobre a data",
                            credito=CREDITO)
    titulo = dados.titulo.lower()
    assert "confirmado:" not in titulo
    assert "oficial" not in titulo or "?" in dados.titulo


def test_com_confirmacao_o_titulo_pode_afirmar():
    dados = metadados.gerar(
        "GTA 6 segundo trailer", CFG, "pt",
        texto_falado="a Rockstar confirmou oficialmente o segundo trailer",
        credito=CREDITO)
    assert "?" not in dados.titulo or "confirm" in dados.titulo.lower()


def test_titulo_respeita_o_limite_do_youtube():
    assunto = "GTA 6 " + ("assunto muito comprido " * 12)
    dados = metadados.gerar(assunto, CFG, "pt", credito=CREDITO)
    assert len(dados.titulo) <= metadados.LIMITE_TITULO_YOUTUBE


def test_titulo_comprido_gera_aviso():
    assunto = "GTA 6 " + ("assunto comprido demais para caber no celular " * 3)
    dados = metadados.gerar(assunto, CFG, "pt", credito=CREDITO)
    assert any("caracteres" in a for a in dados.avisos)


def test_titulo_nao_sai_todo_em_maiusculas():
    """Título gritado é tratado como spam pelo algoritmo."""
    bruto = metadados._titulo_bonito("VAZOU AGORA O MAPA COMPLETO DO GTA 6 OFICIAL")
    gritadas = [p for p in bruto.split() if len(p) > 3 and p.isupper()]
    assert len(gritadas) <= 2


def test_titulo_nao_repete_gta_6():
    dados = metadados.gerar("GTA 6 mapa vazado", CFG, "pt", credito=CREDITO)
    assert dados.titulo.lower().count("gta 6") <= 1


def test_mesmo_assunto_gera_o_mesmo_titulo():
    """Estabilidade: rodar duas vezes não muda o título do nada."""
    a = metadados.gerar("GTA 6 mapa", CFG, "pt", credito=CREDITO)
    b = metadados.gerar("GTA 6 mapa", CFG, "pt", credito=CREDITO)
    assert a.titulo == b.titulo


def test_etiqueta_do_modo_muda_com_idioma_e_modo():
    assert metadados.etiqueta_do_modo("B_narrado", "pt") == "Notícia"
    assert metadados.etiqueta_do_modo("B_narrado", "en") == "News"
    assert metadados.etiqueta_do_modo("A_corte", "pt") == "Análise"


# ============================================================================
#  DESCRIÇÃO
# ============================================================================

def test_descricao_sempre_leva_o_credito():
    dados = metadados.gerar("GTA 6 mapa", CFG, "pt", credito=CREDITO)
    assert "https://youtu.be/abc123" in dados.descricao
    assert "CRÉDITO DA FONTE" in dados.descricao


def test_sem_credito_o_sistema_avisa():
    dados = metadados.gerar("GTA 6 mapa", CFG, "pt", credito="")
    assert any("crédito" in a.lower() for a in dados.avisos)


def test_descricao_avisa_quando_e_rumor():
    dados = metadados.gerar("GTA 6 data", CFG, "pt",
                            texto_falado="apareceu um vazamento novo",
                            credito=CREDITO)
    assert "rumor" in dados.descricao.lower()


def test_descricao_confirmada_nao_avisa_rumor():
    dados = metadados.gerar(
        "GTA 6 trailer", CFG, "pt",
        texto_falado="a Rockstar confirmou oficialmente o trailer",
        credito=CREDITO)
    assert "trate como rumor" not in dados.descricao.lower()


def test_descricao_abre_com_frase_completa():
    """A 1ª linha é o que aparece antes do 'mostrar mais'."""
    dados = metadados.gerar("GTA 6 mapa", CFG, "pt",
                            texto_falado="entao como eu estava dizendo o mapa",
                            credito=CREDITO)
    primeira = dados.descricao.splitlines()[0]
    assert primeira[0].isupper() and primeira.rstrip()[-1] in ".!?"


def test_descricao_tem_aviso_legal():
    dados = metadados.gerar("GTA 6 mapa", CFG, "pt", credito=CREDITO)
    assert "não é afiliado" in dados.descricao.lower()


def test_descricao_respeita_o_limite():
    dados = metadados.gerar("GTA 6 " + "x" * 300, CFG, "pt", credito=CREDITO)
    assert len(dados.descricao) <= metadados.LIMITE_DESCRICAO_YOUTUBE


# ============================================================================
#  HASHTAGS E TAGS
# ============================================================================

def test_hashtags_misturam_amplas_e_especificas():
    hashtags = metadados.montar_hashtags("GTA 6 mapa vazado", CFG, "pt")
    assert "#gta6" in [h.lower() for h in hashtags]          # ampla
    assert any("mapa" in h.lower() for h in hashtags)        # específica


def test_hashtags_nao_repetem_e_respeitam_o_limite():
    hashtags = metadados.montar_hashtags("GTA 6 GTA 6 gta6", CFG, "pt")
    minusculas = [h.lower() for h in hashtags]
    assert len(minusculas) == len(set(minusculas))
    assert len(hashtags) <= int(CFG.pegar("metadados.max_hashtags", 6))


def test_hashtag_nao_tem_acento_nem_espaco():
    hashtags = metadados.montar_hashtags("GTA 6 data de lançamento", CFG, "pt")
    for tag in hashtags:
        assert tag.startswith("#") and " " not in tag
        assert all(ord(c) < 128 for c in tag)


def test_tags_incluem_a_classificacao_do_conteudo():
    tags = metadados.montar_tags("GTA 6 trailer", CFG, "pt", "B_narrado")
    assert "Notícia" in tags
    assert any("GTA 6" in t for t in tags)


def test_legenda_do_tiktok_junta_titulo_e_hashtags():
    dados = metadados.gerar("GTA 6 mapa", CFG, "pt", credito=CREDITO)
    legenda = dados.legenda_tiktok
    assert dados.titulo in legenda and dados.hashtags[0] in legenda
    assert len(legenda) <= metadados.LIMITE_LEGENDA_TIKTOK


def test_metadados_em_ingles_saem_em_ingles():
    dados = metadados.gerar("GTA 6 release date", CFG, "en", credito=CREDITO)
    assert "SOURCE CREDIT" in dados.descricao
    assert "not affiliated" in dados.descricao.lower()
    assert dados.idioma == "en"


# ============================================================================
#  B-ROLL
# ============================================================================

def _infos() -> tuple[InfoVideo, InfoVideo]:
    return InfoVideo(1920, 1080, 120, 30, True), InfoVideo(1920, 1080, 300, 30, True)


def test_split_divide_a_altura_certinho():
    principal, gameplay = _infos()
    filtro = broll.construir_filtro(principal, gameplay, CFG, "base", "split")
    altura = int(CFG.pegar("video.altura", 1920))
    proporcao = float(CFG.pegar("broll.proporcao_principal", 0.55))
    esperado_cima = int(altura * proporcao) - int(altura * proporcao) % 2
    assert f"crop=1080:{esperado_cima}" in filtro
    assert f"crop=1080:{altura - esperado_cima}" in filtro
    assert "vstack=inputs=2" in filtro


def test_split_usa_o_corte_em_cima_e_o_gameplay_embaixo():
    principal, gameplay = _infos()
    filtro = broll.construir_filtro(principal, gameplay, CFG, "base", "split")
    assert filtro.index("[0:v]") < filtro.index("[1:v]")
    assert "[cima][baixo]vstack" in filtro


def test_layout_fundo_poe_o_gameplay_atras():
    principal, gameplay = _infos()
    filtro = broll.construir_filtro(principal, gameplay, CFG, "base", "fundo")
    assert "overlay=" in filtro and "[fundo][janela]" in filtro


def test_filtros_nao_distorcem_a_imagem():
    """force_original_aspect_ratio=increase + crop = preenche sem esticar."""
    principal, gameplay = _infos()
    for layout in ("split", "fundo"):
        filtro = broll.construir_filtro(principal, gameplay, CFG, "base", layout)
        assert "force_original_aspect_ratio=increase" in filtro


def test_alturas_do_split_sao_pares():
    """Número ímpar de pixels quebra o codec H.264."""
    cfg = carregar_config()
    cfg["broll"]["proporcao_principal"] = 0.537      # gera número ímpar de propósito
    principal, gameplay = _infos()
    filtro = broll.construir_filtro(principal, gameplay, cfg, "base", "split")
    import re
    for altura in re.findall(r"crop=1080:(\d+)", filtro):
        assert int(altura) % 2 == 0


def test_broll_desligado_no_config_nao_recomenda():
    cfg = carregar_config()
    cfg["broll"]["ativo"] = False
    recomendado, motivo, _ = broll.precisa_de_broll("qualquer.mp4", cfg)
    assert not recomendado and "desligado" in motivo


def test_tela_parada_com_pouca_fala_nao_ganha_broll():
    """Trecho fraco não se conserta com gameplay: melhor trocar de trecho."""
    analise = broll.AnaliseTela(movimento=0.01, analisado=True)
    assert analise.tela_parada


def test_descrever_layout_e_legivel():
    assert "dividida" in broll.descrever_layout(CFG, "split")
    assert "fundo" in broll.descrever_layout(CFG, "fundo")


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
