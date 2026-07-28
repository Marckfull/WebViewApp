"""
producao.py — o "chefe": é ele que o botão CRIAR aciona.

Junta tudo o que foi construído nas etapas anteriores e produz o lote de
vídeos do dia:

    tendências → fontes → download → transcrição → melhor momento →
    🛡️ segurança → gancho → legenda → b-roll → render → metadados → fila

Duas coisas importantes de entender:

    1. UM VÍDEO QUE FALHA NÃO DERRUBA O LOTE
       Se o terceiro vídeo der erro (link fora do ar, canal bloqueado), os dois
       primeiros continuam prontos na fila. O erro aparece no painel.

    2. NADA É PUBLICADO AQUI
       Este módulo produz e coloca na FILA. Publicar é sempre uma ação sua.
"""

from __future__ import annotations

import traceback
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Callable

from . import (
    banco,
    broll,
    downloader,
    fontes as modulo_fontes,
    gancho,
    legendas,
    metadados,
    narracao,
    render,
    seguranca,
    selecao,
    tendencias,
    transcricao,
)
from .utils import info_video, log, nome_seguro, titulo


@dataclass
class PlanoVideo:
    """O que o sistema pretende fazer para cada um dos vídeos do lote."""

    indice: int
    modo: str                 # "A_corte" ou "B_narrado"
    idioma: str               # "pt" ou "en"
    candidato: object | None = None    # fontes.Candidato (Modo A)
    assunto: str = ""                  # (Modo B)

    def descrever(self) -> str:
        tipo = "corte" if self.modo == "A_corte" else "narrado"
        alvo = getattr(self.candidato, "titulo", "") or self.assunto or "(a definir)"
        return f"vídeo {self.indice + 1} — {tipo} em {self.idioma.upper()}: {alvo}"


@dataclass
class ResultadoVideo:
    """O que aconteceu com cada vídeo do lote."""

    plano: PlanoVideo
    sucesso: bool = False
    arquivo: str = ""
    selo: str = ""
    titulo: str = ""
    id_registro: int = 0
    erro: str = ""
    motivos: list[dict] = field(default_factory=list)


def planejar(cfg, candidatos: list, assuntos: list) -> list[PlanoVideo]:
    """
    Monta a lista do que vai ser produzido, ANTES de gastar tempo com download.

    Segue o que você configurou:
        producao.quantidade      → quantos vídeos (padrão 3)
        producao.idiomas         → ["pt", "pt", "en"]
        producao.modo_a_cortes   → quantos são cortes
        producao.modo_b_narrados → quantos são narrados

    Os cortes ficam com os melhores candidatos (já diversificados por canal e
    assunto); os narrados pegam os assuntos mais quentes que sobraram.
    """
    quantidade = int(cfg.pegar("producao.quantidade", 3))
    idiomas = list(cfg.pegar("producao.idiomas", ["pt", "pt", "en"]))
    total_cortes = int(cfg.pegar("producao.modo_a_cortes", 2))
    total_narrados = int(cfg.pegar("producao.modo_b_narrados", 1))

    # se a conta não fecha, o número de vídeos manda
    if total_cortes + total_narrados != quantidade:
        total_cortes = min(total_cortes, quantidade)
        total_narrados = quantidade - total_cortes

    escolhidos = modulo_fontes.diversificar(candidatos, total_cortes)
    assuntos_livres = [a.termo for a in assuntos]

    # o assunto já usado por um corte não vira vídeo narrado também
    usados = {getattr(c, "assunto", "") for c in escolhidos if getattr(c, "assunto", "")}
    assuntos_livres = [a for a in assuntos_livres if a not in usados]

    planos: list[PlanoVideo] = []
    for i in range(quantidade):
        idioma = idiomas[i] if i < len(idiomas) else idiomas[-1] if idiomas else "pt"
        if i < len(escolhidos) and len([p for p in planos if p.modo == "A_corte"]) < total_cortes:
            planos.append(PlanoVideo(i, "A_corte", idioma,
                                     candidato=escolhidos[len(planos)]))
        else:
            assunto = assuntos_livres.pop(0) if assuntos_livres else "GTA 6"
            planos.append(PlanoVideo(i, "B_narrado", idioma, assunto=assunto))
    return planos


# ============================================================================
#  PRODUÇÃO DE UM VÍDEO
# ============================================================================

