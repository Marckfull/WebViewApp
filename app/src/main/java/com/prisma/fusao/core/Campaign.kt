package com.prisma.fusao.core

import kotlin.random.Random

/**
 * A campanha do PRISMA: 150 fases geradas de forma determinística.
 *
 * O ritmo é proposital — cada bloco de fases apresenta um elemento novo antes de
 * começar a combiná-los:
 *
 * ```
 *   1-4    pontuação pura (aprende a trocar)
 *   5-7    coleta de cor
 *   8-11   cristal
 *   12-19  fusão (o objetivo que ensina a mecânica central)
 *   20-24  pedras
 *   25+    prismoides e combinações de objetivos
 *   30+    dois objetivos por fase
 *   60+    três objetivos por fase
 *   x10    fase-chefe: tabuleiro difícil, menos jogadas, mais estrelas
 * ```
 */
object Campaign {

    const val LEVEL_COUNT = 150
    const val ROWS = 8
    const val COLS = 8

    /** Formatos de tabuleiro reaproveitados ao longo da campanha. */
    private val shapes: List<List<String>> = listOf(
        // 0 — aberto
        listOf(
            "........",
            "........",
            "........",
            "........",
            "........",
            "........",
            "........",
            "........",
        ),
        // 1 — losango
        listOf(
            "##....##",
            "#......#",
            "........",
            "........",
            "........",
            "........",
            "#......#",
            "##....##",
        ),
        // 2 — ampulheta
        listOf(
            "........",
            "........",
            ".#....#.",
            "..#..#..",
            "..#..#..",
            ".#....#.",
            "........",
            "........",
        ),
        // 3 — anel de cristal
        listOf(
            "........",
            ".111111.",
            ".1....1.",
            ".1....1.",
            ".1....1.",
            ".1....1.",
            ".111111.",
            "........",
        ),
        // 4 — fortaleza de pedra
        listOf(
            "........",
            "........",
            "..XX....",
            "....XX..",
            "..XX....",
            "....XX..",
            "........",
            "........",
        ),
        // 5 — cruz
        listOf(
            "##....##",
            "##....##",
            "........",
            "........",
            "........",
            "........",
            "##....##",
            "##....##",
        ),
        // 6 — funil
        listOf(
            "........",
            "........",
            "#......#",
            "##....##",
            "##....##",
            "#......#",
            "........",
            "........",
        ),
        // 7 — geleira profunda
        listOf(
            "........",
            "..2222..",
            ".22..22.",
            ".2....2.",
            ".2....2.",
            ".22..22.",
            "..2222..",
            "........",
        ),
        // 8 — pilares
        listOf(
            "........",
            ".X....X.",
            ".X....X.",
            "........",
            "........",
            ".X....X.",
            ".X....X.",
            "........",
        ),
        // 9 — labirinto
        listOf(
            "........",
            ".##..##.",
            "........",
            "..1111..",
            "..1111..",
            "........",
            ".##..##.",
            "........",
        ),
    )

    /** Nome curto de cada mundo, mostrado no mapa a cada 25 fases. */
    val worldNames = listOf(
        "Vale de Cristal",
        "Cavernas de Âmbar",
        "Deserto de Topázio",
        "Floresta de Esmeralda",
        "Abismo de Safira",
        "Coroa de Ametista",
    )

    val levels: List<LevelSpec> by lazy { List(LEVEL_COUNT) { build(it + 1) } }

    fun level(index: Int): LevelSpec = levels[(index - 1).coerceIn(0, LEVEL_COUNT - 1)]

    fun worldOf(index: Int): Int = ((index - 1) / 25).coerceIn(0, worldNames.lastIndex)

    fun isBoss(index: Int): Boolean = index % 10 == 0

    private fun build(index: Int): LevelSpec {
        val rng = Random(index * 7919L + 13)
        val boss = isBoss(index)
        val difficulty = index / 10.0

        val mask = shapes[pickShape(index, rng, boss)]
        val colorCount = when {
            index <= 10 -> 4
            index <= 40 -> 5
            else -> 6
        }

        val objectives = buildObjectives(index, rng, boss, colorCount, mask)
        val moves = movesFor(index, objectives, boss)

        val baseScore = objectives.firstOrNull { it.type == ObjectiveType.SCORE }?.target
            ?: (1400 + (difficulty * 900).toInt())

        val starScores = listOf(
            baseScore,
            (baseScore * 1.6).toInt(),
            (baseScore * 2.4).toInt(),
        )

        return LevelSpec(
            index = index,
            rows = ROWS,
            cols = COLS,
            moves = moves,
            colorCount = colorCount,
            mask = mask,
            objectives = objectives,
            starScores = starScores,
            seed = index * 104729L + 7,
        )
    }

