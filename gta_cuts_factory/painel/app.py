"""
app.py — o painel local (a sua tela de trabalho).

Abre em http://localhost:5000 e tem tudo que você precisa:

    • botão CRIAR (produz o lote de vídeos)
    • fila de aprovação com prévia de cada vídeo e SELO 🟢🟡🔴
    • título, descrição e hashtags editáveis antes de publicar
    • lista de REPROVADOS, com o motivo de cada bloqueio
    • reputação dos seus canais e botão para registrar um claim

Regra de ouro: nada é publicado sozinho. O painel produz e mostra; quem
libera é você.

COMO RODAR:
    python scripts\\painel.py
"""

from __future__ import annotations

import json
import threading
from datetime import datetime
from pathlib import Path

from flask import (
    Flask,
    abort,
    jsonify,
    redirect,
    render_template,
    request,
    send_file,
    url_for,
)

import sys

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import banco, producao  # noqa: E402
from nucleo.config import carregar_canais, carregar_config  # noqa: E402
from nucleo.utils import log  # noqa: E402


aplicacao = Flask(__name__)

# ---------------------------------------------------------------------------
#  Estado da produção em andamento
# ---------------------------------------------------------------------------
#  O trabalho pesado (download, transcrição, render) roda numa THREAD separada,
#  senão a página ficaria travada esperando. Este dicionário é o que a tela lê
#  para mostrar a barra de progresso.
# ---------------------------------------------------------------------------
ESTADO = {
    "rodando": False,
    "mensagem": "",
    "historico": [],
    "iniciado_em": "",
    "resultados": [],
    "erro": "",
}
TRAVA = threading.Lock()


def _anotar(mensagem: str) -> None:
    """Guarda o andamento para a tela mostrar."""
    with TRAVA:
        ESTADO["mensagem"] = mensagem
        ESTADO["historico"].append(
            {"hora": datetime.now().strftime("%H:%M:%S"), "texto": mensagem})
        ESTADO["historico"] = ESTADO["historico"][-40:]
    log(mensagem, "etapa")


def _produzir_em_segundo_plano(fatos: list[str], fundo: str) -> None:
    """Roda o lote inteiro fora da thread da página."""
    cfg = carregar_config()
    canais = carregar_canais()
    conexao = banco.conectar(cfg)
    try:
        resultados = producao.criar_lote(
            cfg, canais, conexao, avisar=_anotar, fatos=fatos, fundo_narrado=fundo)
        with TRAVA:
            ESTADO["resultados"] = [
                {"titulo": r.titulo or r.plano.descrever(), "sucesso": r.sucesso,
                 "selo": r.selo, "erro": r.erro, "id": r.id_registro}
                for r in resultados
            ]
    except Exception as erro:
        with TRAVA:
            ESTADO["erro"] = str(erro)
        _anotar(f"❌ A produção parou: {erro}")
    finally:
        conexao.close()
        with TRAVA:
            ESTADO["rodando"] = False


def _abrir_banco():
    cfg = carregar_config()
    return cfg, banco.conectar(cfg)


def _item_ou_404(conexao, id_video: int) -> dict:
    for status in ("fila", "reprovado", "publicado", "descartado"):
        for item in banco.listar_fila(conexao, status):
            if int(item["id"]) == int(id_video):
                return item
    abort(404)


# ============================================================================
#  PÁGINAS
# ============================================================================

@aplicacao.route("/")
def inicio():
    """A tela principal: fila, reprovados e reputação das fontes."""
    cfg, conexao = _abrir_banco()
    try:
        canais = carregar_canais()
        banco.sincronizar_canais(conexao, canais)
        return render_template(
            "index.html",
            fila=banco.listar_fila(conexao, "fila"),
            reprovados=banco.listar_fila(conexao, "reprovado"),
            publicados=banco.listar_fila(conexao, "publicado"),
            fontes=banco.listar_fontes(conexao),
            resumo=banco.resumo(conexao),
            estado=ESTADO,
            producao_cfg={
                "quantidade": cfg.pegar("producao.quantidade", 3),
                "idiomas": cfg.pegar("producao.idiomas", ["pt", "pt", "en"]),
                "cortes": cfg.pegar("producao.modo_a_cortes", 2),
                "narrados": cfg.pegar("producao.modo_b_narrados", 1),
            },
        )
    finally:
        conexao.close()


