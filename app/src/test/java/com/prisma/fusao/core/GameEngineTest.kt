package com.prisma.fusao.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * O motor é Kotlin puro justamente para poder ser testado sem emulador.
 * Estes testes cobrem as regras que definem o jogo — principalmente a fusão.
 */
class GameEngineTest {

    /** Monta um tabuleiro controlado a partir de um desenho. */
    private fun engineWith(layout: List<String>, moves: Int = 30): GameEngine {
        val spec = LevelSpec(
            index = 999,
            rows = layout.size,
            cols = layout[0].length,
            moves = moves,
            colorCount = 6,
            mask = LevelSpec.openMask(layout.size, layout[0].length),
            objectives = listOf(Objective(ObjectiveType.SCORE, 1)),
            starScores = listOf(1, 2, 3),
            seed = 1,
        )
        val engine = GameEngine(spec)
        var id = 100_000L
        for (r in layout.indices) {
            for (c in layout[r].indices) {
                val gem = when (val ch = layout[r][c]) {
                    in '0'..'5' -> Gem(id++, GemKind.NORMAL, GemColor.entries[ch - '0'])
                    'a' -> Gem(id++, GemKind.ESSENCE, GemColor.RUBI)
                    'b' -> Gem(id++, GemKind.ESSENCE, GemColor.SAFIRA)
                    'P' -> Gem(id++, GemKind.PRISM, null)
                    'S' -> Gem(id++, GemKind.SUPERNOVA, GemColor.RUBI)
                    'X' -> Gem(id++, GemKind.STONE, hp = 2)
                    '@' -> Gem(id++, GemKind.PRISMOID)
                    else -> null
                }
                engine.board.setGem(Pos(r, c), gem)
            }
        }
        return engine
    }

    private fun emptyCells(engine: GameEngine) =
        engine.board.playablePositions().count { engine.board.gemAt(it) == null }

    @Test
    fun `tabuleiro inicia jogavel e sem match pronto`() {
        val engine = GameEngine(Campaign.level(1))
        assertTrue("não pode começar com match resolvido", !engine.hasMatches())
        assertTrue("precisa ter jogada disponível", engine.hasPossibleMove())
        assertEquals(0, emptyCells(engine))
    }

    @Test
    fun `troca sem match e rejeitada e nao gasta jogada`() {
        val engine = engineWith(
            listOf("012345", "123450", "234501", "345012", "450123", "501234"),
        )
        val before = engine.movesLeft
        val outcome = engine.trySwap(Pos(0, 0), Pos(0, 1))
        assertTrue(outcome is MoveOutcome.Rejected)
        assertEquals(before, engine.movesLeft)
    }

    @Test
    fun `match de tres nao cria peca especial`() {
        // Trocar (2,0) com (2,1) fecha exatamente três na vertical da coluna 0.
        val engine = engineWith(
            listOf("103450", "124503", "215034", "340345", "453450", "504503"),
        )
        assertTrue(!engine.hasMatches())
        val outcome = engine.trySwap(Pos(2, 0), Pos(2, 1))
        assertTrue(outcome is MoveOutcome.Accepted)
        outcome as MoveOutcome.Accepted
        // Só o primeiro quadro é o match do jogador; os seguintes já são cascata.
        assertTrue("match-3 não condensa essência", outcome.steps.first().created.isEmpty())
        assertTrue(engine.score > 0)
    }

    @Test
    fun `match de quatro condensa uma essencia`() {
        val engine = engineWith(
            listOf("123451", "134512", "145123", "000012", "123451", "134512"),
        )
        val steps = engine.useHammer(Pos(5, 5))
        val essences = steps.sumOf { step -> step.created.count { it.gem.kind == GemKind.ESSENCE } }
        assertTrue("esperava ao menos uma essência, veio $essences", essences >= 1)
    }

    @Test
    fun `essencia pode ser movida livremente sem explodir`() {
        val engine = engineWith(
            listOf("123451", "1a4512", "145123", "312012", "123451", "134512"),
        )
        assertTrue(engine.isFreeSwap(Pos(1, 1), Pos(1, 2)))
        val before = engine.movesLeft
        val outcome = engine.trySwap(Pos(1, 1), Pos(1, 2))
        assertTrue(outcome is MoveOutcome.Accepted)
        assertEquals("mover essência custa uma jogada", before - 1, engine.movesLeft)
        assertTrue(
            "a essência não pode detonar ao ser reposicionada",
            engine.board.playablePositions().any { engine.board.gemAt(it)?.kind == GemKind.ESSENCE },
        )
    }

    @Test
    fun `duas essencias encostadas fundem`() {
        val engine = engineWith(
            listOf("123451", "a1a512", "145123", "312012", "123451", "134512"),
        )
        val outcome = engine.trySwap(Pos(1, 1), Pos(1, 0))
        assertTrue(outcome is MoveOutcome.Accepted)
        outcome as MoveOutcome.Accepted
        assertTrue("esperava uma fusão", outcome.steps.sumOf { it.fusions.size } >= 1)
        assertTrue(engine.fusionCount >= 1)
    }

