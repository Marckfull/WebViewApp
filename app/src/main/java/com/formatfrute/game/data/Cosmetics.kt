package com.formatfrute.game.data

import androidx.compose.ui.graphics.Color

/**
 * Peles do tabuleiro. Sao a recompensa "de vitrine" da loja: nao mexem na
 * regra, mexem na vontade de voltar.
 */
enum class BoardTheme(
    val id: String,
    val title: String,
    val desc: String,
    val price: Int,
    val top: Color,
    val bottom: Color,
    val board: Color,
    val cell: Color,
    val frame: Color,
) {
    FEIRA(
        id = "feira",
        title = "Feira de Domingo",
        desc = "O clássico ensolarado da barraca.",
        price = 0,
        top = Color(0xFFFFE9C9),
        bottom = Color(0xFFFFC29B),
        board = Color(0xFFFFF7EA),
        cell = Color(0xFFF6E2C8),
        frame = Color(0xFF3D2B1F),
    ),
    POMAR(
        id = "pomar",
        title = "Pomar ao Amanhecer",
        desc = "Verdinho, calmo, cheirando a mato molhado.",
        price = 800,
        top = Color(0xFFD9F7C4),
        bottom = Color(0xFF8FD98B),
        board = Color(0xFFF3FFE9),
        cell = Color(0xFFD8EEC6),
        frame = Color(0xFF25452A),
    ),
    SORVETE(
        id = "sorvete",
        title = "Sorvete de Frutas",
        desc = "Pastel, fofo e gelado que nem picolé.",
        price = 1200,
        top = Color(0xFFFFE3F1),
        bottom = Color(0xFFBFE6FF),
        board = Color(0xFFFFF6FB),
        cell = Color(0xFFF3DDEC),
        frame = Color(0xFF5A3B57),
    ),
    NOITE(
        id = "noite",
        title = "Feira da Meia-Noite",
        desc = "Modo escuro com luzinha de barraca.",
        price = 1800,
        top = Color(0xFF241B3A),
        bottom = Color(0xFF3B2B5E),
        board = Color(0xFF2E2450),
        cell = Color(0xFF3D3168),
        frame = Color(0xFF120C22),
    ),
    TROPICAL(
        id = "tropical",
        title = "Praia Tropical",
        desc = "Areia, sol e cheiro de coco.",
        price = 2500,
        top = Color(0xFF9BE7FF),
        bottom = Color(0xFFFFE6A7),
        board = Color(0xFFFFF8E4),
        cell = Color(0xFFFFE2B0),
        frame = Color(0xFF1F5C7A),
    );

    val dark: Boolean get() = this == NOITE

    companion object {
        fun byId(id: String?): BoardTheme = entries.firstOrNull { it.id == id } ?: FEIRA
    }
}

/** Patentes do feirante — a barra de XP da tela inicial. */
object Ranks {
    private val titles = listOf(
        "Aprendiz de Feirante",
        "Catador de Cereja",
        "Ajudante de Barraca",
        "Vendedor de Suco",
        "Mestre da Salada",
        "Chef de Vitamina",
        "Guardião do Pomar",
        "Barão da Melancia",
        "Lenda da Feira",
    )

    fun levelFor(xp: Int): Int {
        var level = 1
        var need = 300
        var left = xp
        while (left >= need && level < 99) {
            left -= need
            level++
            need = (need * 1.18f).toInt()
        }
        return level
    }

    fun progress(xp: Int): Pair<Int, Int> {
        var need = 300
        var left = xp
        var level = 1
        while (left >= need && level < 99) {
            left -= need
            level++
            need = (need * 1.18f).toInt()
        }
        return left to need
    }

    fun title(level: Int): String = titles[((level - 1) / 3).coerceIn(0, titles.lastIndex)]
}