    private fun pickShape(index: Int, rng: Random, boss: Boolean): Int = when {
        index <= 4 -> 0
        index <= 7 -> if (index % 2 == 0) 0 else 1
        index in 8..11 -> 3
        index in 12..19 -> listOf(0, 1, 2, 6)[index % 4]
        index in 20..24 -> listOf(4, 8)[index % 2]
        boss -> listOf(5, 7, 9, 2)[(index / 10) % 4]
        else -> rng.nextInt(shapes.size)
    }

    /** Quantas camadas de cristal o formato oferece — teto real para o objetivo de gelo. */
    private fun iceCapacity(mask: List<String>): Int {
        var total = 0
        for (row in mask) for (ch in row) {
            if (ch == '1') total += 1
            if (ch == '2') total += 2
        }
        return total
    }

    /** Quantos blocos de pedra o formato oferece. */
    private fun stoneCapacity(mask: List<String>): Int {
        var total = 0
        for (row in mask) for (ch in row) if (ch == 'X') total++
        return total
    }

    private fun buildObjectives(
        index: Int,
        rng: Random,
        boss: Boolean,
        colorCount: Int,
        mask: List<String>,
    ): List<Objective> {
        val palette = GemColor.entries.take(colorCount)
        // A curva cresce devagar: cada objetivo tem um teto, porque tabuleiro e número
        // de jogadas são finitos. Metas acima do teto tornariam a fase impossível.
        val t = (index - 1) / (LEVEL_COUNT - 1).toDouble()
        fun ramp(from: Int, to: Int): Int = (from + (to - from) * t).toInt()

        val iceCap = iceCapacity(mask)
        val stoneCap = stoneCapacity(mask)

        // Os tetos abaixo saíram de medição: um bot que joga para os objetivos foi
        // usado como piso de "jogador competente", e as metas ficam por volta de
        // 60-75% do que ele alcança. Ver docs/BALANCEAMENTO.md.
        fun make(type: ObjectiveType): Objective = when (type) {
            ObjectiveType.SCORE -> Objective(type, ramp(1100, 2800))
            ObjectiveType.COLLECT ->
                Objective(type, ramp(15, 24), palette[rng.nextInt(palette.size)])
            // Gelo e pedra nunca podem pedir mais do que o tabuleiro oferece.
            ObjectiveType.ICE ->
                Objective(type, ramp(8, maxOf(8, (iceCap * 0.55).toInt())).coerceIn(1, maxOf(1, iceCap)))
            ObjectiveType.STONE ->
                Objective(type, ramp(2, 5).coerceIn(1, maxOf(1, stoneCap)))
            ObjectiveType.FUSION -> Objective(type, ramp(1, 3))
            ObjectiveType.PRISMOID -> Objective(type, ramp(1, 2))
        }

        // Objetivos dependentes do formato só entram se o formato os oferece.
        val pool: List<ObjectiveType> = when {
            index <= 4 -> listOf(ObjectiveType.SCORE)
            index <= 7 -> listOf(ObjectiveType.COLLECT)
            index <= 11 -> listOf(ObjectiveType.ICE)
            index <= 19 -> listOf(ObjectiveType.FUSION)
            index <= 24 -> listOf(ObjectiveType.STONE)
            else -> ObjectiveType.entries.toList()
        }.filter {
            (it != ObjectiveType.ICE || iceCap > 0) && (it != ObjectiveType.STONE || stoneCap > 0)
        }

        // Mais de dois objetivos divide demais a atenção do jogador dentro de um
        // orçamento de jogadas fixo — só as fases-chefe pedem três.
        val count = (if (index < 30) 1 else 2)
            .let { if (boss) it + 1 else it }
            .coerceAtMost(maxOf(1, pool.size))

        val result = pool.shuffled(rng).take(count).map { make(it) }.toMutableList()
        // Toda fase tem uma meta de pontuação para o cálculo de estrelas fazer sentido.
        if (result.none { it.type == ObjectiveType.SCORE }) result += make(ObjectiveType.SCORE)
        return result
    }

    /**
     * Orçamento de jogadas. Os pesos vêm de medição: um prismoide leva por volta de
     * 30 jogadas para atravessar o tabuleiro, então vale muito mais jogadas extras
     * do que um objetivo de coleta.
     */
    private fun movesFor(index: Int, objectives: List<Objective>, boss: Boolean): Int {
        var moves = 26
        for (obj in objectives) {
            moves += when (obj.type) {
                ObjectiveType.SCORE -> 0
                ObjectiveType.COLLECT -> 3
                ObjectiveType.ICE -> 5
                ObjectiveType.FUSION -> obj.target * 3
                ObjectiveType.PRISMOID -> obj.target * 14
                ObjectiveType.STONE -> 6
            }
        }
        moves -= (index / 15)
        if (boss) moves -= 2
        return moves.coerceIn(16, 55)
    }
}
