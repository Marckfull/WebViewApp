"""
config.py — carrega o arquivo de configuração central.

Por que este módulo existe?
    Para você ter UM lugar só (config/config.yaml) onde muda cor de legenda,
    tamanho de fonte, chaves de API etc., sem precisar mexer em código.

Como usar (em qualquer outro módulo):

    from nucleo.config import carregar_config
    cfg = carregar_config()
    print(cfg["legendas"]["tamanho_fonte"])
    print(cfg.pegar("legendas.cor_palavra_chave"))   # atalho com ponto
"""

from __future__ import annotations

import copy
import shutil
from pathlib import Path
from typing import Any

import yaml

# Pasta raiz do projeto (a pasta gta_cuts_factory/), calculada a partir
# da localização deste arquivo. Assim funciona de qualquer lugar que você rode.
RAIZ = Path(__file__).resolve().parent.parent

CAMINHO_CONFIG = RAIZ / "config" / "config.yaml"
CAMINHO_CONFIG_EXEMPLO = RAIZ / "config" / "config.example.yaml"
CAMINHO_CANAIS = RAIZ / "config" / "canais_fontes.yaml"
CAMINHO_CANAIS_EXEMPLO = RAIZ / "config" / "canais_fontes.example.yaml"


class Config(dict):
    """
    Um dicionário comum, com um atalho a mais: pegar("a.b.c").

    Exemplo:
        cfg.pegar("legendas.fonte")          -> "Arial Black"
        cfg.pegar("ia.chave_api", "vazio")   -> valor ou o padrão "vazio"
    """

    def pegar(self, caminho: str, padrao: Any = None) -> Any:
        atual: Any = self
        for parte in caminho.split("."):
            if isinstance(atual, dict) and parte in atual:
                atual = atual[parte]
            else:
                return padrao
        return atual

    @property
    def raiz(self) -> Path:
        """Pasta raiz do projeto."""
        return RAIZ

    def caminho(self, chave_de_pasta: str) -> Path:
        """
        Devolve um caminho absoluto para uma das pastas do config,
        criando a pasta se ela ainda não existir.

            cfg.caminho("downloads")  ->  .../gta_cuts_factory/dados/downloads
        """
        relativo = self.pegar(f"pastas.{chave_de_pasta}", f"dados/{chave_de_pasta}")
        destino = RAIZ / relativo
        # Se o caminho aponta para um arquivo (ex.: banco.sqlite), cria a pasta-pai.
        if destino.suffix:
            destino.parent.mkdir(parents=True, exist_ok=True)
        else:
            destino.mkdir(parents=True, exist_ok=True)
        return destino


def _mesclar(padrao: dict, usuario: dict) -> dict:
    """
    Junta o config do usuário com o config de exemplo.

    Isso é importante: se num futuro update eu adicionar uma opção nova no
    .example, o seu config.yaml antigo continua funcionando — a opção nova
    entra com o valor padrão em vez de dar erro.
    """
    resultado = copy.deepcopy(padrao)
    for chave, valor in (usuario or {}).items():
        if isinstance(valor, dict) and isinstance(resultado.get(chave), dict):
            resultado[chave] = _mesclar(resultado[chave], valor)
        else:
            resultado[chave] = valor
    return resultado


def _ler_yaml(caminho: Path) -> dict:
    if not caminho.exists():
        return {}
    with open(caminho, "r", encoding="utf-8") as arquivo:
        return yaml.safe_load(arquivo) or {}


def carregar_config(criar_se_faltar: bool = True) -> Config:
    """
    Lê config/config.yaml (e completa com o config.example.yaml).

    Se o config.yaml não existir e criar_se_faltar=True, ele é criado
    automaticamente a partir do exemplo — assim o primeiro teste funciona
    sem você precisar copiar nada na mão.
    """
    if not CAMINHO_CONFIG.exists() and criar_se_faltar:
        if not CAMINHO_CONFIG_EXEMPLO.exists():
            raise FileNotFoundError(
                f"Não encontrei o modelo de configuração: {CAMINHO_CONFIG_EXEMPLO}"
            )
        shutil.copyfile(CAMINHO_CONFIG_EXEMPLO, CAMINHO_CONFIG)
        print(f"[config] Criei o seu arquivo de configuração: {CAMINHO_CONFIG}")

    padrao = _ler_yaml(CAMINHO_CONFIG_EXEMPLO)
    usuario = _ler_yaml(CAMINHO_CONFIG)
    return Config(_mesclar(padrao, usuario))


def carregar_canais() -> dict:
    """
    Lê o banco de canais-fonte aprovados (config/canais_fontes.yaml).

    Devolve algo como:
        {"canais": [...], "assuntos_base": {...}, "assuntos_proibidos": [...]}
    """
    if not CAMINHO_CANAIS.exists() and CAMINHO_CANAIS_EXEMPLO.exists():
        shutil.copyfile(CAMINHO_CANAIS_EXEMPLO, CAMINHO_CANAIS)
        print(f"[config] Criei o seu banco de canais: {CAMINHO_CANAIS}")

    dados = _ler_yaml(CAMINHO_CANAIS)
    dados.setdefault("canais", [])
    dados.setdefault("assuntos_base", {"pt": [], "en": []})
    dados.setdefault("assuntos_proibidos", [])
    return dados


if __name__ == "__main__":
    # Rode "python -m nucleo.config" para conferir se está tudo certo.
    cfg = carregar_config()
    print("Raiz do projeto :", cfg.raiz)
    print("ffmpeg          :", cfg.pegar("ffmpeg_caminho"))
    print("Fonte da legenda:", cfg.pegar("legendas.fonte"))
    print("Resolução       :", cfg.pegar("video.largura"), "x", cfg.pegar("video.altura"))
    print("Canais aprovados:", len(carregar_canais()["canais"]))
