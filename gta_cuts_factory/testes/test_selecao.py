"""
test_selecao.py — testes automáticos da escolha dos melhores momentos.

Tudo roda com transcrições de mentirinha, sem internet e sem Whisper.

COMO RODAR (dentro da pasta gta_cuts_factory):

    python testes\\test_selecao.py
"""

from __future__ import annotations

import sqlite3
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import banco, selecao, transcricao  # noqa: E402
from nucleo.config import carregar_config  # noqa: E402
from nucleo.selecao import Momento  # noqa: E402


CFG = carregar_config()


def banco_temporario() -> sqlite3.Connection:
    conexao = sqlite3.connect(":memory:")
    conexao.row_factory = sqlite3.Row
    conexao.executescript(banco.ESQUEMA)
    return conexao


def montar_transcricao(frases: list[tuple[str, float, float]]) -> dict:
    """Monta uma transcrição completa (frases + palavras) a partir de uma lista."""
    lista_frases, palavras = [], []
    for texto, inicio, fim in frases:
        lista_frases.append({"texto": texto, "inicio": float(inicio), "fim": float(fim)})
        partes = texto.split()
        passo = (fim - inicio) / max(1, len(partes))
        for i, parte in enumerate(partes):
            comeco = inicio + i * passo
            palavras.append({"texto": parte, "inicio": round(comeco, 2),
                             "fim": round(comeco + passo * 0.9, 2)})
    return {"idioma": "pt", "texto": " ".join(f[0] for f in frases),
            "frases": lista_frases, "palavras": palavras}


def falatorio(quantidade: int, inicio: float = 0.0, duracao_frase: float = 10.0,
              texto: str = "conteúdo comum de um vídeo sobre o jogo hoje em dia"
              ) -> list[tuple[str, float, float]]:
    """Gera várias frases neutras (para preencher a linha do tempo)."""
    return [(f"{texto} número {i}.", inicio + i * duracao_frase,
             inicio + (i + 1) * duracao_frase) for i in range(quantidade)]


# ============================================================================
#  FRASES
# ============================================================================

def test_frases_vem_da_transcricao():
    dados = montar_transcricao([("Primeira frase aqui.", 0, 5),
                                ("Segunda frase aqui.", 5, 10)])
    frases = selecao.frases_da_transcricao(dados)
    assert len(frases) == 2 and frases[0].texto == "Primeira frase aqui."


def test_frases_reconstruidas_das_palavras():
    """Se a transcrição não trouxer frases, elas são remontadas das palavras."""
    dados = {"palavras": [
        {"texto": "olha", "inicio": 0.0, "fim": 0.4},
        {"texto": "isso.", "inicio": 0.4, "fim": 0.9},
        {"texto": "agora", "inicio": 3.0, "fim": 3.5},   # pausa longa = nova frase
        {"texto": "vamos", "inicio": 3.5, "fim": 4.0},
    ]}
    frases = selecao.frases_da_transcricao(dados)
    assert len(frases) == 2
    assert frases[0].texto == "olha isso."


def test_transcricao_vazia_nao_quebra():
    assert selecao.escolher_momentos({}, CFG) == []


# ============================================================================
#  CANDIDATOS
# ============================================================================

def test_candidatos_respeitam_duracao_minima_e_maxima():
    dados = montar_transcricao(falatorio(40))     # 400 segundos de fala
    candidatos = selecao.construir_candidatos(selecao.frases_da_transcricao(dados), CFG)
    minimo = float(CFG.pegar("video.duracao_minima_corte"))
    maximo = float(CFG.pegar("video.duracao_maxima_corte"))
    assert candidatos
    assert all(minimo <= c.duracao <= maximo for c in candidatos)


def test_candidatos_comecam_e_terminam_em_frase_completa():
    frases_brutas = falatorio(30)
    dados = montar_transcricao(frases_brutas)
    candidatos = selecao.construir_candidatos(selecao.frases_da_transcricao(dados), CFG)
    inicios = {f[1] for f in frases_brutas}
    fins = {f[2] for f in frases_brutas}
    assert all(c.inicio in inicios and c.fim in fins for c in candidatos)


def test_video_curto_demais_nao_gera_candidato():
    dados = montar_transcricao([("Frase curta.", 0, 5), ("Outra frase.", 5, 12)])
    assert selecao.construir_candidatos(selecao.frases_da_transcricao(dados), CFG) == []


# ============================================================================
#  NOTA
# ============================================================================

