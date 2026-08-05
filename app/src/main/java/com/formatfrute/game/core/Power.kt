package com.formatfrute.game.core

import androidx.compose.ui.graphics.Color

/**
 * Poderes da barraquinha. Todos podem ser comprados com Sementes, mas o
 * caminho principal e assistir um anuncio premiado e usar na hora.
 */
enum class Power(
    val id: String,
    val title: String,
    val desc: String,
    val emoji: String,
    val price: Int,
    val color: Color,
    /** Precisa que o jogador escolha uma celula do tabuleiro. */
    val needsTarget: Boolean = false,
) {
    MARTELO(
        id = "martelo",
        title = "Martelinho",
        desc = "Esmaga uma fruta qualquer do tabuleiro.",
        emoji = "🔨",
        price = 120,
        color = Color(0xFFFF6B6B),
        needsTarget = true,
    ),
    ADUBO(
        id = "adubo",
        title = "Adubo Mágico",
        desc = "Faz uma fruta crescer um nível na hora.",
        emoji = "🌱",
        price = 200,
        color = Color(0xFF2FBF71),
        needsTarget = true,
    ),
    VOLTAR(
        id = "voltar",
        title = "Voltar no Tempo",
        desc = "Desfaz a última jogada. Ninguém viu.",
        emoji = "⏪",
        price = 90,
        color = Color(0xFF7B61FF),
    ),
    PENEIRA(
        id = "peneira",
        title = "Peneira",
        desc = "Chacoalha o tabuleiro e embaralha tudo.",
        emoji = "🧺",
        price = 110,
        color = Color(0xFFFF8A00),
    ),
    RELOGIO(
        id = "relogio",
        title = "Relógio de Açúcar",
        desc = "Ganha 15 segundos. Só vale nos modos com relógio.",
        emoji = "⏱️",
        price = 150,
        color = Color(0xFF3FA9F5),
    ),
    ARCOIRIS(
        id = "arcoiris",
        title = "Fruta Arco-íris",
        desc = "Solta um coringa que combina com qualquer fruta.",
        emoji = "🌈",
        price = 260,
        color = Color(0xFFE0197B),
    ),
    DICA(
        id = "dica",
        title = "Olho Bom",
        desc = "Acende as duas frutas que dá para juntar agora.",
        emoji = "👀",
        price = 70,
        color = Color(0xFF3FA9F5),
    );

    companion object {
        fun byId(id: String?): Power? = entries.firstOrNull { it.id == id }
    }
}
