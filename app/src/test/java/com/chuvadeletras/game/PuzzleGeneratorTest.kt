package com.chuvadeletras.game

import com.chuvadeletras.game.domain.generator.PuzzleGenerator
import com.chuvadeletras.game.domain.model.Difficulty
import com.chuvadeletras.game.domain.model.Direction
import com.chuvadeletras.game.domain.model.GridPos
import com.chuvadeletras.game.domain.model.Puzzle
import com.chuvadeletras.game.domain.words.WordBank
import com.chuvadeletras.game.domain.words.WordTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleGeneratorTest {

    /**
     * Revalida a grade do zero, sem confiar no gerador: nenhuma letra pode
     * conflitar, nenhuma palavra pode emendar na outra e toda sequência de duas
     * ou mais letras precisa ser uma palavra declarada (senão a grade mostraria
     * "palavras" que não têm dica).
     */
    private fun problemsIn(puzzle: Puzzle): List<String> {
        val problems = mutableListOf<String>()

        puzzle.entries.forEach { entry ->
            entry.cells.forEachIndexed { index, pos ->
                if (puzzle.solution[pos] != entry.answer[index]) {
                    problems += "letra conflitante em $pos (${entry.answer})"
                }
                if (pos.row !in 0 until puzzle.rows || pos.col !in 0 until puzzle.cols) {
                    problems += "casa fora da grade: $pos"
                }
            }
            val before = if (entry.direction == Direction.ACROSS) {
                GridPos(entry.start.row, entry.start.col - 1)
            } else {
                GridPos(entry.start.row - 1, entry.start.col)
            }
            val after = if (entry.direction == Direction.ACROSS) {
                GridPos(entry.start.row, entry.start.col + entry.length)
            } else {
                GridPos(entry.start.row + entry.length, entry.start.col)
            }
            if (puzzle.solution.containsKey(before)) problems += "${entry.answer} emenda antes"
            if (puzzle.solution.containsKey(after)) problems += "${entry.answer} emenda depois"
        }

        val declared = puzzle.entries.map { it.cells }.toSet()
        fun scan(outer: Int, inner: Int, build: (Int, Int) -> GridPos) {
            for (a in 0 until outer) {
                var run = mutableListOf<GridPos>()
                for (b in 0..inner) {
                    val occupied = b < inner && puzzle.solution.containsKey(build(a, b))
                    if (occupied) {
                        run.add(build(a, b))
                    } else {
                        if (run.size >= 2 && run !in declared) {
                            problems += "sequência sem dica em $run"
                        }
                        run = mutableListOf()
                    }
                }
            }
        }
        scan(puzzle.rows, puzzle.cols) { r, c -> GridPos(r, c) }
        scan(puzzle.cols, puzzle.rows) { c, r -> GridPos(r, c) }

        if (puzzle.entries.map { it.answer }.distinct().size != puzzle.entries.size) {
            problems += "palavra repetida na mesma grade"
        }
        return problems
    }

    @Test
    fun `grades geradas sao sempre validas`() {
        var checked = 0
        for (theme in WordTheme.entries) {
            for (difficulty in Difficulty.entries) {
                for (seed in 1L..15L) {
                    val bank = WordBank.forTheme(theme, difficulty.maxWordLength)
                    val puzzle = PuzzleGenerator.generate(
                        bank = bank,
                        targetWords = difficulty.targetWords,
                        seed = seed * 31 + theme.ordinal,
                        theme = theme.label
                    )
                    assertEquals(
                        "grade inválida (${theme.label}/${difficulty.label}/seed=$seed)",
                        emptyList<String>(),
                        problemsIn(puzzle)
                    )
                    checked++
                }
            }
        }
        assertTrue(checked > 300)
    }

    @Test
    fun `grade cabe na tela do celular`() {
        for (seed in 1L..40L) {
            val puzzle = PuzzleGenerator.generate(
                bank = WordBank.forTheme(WordTheme.GERAL, Difficulty.INSANO.maxWordLength),
                targetWords = Difficulty.INSANO.targetWords,
                seed = seed,
                theme = "Geral"
            )
            assertTrue("largura ${puzzle.cols} passou do limite", puzzle.cols <= 11)
            assertTrue("altura ${puzzle.rows} passou do limite", puzzle.rows <= 15)
        }
    }

    @Test
    fun `a mesma seed produz a mesma grade`() {
        val bank = WordBank.forTheme(WordTheme.GERAL, 9)
        val a = PuzzleGenerator.generate(bank, 9, 20260727L, "Geral")
        val b = PuzzleGenerator.generate(bank, 9, 20260727L, "Geral")
        assertEquals(a.entries, b.entries)
        assertEquals(a.rows, b.rows)
        assertEquals(a.cols, b.cols)
    }

    @Test
    fun `grade tem palavras suficientes para valer a partida`() {
        for (seed in 1L..30L) {
            val puzzle = PuzzleGenerator.generate(
                bank = WordBank.forTheme(WordTheme.GERAL, Difficulty.MEDIO.maxWordLength),
                targetWords = Difficulty.MEDIO.targetWords,
                seed = seed,
                theme = "Geral"
            )
            assertTrue("só ${puzzle.entries.size} palavras", puzzle.entries.size >= 5)
        }
    }

    @Test
    fun `banco de palavras usa apenas letras de A a Z`() {
        WordTheme.entries.forEach { theme ->
            WordBank.forTheme(theme).forEach { word ->
                assertTrue(
                    "resposta com caractere inválido: ${word.answer}",
                    word.answer.all { it in 'A'..'Z' }
                )
                assertTrue("dica vazia em ${word.answer}", word.clue.isNotBlank())
            }
        }
    }
}
