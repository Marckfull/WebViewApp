package com.chuvadeletras.game.domain.engine

import com.chuvadeletras.game.domain.model.CellState
import com.chuvadeletras.game.domain.model.CellStatus
import com.chuvadeletras.game.domain.model.Direction
import com.chuvadeletras.game.domain.model.GameConfig
import com.chuvadeletras.game.domain.model.GameEvent
import com.chuvadeletras.game.domain.model.GameMode
import com.chuvadeletras.game.domain.model.GameState
import com.chuvadeletras.game.domain.model.GameStatus
import com.chuvadeletras.game.domain.model.GridPos
import com.chuvadeletras.game.domain.model.MatchSummary
import com.chuvadeletras.game.domain.model.PowerUp
import com.chuvadeletras.game.domain.model.Puzzle
import com.chuvadeletras.game.domain.model.PuzzleEntry
import com.chuvadeletras.game.domain.model.TrayTile
import kotlin.random.Random

/**
 * Todas as regras da partida, como funções puras sobre [GameState].
 *
 * A mecânica central: a bandeja despeja um punhado de letras, você tem um número
 * limitado de jogadas na rodada e **o que sobra evapora**. Cada letra útil
 * desperdiçada tranca um quadradinho que precisava dela — aquele espaço fica
 * perdido até você gastar um Reparo.
 */
object GameRules {

    private const val POINTS_PER_LETTER = 10
    private const val COMBO_BONUS = 3
    private const val WORD_BASE_POINTS = 40
    private const val WORD_POINTS_PER_LETTER = 15
    private const val PERFECT_BONUS = 500
    private const val ROUND_LEFTOVER_BONUS = 60

    /** Distribuição aproximada das letras em português, para as letras "coringa". */
    private val FILLER_POOL: List<Char> = buildString {
        append("A".repeat(14)); append("E".repeat(13)); append("O".repeat(11))
        append("S".repeat(8)); append("R".repeat(7)); append("I".repeat(7))
        append("N".repeat(5)); append("D".repeat(5)); append("M".repeat(5))
        append("U".repeat(4)); append("T".repeat(4)); append("C".repeat(4))
        append("L".repeat(3)); append("P".repeat(3)); append("V".repeat(2))
        append("G".repeat(2)); append("H".repeat(2)); append("Q").append("B")
        append("F").append("Z").append("J").append("X").append("K")
    }.toList()

    // ------------------------------------------------------------------
    // Início de partida
    // ------------------------------------------------------------------

    fun newGame(
        puzzle: Puzzle,
        config: GameConfig,
        powerUps: Map<PowerUp, Int>,
        random: Random,
        stageIndex: Int = 1,
        carryOverScore: Int = 0,
        carryOverLives: Int? = null
    ): GameState {
        val cells = puzzle.solution.mapValues { (pos, solution) -> CellState(pos, solution) }
        val firstEntry = puzzle.entries.minByOrNull { it.number }

        val base = GameState(
            puzzle = puzzle,
            config = config,
            cells = cells,
            tray = emptyList(),
            round = 1,
            movesLeft = config.movesPerRound,
            selected = firstEntry?.start,
            activeEntryId = firstEntry?.id,
            score = carryOverScore,
            lives = carryOverLives ?: config.lives ?: 0,
            secondsLeft = config.secondsPerRound ?: 0,
            powerUps = powerUps,
            stageIndex = stageIndex
        )
        return base.withFreshTray(random, keepFrozen = false)
    }

    // ------------------------------------------------------------------
    // Seleção e navegação
    // ------------------------------------------------------------------

    fun selectCell(state: GameState, pos: GridPos): GameState {
        if (state.isOver) return state
        if (!state.cells.containsKey(pos)) return state

        val entriesHere = state.puzzle.entriesByCell[pos].orEmpty()
        // Tocar de novo na mesma casa alterna entre a palavra horizontal e a vertical.
        val nextEntry = when {
            entriesHere.isEmpty() -> null
            state.selected == pos && entriesHere.size > 1 -> {
                val current = entriesHere.indexOf(state.activeEntryId)
                entriesHere[(current + 1) % entriesHere.size]
            }

            state.activeEntryId in entriesHere -> state.activeEntryId
            else -> entriesHere.first()
        }
        return state.copy(selected = pos, activeEntryId = nextEntry)
    }

