package com.chuvadeletras.game.domain.model

/** Situação visual/lógica de um quadradinho da grade. */
enum class CellStatus {
    /** Vazio e ainda preenchível. */
    EMPTY,

    /** Preenchido com a letra certa. */
    CORRECT,

    /** Preenchido com uma letra errada (dá para limpar de graça). */
    WRONG,

    /** Trancado: a letra que preenchia essa casa foi desperdiçada e evaporou. */
    LOCKED
}

data class CellState(
    val pos: GridPos,
    val solution: Char,
    val input: Char? = null,
    val status: CellStatus = CellStatus.EMPTY,
    val revealed: Boolean = false
) {
    val isFilledCorrectly: Boolean get() = status == CellStatus.CORRECT
    val isOpen: Boolean get() = status == CellStatus.EMPTY || status == CellStatus.WRONG
}

/** Uma peça de letra na bandeja (parte de baixo da tela). */
data class TrayTile(
    val id: Long,
    val letter: Char,
    val frozen: Boolean = false,
    /** Marcada como "útil": ainda existe alguma casa aberta esperando essa letra. */
    val useful: Boolean = true
)

enum class GameStatus { PLAYING, WON, LOST }

/** Os modos de jogo. Cada um muda radicalmente o ritmo da partida. */
enum class GameMode(
    val title: String,
    val emoji: String,
    val tagline: String,
    val description: String
) {
    CLASSICO(
        title = "Chuva Calma",
        emoji = "🌦️",
        tagline = "O modo de sempre, no seu tempo",
        description = "Sem relógio. Você tem um número fixo de rodadas e de jogadas " +
            "por rodada. Pense bem: as letras que sobrarem na bandeja evaporam."
    ),
    TEMPESTADE(
        title = "Tempestade",
        emoji = "⛈️",
        tagline = "Decida rápido ou perca a letra",
        description = "Cada rodada tem tempo contado. Quando o relógio zera, a bandeja " +
            "é trocada na marra e o que você não usou vira poça."
    ),
    DILUVIO(
        title = "Dilúvio",
        emoji = "🌊",
        tagline = "Sobreviva o quanto aguentar",
        description = "Cruzadinhas em sequência, cada uma maior que a anterior. " +
            "Você tem 3 vidas: cada quadradinho trancado custa uma."
    ),
    DIARIO(
        title = "Desafio Diário",
        emoji = "📅",
        tagline = "A mesma grade para o mundo inteiro",
        description = "Uma grade nova por dia, igual para todos os jogadores. " +
            "Complete para manter sua ofensiva viva."
    ),
    ZEN(
        title = "Sereno",
        emoji = "🍃",
        tagline = "Sem pressa, sem perder nada",
        description = "Nada evapora, nada tranca, ninguém te apressa. " +
            "Perfeito para relaxar ou treinar o mecanismo."
    );

    val requiresDailySeed: Boolean get() = this == DIARIO
}

/** Dificuldade da grade gerada (tamanho e quantidade de palavras). */
enum class Difficulty(val label: String, val targetWords: Int, val maxWordLength: Int) {
    FACIL("Fácil", 6, 7),
    MEDIO("Médio", 9, 9),
    DIFICIL("Difícil", 12, 11),
    INSANO("Insano", 15, 12);

    companion object {
        fun forLevel(level: Int): Difficulty = when {
            level <= 3 -> FACIL
            level <= 8 -> MEDIO
            level <= 15 -> DIFICIL
            else -> INSANO
        }
    }
}

/** Parâmetros que definem o ritmo de uma partida. */
data class GameConfig(
    val mode: GameMode,
    val difficulty: Difficulty,
    /** Quantas letras aparecem na bandeja por rodada. */
    val traySize: Int,
    /** Quantas letras dá para encaixar antes da bandeja virar. */
    val movesPerRound: Int,
    /** Limite de rodadas; null = infinitas. */
    val maxRounds: Int?,
    /** Segundos por rodada; null = sem relógio. */
    val secondsPerRound: Int?,
    /** Quantos quadradinhos, no máximo, podem trancar por rodada. */
    val maxLocksPerRound: Int,
    /** Vidas (só no Dilúvio); null = sem sistema de vidas. */
    val lives: Int?,
    /** Se true, o jogo recusa letras que não encaixam em vez de gastar a jogada. */
    val assistPlacement: Boolean,
    val seed: Long
) {
    companion object {
        /**
         * @param cellCount número real de quadradinhos da grade sorteada. O
         * orçamento de rodadas sai daqui: estimar por dificuldade dava grades
         * impossíveis de terminar.
         */
        fun forMode(
            mode: GameMode,
            difficulty: Difficulty,
            seed: Long,
            cellCount: Int,
            assist: Boolean = false
        ): GameConfig = when (mode) {
            GameMode.CLASSICO -> GameConfig(
                mode = mode,
                difficulty = difficulty,
                traySize = 7,
                movesPerRound = 4,
                maxRounds = roundBudget(cellCount, movesPerRound = 4, slack = 10),
                secondsPerRound = null,
                maxLocksPerRound = 1,
                lives = null,
                assistPlacement = assist,
                seed = seed
            )

            GameMode.TEMPESTADE -> GameConfig(
                mode = mode,
                difficulty = difficulty,
                traySize = 8,
                movesPerRound = 4,
                maxRounds = roundBudget(cellCount, movesPerRound = 4, slack = 8),
                secondsPerRound = 28,
                maxLocksPerRound = 2,
                lives = null,
                assistPlacement = false,
                seed = seed
            )

            GameMode.DILUVIO -> GameConfig(
                mode = mode,
                difficulty = difficulty,
                traySize = 7,
                movesPerRound = 4,
                maxRounds = null,
                secondsPerRound = null,
                maxLocksPerRound = 1,
                lives = 3,
                assistPlacement = false,
                seed = seed
            )

            GameMode.DIARIO -> GameConfig(
                mode = mode,
                difficulty = difficulty,
                traySize = 7,
                movesPerRound = 4,
                maxRounds = roundBudget(cellCount, movesPerRound = 4, slack = 6),
                secondsPerRound = null,
                maxLocksPerRound = 1,
                lives = null,
                assistPlacement = false,
                seed = seed
            )

            GameMode.ZEN -> GameConfig(
                mode = mode,
                difficulty = difficulty,
                traySize = 8,
                movesPerRound = 5,
                maxRounds = null,
                secondsPerRound = null,
                maxLocksPerRound = 0,
                lives = null,
                assistPlacement = true,
                seed = seed
            )
        }

        /**
         * Orçamento de rodadas: o necessário para preencher a grade inteira
         * jogando todas as jogadas, mais uma folga para errar.
         */
        private fun roundBudget(cellCount: Int, movesPerRound: Int, slack: Int): Int =
            ((cellCount + movesPerRound - 1) / movesPerRound) + slack
    }
}