    @Test
    fun `cores iguais geram supernova e cores diferentes geram prisma`() {
        val same = engineWith(listOf("12345", "aa123", "14512", "31201", "12345"))
        val supernova = same.useHammer(Pos(4, 4))
            .any { step -> step.fusions.any { it.result.kind == GemKind.SUPERNOVA } }
        assertTrue("essências da mesma cor viram supernova", supernova)

        val mixed = engineWith(listOf("12345", "ab123", "14512", "31201", "12345"))
        val prism = mixed.useHammer(Pos(4, 4))
            .any { step -> step.fusions.any { it.result.kind == GemKind.PRISM } }
        assertTrue("essências de cores diferentes viram prisma", prism)
    }

    @Test
    fun `duas pecas de nivel dois encostadas geram a Nova Cromatica`() {
        // Prisma parado em (1,1). Duas essências vão fundir exatamente ao lado dele,
        // em (1,2) — e a supernova resultante encosta no prisma, subindo de nível.
        val engine = engineWith(
            listOf("123451", "1P4312", "345123", "451234", "123451", "234512"),
        )
        var id = 900_000L
        engine.board.setGem(Pos(1, 3), Gem(id++, GemKind.ESSENCE, GemColor.RUBI))
        engine.board.setGem(Pos(1, 2), Gem(id, GemKind.ESSENCE, GemColor.RUBI))

        val steps = engine.useHammer(Pos(5, 5))
        val fused = steps.flatMap { it.fusions }.map { it.result.kind }
        assertTrue("as essências precisam fundir primeiro", fused.contains(GemKind.SUPERNOVA))
        assertTrue(
            "duas peças de nível 2 encostadas têm que virar uma Nova, veio $fused",
            fused.contains(GemKind.NOVA),
        )
    }

    @Test
    fun `a Nova detona o tabuleiro inteiro`() {
        val engine = engineWith(
            listOf("123451", "134512", "145123", "312012", "123451", "134512"),
        )
        engine.board.setGem(Pos(2, 2), Gem(999_001, GemKind.NOVA))
        val playable = engine.board.playablePositions().count()

        val outcome = engine.trySwap(Pos(2, 2), Pos(2, 3)) as MoveOutcome.Accepted
        assertTrue(
            "a Nova deveria varrer o tabuleiro, limpou ${outcome.steps.first().cleared.size} de $playable",
            outcome.steps.first().cleared.size >= playable - 2,
        )
    }

    @Test
    fun `a Nova e o topo da escada e nao funde com mais nada`() {
        assertEquals(1, Gem(1, GemKind.ESSENCE, GemColor.RUBI).fusionTier)
        assertEquals(2, Gem(2, GemKind.PRISM).fusionTier)
        assertEquals(2, Gem(3, GemKind.SUPERNOVA, GemColor.RUBI).fusionTier)
        assertEquals("a Nova não sobe mais de nível", 0, Gem(4, GemKind.NOVA).fusionTier)
        assertEquals(0, Gem(5, GemKind.NORMAL, GemColor.RUBI).fusionTier)

        // Duas Novas encostadas não fundem — mas ainda podem ser trocadas entre si.
        val engine = engineWith(
            listOf("123451", "134512", "145123", "312012", "123451", "134512"),
        )
        engine.board.setGem(Pos(2, 2), Gem(999_002, GemKind.NOVA))
        engine.board.setGem(Pos(2, 3), Gem(999_003, GemKind.NOVA))
        val steps = engine.useHammer(Pos(5, 5))
        assertTrue(
            "não pode existir nível 4",
            steps.flatMap { it.fusions }.isEmpty(),
        )
    }

    @Test
    fun `prisma detona linha e coluna`() {
        val engine = engineWith(
            listOf("123451", "134512", "14P123", "312012", "123451", "134512"),
        )
        val prismId = engine.board.gemAt(Pos(2, 2))!!.id
        val outcome = engine.trySwap(Pos(2, 2), Pos(2, 3))
        assertTrue(outcome is MoveOutcome.Accepted)
        outcome as MoveOutcome.Accepted
        assertTrue(
            "linha + coluna deveriam limpar ao menos 10 gemas",
            outcome.steps.first().cleared.size >= 10,
        )
        // A cascata pode gerar outro prisma, então a checagem é pelo id da peça.
        assertTrue(engine.board.playablePositions().none { engine.board.gemAt(it)?.id == prismId })
    }

    @Test
    fun `supernova limpa a cor inteira`() {
        val engine = engineWith(
            listOf("020202", "1S4512", "020202", "312012", "020202", "134512"),
        )
        val reds = engine.board.playablePositions()
            .count { engine.board.gemAt(it)?.color == GemColor.RUBI }
        val outcome = engine.trySwap(Pos(1, 1), Pos(1, 2))
        assertTrue(outcome is MoveOutcome.Accepted)
        outcome as MoveOutcome.Accepted
        assertTrue(outcome.steps.first().cleared.size >= reds)
    }