    fun selectEntry(state: GameState, entryId: Int): GameState {
        val entry = state.puzzle.entryById(entryId) ?: return state
        val target = entry.cells.firstOrNull { state.cells[it]?.isOpen == true } ?: entry.start
        return state.copy(selected = target, activeEntryId = entryId)
    }

    /** Anda para a próxima casa aberta da palavra ativa, dando a volta se precisar. */
    private fun advanceCursor(state: GameState): GameState {
        val entry = state.activeEntry ?: return state
        val from = state.selected
        val ordered = entry.cells
        val startIndex = ordered.indexOf(from).let { if (it < 0) 0 else it }
        for (offset in 1..ordered.size) {
            val candidate = ordered[(startIndex + offset) % ordered.size]
            if (state.cells[candidate]?.isOpen == true) return state.copy(selected = candidate)
        }
        // Palavra terminada: pula para a primeira palavra ainda incompleta.
        val nextEntry = state.puzzle.entries
            .firstOrNull { e -> e.id !in state.solvedEntries && e.cells.any { state.cells[it]?.isOpen == true } }
            ?: return state
        val nextCell = nextEntry.cells.firstOrNull { state.cells[it]?.isOpen == true } ?: nextEntry.start
        return state.copy(selected = nextCell, activeEntryId = nextEntry.id)
    }

    // ------------------------------------------------------------------
    // Jogada principal
    // ------------------------------------------------------------------

    /** Encaixa a letra da bandeja na casa selecionada. É aqui que a jogada é gasta. */
    fun placeTile(state: GameState, tileId: Long, random: Random): GameState {
        if (state.isOver || state.movesLeft <= 0) return state
        val tile = state.tray.firstOrNull { it.id == tileId } ?: return state
        val pos = state.selected ?: return state
        val cell = state.cells[pos] ?: return state
        if (!cell.isOpen) return state

        val correct = cell.solution == tile.letter

        // No modo assistido a letra errada é simplesmente recusada, sem custo.
        if (!correct && state.config.assistPlacement) {
            return state.copy(event = GameEvent.Rejected(tile.letter))
        }

        val updatedCell = cell.copy(
            input = tile.letter,
            status = if (correct) CellStatus.CORRECT else CellStatus.WRONG
        )
        val newCells = state.cells + (pos to updatedCell)
        val newCombo = if (correct) state.combo + 1 else 0
        val gained = if (correct) POINTS_PER_LETTER + newCombo * COMBO_BONUS else 0

        var next = state.copy(
            cells = newCells,
            tray = state.tray.filterNot { it.id == tileId },
            movesLeft = state.movesLeft - 1,
            combo = newCombo,
            bestCombo = maxOf(state.bestCombo, newCombo),
            score = state.score + gained,
            placedLetters = state.placedLetters + if (correct) 1 else 0,
            event = GameEvent.Placed(pos, correct)
        )

        if (correct) {
            next = registerSolvedWords(next, pos)
            next = advanceCursor(next)
        }
        return checkEnd(next, random)
    }

    /** Tira uma letra errada da grade. Sai de graça — mas a letra já se foi. */
    fun clearCell(state: GameState, pos: GridPos): GameState {
        val cell = state.cells[pos] ?: return state
        if (cell.status != CellStatus.WRONG) return state
        return state.copy(
            cells = state.cells + (pos to cell.copy(input = null, status = CellStatus.EMPTY))
        )
    }