@aplicacao.route("/criar", methods=["POST"])
def criar():
    """O botão CRIAR: dispara a produção do lote."""
    with TRAVA:
        if ESTADO["rodando"]:
            return redirect(url_for("inicio"))
        ESTADO.update({"rodando": True, "mensagem": "Começando...",
                       "historico": [], "resultados": [], "erro": "",
                       "iniciado_em": datetime.now().strftime("%H:%M:%S")})

    fatos = [linha.strip() for linha in
             (request.form.get("fatos", "") or "").splitlines() if linha.strip()]
    fundo = (request.form.get("fundo", "") or "").strip()

    threading.Thread(target=_produzir_em_segundo_plano, args=(fatos, fundo),
                     daemon=True).start()
    return redirect(url_for("inicio"))


@aplicacao.route("/status")
def status():
    """A tela consulta este endereço de tempos em tempos (barra de progresso)."""
    with TRAVA:
        return jsonify(dict(ESTADO))


# ============================================================================
#  ARQUIVOS (prévia do vídeo e miniatura)
# ============================================================================

@aplicacao.route("/video/<int:id_video>")
def video(id_video: int):
    cfg, conexao = _abrir_banco()
    try:
        item = _item_ou_404(conexao, id_video)
        caminho = Path(item.get("arquivo") or "")
        if not caminho.exists():
            abort(404)
        return send_file(caminho, mimetype="video/mp4", conditional=True)
    finally:
        conexao.close()


@aplicacao.route("/miniatura/<int:id_video>")
def miniatura(id_video: int):
    cfg, conexao = _abrir_banco()
    try:
        item = _item_ou_404(conexao, id_video)
        caminho = Path(item.get("ficha", {}).get("miniatura") or "")
        if not caminho.exists():
            abort(404)
        return send_file(caminho, mimetype="image/jpeg")
    finally:
        conexao.close()


# ============================================================================
#  AÇÕES SOBRE UM VÍDEO
# ============================================================================

@aplicacao.route("/salvar/<int:id_video>", methods=["POST"])
def salvar(id_video: int):
    """Salva o título, a descrição e as hashtags que VOCÊ editou."""
    cfg, conexao = _abrir_banco()
    try:
        item = _item_ou_404(conexao, id_video)
        ficha = item.get("ficha", {})
        dados = ficha.get("metadados", {})

        dados["titulo"] = (request.form.get("titulo", "") or "").strip()
        dados["descricao"] = (request.form.get("descricao", "") or "").strip()
        dados["hashtags"] = [
            h if h.startswith("#") else f"#{h}"
            for h in (request.form.get("hashtags", "") or "").split()
        ]
        ficha["metadados"] = dados
        ficha["titulo"] = dados["titulo"]
        ficha["editado_em"] = datetime.now().isoformat(timespec="seconds")

        conexao.execute(
            "UPDATE videos_gerados SET titulo = ?, ficha = ? WHERE id = ?",
            (dados["titulo"], json.dumps(ficha, ensure_ascii=False), id_video))
        conexao.commit()

        # atualiza também o .txt do TikTok, para não ficar desencontrado
        caminho_tiktok = ficha.get("tiktok_txt", "")
        if caminho_tiktok:
            try:
                Path(caminho_tiktok).write_text(
                    f"{dados['titulo']} {' '.join(dados['hashtags'])}\n\n"
                    f"--- vídeo: {ficha.get('arquivo', '')}\n"
                    f"--- editado por você no painel\n", encoding="utf-8")
            except OSError:
                pass

        return redirect(url_for("inicio", _anchor=f"video-{id_video}"))
    finally:
        conexao.close()


