#!/usr/bin/env python3
"""Exporta retenção de audiência da YouTube Analytics API.

Saída: um CSV por vídeo em `analytics/retencao/`, no formato que o agente
`analista` espera (`/retro`).

Dependências (opcionais, só para o modo real):
    pip install google-auth-oauthlib google-api-python-client

Credenciais — nunca versionadas:
    YOUTUBE_CLIENT_SECRETS  caminho do client_secret.json (OAuth de desktop)
    YOUTUBE_TOKEN           caminho do token salvo (padrão: ./secrets/token.json)

Uso:
    python3 scripts/youtube_analytics.py --dias 28 --saida analytics/retencao/
    python3 scripts/youtube_analytics.py --verificar     # só checa o setup

Escopos usados (somente leitura):
    yt-analytics.readonly, youtube.readonly
"""

import argparse
import csv
import datetime
import os
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ESCOPOS = [
    "https://www.googleapis.com/auth/yt-analytics.readonly",
    "https://www.googleapis.com/auth/youtube.readonly",
]
TOKEN_PADRAO = os.path.join(RAIZ, "secrets", "token.json")


def autenticar():
    """Devolve (analytics, data_api). Levanta RuntimeError com instrução clara."""
    try:
        from google.auth.transport.requests import Request
        from google.oauth2.credentials import Credentials
        from google_auth_oauthlib.flow import InstalledAppFlow
        from googleapiclient.discovery import build
    except ImportError as err:
        raise RuntimeError(
            "faltam dependências: pip install google-auth-oauthlib "
            "google-api-python-client (%s)" % err
        )

    segredos = os.environ.get("YOUTUBE_CLIENT_SECRETS")
    token = os.environ.get("YOUTUBE_TOKEN", TOKEN_PADRAO)

    credenciais = None
    if os.path.isfile(token):
        credenciais = Credentials.from_authorized_user_file(token, ESCOPOS)
    if credenciais and credenciais.expired and credenciais.refresh_token:
        credenciais.refresh(Request())
    if not credenciais or not credenciais.valid:
        if not segredos or not os.path.isfile(segredos):
            raise RuntimeError(
                "defina YOUTUBE_CLIENT_SECRETS com o caminho do client_secret.json "
                "(OAuth de aplicativo desktop, no Google Cloud Console)"
            )
        credenciais = InstalledAppFlow.from_client_secrets_file(
            segredos, ESCOPOS
        ).run_local_server(port=0)
        os.makedirs(os.path.dirname(token), exist_ok=True)
        with open(token, "w", encoding="utf-8") as fh:
            fh.write(credenciais.to_json())

    return (
        build("youtubeAnalytics", "v2", credentials=credenciais),
        build("youtube", "v3", credentials=credenciais),
    )


def listar_videos(data_api, maximo):
    canal = data_api.channels().list(part="contentDetails", mine=True).execute()
    uploads = canal["items"][0]["contentDetails"]["relatedPlaylists"]["uploads"]
    videos, pagina = [], None
    while len(videos) < maximo:
        resposta = (
            data_api.playlistItems()
            .list(part="snippet", playlistId=uploads, maxResults=50, pageToken=pagina)
            .execute()
        )
        for item in resposta.get("items", []):
            videos.append(
                {
                    "id": item["snippet"]["resourceId"]["videoId"],
                    "titulo": item["snippet"]["title"],
                }
            )
        pagina = resposta.get("nextPageToken")
        if not pagina:
            break
    return videos[:maximo]


def retencao(analytics, video_id, inicio, fim):
    return (
        analytics.reports()
        .query(
            ids="channel==MINE",
            startDate=inicio,
            endDate=fim,
            metrics="audienceWatchRatio,relativeRetentionPerformance",
            dimensions="elapsedVideoTimeRatio",
            filters="video==%s" % video_id,
        )
        .execute()
    )


def salvar(resposta, destino):
    colunas = [c["name"] for c in resposta.get("columnHeaders", [])]
    with open(destino, "w", encoding="utf-8", newline="") as fh:
        escritor = csv.writer(fh)
        escritor.writerow(colunas)
        escritor.writerows(resposta.get("rows", []))
    return len(resposta.get("rows", []))


def main():
    parser = argparse.ArgumentParser(description="Export de retenção do YouTube")
    parser.add_argument("--dias", type=int, default=28)
    parser.add_argument("--saida", default=os.path.join("analytics", "retencao"))
    parser.add_argument("--max-videos", type=int, default=50)
    parser.add_argument("--verificar", action="store_true", help="só checa o setup")
    args = parser.parse_args()

    try:
        analytics, data_api = autenticar()
    except RuntimeError as err:
        print("setup incompleto: %s" % err, file=sys.stderr)
        print(
            "\nSem credencial não há dado. NÃO invente números para o /retro — "
            "relatório com dado inventado vira instrução de roteiro errada.",
            file=sys.stderr,
        )
        return 1

    if args.verificar:
        print("credenciais ok")
        return 0

    fim = datetime.date.today()
    inicio = fim - datetime.timedelta(days=args.dias)
    destino_dir = args.saida if os.path.isabs(args.saida) else os.path.join(RAIZ, args.saida)
    os.makedirs(destino_dir, exist_ok=True)

    videos = listar_videos(data_api, args.max_videos)
    if not videos:
        print("nenhum vídeo no canal", file=sys.stderr)
        return 1

    total = 0
    for video in videos:
        destino = os.path.join(destino_dir, "%s.csv" % video["id"])
        try:
            linhas = salvar(retencao(analytics, video["id"], str(inicio), str(fim)), destino)
        except Exception as err:  # a API falha por vídeo (privado, curto demais)
            print("  %s: %s" % (video["id"], err), file=sys.stderr)
            continue
        total += 1
        print("  %s  %-50s %d pontos" % (video["id"], video["titulo"][:50], linhas))

    indice = os.path.join(destino_dir, "index.csv")
    with open(indice, "w", encoding="utf-8", newline="") as fh:
        escritor = csv.writer(fh)
        escritor.writerow(["video_id", "titulo", "periodo_inicio", "periodo_fim"])
        for video in videos:
            escritor.writerow([video["id"], video["titulo"], inicio, fim])

    print("\n%d vídeos exportados para %s" % (total, destino_dir))
    print("Próximo: /retro")
    return 0


if __name__ == "__main__":
    sys.exit(main())