    /** Detecta palavras concluídas por causa da última letra e pontua. */
    private fun registerSolvedWords(state: GameState, pos: GridPos): GameState {
        val candidates = state.puzzle.entriesByCell[pos].orEmpty()
            .mapNotNull { state.puzzle.entryById(it) }
            .filter { it.id !in state.solvedEntries }
            .filter { entry -> entry.cells.all { state.cells[it]?.isFilledCorrectly == true } }

        if (candidates.isEmpty()) return state

        var score = state.score
        var event: GameEvent = state.event ?: GameEvent.Placed(pos, true)
        candidates.forEach { entry ->
            val points = WORD_BASE_POINTS + entry.length * WORD_POINTS_PER_LETTER +
                state.combo * COMBO_BONUS * 2
            score += points
            event = GameEvent.WordSolved(entry.id, entry.answer, points)
        }
        return state.copy(
            score = score,
            solvedEntries = state.solvedEntries + candidates.map { it.id },
            event = event
        )
    }

    // ------------------------------------------------------------------
    // Virada de rodada — o coração do jogo
    // ------------------------------------------------------------------

    /**
     * Encerra a rodada: o que sobrou na bandeja evapora. Cada letra útil
     * desperdiçada tranca um quadradinho que dependia dela, até o limite do modo.
     */
    fun endRound(state: GameState, random: Random): GameState {
        if (state.isOver) return state

        val leftovers = state.tray.filterNot { it.frozen }
        val locked = mutableListOf<GridPos>()
        val wasted = mutableListOf<Char>()
        var cells = state.cells

        // A bandeja sempre entrega mais letras do que jogadas: sobrar letra é
        // normal e não custa nada. O que tranca um quadradinho é sobrar *jogada* —
        // aí sim você abriu mão de uma letra que dava para usar.
        val locksAllowed = minOf(state.config.maxLocksPerRound, state.movesLeft)

        if (locksAllowed > 0) {
            for (tile in leftovers) {
                if (locked.size >= locksAllowed) break
                val victim = pickCellToLock(state.puzzle, cells, tile.letter, locked) ?: continue
                cells = cells + (victim to cells.getValue(victim).copy(
                    input = null,
                    status = CellStatus.LOCKED
                ))
                locked += victim
                wasted += tile.letter
            }
        }

        val livesLeft = if (state.config.lives != null) {
            (state.lives - locked.size).coerceAtLeast(0)
        } else {
            state.lives
        }

        val advanced = state.copy(
            cells = cells,
            lives = livesLeft,
            round = state.round + 1,
            movesLeft = state.config.movesPerRound,
            combo = 0,
            wastedLetters = state.wastedLetters + leftovers.size,
            secondsLeft = state.config.secondsPerRound ?: 0,
            event = GameEvent.RoundBurned(wasted, locked)
        ).withFreshTray(random, keepFrozen = true)

        return checkEnd(advanced, random)
    }

    /**
     * Escolhe qual casa perder. Prefere poupar a palavra que está mais perto de
     * ser concluída: a punição cai sobre a palavra menos adiantada.
     */
    private fun pickCellToLock(
        puzzle: Puzzle,
        cells: Map<GridPos, CellState>,
        letter: Char,
        alreadyLocked: List<GridPos>
    ): GridPos? {
        val candidates = cells.values.filter {
            it.status == CellStatus.EMPTY && it.solution == letter && it.pos !in alreadyLocked
        }
        if (candidates.isEmpty()) return null

        fun progressAround(pos: GridPos): Int = puzzle.entriesByCell[pos].orEmpty()
            .mapNotNull { puzzle.entryById(it) }
            .maxOfOrNull { entry -> entry.cells.count { cells[it]?.isFilledCorrectly == true } }
            ?: 0

        return candidates
            .sortedWith(compareBy({ progressAround(it.pos) }, { it.pos.row }, { it.pos.col }))
            .first()
            .pos
    }

