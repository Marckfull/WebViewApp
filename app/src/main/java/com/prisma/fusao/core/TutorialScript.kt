package com.prisma.fusao.core

/**
 * Roteiro do tutorial, separado da UI de propósito: assim os tabuleiros podem ser
 * verificados por teste automatizado. Um tutorial quebrado trava a primeira
 * experiência do jogador inteira, então ele não pode depender de sorte.
 */
object TutorialScript {

    /** O que o jogador precisa fazer para o passo avançar. */
    sealed interface Goal {
        /** Uma troca específica, destacada no tabuleiro. */
        data class Swap(val from: Pos, val to: Pos) : Goal

        /** Trocar a peça do tipo indicado com qualquer vizinha. */
        data class ActivateKind(val kind: GemKind) : Goal
    }

    class Step(
        val title: String,
        val instruction: String,
        /** Quando não nulo, o tabuleiro é remontado neste desenho antes do passo. */
        val layout: List<String>?,
        val goal: Goal,
        val celebration: String,
    )

    const val ROWS = 6
    const val COLS = 6

    val spec = LevelSpec(
        index = 0,
        rows = ROWS,
        cols = COLS,
        moves = 999,
        colorCount = 6,
        mask = LevelSpec.openMask(ROWS, COLS),
        objectives = listOf(Objective(ObjectiveType.SCORE, Int.MAX_VALUE)),
        starScores = listOf(1, 2, 3),
        seed = 12345,
    )

    /**
     * Os tabuleiros são desenhados à mão. Legenda: dígitos 0-5 são as cores,
     * 'a' e 'b' são essências (rubi e safira).
     */
    val steps: List<Step> = listOf(
        Step(
            title = "Junte três",
            instruction = "Arraste a gema destacada para formar uma linha de três. " +
                "É assim que se estoura no PRISMA.",
            layout = listOf(
                "103450",
                "124503",
                "215034",
                "340345",
                "453450",
                "504503",
            ),
            goal = Goal.Swap(Pos(2, 0), Pos(2, 1)),
            celebration = "Isso! Três em linha estouram.",
        ),
        Step(
            title = "Quatro condensam uma ESSÊNCIA",
            instruction = "Agora forme uma linha de quatro. Em vez de sumir, elas viram " +
                "uma essência — a peça mais importante do jogo.",
            layout = listOf(
                "123451",
                "234512",
                "305123",
                "030045",
                "123451",
                "234512",
            ),
            goal = Goal.Swap(Pos(2, 1), Pos(3, 1)),
            celebration = "Nasceu uma essência!",
        ),
        Step(
            title = "A essência anda livre",
            instruction = "Essências podem ser trocadas com qualquer vizinha, sem precisar " +
                "formar match. Leve esta até encostar na outra.",
            layout = listOf(
                "123451",
                "2a3a12",
                "345123",
                "451234",
                "123451",
                "234512",
            ),
            goal = Goal.Swap(Pos(1, 1), Pos(1, 2)),
            celebration = "FUSÃO! Duas essências da mesma cor viraram uma Supernova.",
        ),
        Step(
            title = "Detone a Supernova",
            instruction = "Prismas e Supernovas explodem quando você os troca com uma " +
                "vizinha. Troque a Supernova e veja o estrago.",
            // Continua do tabuleiro anterior: a Supernova que o jogador acabou de criar.
            layout = null,
            goal = Goal.ActivateKind(GemKind.SUPERNOVA),
            celebration = "A Supernova limpou a cor inteira!",
        ),
    )

    /** Reescreve o tabuleiro conforme o desenho do passo. */
    fun applyLayout(engine: GameEngine, layout: List<String>, startId: Long = 900_000L) {
        var id = startId
        for (r in 0 until engine.rows) {
            val line = layout.getOrNull(r) ?: continue
            for (c in 0 until engine.cols) {
                val gem = when (val ch = line.getOrNull(c) ?: '.') {
                    in '0'..'5' -> Gem(id++, GemKind.NORMAL, GemColor.entries[ch - '0'])
                    'a' -> Gem(id++, GemKind.ESSENCE, GemColor.RUBI)
                    'b' -> Gem(id++, GemKind.ESSENCE, GemColor.SAFIRA)
                    else -> null
                }
                engine.board.setGem(Pos(r, c), gem)
            }
        }
    }

    /**
     * Acha uma peça do tipo pedido e uma vizinha com quem ela possa ser trocada.
     * Devolve null se a peça não estiver mais no tabuleiro — a UI usa isso para
     * não travar o tutorial.
     */
    fun findActivatable(engine: GameEngine, kind: GemKind): Pair<Pos, Pos>? {
        for (pos in engine.board.playablePositions()) {
            if (engine.board.gemAt(pos)?.kind != kind) continue
            for (delta in listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)) {
                val neighbor = Pos(pos.r + delta.first, pos.c + delta.second)
                if (engine.board.inBounds(neighbor) && engine.canSwap(pos, neighbor)) {
                    return pos to neighbor
                }
            }
        }
        return null
    }
}
