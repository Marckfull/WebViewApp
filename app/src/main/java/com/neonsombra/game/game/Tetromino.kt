package com.neonsombra.game.game

/**
 * As sete pecas classicas do Tetris.
 *
 * Cada tipo guarda as suas quatro rotacoes ja resolvidas (padrao SRS simplificado).
 * Cada rotacao e uma lista de celulas (x, y) dentro da caixa do tipo, com y crescendo
 * para baixo -- a mesma orientacao do tabuleiro e do Canvas.
 */
enum class TetrominoType(val boxSize: Int, val rotations: Array<Array<IntArray>>) {

    I(
        4,
        arrayOf(
            arrayOf(intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(2, 1), intArrayOf(3, 1)),
            arrayOf(intArrayOf(2, 0), intArrayOf(2, 1), intArrayOf(2, 2), intArrayOf(2, 3)),
            arrayOf(intArrayOf(0, 2), intArrayOf(1, 2), intArrayOf(2, 2), intArrayOf(3, 2)),
            arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(1, 2), intArrayOf(1, 3)),
        ),
    ),

    O(
        2,
        arrayOf(
            arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1)),
            arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1)),
            arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1)),
            arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1)),
        ),
    ),

    T(
        3,
        arrayOf(
            arrayOf(intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(2, 1)),
            arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(2, 1), intArrayOf(1, 2)),
            arrayOf(intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(2, 1), intArrayOf(1, 2)),
            arrayOf(intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, 2)),
        ),
    ),

    S(
        3,
        arrayOf(
            arrayOf(intArrayOf(1, 0), intArrayOf(2, 0), intArrayOf(0, 1), intArrayOf(1, 1)),
            arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(2, 1), intArrayOf(2, 2)),
            arrayOf(intArrayOf(1, 1), intArrayOf(2, 1), intArrayOf(0, 2), intArrayOf(1, 2)),
            arrayOf(intArrayOf(0, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, 2)),
        ),
    ),

    Z(
        3,
        arrayOf(
            arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(2, 1)),
            arrayOf(intArrayOf(2, 0), intArrayOf(1, 1), intArrayOf(2, 1), intArrayOf(1, 2)),
            arrayOf(intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(1, 2), intArrayOf(2, 2)),
            arrayOf(intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(0, 2)),
        ),
    ),

    J(
        3,
        arrayOf(
            arrayOf(intArrayOf(0, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(2, 1)),
            arrayOf(intArrayOf(1, 0), intArrayOf(2, 0), intArrayOf(1, 1), intArrayOf(1, 2)),
            arrayOf(intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(2, 1), intArrayOf(2, 2)),
            arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, 2), intArrayOf(1, 2)),
        ),
    ),

    L(
        3,
        arrayOf(
            arrayOf(intArrayOf(2, 0), intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(2, 1)),
            arrayOf(intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(1, 2), intArrayOf(2, 2)),
            arrayOf(intArrayOf(0, 1), intArrayOf(1, 1), intArrayOf(2, 1), intArrayOf(0, 2)),
            arrayOf(intArrayOf(0, 0), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(1, 2)),
        ),
    ),
    ;

    /** Celulas da rotacao pedida, ja normalizadas para o intervalo 0..3. */
    fun cells(rotation: Int): Array<IntArray> = rotations[((rotation % 4) + 4) % 4]

    companion object {
        /** Codigo guardado no tabuleiro (0 significa celula vazia). */
        fun fromCode(code: Int): TetrominoType? = entries.getOrNull(code - 1)
    }
}

/** Codigo da peca tal como e gravado no tabuleiro. */
val TetrominoType.code: Int get() = ordinal + 1

/**
 * A peca que esta a cair. [x] e [y] sao a posicao da caixa da peca no tabuleiro.
 */
data class ActivePiece(
    val type: TetrominoType,
    val rotation: Int = 0,
    val x: Int = 0,
    val y: Int = 0,
) {
    /** Celulas absolutas da peca no tabuleiro. */
    inline fun forEachCell(action: (cx: Int, cy: Int) -> Unit) {
        for (cell in type.cells(rotation)) {
            action(x + cell[0], y + cell[1])
        }
    }

    fun absoluteCells(): List<IntArray> =
        type.cells(rotation).map { intArrayOf(x + it[0], y + it[1]) }
}
