"""
etapa2_teste.py — TESTE 3: banco anti-repetição + módulo de segurança 🛡️.

Este script conta uma "história" com 7 casos, mostrando o sistema aceitando e
recusando vídeos. Ele usa um banco SEPARADO (dados/banco_teste.sqlite), então
não bagunça o seu banco de verdade.

COMO RODAR (PowerShell, dentro da pasta gta_cuts_factory):

    python scripts\\etapa2_teste.py

Não precisa de internet e não baixa nada.
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import banco, seguranca  # noqa: E402
from nucleo.config import carregar_config  # noqa: E402
from nucleo.downloader import VideoFonte  # noqa: E402
from nucleo.utils import achar_ffmpeg, log, rodar, titulo  # noqa: E402


# ---------------------------------------------------------------------------
#  Dados de mentirinha para a demonstração
# ---------------------------------------------------------------------------

def video_de(id_video: str, canal_id: str, canal: str, arquivo: str = "") -> VideoFonte:
    return VideoFonte(
        id=id_video, titulo=f"Vídeo {id_video}", canal=canal, canal_id=canal_id,
        canal_url=f"https://youtube.com/@{canal_id}",
        url=f"https://youtu.be/{id_video}", duracao=900.0, data_envio="20260101",
        arquivo=arquivo,
    )


def fala(palavras: int, duracao: float = 60.0, texto: str = "") -> dict:
    """Simula uma transcrição com a quantidade de fala que a gente quiser."""
    passo = duracao / max(1, palavras)
    # cada palavra dura no máximo 0,5s — é o tamanho real de uma palavra
    # falada; sem esse limite, 2 palavras em 60s pareceriam cobrir o vídeo todo
    duracao_palavra = min(0.5, passo * 0.9)
    lista = [{"texto": f"palavra{i}", "inicio": i * passo,
              "fim": i * passo + duracao_palavra} for i in range(palavras)]
    return {"idioma": "pt", "texto": texto or " ".join(p["texto"] for p in lista),
            "palavras": lista, "frases": []}


def caso(numero: int, descricao: str) -> None:
    print(f"\n{'─' * 68}\nCASO {numero}: {descricao}\n{'─' * 68}")


def executar(conexao, cfg, titulo_caso: str, numero: int,
             registrar: bool = True, **kwargs) -> seguranca.Avaliacao:
    """
    Roda uma avaliação, imprime o resultado e registra no banco.

    Registrar sempre é importante: é assim que o aprovado entra na FILA e o
    reprovado vai para a lista de REPROVADOS com o motivo.
    """
    caso(numero, titulo_caso)
    avaliacao = seguranca.avaliar(conexao, cfg, analisar_imagens=False, **kwargs)
    seguranca.imprimir_avaliacao(avaliacao)

    if registrar:
        seguranca.registrar_aprovacao(
            conexao,
            ficha_de(kwargs["fonte_video"], kwargs["inicio"], kwargs["duracao"],
                     kwargs.get("assunto", ""), kwargs.get("idioma", "pt")),
            avaliacao,
        )
    return avaliacao


def ficha_de(fonte: VideoFonte, inicio: float, duracao: float, assunto: str,
             idioma: str = "pt") -> dict:
    return {
        "arquivo": f"saida/demo_{fonte.id}_{int(inicio)}.mp4",
        "idioma": idioma, "modo": "A_corte", "assunto": assunto,
        "trecho": {"inicio": inicio, "fim": inicio + duracao, "duracao": duracao},
        "fonte": fonte.para_dict(), "credito": fonte.credito,
    }


# ---------------------------------------------------------------------------
#  Teste do detector de violência com vídeo de verdade
# ---------------------------------------------------------------------------

def testar_detector_de_violencia(cfg) -> None:
    """
    Cria dois clipes curtos (um bem vermelho, outro com cores de cidade) e passa
    os dois pelo detector, para você ver que ele diferencia os casos.
    """
    caso(7, "O detector de violência funciona mesmo? (teste com vídeo real)")
    try:
        ffmpeg = achar_ffmpeg(str(cfg.pegar("ffmpeg_caminho", "ffmpeg")))
    except Exception as erro:
        log(f"ffmpeg indisponível, pulando este caso ({erro})", "aviso")
        return

    pasta = cfg.caminho("temporario")
    # Três clipes de 4s. O terceiro é a armadilha: vermelho chapado, que um
    # detector ingênuo confunde com sangue (carro vermelho, menu, tela "WASTED").
    clipes = [
        ("sangue (vermelho escuro e texturizado)",
         "color=c=0x6E1111:size=640x360:rate=15:duration=4,noise=alls=45:allf=t+u",
         pasta / "demo_sangue.mp4"),
        ("cena normal (verde/cidade)",
         "color=c=0x2E7D32:size=640x360:rate=15:duration=4,noise=alls=20:allf=t+u",
         pasta / "demo_normal.mp4"),
        ("carro vermelho / menu (vermelho chapado — NÃO é sangue)",
         "color=c=red:size=640x360:rate=15:duration=4",
         pasta / "demo_vermelho_chapado.mp4"),
    ]

    for descricao, fonte_lavfi, caminho in clipes:
        rodar(
            [ffmpeg, "-y", "-loglevel", "error", "-f", "lavfi", "-i", fonte_lavfi,
             "-c:v", "libx264", "-preset", "ultrafast", "-pix_fmt", "yuv420p",
             str(caminho)],
            descricao="criar clipe de teste",
        )
        analise = seguranca.analisar_violencia_visual(caminho, 0, 4, amostras=8)
        if not analise["analisado"]:
            log("OpenCV indisponível — o sistema marcaria o vídeo com aviso "
                "amarelo, para você conferir na mão.", "aviso")
            return
        motivos = seguranca.checar_violencia(analise, "", cfg)
        gravidades = {m.gravidade for m in motivos}
        veredito = ("🔴 BLOQUEADO" if seguranca.BLOQUEIO in gravidades
                    else "🟡 AVISO" if seguranca.AVISO in gravidades
                    else "🟢 LIMPO")
        print(f"   {descricao}: {analise['frames_graficos']}/"
              f"{analise['total_frames']} frames marcados, "
              f"~{analise['segundos_estimados']}s de cena gráfica → {veredito}")


# ---------------------------------------------------------------------------
#  Demonstração completa
# ---------------------------------------------------------------------------

def main() -> int:
    titulo("TESTE 3 — BANCO ANTI-REPETIÇÃO + MÓDULO DE SEGURANÇA 🛡️")

    cfg = carregar_config()
    cfg["pastas"]["banco"] = "dados/banco_teste.sqlite"   # banco só da demonstração

    caminho_banco = cfg.caminho("banco")
    if caminho_banco.exists():
        caminho_banco.unlink()   # começa do zero a cada execução
    conexao = banco.conectar(cfg)

    try:
        # Cadastro das fontes: uma oficial, uma sua, uma de terceiro
        banco.registrar_fonte(conexao, "rockstar", "Rockstar Games",
                              status="oficial", peso=10)
        banco.registrar_fonte(conexao, "proprio", "Minhas gravações",
                              status="proprio", peso=10)
        banco.registrar_fonte(conexao, "canal_noticias", "Canal de Notícias GTA",
                              status="em_teste", peso=5)
        log("3 fontes cadastradas: 1 oficial, 1 própria, 1 em teste", "ok")

        noticias = video_de("news001", "canal_noticias", "Canal de Notícias GTA")
        oficial = video_de("rock001", "rockstar", "Rockstar Games")

        # ---------------------------------------------------------------- 1
        avaliacao = executar(
            conexao, cfg, "corte comentado de um canal em teste (deve PASSAR)", 1,
            fonte_video=noticias, inicio=120, duracao=60,
            transcricao=fala(180), assunto="GTA 6 data de lançamento", idioma="pt",
        )
        assert avaliacao.aprovado, "caso 1 deveria passar"
        log("Vídeo registrado na fila. O trecho 120s–180s agora está 'usado'.", "ok")

        # ---------------------------------------------------------------- 2
        avaliacao = executar(
            conexao, cfg, "o MESMO trecho de novo (deve ser BLOQUEADO)", 2,
            fonte_video=noticias, inicio=120, duracao=60,
            transcricao=fala(180), assunto="GTA 6 mapa", idioma="pt",
        )
        assert not avaliacao.aprovado, "caso 2 deveria ser bloqueado"

        # ---------------------------------------------------------------- 3
        avaliacao = executar(
            conexao, cfg, "trecho deslocado em 5s — a tentativa clássica de "
                          "burlar (deve ser BLOQUEADO)", 3,
            fonte_video=noticias, inicio=115, duracao=60,
            transcricao=fala(180), assunto="GTA 6 armas", idioma="pt",
        )
        assert not avaliacao.aprovado, "caso 3 deveria ser bloqueado"

        # ---------------------------------------------------------------- 4
        avaliacao = executar(
            conexao, cfg, "outro trecho, mas o MESMO assunto (deve ser BLOQUEADO)", 4,
            fonte_video=noticias, inicio=600, duracao=60,
            transcricao=fala(180), assunto="a data de lançamento do GTA 6",
            idioma="pt",
        )
        assert not avaliacao.aprovado, "caso 4 deveria ser bloqueado"

        # ---------------------------------------------------------------- 5
        avaliacao = executar(
            conexao, cfg, "cinemática oficial da Rockstar SEM narração "
                          "(deve ser BLOQUEADO — regra da Rockstar)", 5,
            fonte_video=oficial, inicio=0, duracao=60,
            transcricao=fala(2), assunto="GTA 6 trailer 2", idioma="pt",
        )
        assert not avaliacao.aprovado, "caso 5 deveria ser bloqueado"

        avaliacao = executar(
            conexao, cfg, "o MESMO material oficial, agora no MODO B narrado "
                          "(deve PASSAR)", 5,
            fonte_video=oficial, inicio=0, duracao=60,
            transcricao=fala(2), assunto="GTA 6 trailer 2", idioma="pt",
            modo="B_narrado",
        )
        assert avaliacao.aprovado, "modo B deveria passar"

        # ---------------------------------------------------------------- 6
        caso(6, "o canal tomou um Content ID claim (fonte é BLOQUEADA na hora)")
        banco.registrar_ocorrencia(
            conexao, "canal_noticias", "claim",
            "Content ID por trilha sonora no vídeo news001", video_id="news001")

        segundo_video = video_de("news002", "canal_noticias", "Canal de Notícias GTA")
        avaliacao = seguranca.avaliar(
            conexao, cfg, segundo_video, inicio=0, duracao=60,
            transcricao=fala(180), assunto="GTA 5 segredos do mapa",
            idioma="pt", analisar_imagens=False,
        )
        seguranca.imprimir_avaliacao(avaliacao)
        seguranca.registrar_aprovacao(
            conexao, ficha_de(segundo_video, 0, 60, "GTA 5 segredos do mapa"),
            avaliacao)
        assert not avaliacao.aprovado, "fonte bloqueada deveria reprovar"

        # ---------------------------------------------------------------- 7
        testar_detector_de_violencia(cfg)

        # ------------------------------------------------------------ resumo
        titulo("SITUAÇÃO FINAL DO SISTEMA")
        for chave, valor in banco.resumo(conexao).items():
            print(f"   {chave:20s}: {valor}")

        print("\n📺 REPUTAÇÃO DAS FONTES")
        for fonte in banco.listar_fontes(conexao):
            marca = {"oficial": "🟢", "proprio": "🟢",
                     "em_teste": "🟡", "bloqueado": "🔴"}[fonte.status]
            print(f"   {marca} [{fonte.status:9s}] {fonte.nome:26s} "
                  f"gerados: {fonte.videos_gerados} | problemas: {fonte.problemas}")

        print("\n🚫 LISTA DE REPROVADOS (o que o sistema barrou)")
        for item in banco.listar_fila(conexao, "reprovado"):
            motivos = "; ".join(m["mensagem"] for m in item["motivos"]
                                if m["gravidade"] == seguranca.BLOQUEIO)
            print(f"   • {item['assunto']} → {motivos}")

        titulo("ETAPA 2 FUNCIONANDO ✅")
        print("O que ficou provado:")
        print("  1. trecho repetido é barrado (mesmo deslocando alguns segundos)")
        print("  2. assunto repetido é barrado dentro do mesmo idioma")
        print("  3. cinemática oficial sem narração é barrada (regra da Rockstar)")
        print("  4. o mesmo material passa no MODO B narrado")
        print("  5. um claim bloqueia a fonte na hora, sozinho")
        print("  6. o que é reprovado fica numa lista separada, com o motivo")
        print(f"\nBanco desta demonstração: {caminho_banco}")
        print("(o seu banco de verdade, dados/banco.sqlite, não foi tocado)")
        return 0
    finally:
        conexao.close()


if __name__ == "__main__":
    raise SystemExit(main())