def _nota(texto_primeira: str, corpo: list[str], inicio: float = 100.0) -> float:
    momento = Momento(inicio=inicio, fim=inicio + 90,
                      texto=" ".join([texto_primeira] + corpo),
                      primeira_frase=texto_primeira)
    nota, _, _ = selecao.pontuar_momento(momento, CFG, "pt", duracao_video=600)
    return nota


def test_gancho_na_abertura_vale_mais_que_no_meio():
    com_gancho = _nota("Olha isso que vazou agora.", ["Falando do jogo."] * 8)
    sem_gancho = _nota("Falando do jogo normalmente.", ["Falando do jogo."] * 8)
    assert com_gancho > sem_gancho


def test_autopromocao_derruba_a_nota():
    limpo = _nota("A Rockstar confirmou a data.", ["Detalhe importante."] * 8)
    promocional = _nota("A Rockstar confirmou a data.",
                        ["Se inscreve no canal e deixa o like."] * 8)
    assert promocional < limpo
    momento = Momento(100, 190, "se inscreve no canal, link na descrição",
                      primeira_frase="se inscreve no canal")
    _, motivos, _ = selecao.pontuar_momento(momento, CFG, "pt", 600)
    assert any("autopromoção" in m for m in motivos)


def test_palavras_chave_somam_pontos():
    com_chave = _nota("A Rockstar confirmou o GTA 6.",
                      ["O trailer oficial vazou com a data."] * 6)
    sem_chave = _nota("Ele falou uma coisa qualquer.",
                      ["Assunto comum sem termo forte."] * 6)
    assert com_chave > sem_chave


def test_emocao_soma_pontos():
    com_emocao = _nota("A data saiu.", ["Caramba, que loucura isso!"] * 6)
    sem_emocao = _nota("A data saiu.", ["Achei interessante o anúncio."] * 6)
    assert com_emocao > sem_emocao


def test_trecho_na_introducao_perde_pontos():
    inicio_do_video = _nota("A Rockstar confirmou a data.", ["Detalhe."] * 8, inicio=5)
    meio_do_video = _nota("A Rockstar confirmou a data.", ["Detalhe."] * 8, inicio=200)
    assert meio_do_video > inicio_do_video


def test_trecho_na_despedida_perde_pontos():
    momento_fim = Momento(500, 590, "A Rockstar confirmou a data do GTA 6.",
                          primeira_frase="A Rockstar confirmou a data do GTA 6.")
    momento_meio = Momento(200, 290, "A Rockstar confirmou a data do GTA 6.",
                           primeira_frase="A Rockstar confirmou a data do GTA 6.")
    nota_fim, motivos, _ = selecao.pontuar_momento(momento_fim, CFG, "pt", 600)
    nota_meio, _, _ = selecao.pontuar_momento(momento_meio, CFG, "pt", 600)
    assert nota_fim < nota_meio
    assert any("despedida" in m for m in motivos)


def test_pouca_fala_perde_pontos():
    momento_cheio = Momento(100, 190, " ".join(["palavra"] * 200),
                            primeira_frase="palavra")
    momento_vazio = Momento(100, 190, "só isso aqui", primeira_frase="só isso aqui")
    nota_cheio, _, _ = selecao.pontuar_momento(momento_cheio, CFG, "pt", 600)
    nota_vazio, _, _ = selecao.pontuar_momento(momento_vazio, CFG, "pt", 600)
    assert nota_cheio > nota_vazio


# ============================================================================
#  ESCOLHA FINAL
# ============================================================================

def transcricao_com_dois_ouros() -> dict:
    """Intro promocional, papo morno, e dois trechos fortes bem separados."""
    frases: list[tuple[str, float, float]] = [
        ("Se inscreve no canal e deixa o like, ativa o sininho.", 0, 10),
        ("Esse vídeo é patrocinado, o cupom está no link na descrição.", 10, 20),
    ]
    frases += falatorio(8, inicio=20)                       # 20s → 100s: morno
    frases += [
        ("Olha isso que vazou agora sobre o GTA 6!", 100, 110),
        ("A Rockstar confirmou a data de lançamento oficial.", 110, 120),
        ("Caramba, isso muda tudo o que a gente sabia!", 120, 130),
        ("O trailer novo do GTA 6 vem junto com o anúncio.", 130, 140),
        ("É impressionante o tamanho dessa confirmação.", 140, 150),
        ("A data está confirmada no comunicado oficial.", 150, 160),
        ("Muita gente vai ficar chocada com isso.", 160, 170),
    ]
    frases += falatorio(6, inicio=170)                      # 170s → 230s: morno
    frases += [
        ("Agora presta atenção no mapa do GTA 6, ninguém percebeu.", 230, 240),
        ("Meu Deus, o mapa é muito maior do que imaginavam.", 240, 250),
        ("Comparando com o GTA 5 dá quase o dobro de tamanho.", 250, 260),
        ("Tem uma região secreta que não apareceu no trailer.", 260, 270),
        ("Isso confirma o vazamento antigo do mapa completo.", 270, 280),
        ("Que loucura o trabalho da Rockstar nesse mapa novo.", 280, 290),
        ("Eu fiquei impressionado com o nível de detalhe.", 290, 300),
    ]
    frases += falatorio(4, inicio=300)
    return montar_transcricao(frases)