def _produzir_corte(plano: PlanoVideo, cfg, canais: dict, conexao,
                    avisar: Callable[[str], None]) -> ResultadoVideo:
    """MODO A: baixa o vídeo-fonte, acha o melhor momento e monta o corte."""
    resultado = ResultadoVideo(plano=plano)
    candidato = plano.candidato

    # ------------------------------------------------------------- 1) fonte
    avisar(f"Vídeo {plano.indice + 1}: obtendo o vídeo-fonte...")
    if getattr(candidato, "arquivo_local", ""):
        fonte = downloader.usar_arquivo_local(candidato.arquivo_local)
    else:
        fonte = downloader.baixar(candidato.url, cfg)
    banco.registrar_video_fonte(conexao, fonte)

    # -------------------------------------------------------- 2) transcrição
    avisar(f"Vídeo {plano.indice + 1}: transcrevendo (pode demorar)...")
    completa = transcricao.transcrever(fonte.arquivo, cfg, idioma=plano.idioma)

    # ------------------------------------------------------- 3) melhor momento
    avisar(f"Vídeo {plano.indice + 1}: escolhendo o melhor momento...")
    informacoes = info_video(fonte.arquivo,
                             str(cfg.pegar("ffprobe_caminho", "ffprobe")),
                             str(cfg.pegar("ffmpeg_caminho", "ffmpeg")))
    momento = selecao.melhor_momento(
        completa, cfg, idioma=plano.idioma, duracao_video=informacoes.duracao,
        conexao=conexao, video_id=fonte.id,
    )
    if momento is None:
        resultado.erro = ("nenhum trecho bom neste vídeo (ou todos já foram "
                          "usados antes)")
        return resultado

    inicio, duracao = momento.inicio, momento.duracao
    recorte = transcricao.recortar(completa, inicio, inicio + duracao)
    palavras = transcricao.palavras_de(recorte)
    assunto = getattr(candidato, "assunto", "") or gancho.assunto_a_partir_do_texto(
        recorte.get("texto", ""), plano.idioma)

    # ---------------------------------------------------------- 4) segurança
    avisar(f"Vídeo {plano.indice + 1}: checagem de segurança...")
    avaliacao = seguranca.avaliar(
        conexao, cfg, fonte, inicio=inicio, duracao=duracao, transcricao=recorte,
        assunto=assunto, idioma=plano.idioma, modo="A_corte",
        assuntos_proibidos=canais.get("assuntos_proibidos", []),
    )
    resultado.selo = avaliacao.selo
    resultado.motivos = avaliacao.motivos

    ficha_base = {
        "idioma": plano.idioma, "modo": "A_corte", "assunto": assunto,
        "trecho": {"inicio": inicio, "fim": inicio + duracao, "duracao": duracao},
        "fonte": fonte.para_dict(), "credito": fonte.credito,
        "texto_transcrito": recorte.get("texto", ""),
        "selo": avaliacao.selo, "motivos": avaliacao.motivos,
        "detalhes_seguranca": avaliacao.detalhes,
        "gerado_em": datetime.now().isoformat(timespec="seconds"),
    }

    if not avaliacao.aprovado:
        ficha_base["arquivo"] = ""
        resultado.id_registro = seguranca.registrar_aprovacao(
            conexao, ficha_base, avaliacao)
        resultado.erro = avaliacao.resumo_texto()
        return resultado

    # ------------------------------------------------- 5) gancho e legenda
    avisar(f"Vídeo {plano.indice + 1}: gancho e legenda...")
    frase = gancho.criar_frase(assunto, cfg, idioma=plano.idioma,
                               primeira_fala=transcricao.texto_inicial(recorte))
    pasta_saida = cfg.caminho("saida") / datetime.now().strftime("%Y-%m-%d")
    base = f"{nome_seguro(fonte.titulo or 'corte')}_{int(inicio)}_{plano.idioma}"
    arquivo_ass = legendas.salvar_ass(
        palavras, cfg, pasta_saida / f"{base}.ass", idioma=plano.idioma,
        eventos_extra=gancho.eventos_gancho(frase, cfg, idioma=plano.idioma),
    )

    # ------------------------------------------------------------ 6) b-roll
    video_broll = None
    cobertura = float(avaliacao.detalhes.get("fala", {}).get("cobertura", 0))
    recomendado, explicacao, _ = broll.precisa_de_broll(
        fonte.arquivo, cfg, inicio, duracao, cobertura)
    if recomendado:
        video_broll = broll.escolher_gameplay(cfg, evitar=fonte.arquivo)
        log(f"B-roll: {explicacao}", "ok")

    # ------------------------------------------------------------ 7) render
    avisar(f"Vídeo {plano.indice + 1}: renderizando (a parte mais demorada)...")
    if video_broll is not None:
        final = render.renderizar_corte_com_broll(
            fonte.arquivo, video_broll, pasta_saida / f"{base}.mp4", cfg,
            arquivo_ass=arquivo_ass, inicio=inicio, duracao=duracao)
    else:
        final = render.renderizar_corte(
            fonte.arquivo, pasta_saida / f"{base}.mp4", cfg,
            arquivo_ass=arquivo_ass, inicio=inicio, duracao=duracao)
    render.gerar_miniatura(final, pasta_saida / f"{base}.jpg", cfg)

    # -------------------------------------------------------- 8) metadados
    dados = metadados.gerar(assunto, cfg, idioma=plano.idioma, modo="A_corte",
                            credito=fonte.credito,
                            texto_falado=recorte.get("texto", ""), gancho=frase)
    arquivo_tiktok = metadados.salvar_para_tiktok(
        dados, str(final),
        cfg.raiz / str(cfg.pegar("tiktok.pasta_saida", "saida/tiktok")))

    ficha = dict(ficha_base)
    ficha.update({
        "arquivo": str(final), "miniatura": str(pasta_saida / f"{base}.jpg"),
        "gancho": frase, "titulo": dados.titulo,
        "metadados": dados.para_dict(),
        "broll": str(video_broll) if video_broll else "",
        "tiktok_txt": str(arquivo_tiktok),
    })
    resultado.id_registro = seguranca.registrar_aprovacao(conexao, ficha, avaliacao)
    resultado.sucesso = True
    resultado.arquivo = str(final)
    resultado.titulo = dados.titulo
    return resultado


