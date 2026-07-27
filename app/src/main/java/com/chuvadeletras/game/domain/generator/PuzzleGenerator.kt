package com.chuvadeletras.game.domain.generator

import com.chuvadeletras.game.domain.model.Direction
import com.chuvadeletras.game.domain.model.GridPos
import com.chuvadeletras.game.domain.model.Puzzle
import com.chuvadeletras.game.domain.model.PuzzleEntry
import com.chuvadeletras.game.domain.model.WordClue
import kotlin.random.Random

/**
 * Monta cruzadinhas a partir de um banco de palavras.
 *
 * A grade é construída por encaixe: a primeira palavra vai deitada no centro e
 * cada palavra seguinte precisa cruzar alguma já colocada. Como a validação
 * acontece antes de cada encaixe, toda grade gerada é, por construção, válida —
 * não existe palavra colada em outra sem cruzamento nem letra conflitante.
 *
 * Uma mesma `seed` sempre produz a mesma grade: é isso que faz o Desafio Diário
 * ser idêntico para todo mundo.
 */
object PuzzleGenerator {

    private const val CANVAS = 40
    private const val MAX_ATTEMPTS = 12

    /**
     * @param maxWidth largura máxima em colunas. Tela de celular é estreita: sem
     * esse teto o encaixe produz grades de 20+ colunas e o quadradinho fica
     * pequeno demais para o dedo.
     */
    fun generate(
        bank: List<WordClue>,
        targetWords: Int,
        seed: Long,
        theme: String,
        id: String = "puzzle-$seed",
        maxWidth: Int = 11,
        maxHeight: Int = 15
    ): Puzzle {
        require(bank.isNotEmpty()) { "Banco de palavras vazio" }

        var best: Puzzle? = null
        for (round in 0 until MAX_ATTEMPTS) {
            val candidate = attempt(bank, targetWords, seed + round * 7919L, theme, id, maxWidth, maxHeight)
            if (candidate != null) {
                if (candidate.entries.size >= targetWords) return candidate
                if (best == null || candidate.entries.size > best.entries.size) best = candidate
            }
        }
        return best ?: fallback(bank, seed, theme, id)
    }

    private data class Placement(
        val word: WordClue,
        val start: GridPos,
        val direction: Direction
    ) {
        val cells: List<GridPos> = List(word.answer.length) { i ->
            if (direction == Direction.ACROSS) GridPos(start.row, start.col + i)
            else GridPos(start.row + i, start.col)
        }
    }

    private fun attempt(
        bank: List<WordClue>,
        targetWords: Int,
        seed: Long,
        theme: String,
        id: String,
        maxWidth: Int,
        maxHeight: Int
    ): Puzzle? {
        val random = Random(seed)
        val pool = bank
            .filter { it.answer.length in 3..minOf(maxWidth, maxHeight) }
            .shuffled(random)
            .sortedByDescending { it.answer.length }

        if (pool.isEmpty()) return null

        val letters = HashMap<GridPos, Char>()
        val directions = HashMap<GridPos, MutableSet<Direction>>()
        val placements = mutableListOf<Placement>()

        // A palavra mais longa abre a grade, deitada, no meio do canvas.
        val first = pool.first()
        val firstStart = GridPos(CANVAS / 2, CANVAS / 2 - first.answer.length / 2)
        commit(Placement(first, firstStart, Direction.ACROSS), letters, directions, placements)

        for (word in pool.drop(1)) {
            if (placements.size >= targetWords) break
            if (placements.any { it.word.answer == word.answer }) continue

            val placement = bestPlacement(word, letters, directions, random, maxWidth, maxHeight) ?: continue
            commit(placement, letters, directions, placements)
        }

        if (placements.size < 3) return null
        return buildPuzzle(placements, theme, id)
    }

    /** Escolhe o melhor encaixe possível da palavra na grade atual. */
    private fun bestPlacement(
        word: WordClue,
        letters: Map<GridPos, Char>,
        directions: Map<GridPos, Set<Direction>>,
        random: Random,
        maxWidth: Int,
        maxHeight: Int
    ): Placement? {
        val answer = word.answer
        var best: Placement? = null
        var bestScore = Int.MIN_VALUE

        // Para cada casa já ocupada, tenta cruzar por cada letra igual da palavra.
        for ((anchor, anchorChar) in letters) {
            val anchorDirs = directions[anchor].orEmpty()
            // Cruza na direção perpendicular à(s) palavra(s) que já passam ali.
            val direction = when {
                Direction.ACROSS in anchorDirs && Direction.DOWN in anchorDirs -> continue
                Direction.ACROSS in anchorDirs -> Direction.DOWN
                else -> Direction.ACROSS
            }

            answer.forEachIndexed { index, c ->
                if (c != anchorChar) return@forEachIndexed
                val start = if (direction == Direction.ACROSS) {
                    GridPos(anchor.row, anchor.col - index)
                } else {
                    GridPos(anchor.row - index, anchor.col)
                }
                val candidate = Placement(word, start, direction)
                val crossings = validate(candidate, letters, directions) ?: return@forEachIndexed

                // A grade não pode estourar o retângulo que cabe na tela.
                val cols = candidate.cells.map { it.col } + letters.keys.map { it.col }
                val rows = candidate.cells.map { it.row } + letters.keys.map { it.row }
                val width = cols.max() - cols.min() + 1
                val height = rows.max() - rows.min() + 1
                if (width > maxWidth || height > maxHeight) return@forEachIndexed

                // Prioriza cruzamentos e penaliza crescimento — a largura pesa mais
                // que a altura, porque a tela do celular é em pé.
                val score = crossings * 120 - width * 14 - height * 6 + random.nextInt(24)
                if (score > bestScore) {
                    bestScore = score
                    best = candidate
                }
            }
        }
        return best
    }