/** Itens que o jogador compra com gotas ou ganha assistindo anúncio. */
enum class PowerUp(
    val label: String,
    val emoji: String,
    val price: Int,
    val description: String
) {
    CONGELAR(
        "Congelar",
        "❄️",
        40,
        "Segura uma letra da bandeja para a próxima rodada. Ela não evapora."
    ),
    TROCAR(
        "Trocar",
        "🔄",
        60,
        "Descarta a bandeja inteira e sorteia letras novas, sem trancar nada."
    ),
    REVELAR(
        "Revelar",
        "💡",
        80,
        "Preenche o quadradinho selecionado com a letra certa, de graça."
    ),
    REPARO(
        "Reparo",
        "🔧",
        120,
        "Destranca um quadradinho perdido e devolve a chance de preenchê-lo."
    ),
    TEMPO(
        "Tempo",
        "⏱️",
        50,
        "Adiciona 20 segundos ao relógio da rodada atual (só na Tempestade)."
    )
}

/**
 * Evento de uma jogada. A UI consome para disparar animação, som e vibração,
 * e depois limpa.
 */
sealed interface GameEvent {
    data class Placed(val pos: GridPos, val correct: Boolean) : GameEvent
    data class Rejected(val letter: Char) : GameEvent
    data class WordSolved(val entryId: Int, val word: String, val points: Int) : GameEvent
    data class RoundBurned(val wasted: List<Char>, val locked: List<GridPos>) : GameEvent
    data class CellRevealed(val pos: GridPos) : GameEvent
    data class CellRepaired(val pos: GridPos) : GameEvent
    data class Finished(val won: Boolean, val summary: MatchSummary) : GameEvent
}

/** Resultado final de uma partida — alimenta o diálogo de comemoração e a economia. */
data class MatchSummary(
    val mode: GameMode,
    val difficulty: Difficulty,
    val won: Boolean,
    val score: Int,
    val stars: Int,
    val wordsSolved: Int,
    val totalWords: Int,
    val lockedCells: Int,
    val roundsUsed: Int,
    val bestCombo: Int,
    val coinsEarned: Int,
    val xpEarned: Int,
    val perfect: Boolean
)

/** Estado completo de uma partida. */
data class GameState(
    val puzzle: Puzzle,
    val config: GameConfig,
    val cells: Map<GridPos, CellState>,
    val tray: List<TrayTile>,
    val round: Int = 1,
    val movesLeft: Int = 0,
    val selected: GridPos? = null,
    val activeEntryId: Int? = null,
    val score: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val solvedEntries: Set<Int> = emptySet(),
    val lives: Int = 0,
    val secondsLeft: Int = 0,
    val status: GameStatus = GameStatus.PLAYING,
    val powerUps: Map<PowerUp, Int> = emptyMap(),
    val wastedLetters: Int = 0,
    val placedLetters: Int = 0,
    val nextTileId: Long = 0L,
    val stageIndex: Int = 1,
    val event: GameEvent? = null,
    val summary: MatchSummary? = null
) {
    val lockedCells: List<GridPos> get() = cells.values.filter { it.status == CellStatus.LOCKED }.map { it.pos }
    val lockedCount: Int get() = cells.values.count { it.status == CellStatus.LOCKED }
    val filledCount: Int get() = cells.values.count { it.isFilledCorrectly }
    val openCells: List<CellState> get() = cells.values.filter { it.isOpen }

    val roundsLeft: Int? get() = config.maxRounds?.let { (it - round + 1).coerceAtLeast(0) }

    val progress: Float
        get() = if (puzzle.cellCount == 0) 0f else filledCount.toFloat() / puzzle.cellCount

    val activeEntry: PuzzleEntry? get() = activeEntryId?.let { puzzle.entryById(it) }

    val isOver: Boolean get() = status != GameStatus.PLAYING
}