    /** Sorteia a bandeja da rodada, garantindo letras jogáveis. */
    private fun GameState.withFreshTray(random: Random, keepFrozen: Boolean): GameState {
        val kept = if (keepFrozen) tray.filter { it.frozen }.map { it.copy(frozen = false) } else emptyList()
        val slots = (config.traySize - kept.size).coerceAtLeast(0)

        val open = cells.values.filter { it.isOpen }
        val needed = open.groupingBy { it.solution }.eachCount().toMutableMap()
        kept.forEach { tile -> needed[tile.letter]?.let { count -> needed[tile.letter] = count - 1 } }

        // Pelo menos uma bandeja inteira de jogadas precisa ser aproveitável,
        // senão a rodada vira injustiça em vez de decisão.
        val usefulTarget = minOf(config.movesPerRound + 1, slots, open.size)
        val drawn = mutableListOf<Char>()

        repeat(usefulTarget) {
            val pool = needed.filterValues { it > 0 }
            if (pool.isEmpty()) return@repeat
            val total = pool.values.sum()
            var pick = random.nextInt(total)
            var chosen = pool.keys.first()
            for ((letter, count) in pool) {
                pick -= count
                if (pick < 0) {
                    chosen = letter
                    break
                }
            }
            drawn += chosen
            needed[chosen] = (needed[chosen] ?: 1) - 1
        }

        while (drawn.size < slots) {
            drawn += FILLER_POOL[random.nextInt(FILLER_POOL.size)]
        }

        var id = nextTileId
        val usefulLetters = open.map { it.solution }.toSet()
        val fresh = drawn.shuffled(random).map { letter ->
            TrayTile(id = id++, letter = letter, useful = letter in usefulLetters)
        }

        return copy(tray = kept + fresh, nextTileId = id)
    }

    // ------------------------------------------------------------------
    // Relógio (modo Tempestade)
    // ------------------------------------------------------------------

    fun tick(state: GameState): GameState {
        if (state.isOver || state.config.secondsPerRound == null) return state
        return state.copy(secondsLeft = (state.secondsLeft - 1).coerceAtLeast(0))
    }

    // ------------------------------------------------------------------
    // Power-ups
    // ------------------------------------------------------------------

    fun freezeTile(state: GameState, tileId: Long): GameState {
        if (!state.has(PowerUp.CONGELAR)) return state
        val tile = state.tray.firstOrNull { it.id == tileId } ?: return state
        if (tile.frozen) return state
        return state.spend(PowerUp.CONGELAR).copy(
            tray = state.tray.map { if (it.id == tileId) it.copy(frozen = true) else it }
        )
    }

    fun swapTray(state: GameState, random: Random): GameState {
        if (!state.has(PowerUp.TROCAR)) return state
        // Trocar não queima nada: descarta a bandeja e sorteia outra, sem trancar casas.
        return state.spend(PowerUp.TROCAR).copy(tray = state.tray.filter { it.frozen })
            .withFreshTray(random, keepFrozen = true)
    }

    fun revealCell(state: GameState, random: Random): GameState {
        if (!state.has(PowerUp.REVELAR)) return state
        val pos = state.selected ?: return state
        val cell = state.cells[pos] ?: return state
        if (!cell.isOpen) return state

        var next = state.spend(PowerUp.REVELAR).copy(
            cells = state.cells + (pos to cell.copy(
                input = cell.solution,
                status = CellStatus.CORRECT,
                revealed = true
            )),
            event = GameEvent.CellRevealed(pos)
        )
        next = registerSolvedWords(next, pos)
        next = advanceCursor(next)
        return checkEnd(next, random)
    }

    fun repairCell(state: GameState, pos: GridPos): GameState {
        if (!state.has(PowerUp.REPARO)) return state
        val cell = state.cells[pos] ?: return state
        if (cell.status != CellStatus.LOCKED) return state
        return state.spend(PowerUp.REPARO).copy(
            cells = state.cells + (pos to cell.copy(status = CellStatus.EMPTY, input = null)),
            event = GameEvent.CellRepaired(pos)
        )
    }

    fun addTime(state: GameState): GameState {
        if (!state.has(PowerUp.TEMPO) || state.config.secondsPerRound == null) return state
        return state.spend(PowerUp.TEMPO).copy(secondsLeft = state.secondsLeft + 20)
    }

    fun grantPowerUp(state: GameState, powerUp: PowerUp, amount: Int = 1): GameState =
        state.copy(powerUps = state.powerUps + (powerUp to (state.powerUps[powerUp] ?: 0) + amount))

