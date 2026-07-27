package com.chuvadeletras.game.domain.model

/** Direção de uma palavra dentro da grade. */
enum class Direction { ACROSS, DOWN }

/** Coordenada de uma casa da grade (linha, coluna). */
data class GridPos(val row: Int, val col: Int)

/** Uma palavra do banco: resposta + dica. */
data class WordClue(val answer: String, val clue: String)

/**
 * Uma palavra já posicionada na grade, com número de referência
 * (o número que aparece no quadradinho inicial e ao lado da dica).
 */
data class PuzzleEntry(
    val id: Int,
    val number: Int,
    val clue: String,
    val answer: String,
    val start: GridPos,
    val direction: Direction
) {
    val length: Int get() = answer.length

    val cells: List<GridPos> = List(answer.length) { i ->
        if (direction == Direction.ACROSS) {
            GridPos(start.row, start.col + i)
        } else {
            GridPos(start.row + i, start.col)
        }
    }

    fun charAt(pos: GridPos): Char? {
        val index = cells.indexOf(pos)
        return if (index >= 0) answer[index] else null
    }
}

/**
 * Uma cruzadinha completa e resolvível. A grade é definida apenas pelas
 * palavras: qualquer casa que não pertença a nenhuma palavra é "parede".
 */
data class Puzzle(
    val id: String,
    val theme: String,
    val rows: Int,
    val cols: Int,
    val entries: List<PuzzleEntry>
) {
    /** Letra correta de cada casa preenchível. */
    val solution: Map<GridPos, Char> = entries
        .flatMap { entry -> entry.cells.mapIndexed { index, pos -> pos to entry.answer[index] } }
        .toMap()

    /** Número exibido no canto do quadradinho, quando ele inicia uma palavra. */
    val startNumbers: Map<GridPos, Int> = entries
        .groupBy { it.start }
        .mapValues { (_, list) -> list.minOf { it.number } }

    /** Ids das palavras que passam por cada casa (1 quando não há cruzamento, 2 quando há). */
    val entriesByCell: Map<GridPos, List<Int>> = entries
        .flatMap { entry -> entry.cells.map { pos -> pos to entry.id } }
        .groupBy({ it.first }, { it.second })

    val cellCount: Int get() = solution.size

    val across: List<PuzzleEntry> get() = entries.filter { it.direction == Direction.ACROSS }
    val down: List<PuzzleEntry> get() = entries.filter { it.direction == Direction.DOWN }

    fun entryById(id: Int): PuzzleEntry? = entries.firstOrNull { it.id == id }
}