    /**
     * Devolve o número de cruzamentos se o encaixe for legal, ou null se for
     * ilegal (letra conflitante, palavra colada em outra ou emenda nas pontas).
     */
    private fun validate(
        placement: Placement,
        letters: Map<GridPos, Char>,
        directions: Map<GridPos, Set<Direction>>
    ): Int? {
        val cells = placement.cells
        if (cells.any { it.row !in 1 until CANVAS - 1 || it.col !in 1 until CANVAS - 1 }) return null

        val before: GridPos
        val after: GridPos
        if (placement.direction == Direction.ACROSS) {
            before = GridPos(placement.start.row, placement.start.col - 1)
            after = GridPos(placement.start.row, placement.start.col + cells.size)
        } else {
            before = GridPos(placement.start.row - 1, placement.start.col)
            after = GridPos(placement.start.row + cells.size, placement.start.col)
        }
        // As pontas precisam respirar: nada de emendar duas palavras.
        if (letters.containsKey(before) || letters.containsKey(after)) return null

        var crossings = 0
        cells.forEachIndexed { index, pos ->
            val existing = letters[pos]
            if (existing != null) {
                if (existing != placement.word.answer[index]) return null
                // Não pode sobrepor uma palavra que já corre na mesma direção.
                if (placement.direction in directions[pos].orEmpty()) return null
                crossings++
            } else {
                // Casa nova: os vizinhos perpendiculares precisam estar vazios,
                // senão criaríamos uma "palavra" acidental de duas letras.
                val (n1, n2) = if (placement.direction == Direction.ACROSS) {
                    GridPos(pos.row - 1, pos.col) to GridPos(pos.row + 1, pos.col)
                } else {
                    GridPos(pos.row, pos.col - 1) to GridPos(pos.row, pos.col + 1)
                }
                if (letters.containsKey(n1) || letters.containsKey(n2)) return null
            }
        }
        return if (crossings > 0) crossings else null
    }

    private fun commit(
        placement: Placement,
        letters: MutableMap<GridPos, Char>,
        directions: MutableMap<GridPos, MutableSet<Direction>>,
        placements: MutableList<Placement>
    ) {
        placement.cells.forEachIndexed { index, pos ->
            letters[pos] = placement.word.answer[index]
            directions.getOrPut(pos) { mutableSetOf() }.add(placement.direction)
        }
        placements.add(placement)
    }

    /** Recorta o canvas para o retângulo usado e numera as palavras. */
    private fun buildPuzzle(
        placements: List<Placement>,
        theme: String,
        id: String
    ): Puzzle {
        val all = placements.flatMap { it.cells }
        val minRow = all.minOf { it.row }
        val minCol = all.minOf { it.col }
        val shifted = placements.map { p ->
            p.copy(start = GridPos(p.start.row - minRow, p.start.col - minCol))
        }

        val rows = shifted.flatMap { it.cells }.maxOf { it.row } + 1
        val cols = shifted.flatMap { it.cells }.maxOf { it.col } + 1

        // Numeração de cruzadinha: casas que iniciam palavra, em ordem de leitura.
        val startCells = shifted.map { it.start }
            .distinct()
            .sortedWith(compareBy({ it.row }, { it.col }))
        val numberOf = startCells.withIndex().associate { (index, pos) -> pos to index + 1 }

        val entries = shifted
            .sortedWith(compareBy({ numberOf.getValue(it.start) }, { it.direction }))
            .mapIndexed { index, p ->
                PuzzleEntry(
                    id = index,
                    number = numberOf.getValue(p.start),
                    clue = p.word.clue,
                    answer = p.word.answer,
                    start = p.start,
                    direction = p.direction
                )
            }

        return Puzzle(id = id, theme = theme, rows = rows, cols = cols, entries = entries)
    }

    /** Rede de segurança: uma grade mínima de uma palavra só, nunca deve acontecer. */
    private fun fallback(bank: List<WordClue>, seed: Long, theme: String, id: String): Puzzle {
        val word = bank.maxByOrNull { it.answer.length } ?: WordClue("LETRA", "Cada símbolo do alfabeto")
        val entry = PuzzleEntry(
            id = 0,
            number = 1,
            clue = word.clue,
            answer = word.answer,
            start = GridPos(0, 0),
            direction = Direction.ACROSS
        )
        return Puzzle(id = id, theme = theme, rows = 1, cols = word.answer.length, entries = listOf(entry))
    }
}