def _produzir_narrado(plano: PlanoVideo, cfg, canais: dict, conexao,
                      avisar: Callable[[str], None],
                      fatos: list[str] | None = None,
                      fundo: str = "") -> ResultadoVideo:
    """MODO B: escreve o roteiro, narra e monta o vídeo sobre o fundo."""
    resultado = ResultadoVideo(plano=plano)

    # ------------------------------------------------------------ 1) roteiro
    avisar(f"Vídeo {plano.indice + 1}: escrevendo o roteiro...")
    roteiro = narracao.criar_roteiro(plano.assunto, cfg, plano.idioma, fatos or [])

    # ---------------------------------------------------- 2) vídeo de fundo
    caminho_fundo = Path(fundo) if fundo else broll.escolher_gameplay(cfg)
    if caminho_fundo is None or not Path(caminho_fundo).exists():
        resultado.erro = ("sem vídeo de fundo: configure 'broll.pasta' no "
                          "config.yaml ou escolha um arquivo no painel")
        return resultado

    # ----------------------------------------------------------- 3) narração
    avisar(f"Vídeo {plano.indice + 1}: gerando a narração...")
    base = f"{nome_seguro(plano.assunto)}_{plano.idioma}"
    voz = narracao.narrar(roteiro, cfg, cfg.caminho("temporario") / f"{base}.mp3")

    # ---------------------------------------------------------- 4) segurança
    avisar(f"Vídeo {plano.indice + 1}: checagem de segurança...")
    fonte = downloader.usar_arquivo_local(caminho_fundo)
    banco.registrar_video_fonte(conexao, fonte)
    transcricao_narrada = narracao.transcricao_do_roteiro(roteiro, voz)

    avaliacao = seguranca.avaliar(
        conexao, cfg, fonte, inicio=0, duracao=voz.duracao,
        transcricao=transcricao_narrada, assunto=plano.assunto,
        idioma=plano.idioma, modo="B_narrado",
        assuntos_proibidos=canais.get("assuntos_proibidos", []),
    )
    resultado.selo = avaliacao.selo
    resultado.motivos = avaliacao.motivos

    ficha_base = {
        "idioma": plano.idioma, "modo": "B_narrado", "assunto": plano.assunto,
        "trecho": {"inicio": 0, "fim": voz.duracao, "duracao": voz.duracao},
        "fonte": fonte.para_dict(), "credito": fonte.credito,
        "texto_transcrito": roteiro.texto, "roteiro": roteiro.para_dict(),
        "selo": avaliacao.selo, "motivos": avaliacao.motivos,
        "gerado_em": datetime.now().isoformat(timespec="seconds"),
    }

    if not avaliacao.aprovado:
        ficha_base["arquivo"] = ""
        resultado.id_registro = seguranca.registrar_aprovacao(
            conexao, ficha_base, avaliacao)
        resultado.erro = avaliacao.resumo_texto()
        return resultado

    # ------------------------------------------------- 5) gancho e legenda
    frase = gancho.criar_frase(plano.assunto, cfg, idioma=plano.idioma,
                               primeira_fala=roteiro.texto[:400])
    pasta_saida = cfg.caminho("saida") / datetime.now().strftime("%Y-%m-%d")
    arquivo_ass = legendas.salvar_ass(
        voz.palavras, cfg, pasta_saida / f"{base}.ass", idioma=plano.idioma,
        eventos_extra=gancho.eventos_gancho(frase, cfg, idioma=plano.idioma),
    )

    # ------------------------------------------------------------ 6) render
    avisar(f"Vídeo {plano.indice + 1}: renderizando o vídeo narrado...")
    final = render.renderizar_narrado(
        caminho_fundo, voz.arquivo, pasta_saida / f"{base}.mp4", cfg,
        arquivo_ass=arquivo_ass, duracao=voz.duracao)
    render.gerar_miniatura(final, pasta_saida / f"{base}.jpg", cfg)

    dados = metadados.gerar(plano.assunto, cfg, idioma=plano.idioma,
                            modo="B_narrado", credito=fonte.credito,
                            texto_falado=roteiro.texto, gancho=frase)
    arquivo_tiktok = metadados.salvar_para_tiktok(
        dados, str(final),
        cfg.raiz / str(cfg.pegar("tiktok.pasta_saida", "saida/tiktok")))

    ficha = dict(ficha_base)
    ficha.update({
        "arquivo": str(final), "miniatura": str(pasta_saida / f"{base}.jpg"),
        "gancho": frase, "titulo": dados.titulo,
        "metadados": dados.para_dict(), "tiktok_txt": str(arquivo_tiktok),
        "narracao": {"voz": voz.voz, "provedor": voz.provedor,
                     "tempos_reais": voz.tempos_reais},
    })
    resultado.id_registro = seguranca.registrar_aprovacao(conexao, ficha, avaliacao)
    resultado.sucesso = True
    resultado.arquivo = str(final)
    resultado.titulo = dados.titulo
    return resultado


