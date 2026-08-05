package com.formatfrute.game.core

/** Natureza da peca no tabuleiro. */
enum class TileKind {
    /** Fruta normal: desliza, funde com a igual. */
    FRUIT,

    /** Fruta podre: desliza, nunca funde. Some quando uma fusao acontece do lado. */
    ROTTEN,

    /** Coringa arco-iris: funde com qualquer fruta e sobe um nivel. */
    RAINBOW,
}

/**
 * Uma peca do tabuleiro. O [id] e estavel entre jogadas: e ele que permite
 * animar o deslize da mesma fruta de uma celula para outra.
 */
data class Tile(
    val id: Long,
    val level: Int,
    val row: Int,
    val col: Int,
    val kind: TileKind = TileKind.FRUIT,
    /** Camadas de gelo. Enquanto houver gelo a peca fica travada no lugar. */
    val ice: Int = 0,
    /** Acabou de nascer: a UI anima a entrada. */
    val spawned: Boolean = false,
    /** Nasceu de uma fusao: a UI da o "pop". */
    val merged: Boolean = false,
) {
    val frozen: Boolean get() = ice > 0
    val fruit: Fruit get() = Fruit.of(level)
}

enum class Direction { LEFT, RIGHT, UP, DOWN }

/** Estado imutavel de uma partida. */
data class GameState(
    val size: Int,
    val tiles: List<Tile> = emptyList(),
    /** Pecas que foram absorvidas na ultima jogada; a UI as desenha morrendo. */
    val dying: List<Tile> = emptyList(),
    val score: Int = 0,
    val moves: Int = 0,
    val nextId: Long = 1L,
) {
    val highestLevel: Int get() = tiles.maxOfOrNull { it.level } ?: 0
    val isFull: Boolean get() = tiles.size >= size * size

    fun at(row: Int, col: Int): Tile? = tiles.firstOrNull { it.row == row && it.col == col }
}

/** Tudo que aconteceu numa jogada — a UI usa para som, vibracao e particulas. */
data class MoveResult(
    val state: GameState,
    val changed: Boolean,
    val scoreGain: Int,
    val merges: List<Merge> = emptyList(),
    val brokenIce: Int = 0,
    val cleanedRotten: Int = 0,
    val harvests: Int = 0,
)

/** Uma fusao concreta: onde aconteceu e qual fruta nasceu. */
data class Merge(
    val row: Int,
    val col: Int,
    val level: Int,
    val value: Int,
    /** Fusao de duas melancias: as duas somem e viram colheita. */
    val harvest: Boolean = false,
)
