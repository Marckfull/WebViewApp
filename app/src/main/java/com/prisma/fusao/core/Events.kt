package com.prisma.fusao.core

/** Uma gema que saiu do tabuleiro. */
data class ClearedGem(val pos: Pos, val gem: Gem)

/** Uma peça especial nascendo de um match grande. */
data class CreatedGem(val pos: Pos, val gem: Gem)

/** Duas essências encostaram e viraram uma peça maior. */
data class FusionEvent(val a: Pos, val b: Pos, val at: Pos, val result: Gem)

/** Detonação de uma peça especial — a camada visual desenha o feixe a partir daqui. */
data class Blast(val origin: Pos, val kind: GemKind, val color: GemColor?, val cells: List<Pos>)

/** Deslocamento de uma peça já existente (queda por gravidade). */
data class GemMove(val id: Long, val from: Pos, val to: Pos)

/** Peça nova entrando pelo topo. [from] tem linha negativa: está fora da tela. */
data class GemSpawn(val gem: Gem, val from: Pos, val to: Pos)

/** Camada de cristal quebrada. */
data class IceBreak(val pos: Pos, val remaining: Int)

/**
 * Um quadro da resolução de uma jogada. A UI reproduz os passos em sequência:
 * primeiro a limpeza/fusão, depois a queda das peças.
 */
data class ResolveStep(
    val cascade: Int,
    val cleared: List<ClearedGem> = emptyList(),
    val created: List<CreatedGem> = emptyList(),
    val fusions: List<FusionEvent> = emptyList(),
    val blasts: List<Blast> = emptyList(),
    val iceBroken: List<IceBreak> = emptyList(),
    val stonesHit: List<Pos> = emptyList(),
    val prismoidsDelivered: List<Pos> = emptyList(),
    val moves: List<GemMove> = emptyList(),
    val spawns: List<GemSpawn> = emptyList(),
    val scoreGained: Int = 0,
    val totalScore: Int = 0,
) {
    val isEmpty: Boolean
        get() = cleared.isEmpty() && created.isEmpty() && fusions.isEmpty() &&
            moves.isEmpty() && spawns.isEmpty() && prismoidsDelivered.isEmpty()
}

/** Resultado imediato de um toque/arrasto do jogador. */
sealed interface MoveOutcome {
    /** A troca não é válida (não forma match e não envolve peça especial). */
    data class Rejected(val a: Pos, val b: Pos) : MoveOutcome

    /** A troca aconteceu; [steps] é a linha do tempo para animar. */
    data class Accepted(
        val a: Pos,
        val b: Pos,
        val steps: List<ResolveStep>,
        val freeSwap: Boolean,
        val status: LevelStatus,
    ) : MoveOutcome
}

enum class LevelStatus { PLAYING, WON, LOST }
