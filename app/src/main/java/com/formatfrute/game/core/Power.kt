package com.formatfrute.game.core

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.formatfrute.game.R

/**
 * Poderes da barraquinha. Todos podem ser comprados com Sementes, mas o
 * caminho principal e assistir um anuncio premiado e usar na hora.
 */
enum class Power(
    val id: String,
    @StringRes val title: Int,
    @StringRes val desc: Int,
    val emoji: String,
    val price: Int,
    val color: Color,
    /** Precisa que o jogador escolha uma celula do tabuleiro. */
    val needsTarget: Boolean = false,
) {
    MARTELO(
        id = "martelo",
        title = R.string.power_martelo_title,
        desc = R.string.power_martelo_desc,
        emoji = "🔨",
        price = 120,
        color = Color(0xFFFF6B6B),
        needsTarget = true,
    ),
    ADUBO(
        id = "adubo",
        title = R.string.power_adubo_title,
        desc = R.string.power_adubo_desc,
        emoji = "🌱",
        price = 200,
        color = Color(0xFF2FBF71),
        needsTarget = true,
    ),
    VOLTAR(
        id = "voltar",
        title = R.string.power_voltar_title,
        desc = R.string.power_voltar_desc,
        emoji = "⏪",
        price = 90,
        color = Color(0xFF7B61FF),
    ),
    PENEIRA(
        id = "peneira",
        title = R.string.power_peneira_title,
        desc = R.string.power_peneira_desc,
        emoji = "🧺",
        price = 110,
        color = Color(0xFFFF8A00),
    ),
    RELOGIO(
        id = "relogio",
        title = R.string.power_relogio_title,
        desc = R.string.power_relogio_desc,
        emoji = "⏱️",
        price = 150,
        color = Color(0xFF3FA9F5),
    ),
    ARCOIRIS(
        id = "arcoiris",
        title = R.string.power_arcoiris_title,
        desc = R.string.power_arcoiris_desc,
        emoji = "🌈",
        price = 260,
        color = Color(0xFFE0197B),
    ),
    DICA(
        id = "dica",
        title = R.string.power_dica_title,
        desc = R.string.power_dica_desc,
        emoji = "👀",
        price = 70,
        color = Color(0xFF3FA9F5),
    );

    companion object {
        fun byId(id: String?): Power? = entries.firstOrNull { it.id == id }
    }
}
