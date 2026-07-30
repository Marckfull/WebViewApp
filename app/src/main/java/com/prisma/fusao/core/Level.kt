package com.prisma.fusao.core

/** Tipos de objetivo que uma fase pode pedir. */
enum class ObjectiveType {
    /** Alcançar uma pontuação. */
    SCORE,

    /** Coletar N gemas de uma cor específica. */
    COLLECT,

    /** Quebrar N camadas de cristal. */
    ICE,

    /** Realizar N fusões (encostar duas essências). */
    FUSION,

    /** Levar N prismoides até a base do tabuleiro. */
    PRISMOID,

    /** Destruir N blocos de pedra. */
    STONE,
}

data class Objective(
    val type: ObjectiveType,
    val target: Int,
    val color: GemColor? = null,
)

/**
 * Definição estática de uma fase.
 *
 * O [mask] descreve o formato do tabuleiro, uma string por linha:
 * ```
 *   '.'  casa jogável vazia (recebe gema aleatória)
 *   '#'  buraco (fora do tabuleiro)
 *   'X'  bloco de pedra
 *   '1'  casa com 1 camada de cristal
 *   '2'  casa com 2 camadas de cristal
 * ```
 */
data class LevelSpec(
    val index: Int,
    val rows: Int,
    val cols: Int,
    val moves: Int,
    val colorCount: Int,
    val mask: List<String>,
    val objectives: List<Objective>,
    val starScores: List<Int>,
    val seed: Long,
) {
    /** Cores em jogo nesta fase (as primeiras [colorCount] da enum). */
    val palette: List<GemColor> = GemColor.entries.take(colorCount.coerceIn(3, 6))

    val prismoidsRequested: Int =
        objectives.firstOrNull { it.type == ObjectiveType.PRISMOID }?.target ?: 0

    fun starsFor(score: Int): Int = starScores.count { score >= it }.coerceIn(0, 3)

    companion object {
        /** Máscara retangular simples, sem obstáculos. */
        fun openMask(rows: Int, cols: Int): List<String> = List(rows) { ".".repeat(cols) }
    }
}

/** Quanto de cada objetivo já foi cumprido. */
class ObjectiveProgress(private val objectives: List<Objective>) {
    private val counts = IntArray(objectives.size)

    fun snapshot(): List<ObjectiveState> = objectives.mapIndexed { i, obj ->
        ObjectiveState(obj, counts[i].coerceAtMost(obj.target))
    }

    fun add(type: ObjectiveType, amount: Int, color: GemColor? = null) {
        if (amount <= 0) return
        objectives.forEachIndexed { i, obj ->
            if (obj.type == type && (obj.color == null || obj.color == color)) {
                counts[i] += amount
            }
        }
    }

    fun setScore(score: Int) {
        objectives.forEachIndexed { i, obj ->
            if (obj.type == ObjectiveType.SCORE) counts[i] = score
        }
    }

    fun allComplete(): Boolean = objectives.indices.all { counts[it] >= objectives[it].target }
}

data class ObjectiveState(val objective: Objective, val current: Int) {
    val complete: Boolean get() = current >= objective.target
}