    /** Recompensa de anúncio: jogadas extras nesta rodada. */
    fun grantExtraMoves(state: GameState, moves: Int): GameState =
        if (state.isOver) state else state.copy(movesLeft = state.movesLeft + moves)

    /** Recompensa de anúncio: continua depois de perder. */
    fun revive(state: GameState, random: Random): GameState {
        if (state.status != GameStatus.LOST) return state
        val repaired = state.cells.mapValues { (_, cell) ->
            if (cell.status == CellStatus.LOCKED) cell.copy(status = CellStatus.EMPTY, input = null) else cell
        }
        val extraRounds = 3
        val config = state.config.copy(maxRounds = state.config.maxRounds?.plus(extraRounds))
        return state.copy(
            cells = repaired,
            config = config,
            lives = if (state.config.lives != null) 1 else state.lives,
            status = GameStatus.PLAYING,
            summary = null,
            movesLeft = config.movesPerRound,
            secondsLeft = config.secondsPerRound ?: 0,
            event = null
        ).withFreshTray(random, keepFrozen = true)
    }

    private fun GameState.has(powerUp: PowerUp): Boolean = (powerUps[powerUp] ?: 0) > 0

    private fun GameState.spend(powerUp: PowerUp): GameState =
        copy(powerUps = powerUps + (powerUp to ((powerUps[powerUp] ?: 1) - 1).coerceAtLeast(0)))

    fun consumeEvent(state: GameState): GameState = state.copy(event = null)

    // ------------------------------------------------------------------
    // Fim de partida
    // ------------------------------------------------------------------

    private fun checkEnd(state: GameState, random: Random): GameState {
        if (state.isOver) return state

        val everythingResolved = state.cells.values.none { it.isOpen }
        if (everythingResolved) {
            val won = state.lockedCount == 0 || state.config.mode == GameMode.ZEN ||
                state.solvedEntries.size >= (state.puzzle.entries.size + 1) / 2
            return finish(state, won)
        }

        val outOfLives = state.config.lives != null && state.lives <= 0
        val outOfRounds = state.config.maxRounds?.let { state.round > it } ?: false
        // Casas ainda abertas, mas sem letra possível: só resta perder.
        if (outOfLives || outOfRounds) return finish(state, won = false)

        return state
    }

    private fun finish(state: GameState, won: Boolean): GameState {
        val summary = buildSummary(state, won)
        return state.copy(
            status = if (won) GameStatus.WON else GameStatus.LOST,
            summary = summary,
            event = GameEvent.Finished(won, summary)
        )
    }

    fun buildSummary(state: GameState, won: Boolean): MatchSummary {
        val locked = state.lockedCount
        val perfect = won && locked == 0 && state.solvedEntries.size == state.puzzle.entries.size
        val stars = when {
            !won -> 0
            perfect -> 3
            locked <= 2 -> 2
            else -> 1
        }

        val leftoverRounds = state.roundsLeft ?: 0
        val bonus = if (won) leftoverRounds * ROUND_LEFTOVER_BONUS + if (perfect) PERFECT_BONUS else 0 else 0
        val finalScore = state.score + bonus

        val coins = if (won) {
            val base = 20 + state.config.difficulty.targetWords * 3
            base * maxOf(stars, 1) + finalScore / 40
        } else {
            5 + state.solvedEntries.size * 2
        }
        val xp = if (won) 30 + state.config.difficulty.targetWords * 4 + stars * 10 else 8

        return MatchSummary(
            mode = state.config.mode,
            difficulty = state.config.difficulty,
            won = won,
            score = finalScore,
            stars = stars,
            wordsSolved = state.solvedEntries.size,
            totalWords = state.puzzle.entries.size,
            lockedCells = locked,
            roundsUsed = state.round,
            bestCombo = state.bestCombo,
            coinsEarned = coins,
            xpEarned = xp,
            perfect = perfect
        )
    }

    /** Dicas visíveis da grade, já separadas por direção. */
    fun cluesFor(puzzle: Puzzle, direction: Direction): List<PuzzleEntry> =
        puzzle.entries.filter { it.direction == direction }.sortedBy { it.number }
}
