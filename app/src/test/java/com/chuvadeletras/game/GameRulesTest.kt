package com.chuvadeletras.game

import com.chuvadeletras.game.domain.engine.GameRules
import com.chuvadeletras.game.domain.generator.PuzzleGenerator
import com.chuvadeletras.game.domain.model.CellStatus
import com.chuvadeletras.game.domain.model.Difficulty
import com.chuvadeletras.game.domain.model.GameConfig
import com.chuvadeletras.game.domain.model.GameMode
import com.chuvadeletras.game.domain.model.GameState
import com.chuvadeletras.game.domain.model.MatchSummary
import com.chuvadeletras.game.domain.model.PowerUp
import com.chuvadeletras.game.domain.words.WordBank
import com.chuvadeletras.game.domain.words.WordTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameRulesTest {

    private fun newGame(
        mode: GameMode = GameMode.CLASSICO,
        difficulty: Difficulty = Difficulty.MEDIO,
        seed: Long = 42L,
        powerUps: Map<PowerUp, Int> = emptyMap()
    ): GameState {
        val puzzle = PuzzleGenerator.generate(
            bank = WordBank.forTheme(WordTheme.GERAL, difficulty.maxWordLength),
            targetWords = difficulty.targetWords,
            seed = seed,
            theme = "Geral"
        )
        val config = GameConfig.forMode(mode, difficulty, seed, puzzle.cellCount)
        return GameRules.newGame(puzzle, config, powerUps, Random(seed))
    }

    // -- Bandeja ---------------------------------------------------------

    @Test
    fun `a bandeja sempre entrega letras jogaveis`() {
        for (seed in 1L..40L) {
            val game = newGame(seed = seed)
            val needed = game.cells.values.filter { it.isOpen }.map { it.solution }.toSet()
            val useful = game.tray.count { it.letter in needed }
            assertTrue(
                "bandeja com só $useful letras úteis para ${game.config.movesPerRound} jogadas",
                useful >= game.config.movesPerRound
            )
        }
    }

    @Test
    fun `bandeja tem o tamanho configurado`() {
        val game = newGame()
        assertEquals(game.config.traySize, game.tray.size)
    }

    // -- Jogada ----------------------------------------------------------

    @Test
    fun `encaixar a letra certa preenche a casa e gasta uma jogada`() {
        val game = newGame()
        val target = game.cells.values.first { it.isOpen }
        val tile = game.tray.first { it.letter == target.solution }

        val after = GameRules.placeTile(
            GameRules.selectCell(game, target.pos),
            tile.id,
            Random(1)
        )

        assertEquals(CellStatus.CORRECT, after.cells.getValue(target.pos).status)
        assertEquals(game.movesLeft - 1, after.movesLeft)
        assertTrue(after.score > game.score)
        assertTrue("a peça usada deveria sair da bandeja", after.tray.none { it.id == tile.id })
    }

    @Test
    fun `letra errada marca a casa e pode ser limpa de graca`() {
        val game = newGame()
        val target = game.cells.values.first { it.isOpen }
        val wrong = game.tray.firstOrNull { it.letter != target.solution } ?: return

        val after = GameRules.placeTile(
            GameRules.selectCell(game, target.pos),
            wrong.id,
            Random(1)
        )
        assertEquals(CellStatus.WRONG, after.cells.getValue(target.pos).status)
        assertEquals(0, after.combo)

        val cleared = GameRules.clearCell(after, target.pos)
        assertEquals(CellStatus.EMPTY, cleared.cells.getValue(target.pos).status)
        assertEquals(after.movesLeft, cleared.movesLeft)
    }

    @Test
    fun `no modo assistido a letra errada e recusada sem custo`() {
        val puzzle = PuzzleGenerator.generate(
            WordBank.forTheme(WordTheme.GERAL, 9), 9, 5L, "Geral"
        )
        val config = GameConfig.forMode(GameMode.ZEN, Difficulty.MEDIO, 5L, puzzle.cellCount)
        val game = GameRules.newGame(puzzle, config, emptyMap(), Random(5))

        val target = game.cells.values.first { it.isOpen }
        val wrong = game.tray.firstOrNull { it.letter != target.solution } ?: return
        val after = GameRules.placeTile(GameRules.selectCell(game, target.pos), wrong.id, Random(1))

        assertEquals(CellStatus.EMPTY, after.cells.getValue(target.pos).status)
        assertEquals(game.movesLeft, after.movesLeft)
    }

    // -- A regra central: letra desperdiçada tranca quadradinho -----------

    @Test
    fun `virar a rodada com jogada sobrando tranca um quadradinho`() {
        val game = newGame()
        val after = GameRules.endRound(game, Random(7))
        assertEquals(
            "deveria trancar exatamente o limite do modo",
            game.config.maxLocksPerRound,
            after.lockedCount
        )
    }

    @Test
    fun `usar todas as jogadas nao tranca nada`() {
        var game = newGame()
        repeat(game.config.movesPerRound) {
            val target = game.cells.values.firstOrNull { cell ->
                cell.isOpen && game.tray.any { it.letter == cell.solution }
            } ?: return@repeat
            val tile = game.tray.first { it.letter == target.solution }
            game = GameRules.placeTile(GameRules.selectCell(game, target.pos), tile.id, Random(1))
        }
        assertEquals(0, game.movesLeft)

        val after = GameRules.endRound(game, Random(7))
        assertEquals("jogou tudo, não pode perder casa", 0, after.lockedCount)
    }

    @Test
    fun `a casa trancada nao aceita mais letra e o reparo devolve ela`() {
        val burned = GameRules.endRound(newGame(), Random(7))
        val locked = burned.cells.values.first { it.status == CellStatus.LOCKED }

        val withPowerUp = GameRules.grantPowerUp(burned, PowerUp.REPARO)
        val repaired = GameRules.repairCell(withPowerUp, locked.pos)

        assertEquals(CellStatus.EMPTY, repaired.cells.getValue(locked.pos).status)
        assertEquals(0, repaired.powerUps[PowerUp.REPARO])
    }

    @Test
    fun `no modo Sereno nada tranca`() {
        val puzzle = PuzzleGenerator.generate(WordBank.forTheme(WordTheme.GERAL, 9), 9, 3L, "Geral")
        val config = GameConfig.forMode(GameMode.ZEN, Difficulty.MEDIO, 3L, puzzle.cellCount)
        val game = GameRules.newGame(puzzle, config, emptyMap(), Random(3))

        var state = game
        repeat(5) { state = GameRules.endRound(state, Random(it.toLong())) }
        assertEquals(0, state.lockedCount)
    }

    // -- Power-ups -------------------------------------------------------

    @Test
    fun `letra congelada sobrevive a virada de rodada`() {
        val game = newGame(powerUps = mapOf(PowerUp.CONGELAR to 1))
        val tile = game.tray.first()
        val frozen = GameRules.freezeTile(game, tile.id)
        assertTrue(frozen.tray.first { it.id == tile.id }.frozen)

        val next = GameRules.endRound(frozen, Random(9))
        assertTrue(
            "a letra congelada deveria continuar na bandeja",
            next.tray.any { it.letter == tile.letter }
        )
    }

    @Test
    fun `trocar a bandeja nao tranca nenhuma casa`() {
        val game = newGame(powerUps = mapOf(PowerUp.TROCAR to 1))
        val swapped = GameRules.swapTray(game, Random(4))
        assertEquals(0, swapped.lockedCount)
        assertEquals(game.config.traySize, swapped.tray.size)
        assertEquals(game.movesLeft, swapped.movesLeft)
    }

    @Test
    fun `revelar preenche a casa selecionada sem gastar jogada`() {
        val game = newGame(powerUps = mapOf(PowerUp.REVELAR to 1))
        val target = game.cells.values.first { it.isOpen }
        val selected = GameRules.selectCell(game, target.pos)

        val revealed = GameRules.revealCell(selected, Random(2))
        assertEquals(CellStatus.CORRECT, revealed.cells.getValue(target.pos).status)
        assertTrue(revealed.cells.getValue(target.pos).revealed)
        assertEquals(selected.movesLeft, revealed.movesLeft)
    }

    // -- Fim de partida --------------------------------------------------

    /** Bot que sempre acha uma jogada válida — mede se o modo é vencível. */
    private fun playPerfectly(mode: GameMode, difficulty: Difficulty, seed: Long): MatchSummary {
        var state = newGame(mode, difficulty, seed)
        val random = Random(seed)
        var guard = 0
        while (!state.isOver && guard++ < 5000) {
            val move = state.cells.values
                .filter { it.isOpen }
                .firstNotNullOfOrNull { cell ->
                    state.tray.firstOrNull { it.letter == cell.solution }?.let { cell.pos to it.id }
                }
            state = if (move != null && state.movesLeft > 0) {
                GameRules.placeTile(GameRules.selectCell(state, move.first), move.second, random)
            } else {
                GameRules.endRound(state, random)
            }
        }
        assertTrue("partida não terminou em $mode/$difficulty (seed=$seed)", state.isOver)
        return state.summary ?: GameRules.buildSummary(state, false)
    }

    @Test
    fun `jogando bem da para vencer todos os modos com 3 estrelas`() {
        GameMode.entries.forEach { mode ->
            repeat(8) { index ->
                val summary = playPerfectly(mode, Difficulty.MEDIO, index * 17L + 1)
                assertTrue("${mode.title} deveria ser vencível", summary.won)
                assertEquals("${mode.title} sem 3 estrelas", 3, summary.stars)
                assertEquals(0, summary.lockedCells)
                assertTrue(summary.coinsEarned > 0)
                assertTrue(summary.xpEarned > 0)
            }
        }
    }

    @Test
    fun `desperdicar jogada custa estrelas`() {
        var stars = 0
        val runs = 25
        repeat(runs) { index ->
            var state = newGame(seed = index * 13L + 1)
            val random = Random(index.toLong())
            var guard = 0
            while (!state.isOver && guard++ < 5000) {
                // Joga só metade das jogadas de cada rodada, de propósito.
                if (state.movesLeft <= state.config.movesPerRound / 2) {
                    state = GameRules.endRound(state, random)
                    continue
                }
                val move = state.cells.values
                    .filter { it.isOpen }
                    .firstNotNullOfOrNull { cell ->
                        state.tray.firstOrNull { it.letter == cell.solution }
                            ?.let { cell.pos to it.id }
                    }
                state = if (move != null) {
                    GameRules.placeTile(GameRules.selectCell(state, move.first), move.second, random)
                } else {
                    GameRules.endRound(state, random)
                }
            }
            stars += (state.summary ?: GameRules.buildSummary(state, false)).stars
        }
        val average = stars.toFloat() / runs
        assertTrue("jogar mal deveria render menos de 2 estrelas, deu $average", average < 2f)
    }

    @Test
    fun `vitoria perfeita rende as 3 estrelas e o bonus`() {
        val summary = playPerfectly(GameMode.CLASSICO, Difficulty.FACIL, 11L)
        assertTrue(summary.perfect)
        assertEquals(summary.totalWords, summary.wordsSolved)
        assertNotNull(summary.mode)
    }

    @Test
    fun `reviver devolve a partida e destrava as casas perdidas`() {
        var state = newGame(mode = GameMode.DILUVIO)
        val random = Random(3)
        var guard = 0
        // Só passa rodada: perde as vidas rapidinho.
        while (!state.isOver && guard++ < 200) state = GameRules.endRound(state, random)

        assertTrue(state.isOver)
        val revived = GameRules.revive(state, random)
        assertEquals(0, revived.lockedCount)
        assertTrue(revived.lives > 0)
        assertTrue(!revived.isOver)
    }
}