# ============================================================================
#  O LOTE INTEIRO (é isto que o botão CRIAR chama)
# ============================================================================

def criar_lote(
    cfg,
    canais: dict,
    conexao,
    avisar: Callable[[str], None] | None = None,
    fatos: list[str] | None = None,
    fundo_narrado: str = "",
    usar_rede: bool = True,
) -> list[ResultadoVideo]:
    """
    Produz o lote de vídeos do dia.

    avisar: função que recebe o texto do andamento (o painel usa para mostrar
    a barra de progresso). Pode ser None.
    """
    avisar = avisar or (lambda mensagem: log(mensagem, "etapa"))
    resultados: list[ResultadoVideo] = []

    titulo("PRODUZINDO O LOTE DE VÍDEOS")
    banco.sincronizar_canais(conexao, canais)

    # --------------------------------------------------------- 1) tendências
    avisar("Procurando os assuntos em alta...")
    idioma_principal = (list(cfg.pegar("producao.idiomas", ["pt"])) or ["pt"])[0]
    assuntos = tendencias.descobrir_assuntos(
        cfg, canais, idioma=idioma_principal, usar_rede=usar_rede, conexao=conexao)

    # ------------------------------------------------------------ 2) fontes
    avisar("Procurando vídeos nos seus canais aprovados...")
    candidatos = modulo_fontes.buscar_candidatos(
        conexao, cfg, canais, assuntos, idioma=idioma_principal, usar_rede=usar_rede)

    # ------------------------------------------------------------ 3) plano
    planos = planejar(cfg, candidatos, assuntos)
    for plano in planos:
        log(f"Plano: {plano.descrever()}", "info")

    # --------------------------------------------------------- 4) produção
    for plano in planos:
        try:
            if plano.modo == "A_corte" and plano.candidato is not None:
                resultado = _produzir_corte(plano, cfg, canais, conexao, avisar)
            else:
                resultado = _produzir_narrado(plano, cfg, canais, conexao, avisar,
                                              fatos=fatos, fundo=fundo_narrado)
        except Exception as erro:
            # Um vídeo com problema não pode derrubar o lote inteiro.
            resultado = ResultadoVideo(plano=plano, erro=str(erro))
            log(f"Vídeo {plano.indice + 1} falhou: {erro}", "erro")
            log(traceback.format_exc(limit=3), "info")

        resultados.append(resultado)
        estado = "pronto" if resultado.sucesso else f"não entrou na fila ({resultado.erro})"
        avisar(f"Vídeo {plano.indice + 1}: {estado}")

    prontos = sum(1 for r in resultados if r.sucesso)
    avisar(f"Lote encerrado: {prontos} de {len(resultados)} vídeo(s) na fila.")
    return resultados