    @Test
    fun `pedra resiste ao primeiro estouro`() {
        val engine = engineWith(listOf("12345", "1X451", "14512", "31201", "12345"))
        engine.useHammer(Pos(1, 0))
        assertEquals(GemKind.STONE, engine.board.gemAt(Pos(1, 1))?.kind)
    }

    @Test
    fun `prismoide e entregue ao chegar na base`() {
        val spec = LevelSpec(
            index = 900, rows = 6, cols = 6, moves = 40, colorCount = 5,
            mask = LevelSpec.openMask(6, 6),
            objectives = listOf(Objective(ObjectiveType.PRISMOID, 1)),
            starScores = listOf(100, 200, 300), seed = 5,
        )
        val engine = GameEngine(spec)
        engine.board.setGem(Pos(4, 2), Gem(999_999, GemKind.PRISMOID))
        engine.useHammer(Pos(5, 2))
        val delivered = engine.objectives()
            .first { it.objective.type == ObjectiveType.PRISMOID }.current
        assertTrue("prismoide na base precisa ser coletado", delivered >= 1)
    }

    @Test
    fun `derrota sem jogadas e jogadas extras devolvem a partida`() {
        val spec = LevelSpec(
            index = 901, rows = 6, cols = 6, moves = 1, colorCount = 5,
            mask = LevelSpec.openMask(6, 6),
            objectives = listOf(Objective(ObjectiveType.SCORE, 100_000_000)),
            starScores = listOf(1, 2, 3), seed = 9,
        )
        val engine = GameEngine(spec)
        val move = firstLegalMove(engine)
        assertNotNull("o tabuleiro deveria abrir com jogada possível", move)
        engine.trySwap(move!!.first, move.second)
        assertEquals(LevelStatus.LOST, engine.status)

        engine.grantMoves(5)
        assertEquals(LevelStatus.PLAYING, engine.status)
        assertEquals(5, engine.movesLeft)
    }

    @Test
    fun `campanha tem 150 fases coerentes`() {
        assertEquals(Campaign.LEVEL_COUNT, Campaign.levels.size)
        for (level in Campaign.levels) {
            assertEquals("máscara com altura errada na fase ${level.index}", level.rows, level.mask.size)
            assertEquals(3, level.starScores.size)
            assertTrue("fase ${level.index} sem objetivo", level.objectives.isNotEmpty())
            assertTrue(
                "estrelas fora de ordem na fase ${level.index}",
                level.starScores[0] < level.starScores[1] && level.starScores[1] < level.starScores[2],
            )
            val iceCapacity = level.mask.sumOf { row ->
                row.count { it == '1' } + 2 * row.count { it == '2' }
            }
            val stoneCapacity = level.mask.sumOf { row -> row.count { it == 'X' } }
            for (objective in level.objectives) {
                assertTrue("meta zerada na fase ${level.index}", objective.target > 0)
                // Uma meta acima da capacidade do tabuleiro tornaria a fase impossível.
                if (objective.type == ObjectiveType.ICE) {
                    assertTrue(
                        "fase ${level.index} pede ${objective.target} de gelo e só tem $iceCapacity",
                        objective.target <= iceCapacity,
                    )
                }
                if (objective.type == ObjectiveType.STONE) {
                    assertTrue(
                        "fase ${level.index} pede ${objective.target} pedras e só tem $stoneCapacity",
                        objective.target <= stoneCapacity,
                    )
                }
            }
        }
    }

    @Test
    fun `todas as fases abrem jogaveis`() {
        for (level in Campaign.levels) {
            val engine = GameEngine(level)
            assertTrue("fase ${level.index} abre com match pronto", !engine.hasMatches())
            assertTrue("fase ${level.index} abre sem jogada", engine.hasPossibleMove())
            assertEquals("fase ${level.index} abre com buraco", 0, emptyCells(engine))
        }
    }

    @Test
    fun `motor nunca deixa buraco depois de muitas jogadas`() {
        val random = Random(42)
        for (index in listOf(1, 17, 33, 64, 97, 128, 150)) {
            val engine = GameEngine(Campaign.level(index))
            var guard = 0
            while (engine.status == LevelStatus.PLAYING && guard++ < 200) {
                val moves = legalMoves(engine)
                if (moves.isEmpty()) {
                    if (!engine.shuffle()) break else continue
                }
                val (a, b) = moves[random.nextInt(moves.size)]
                engine.trySwap(a, b)
                assertEquals("buraco aberto na fase $index", 0, emptyCells(engine))
            }
        }
    }

    private fun legalMoves(engine: GameEngine): List<Pair<Pos, Pos>> {
        val moves = mutableListOf<Pair<Pos, Pos>>()
        for (p in engine.board.playablePositions()) {
            for (d in listOf(Pos(p.r, p.c + 1), Pos(p.r + 1, p.c))) {
                if (engine.board.inBounds(d) && engine.canSwap(p, d)) moves += p to d
            }
        }
        return moves
    }

    private fun firstLegalMove(engine: GameEngine): Pair<Pos, Pos>? = legalMoves(engine).firstOrNull()
}
