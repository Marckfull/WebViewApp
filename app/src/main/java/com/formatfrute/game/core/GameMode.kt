package com.formatfrute.game.core

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.formatfrute.game.R

/**
 * Os modos do Format Frute. Cada um muda a regra de verdade — nao e o mesmo
 * jogo com outra cor.
 */
enum class GameMode(
    val id: String,
    @StringRes val title: Int,
    @StringRes val tagline: Int,
    @StringRes val howTo: Int,
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
        title = R.string.mode_pomar_title,
        tagline = R.string.mode_pomar_tagline,
        howTo = R.string.mode_pomar_howto,
        emblem = Fruit.MACA,
        gridSize = 4,
        accent = Color(0xFFFF4D6D),
        accent2 = Color(0xFFFFA08C),
        goalLevel = Fruit.PITAYA.ordinal,
    ),

    VITAMINA(
        id = "vitamina",
        title = R.string.mode_vitamina_title,
        tagline = R.string.mode_vitamina_tagline,
        howTo = R.string.mode_vitamina_howto,
        emblem = Fruit.LARANJA,
        gridSize = 4,
        accent = Color(0xFFFF8A00),
        accent2 = Color(0xFFFFD246),
        timeLimit = 60,
        rainbowChance = 0.04f,
    ),

    GELEIA(
        id = "geleia",
        title = R.string.mode_geleia_title,
        tagline = R.string.mode_geleia_tagline,
        howTo = R.string.mode_geleia_howto,
        emblem = Fruit.UVA,
        gridSize = 5,
        accent = Color(0xFF3FA9F5),
        accent2 = Color(0xFF9BE7FF),
        iceChance = 0.20f,
    ),

    RECEITA(
        id = "receita",
        title = R.string.mode_receita_title,
        tagline = R.string.mode_receita_tagline,
        howTo = R.string.mode_receita_howto,
        emblem = Fruit.MORANGO,
        gridSize = 4,
        accent = Color(0xFFE0197B),
        accent2 = Color(0xFFFFA3D1),
        moveLimit = 25,
        rainbowChance = 0.04f,
    ),

    ZEN(
        id = "zen",
        title = R.string.mode_zen_title,
        tagline = R.string.mode_zen_tagline,
        howTo = R.string.mode_zen_howto,
        emblem = Fruit.PERA,
        gridSize = 5,
        accent = Color(0xFF2FBF71),
        accent2 = Color(0xFFB7EFC5),
        endless = true,
    ),

    BATALHA(
        id = "batalha",
        title = R.string.mode_batalha_title,
        tagline = R.string.mode_batalha_tagline,
        howTo = R.string.mode_batalha_howto,
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