def test_escolhe_os_trechos_fortes_e_ignora_a_intro():
    momentos = selecao.escolher_momentos(
        transcricao_com_dois_ouros(), CFG, quantidade=2, duracao_video=340)
    assert len(momentos) == 2
    textos = " ".join(m.texto.lower() for m in momentos)
    assert "se inscreve" not in textos and "cupom" not in textos
    assert all(m.inicio >= 90 for m in momentos)


def test_momentos_escolhidos_nao_se_cruzam():
    momentos = selecao.escolher_momentos(
        transcricao_com_dois_ouros(), CFG, quantidade=3, duracao_video=340)
    folga = float(CFG.pegar("selecao.distancia_minima_entre_cortes", 30))
    for i, a in enumerate(momentos):
        for b in momentos[i + 1:]:
            assert not a.sobrepoe(b, folga)


def test_trecho_ruim_nao_entra_so_para_completar_a_cota():
    """Melhor entregar 2 cortes bons do que 3 com um pegando patrocínio."""
    momentos = selecao.escolher_momentos(
        transcricao_com_dois_ouros(), CFG, quantidade=3, duracao_video=340)
    assert len(momentos) < 3
    nota_minima = float(CFG.pegar("selecao.nota_minima", 1.0))
    assert all(m.pontuacao >= nota_minima for m in momentos)


def test_momentos_saem_em_ordem_cronologica():
    momentos = selecao.escolher_momentos(
        transcricao_com_dois_ouros(), CFG, quantidade=3, duracao_video=340)
    assert momentos == sorted(momentos, key=lambda m: m.inicio)


def test_trecho_ja_usado_no_banco_nao_e_escolhido():
    with banco_temporario() as conexao:
        dados = transcricao_com_dois_ouros()
        primeiro = selecao.escolher_momentos(dados, CFG, 1, duracao_video=340)[0]
        banco.registrar_trecho(conexao, "video_x", primeiro.inicio, primeiro.fim)

        seguintes = selecao.escolher_momentos(
            dados, CFG, 2, duracao_video=340, conexao=conexao, video_id="video_x")
        for momento in seguintes:
            repetido, _ = banco.trecho_ja_usado(
                conexao, "video_x", momento.inicio, momento.fim)
            assert not repetido


def test_deslocamento_ajusta_os_tempos():
    """Transcrição feita a partir do minuto 10 tem que somar 600s nos tempos."""
    dados = transcricao_com_dois_ouros()
    normal = selecao.escolher_momentos(dados, CFG, 1, duracao_video=340)[0]
    deslocado = selecao.escolher_momentos(
        dados, CFG, 1, duracao_video=340, deslocamento=600)[0]
    assert abs(deslocado.inicio - (normal.inicio + 600)) < 0.01


# ============================================================================
#  RECORTE DA TRANSCRIÇÃO (usado pelo modo automático)
# ============================================================================

def test_recortar_transcricao_reinicia_os_tempos():
    dados = montar_transcricao([("Primeira parte do vídeo.", 0, 10),
                                ("Segunda parte do vídeo.", 10, 20),
                                ("Terceira parte do vídeo.", 20, 30)])
    recorte = transcricao.recortar(dados, 10, 20)
    assert len(recorte["frases"]) == 1
    assert recorte["frases"][0]["inicio"] == 0.0
    assert "Segunda" in recorte["texto"]
    assert all(p["inicio"] >= 0 for p in recorte["palavras"])


def test_recortar_guarda_a_posicao_no_original():
    dados = montar_transcricao([("Uma frase aqui.", 0, 10), ("Outra frase.", 10, 20)])
    recorte = transcricao.recortar(dados, 10, 20)
    assert recorte["inicio_no_original"] == 10.0


def test_recortar_fora_do_intervalo_devolve_vazio():
    dados = montar_transcricao([("Uma frase aqui.", 0, 10)])
    recorte = transcricao.recortar(dados, 100, 200)
    assert recorte["palavras"] == [] and recorte["frases"] == []


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
