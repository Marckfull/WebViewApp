"""
fontes.py — gerenciar seus canais-fonte e a reputação deles pelo terminal.

É por aqui que você AVISA o sistema quando algo dá errado no YouTube. Assim que
você registra um claim, aquele canal é bloqueado e nunca mais é usado.
(Na Etapa 7 isso vira um botão no painel; por enquanto é este comando.)

COMO USAR (PowerShell, dentro da pasta gta_cuts_factory):

    # ver todos os canais e a reputação de cada um
    python scripts\\fontes.py listar

    # avisar que um vídeo daquele canal tomou Content ID  → BLOQUEIA o canal
    python scripts\\fontes.py claim "@CanalX" --nota "claim de trilha sonora"

    # outros problemas
    python scripts\\fontes.py restricao "@CanalX"     # restrição de idade
    python scripts\\fontes.py remocao "@CanalX"       # vídeo removido

    # liberar um canal de novo (decisão sua)
    python scripts\\fontes.py desbloquear "@CanalX"

    # ver a fila de aprovação e os reprovados
    python scripts\\fontes.py fila
    python scripts\\fontes.py reprovados
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from nucleo import banco  # noqa: E402
from nucleo.config import carregar_canais, carregar_config  # noqa: E402
from nucleo.utils import log, titulo  # noqa: E402


MARCAS = {"oficial": "🟢", "proprio": "🟢", "em_teste": "🟡", "bloqueado": "🔴"}


def achar_canal(conexao, procurado: str) -> banco.Fonte | None:
    """
    Acha um canal pelo identificador, pela URL ou por parte do nome —
    para você não precisar decorar o ID exato.
    """
    fonte = banco.obter_fonte(conexao, procurado)
    if fonte:
        return fonte

    alvo = procurado.strip().lower().lstrip("@")
    for candidata in banco.listar_fontes(conexao):
        if (alvo in candidata.nome.lower()
                or alvo in candidata.canal_id.lower()
                or alvo in (candidata.url or "").lower()):
            return candidata
    return None


def comando_listar(conexao) -> int:
    titulo("SEUS CANAIS-FONTE")
    fontes = banco.listar_fontes(conexao)
    if not fontes:
        log("Nenhum canal cadastrado ainda. Edite config/canais_fontes.yaml.", "aviso")
        return 0

    for fonte in fontes:
        print(f"{MARCAS[fonte.status]} [{fonte.status:9s}] {fonte.nome}")
        print(f"      id       : {fonte.canal_id}")
        print(f"      gerados  : {fonte.videos_gerados} vídeo(s)")
        print(f"      problemas: {fonte.claims} claim(s), "
              f"{fonte.restricoes_idade} restrição(ões), {fonte.remocoes} remoção(ões)")
    print(f"\nTotal: {len(fontes)} canal(is).")
    return 0


def comando_ocorrencia(conexao, procurado: str, tipo: str, nota: str) -> int:
    fonte = achar_canal(conexao, procurado)
    if fonte is None:
        log(f"Não achei nenhum canal parecido com '{procurado}'. "
            f"Rode 'python scripts/fontes.py listar' para ver os nomes.", "erro")
        return 1

    atualizada = banco.registrar_ocorrencia(conexao, fonte.canal_id, tipo, nota)
    if atualizada is None:
        return 1

    titulo(f"OCORRÊNCIA REGISTRADA: {tipo.upper()}")
    print(f"Canal : {atualizada.nome}")
    print(f"Status: {MARCAS[atualizada.status]} {atualizada.status}")
    print(f"Placar: {atualizada.claims} claim(s), "
          f"{atualizada.restricoes_idade} restrição(ões), {atualizada.remocoes} remoção(ões)")
    if atualizada.bloqueada:
        print("\nEste canal NÃO será mais usado pelo sistema.")
        print("Se quiser liberar depois: python scripts/fontes.py desbloquear "
              f'"{atualizada.nome}"')
    return 0


def comando_desbloquear(conexao, procurado: str) -> int:
    fonte = achar_canal(conexao, procurado)
    if fonte is None:
        log(f"Não achei nenhum canal parecido com '{procurado}'.", "erro")
        return 1

    atualizada = banco.desbloquear_fonte(conexao, fonte.canal_id)
    log(f"'{atualizada.nome}' voltou para o status '{atualizada.status}'. "
        f"Ele continua com {atualizada.problemas} problema(s) no histórico — "
        f"use com atenção.", "aviso")
    return 0


def comando_fila(conexao, status: str) -> int:
    titulo("FILA DE APROVAÇÃO" if status == "fila" else "REPROVADOS")
    itens = banco.listar_fila(conexao, status)
    if not itens:
        print("(vazio)")
        return 0

    for item in itens:
        marca = {"verde": "🟢", "amarelo": "🟡", "vermelho": "🔴"}.get(
            item.get("selo", ""), "⚪")
        print(f"\n{marca} #{item['id']} [{item['idioma']}] {item['assunto']}")
        print(f"   trecho : {item['inicio']:.0f}s – {item['fim']:.0f}s")
        print(f"   arquivo: {item['arquivo'] or '(não renderizado)'}")
        credito = item.get("ficha", {}).get("credito", "")
        if credito:
            print(f"   crédito: {credito.splitlines()[0]}")
        for motivo in item["motivos"]:
            if motivo["gravidade"] in ("bloqueio", "aviso"):
                icone = "🔴" if motivo["gravidade"] == "bloqueio" else "🟡"
                print(f"   {icone} {motivo['mensagem']}")
    print(f"\nTotal: {len(itens)}.")
    return 0


def comando_resumo(conexao) -> int:
    titulo("RESUMO DO SISTEMA")
    for chave, valor in banco.resumo(conexao).items():
        print(f"   {chave:20s}: {valor}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Gerenciar canais-fonte, reputação e fila do GTA Cuts Factory"
    )
    sub = parser.add_subparsers(dest="comando", required=True)

    sub.add_parser("listar", help="mostra os canais e a reputação de cada um")
    sub.add_parser("resumo", help="números gerais do sistema")
    sub.add_parser("fila", help="vídeos esperando sua aprovação")
    sub.add_parser("reprovados", help="vídeos barrados pela checagem, com o motivo")

    for nome, ajuda in [
        ("claim", "registra um Content ID claim (bloqueia o canal)"),
        ("restricao", "registra restrição de idade (bloqueia o canal)"),
        ("remocao", "registra remoção de vídeo (bloqueia o canal)"),
    ]:
        p = sub.add_parser(nome, help=ajuda)
        p.add_argument("canal", help="nome, id ou URL do canal")
        p.add_argument("--nota", default="", help="observação sua sobre o caso")

    p = sub.add_parser("desbloquear", help="libera um canal bloqueado")
    p.add_argument("canal", help="nome, id ou URL do canal")

    args = parser.parse_args()

    cfg = carregar_config()
    conexao = banco.conectar(cfg)
    banco.sincronizar_canais(conexao, carregar_canais())

    try:
        if args.comando == "listar":
            return comando_listar(conexao)
        if args.comando == "resumo":
            return comando_resumo(conexao)
        if args.comando == "fila":
            return comando_fila(conexao, "fila")
        if args.comando == "reprovados":
            return comando_fila(conexao, "reprovado")
        if args.comando == "desbloquear":
            return comando_desbloquear(conexao, args.canal)

        tipos = {"claim": "claim", "restricao": "restricao_idade", "remocao": "remocao"}
        return comando_ocorrencia(conexao, args.canal, tipos[args.comando], args.nota)
    finally:
        conexao.close()


if __name__ == "__main__":
    raise SystemExit(main())
