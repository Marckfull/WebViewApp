package com.formatfrute.game.core

import androidx.compose.ui.graphics.Color

/**
 * Os modos do Format Frute. Cada um muda a regra de verdade — nao e o mesmo
 * jogo com outra cor.
 */
enum class GameMode(
    val id: String,
    val title: String,
    val tagline: String,
    val howTo: String,
    val emblem: Fruit,
    val gridSize: Int,
    val accent: Color,
    val accent2: Color,
    /** Segundos de relogio; 0 = sem relogio. */
    val timeLimit: Int = 0,
    /** Jogadas permitidas; 0 = ilimitado. */
    val moveLimit: Int = 0,
    val iceChance: Float = 0f,
    val rainbowChance: Float = 0.02f,
    /** Nunca perde: quando lota, colhe as menores. */
    val endless: Boolean = false,
    val boss: Boolean = false,
    /** Fruta que fecha o modo. */
    val goalLevel: Int = Fruit.MAX,
) {
    POMAR(
        id = "pomar",
        title = "Pomar Clássico",
        tagline = "O começo de todo feirante",
        howTo = "Arraste para juntar frutas iguais. Chegue na Pitaya para vencer — " +
            "e continue jogando para colher a Melancia.",
        emblem = Fruit.MACA,
        gridSize = 4,
        accent = Color(0xFFFF4D6D),
        accent2 = Color(0xFFFFA08C),
        goalLevel = Fruit.PITAYA.ordinal,
    ),

    VITAMINA(
        id = "vitamina",
        title = "Vitamina Turbo",
        tagline = "60 segundos de liquidificador",
        howTo = "O relógio corre! Cada fusão devolve segundos e o combo multiplica " +
            "os pontos. Pare de juntar e o tempo te alcança.",
        emblem = Fruit.LARANJA,
        gridSize = 4,
        accent = Color(0xFFFF8A00),
        accent2 = Color(0xFFFFD246),
        timeLimit = 60,
        rainbowChance = 0.04f,
    ),

    GELEIA(
        id = "geleia",
        title = "Geleia Congelada",
        tagline = "Frutas presas no gelo",
        howTo = "Algumas frutas nascem congeladas: não deslizam nem fundem. " +
            "Fusões vizinhas racham o gelo e libertam a fruta.",
        emblem = Fruit.UVA,
        gridSize = 5,
        accent = Color(0xFF3FA9F5),
        accent2 = Color(0xFF9BE7FF),
        iceChance = 0.20f,
    ),

    ZEN(
        id = "zen",
        title = "Zen do Pomar",
        tagline = "Sem relógio, sem derrota",
        howTo = "Tabuleiro grande e nenhuma pressão. Quando enche, o pomar colhe " +
            "sozinho as frutinhas menores e o jogo continua.",
        emblem = Fruit.PERA,
        gridSize = 5,
        accent = Color(0xFF2FBF71),
        accent2 = Color(0xFFB7EFC5),
        endless = true,
    ),

    RECEITA(
        id = "receita",
        title = "Modo Receita",
        tagline = "Monte o pedido do freguês",
        howTo = "Cada fase pede frutas específicas. Só conta a fruta que você " +
            "CRIAR fundindo — o que já está no tabuleiro é matéria-prima. " +
            "Sobrar jogada no fim vale estrela.",
        emblem = Fruit.MORANGO,
        gridSize = 4,
        accent = Color(0xFFE0197B),
        accent2 = Color(0xFFFFA3D1),
        moveLimit = 25,
        rainbowChance = 0.04f,
    ),

    BATALHA(
        id = "batalha",
        title = "Batalha do Suco",
        tagline = "Contra o Monstro Azedo",
        howTo = "Cada fusão vira dano no Monstro Azedo. A cada 3 jogadas ele " +
            "cospe uma fruta podre no seu tabuleiro. Zere a vida dele!",
        emblem = Fruit.LIMAO,
        gridSize = 4,
        accent = Color(0xFF00A67E),
        accent2 = Color(0xFF8FE3C7),
        boss = true,
    );

    val hasClock: Boolean get() = timeLimit > 0

    /** O Modo Receita entra pelo mapa de fases, não pela lista de modos. */
    val isCampaign: Boolean get() = this == RECEITA

    companion object {
        fun byId(id: String?): GameMode = entries.firstOrNull { it.id == id } ?: POMAR

        /** Modos avulsos, que começam direto da tela inicial. */
        val arcade: List<GameMode> get() = entries.filterNot { it.isCampaign }
    }
}
