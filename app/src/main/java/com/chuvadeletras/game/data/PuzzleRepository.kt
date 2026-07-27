package com.chuvadeletras.game.data

import com.chuvadeletras.game.domain.generator.PuzzleGenerator
import com.chuvadeletras.game.domain.model.Difficulty
import com.chuvadeletras.game.domain.model.GameMode
import com.chuvadeletras.game.domain.model.Puzzle
import com.chuvadeletras.game.domain.words.WordBank
import com.chuvadeletras.game.domain.words.WordTheme
import java.time.LocalDate
import kotlin.random.Random

/** Fabrica as grades do jogo. */
class PuzzleRepository {

    /** Seed do dia: todo jogador recebe exatamente a mesma grade no Desafio Diário. */
    fun dailySeed(date: LocalDate = LocalDate.now()): Long = date.toEpochDay() * 1_000_003L

    fun dailyTheme(date: LocalDate = LocalDate.now()): WordTheme {
        val themes = WordTheme.entries
        return themes[(date.toEpochDay() % themes.size).toInt()]
    }

    fun create(
        mode: GameMode,
        difficulty: Difficulty,
        theme: WordTheme? = null,
        stage: Int = 1
    ): Puzzle {
        val seed = when (mode) {
            GameMode.DIARIO -> dailySeed()
            else -> Random.nextLong(1, Long.MAX_VALUE / 2)
        } + stage * 104_729L

        val chosenTheme = theme ?: when (mode) {
            GameMode.DIARIO -> dailyTheme()
            else -> WordTheme.GERAL
        }

        val bank = WordBank.forTheme(chosenTheme, difficulty.maxWordLength)
        return PuzzleGenerator.generate(
            bank = bank,
            targetWords = difficulty.targetWords,
            seed = seed,
            theme = chosenTheme.label,
            id = "${mode.name}-${difficulty.name}-$seed"
        )
    }

    /** Grade fixa e pequena usada pelo tutorial: previsível e curta de propósito. */
    fun tutorialPuzzle(): Puzzle = PuzzleGenerator.generate(
        bank = WordBank.forTheme(WordTheme.GERAL, 6),
        targetWords = 4,
        seed = 777L,
        theme = "Tutorial",
        id = "tutorial"
    )
}
