package com.kardiapulse.game.core.model

/** Quem está com o turno. */
enum class Side { VOCE, RIVAL;
    val other: Side get() = if (this == VOCE) RIVAL else VOCE
}

/** Em que ponto do duelo estamos. */
enum class Phase { JOGANDO, FIM_DE_RODADA, FIM_DE_DUELO }

data class GameConfig(
    val mode: GameMode = GameMode.DUELO,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val modifiers: Set<Modifier> = emptySet(),
    val baseLimit: Int = 20,
    val baseHandSize: Int = 5,
    val startHp: Int = 50,
    val turnSeconds: Int = 0
) {
    /** O limite do Núcleo no começo da rodada, depois dos modificadores. */
    val limit: Int get() = if (Modifier.ESPELHO in modifiers) 13 else baseLimit

    /** O piso do Colapso: por mais que o campo aperte, ele nunca fica menor que isso. */
    val limitFloor: Int get() = 2

    /** Cartas de folga antes de o Colapso começar. Dá espaço para a abertura respirar. */
    val collapseGrace: Int get() = 4

    /** O tamanho de mão efetivo depois dos modificadores. */
    val handSize: Int get() = if (Modifier.FRENESI in modifiers) 3 else baseHandSize

    val hasGravity: Boolean get() = Modifier.GRAVIDADE in modifiers
    val hasStorm: Boolean get() = Modifier.TEMPESTADE in modifiers
    val hasFog: Boolean get() = Modifier.NEVOA in modifiers
}

data class PlayerState(
    val name: String,
    val hp: Int,
    val hand: List<Card>,
    val shielded: Boolean = false,
    val powers: Map<PowerType, Int> = emptyMap(),
    val roundsWon: Int = 0
) {
    fun powerCount(type: PowerType): Int = powers[type] ?: 0

    fun withPowerConsumed(type: PowerType): PlayerState {
        val left = powerCount(type) - 1
        val next = powers.toMutableMap()
        if (left <= 0) next.remove(type) else next[type] = left
        return copy(powers = next)
    }
}

/** Uma jogada possível. */
sealed interface Move {
    data class Play(val cardId: Int, val sign: Int) : Move
    data class UsePower(val power: PowerType) : Move
    data object Concede : Move
}

/** Eventos que a UI transforma em animação, som e vibração. */
sealed interface GameEvent {
    data class CardPlayed(
        val by: Side,
        val card: Card,
        val sign: Int,
        val nucleusBefore: Int,
        val nucleusAfter: Int,
        val flipped: Boolean,
        val chain: Int
    ) : GameEvent

    data class Gravity(val by: Side, val nucleusAfter: Int) : GameEvent

    data class PowerUsed(val by: Side, val power: PowerType) : GameEvent

    data class Overload(
        val loser: Side,
        val nucleus: Int,
        val damage: Int,
        val blockedByShield: Boolean
    ) : GameEvent

    data class MatchOver(val winner: Side) : GameEvent
}

data class LogEntry(val text: String, val side: Side?)

/**
 * Estado completo e imutável de um duelo. Tudo que o motor precisa está aqui, inclusive o
 * estado do gerador aleatório — o que torna qualquer partida perfeitamente reproduzível a
 * partir de uma semente (essencial para o Desafio Diário).
 */
data class GameState(
    val config: GameConfig,
    val nucleus: Int = 0,
    /** 0 = livre (a próxima carta define a direção), senão +1 ou -1. */
    val direction: Int = 0,
    val lastElement: Element? = null,
    val chain: Int = 0,
    val maxChainThisRound: Int = 0,
    val you: PlayerState,
    val foe: PlayerState,
    val turn: Side = Side.VOCE,
    val round: Int = 1,
    val deck: List<Card> = emptyList(),
    val discard: List<Card> = emptyList(),
    val phase: Phase = Phase.JOGANDO,
    val rngState: Long = 1L,
    val turnCounter: Int = 0,
    val cardsThisRound: Int = 0,
    val revealFoeUntilTurn: Int = -1,
    val extraTurnFor: Side? = null,
    val directionUnlockedFor: Side? = null,
    val lastRoundLoser: Side? = null,
    val events: List<GameEvent> = emptyList(),
    val log: List<LogEntry> = emptyList(),
    val winner: Side? = null,
    val statsCardsPlayed: Int = 0,
    val statsFlips: Int = 0,
    val statsBestChain: Int = 0
) {
    fun player(side: Side): PlayerState = if (side == Side.VOCE) you else foe

    fun withPlayer(side: Side, p: PlayerState): GameState =
        if (side == Side.VOCE) copy(you = p) else copy(foe = p)

    /**
     * O COLAPSO: o limite do Núcleo encolhe conforme a rodada se arrasta.
     *
     * Sem isso, dois jogadores poderiam ficar invertendo a direção em volta do zero para
     * sempre. Com isso, o campo aperta a cada duas cartas e alguém sempre acaba sem saída —
     * quanto mais longa a rodada, mais afiada ela fica.
     */
    val limitNow: Int
        get() = limitAfter(cardsThisRound)

    /**
     * O limite que valerá assim que a próxima carta cair. A legalidade usa ESTE valor, e não
     * [limitNow], para que o Colapso nunca deixe o Núcleo fora dos limites de surpresa — quem
     * joga sempre enxerga o campo em que vai aterrissar.
     */
    val limitAfterNextCard: Int
        get() = limitAfter(cardsThisRound + 1)

    private fun limitAfter(cards: Int): Int =
        maxOf(config.limitFloor, config.limit - maxOf(0, cards - config.collapseGrace))

    /** Folga restante antes da sobrecarga. */
    val headroom: Int get() = limitNow - kotlin.math.abs(nucleus)

    /** Quanto o Colapso já comeu do campo nesta rodada. */
    val collapseAmount: Int get() = config.limit - limitNow

    val foeHandVisible: Boolean get() = turnCounter <= revealFoeUntilTurn
}