@aplicacao.route("/publicar/<int:id_video>", methods=["POST"])
def publicar(id_video: int):
    """
    Marca o vídeo como publicado.

    O upload automático para o YouTube chega na Etapa 8. Por enquanto, o painel
    marca o vídeo e mostra onde está o arquivo e o texto do TikTok — você faz o
    upload e volta aqui para marcar.
    """
    cfg, conexao = _abrir_banco()
    try:
        _item_ou_404(conexao, id_video)
        banco.atualizar_status_video(conexao, id_video, "publicado")
        return redirect(url_for("inicio"))
    finally:
        conexao.close()


@aplicacao.route("/descartar/<int:id_video>", methods=["POST"])
def descartar(id_video: int):
    """Tira o vídeo da fila (sem apagar o arquivo)."""
    cfg, conexao = _abrir_banco()
    try:
        _item_ou_404(conexao, id_video)
        banco.atualizar_status_video(conexao, id_video, "descartado")
        return redirect(url_for("inicio"))
    finally:
        conexao.close()


@aplicacao.route("/reavaliar/<int:id_video>", methods=["POST"])
def reavaliar(id_video: int):
    """
    Manda um REPROVADO para a fila mesmo assim (decisão sua, e fica registrada).

    Só aparece na lista de reprovados. Serve para quando você olha o motivo e
    discorda — por exemplo, o detector marcou violência num pôr do sol.
    """
    cfg, conexao = _abrir_banco()
    try:
        item = _item_ou_404(conexao, id_video)
        if not (item.get("ficha", {}).get("arquivo") or item.get("arquivo")):
            # reprovados normalmente não chegam a ser renderizados
            return redirect(url_for("inicio"))
        banco.atualizar_status_video(conexao, id_video, "fila")
        return redirect(url_for("inicio"))
    finally:
        conexao.close()


# ============================================================================
#  FONTES (reputação)
# ============================================================================

@aplicacao.route("/fonte/<acao>", methods=["POST"])
def fonte(acao: str):
    """
    Registra um problema com um canal (ou desbloqueia).

    O identificador do canal vem no FORMULÁRIO, não na URL: id de canal é uma
    URL do YouTube (com "://" e barras) e não caberia bem num endereço.
    """
    canal_id = (request.form.get("canal_id", "") or "").strip()
    if not canal_id:
        return redirect(url_for("inicio", _anchor="fontes"))

    cfg, conexao = _abrir_banco()
    try:
        if acao == "desbloquear":
            banco.desbloquear_fonte(conexao, canal_id)
        elif acao in ("claim", "restricao_idade", "remocao"):
            banco.registrar_ocorrencia(conexao, canal_id, acao,
                                       "registrado pelo painel")
        return redirect(url_for("inicio", _anchor="fontes"))
    finally:
        conexao.close()


# ============================================================================
#  FILTROS DE APRESENTAÇÃO (usados nos templates)
# ============================================================================

@aplicacao.template_filter("emoji_selo")
def emoji_selo(selo: str) -> str:
    return {"verde": "🟢", "amarelo": "🟡", "vermelho": "🔴"}.get(selo, "⚪")


@aplicacao.template_filter("emoji_status")
def emoji_status(status: str) -> str:
    return {"oficial": "🟢", "proprio": "🟢",
            "em_teste": "🟡", "bloqueado": "🔴"}.get(status, "⚪")


@aplicacao.template_filter("tempo")
def tempo(segundos) -> str:
    total = int(float(segundos or 0))
    return f"{total // 60:02d}:{total % 60:02d}"


def criar_aplicacao():
    """Usado pelos testes e pelo scripts/painel.py."""
    return aplicacao


if __name__ == "__main__":
    aplicacao.run(host="127.0.0.1", port=5000, debug=False)
