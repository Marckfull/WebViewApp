package com.formatfrute.game.data

import androidx.compose.ui.graphics.Color

/**
 * Peles do tabuleiro.
 *
 * O catálogo é dividido de propósito: as da **loja** existem para dar destino
 * às Sementes, e as do **Passe** são exclusivas da temporada e nunca entram à
 * venda. Quando os dois vendem a mesma coisa, um esvazia o outro — o jogador
 * ganha de graça no Passe justamente o que ia comprar, e a vitrine perde a
 * função.
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
    /** Só sai no Passe da Feira. A loja nunca lista, nem por preço nenhum. */
    val exclusive: Boolean = false,
) {
    // ----------------------------------------------------------- vitrine

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
        price = 900,
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
        price = 2200,
        top = Color(0xFFFFE3F1),
        bottom = Color(0xFFBFE6FF),
        board = Color(0xFFFFF6FB),
        cell = Color(0xFFF3DDEC),
        frame = Color(0xFF5A3B57),
    ),
    TROPICAL(
        id = "tropical",
        title = "Praia Tropical",
        desc = "Areia, sol e cheiro de coco.",
        price = 4000,
        top = Color(0xFF9BE7FF),
        bottom = Color(0xFFFFE6A7),
        board = Color(0xFFFFF8E4),
        cell = Color(0xFFFFE2B0),
        frame = Color(0xFF1F5C7A),
    ),

    // -------------------------------------------- exclusivas do Passe

    NOITE(
        id = "noite",
        title = "Feira da Meia-Noite",
        desc = "Modo escuro com luzinha de barraca. Só no Passe.",
        price = 0,
        top = Color(0xFF241B3A),
        bottom = Color(0xFF3B2B5E),
        board = Color(0xFF2E2450),
        cell = Color(0xFF3D3168),
        frame = Color(0xFF120C22),
        exclusive = true,
    ),
    JUNINA(
        id = "junina",
        title = "Arraiá da Fruta",
        desc = "Bandeirinha, fogueira e quentão. Só no Passe.",
        price = 0,
        top = Color(0xFFFFD98A),
        bottom = Color(0xFFE86A4A),
        board = Color(0xFFFFF3DC),
        cell = Color(0xFFF7D9A8),
        frame = Color(0xFF7A3B1F),
        exclusive = true,
    ),
    ENCANTADO(
        id = "encantado",
        title = "Pomar Encantado",
        desc = "Roxo, dourado e meio mágico. Só no Passe.",
        price = 0,
        top = Color(0xFF3B2A6B),
        bottom = Color(0xFF7A4FB5),
        board = Color(0xFF4A3785),
        cell = Color(0xFF5C459B),
        frame = Color(0xFF1E1440),
        exclusive = true,
    );

    val dark: Boolean get() = this == NOITE || this == ENCANTADO

    companion object {
        fun byId(id: String?): BoardTheme = entries.firstOrNull { it.id == id } ?: FEIRA

        /** O que a Barraquinha põe na vitrine. */
        val shop: List<BoardTheme> get() = entries.filterNot { it.exclusive }

        /** O que só sai no Passe da Feira. */
        val seasonal: List<BoardTheme> get() = entries.filter { it.exclusive }
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
