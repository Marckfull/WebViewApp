package com.neuroflip.game.domain

/**
 * Tipos de carta. O tipo é sempre aplicado ao PAR inteiro (as duas cartas),
 * o que garante que o tabuleiro sempre tenha solução.
 */
enum class CardKind(val glyph: String, val label: String, val description: String) {
    NORMAL("", "Comum", "Par comum."),
    CRYO("❄", "Crio", "Congela o cronômetro por 6s e devolve tempo ao ser resolvido."),
    GOLDEN("★", "Dourada", "Vale 3x pontos."),
    MIRROR("◈", "Espelho", "Ao resolver, marca permanentemente um par ainda escondido."),
    PHANTOM("⌭", "Fantasma", "Se você errar com ela, ela troca de lugar sozinha."),
    UNSTABLE("☢", "Instável", "Se não for resolvida em 3 jogadas depois de vista, sacode o tabuleiro.")
}

/** Face visível da carta (símbolo). */
enum class Symbol(val glyph: String) {
    AXON("🧬"), SPARK("⚡"), ORBIT("🪐"), PRISM("🔷"), FLAME("🔥"), WAVE("🌊"),
    LEAF("🍀"), STAR("✨"), MOON("🌙"), SUN("☀️"), EYE("👁"), KEY("🗝"),
    BOLT("🌀"), CUBE("🧊"), HEART("💠"), CROWN("👑"), ATOM("⚛"), MASK("🎭"),
    ROCKET("🚀"), BELL("🔔"), GEM("💎"), CLOCK("⏳"), MAGNET("🧲"), DNA("🦠")
}

data class Card(
    val id: Int,
    val pairId: Int,
    val symbol: Symbol,
    val kind: CardKind = CardKind.NORMAL,
    val faceUp: Boolean = false,
    val matched: Boolean = false,
    /** Revelada temporariamente pelo pulso ECO / Sobrecarga / Scan. */
    val previewUntilMs: Long = 0L,
    /** Marcada permanentemente pela carta Espelho. */
    val tagged: Boolean = false
) {
    fun isVisible(nowMs: Long): Boolean =
        faceUp || matched || tagged || nowMs < previewUntilMs
}

enum class GameMode { CAMPAIGN, BLITZ, ZEN }

enum class GameStatus { READY, RUNNING, PAUSED, WON, LOST }

data class LevelConfig(
    val id: Int,
    val title: String,
    val columns: Int,
    val rows: Int,
    val timeLimitMs: Long,
    val mutationEveryMoves: Int,
    val specials: Set<CardKind>,
    val parMoves: Int,
    val ecoEnabled: Boolean,
    val mode: GameMode = GameMode.CAMPAIGN
) {
    val cellCount: Int get() = columns * rows
    val pairCount: Int get() = cellCount / 2
}

/** Snapshot imutável consumido pela UI. */
data class GameState(
    val config: LevelConfig,
    val cards: List<Card> = emptyList(),
    val status: GameStatus = GameStatus.READY,
    val moves: Int = 0,
    val matches: Int = 0,
    val score: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val synapse: Float = 0f,
    val overloadUntilMs: Long = 0L,
    val timeLeftMs: Long = 0L,
    val elapsedMs: Long = 0L,
    val frozenUntilMs: Long = 0L,
    val nowMs: Long = 0L,
    val selection: List<Int> = emptyList(),
    val stars: Int = 0
) {
    val overloadActive: Boolean get() = nowMs < overloadUntilMs
    val overloadReady: Boolean get() = synapse >= 1f && !overloadActive
    val frozen: Boolean get() = nowMs < frozenUntilMs
    val comboMultiplier: Float get() = (1f + combo * 0.25f).coerceAtMost(4f)
    val progress: Float
        get() = if (config.pairCount == 0) 0f else matches.toFloat() / config.pairCount
}

/** Eventos de uma jogada — a UI/áudio/háptica reagem a eles. */
sealed interface GameEvent {
    data class Flipped(val index: Int) : GameEvent
    data class Matched(val a: Int, val b: Int, val kind: CardKind, val points: Int) : GameEvent
    data class Mismatched(val a: Int, val b: Int) : GameEvent
    data class EcoPulse(val origins: List<Int>, val revealed: List<Int>) : GameEvent
    data class Mutation(val from: Int, val to: Int) : GameEvent
    data class Combo(val level: Int) : GameEvent
    data object OverloadReady : GameEvent
    data object OverloadStarted : GameEvent
    data class Tagged(val indices: List<Int>) : GameEvent
    data class TimeGained(val ms: Long) : GameEvent
    data object Frozen : GameEvent
    data class Won(val score: Int, val stars: Int) : GameEvent
    data object Lost : GameEvent
}
