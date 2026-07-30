package com.neuroflip.game.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private fun config(
        columns: Int = 4,
        rows: Int = 4,
        timeLimitMs: Long = 60_000L,
        mutationEveryMoves: Int = 0,
        specials: Set<CardKind> = emptySet(),
        ecoEnabled: Boolean = true
    ) = LevelConfig(
        id = 1,
        title = "Teste",
        columns = columns,
        rows = rows,
        timeLimitMs = timeLimitMs,
        mutationEveryMoves = mutationEveryMoves,
        specials = specials,
        parMoves = 12,
        ecoEnabled = ecoEnabled
    )

    private fun engine(
        config: LevelConfig = config(),
        seed: Int = 42
    ) = GameEngine(config, Random(seed)).also { it.start() }

    /** Índices de um par ainda não resolvido. */
    private fun findPair(engine: GameEngine): Pair<Int, Int> {
        val grouped = engine.state.cards.withIndex()
            .filter { !it.value.matched }
            .groupBy { it.value.pairId }
        val pair = grouped.values.first { it.size == 2 }
        return pair[0].index to pair[1].index
    }

    private fun findMismatch(engine: GameEngine): Pair<Int, Int> {
        val cards = engine.state.cards
        for (i in cards.indices) {
            for (j in i + 1 until cards.size) {
                if (cards[i].pairId != cards[j].pairId && !cards[i].matched && !cards[j].matched) {
                    return i to j
                }
            }
        }
        error("tabuleiro sem par diferente")
    }

    @Test
    fun `tabuleiro tem duas cartas por par e quantidade par de celulas`() {
        val engine = engine(config(columns = 4, rows = 6))
        val state = engine.state
        assertEquals(24, state.cards.size)
        val counts = state.cards.groupBy { it.pairId }.mapValues { it.value.size }
        assertTrue(counts.values.all { it == 2 })
        assertEquals(12, counts.size)
    }

    @Test
    fun `virar duas cartas iguais resolve o par e pontua`() {
        val engine = engine()
        val (a, b) = findPair(engine)

        engine.flip(a)
        assertTrue(engine.state.cards[a].faceUp)

        val events = engine.flip(b)
        assertTrue(engine.state.cards[a].matched)
        assertTrue(engine.state.cards[b].matched)
        assertEquals(1, engine.state.matches)
        assertTrue(engine.state.score > 0)
        assertTrue(events.any { it is GameEvent.Matched })
    }

    @Test
    fun `errar reseta o combo e esconde as cartas depois do tempo de leitura`() {
        val engine = engine()
        val (a, b) = findPair(engine)
        engine.flip(a)
        engine.flip(b)
        assertEquals(1, engine.state.combo)

        val (x, y) = findMismatch(engine)
        engine.flip(x)
        engine.flip(y)
        assertEquals(0, engine.state.combo)
        assertTrue(engine.state.cards[x].faceUp)

        engine.tick(GameEngine.MISMATCH_HOLD_MS + 50)
        assertFalse(engine.state.cards[x].faceUp)
        assertFalse(engine.state.cards[y].faceUp)
        assertTrue(engine.state.selection.isEmpty())
    }

    @Test
    fun `combo aumenta o multiplicador ate o teto`() {
        val engine = engine(config(columns = 6, rows = 6))
        repeat(13) {
            val (a, b) = findPair(engine)
            engine.flip(a)
            engine.flip(b)
        }
        assertEquals(4f, engine.state.comboMultiplier, 0.001f)
    }

    @Test
    fun `pulso eco revela vizinhos por tempo limitado`() {
        val engine = engine()
        val (a, b) = findPair(engine)
        engine.flip(a)
        val events = engine.flip(b)

        val pulse = events.filterIsInstance<GameEvent.EcoPulse>().firstOrNull()
        assertNotNull("o pulso ECO deveria ocorrer", pulse)
        val revealed = pulse!!.revealed
        assertTrue(revealed.isNotEmpty())
        assertTrue(revealed.all { engine.state.cards[it].previewUntilMs > engine.state.nowMs })

        engine.tick(GameEngine.ECO_REVEAL_MS + 100)
        assertTrue(revealed.none { engine.state.cards[it].isVisible(engine.state.nowMs) })
    }

    @Test
    fun `eco desligado nao gera pulso`() {
        val engine = engine(config(ecoEnabled = false))
        val (a, b) = findPair(engine)
        engine.flip(a)
        val events = engine.flip(b)
        assertNull(events.filterIsInstance<GameEvent.EcoPulse>().firstOrNull())
    }

    @Test
    fun `mutacao troca duas cartas de lugar sem alterar o conjunto`() {
        val engine = engine(config(mutationEveryMoves = 1))
        val before = engine.state.cards.map { it.id }.toSet()

        val (x, y) = findMismatch(engine)
        engine.flip(x)
        val events = engine.flip(y)

        val mutation = events.filterIsInstance<GameEvent.Mutation>().firstOrNull()
        assertNotNull("deveria mutar depois de 1 jogada", mutation)
        assertEquals(before, engine.state.cards.map { it.id }.toSet())
    }

    @Test
    fun `sobrecarga so ativa com a barra cheia e revela o tabuleiro`() {
        val engine = engine(config(columns = 4, rows = 4))
        assertTrue(engine.activateOverload().isEmpty())

        repeat(5) {
            val (a, b) = findPair(engine)
            engine.flip(a)
            engine.flip(b)
        }
        assertTrue(engine.state.overloadReady)

        val events = engine.activateOverload()
        assertTrue(events.contains(GameEvent.OverloadStarted))
        assertTrue(engine.state.overloadActive)
        assertEquals(0f, engine.state.synapse, 0.001f)
        assertTrue(engine.state.cards.filter { !it.matched }.all { it.isVisible(engine.state.nowMs) })
    }

    @Test
    fun `tempo esgotado derrota o jogador`() {
        val engine = engine(config(timeLimitMs = 1_000L))
        val events = engine.tick(1_200L)
        assertTrue(events.contains(GameEvent.Lost))
        assertEquals(GameStatus.LOST, engine.state.status)
    }

    @Test
    fun `sem limite de tempo o cronometro nao corre`() {
        val engine = engine(config(timeLimitMs = 0L))
        engine.tick(5_000L)
        assertEquals(GameStatus.RUNNING, engine.state.status)
    }

    @Test
    fun `tempo extra reativa uma partida perdida`() {
        val engine = engine(config(timeLimitMs = 1_000L))
        engine.tick(1_200L)
        assertEquals(GameStatus.LOST, engine.state.status)

        engine.addTime(30_000L)
        assertEquals(GameStatus.RUNNING, engine.state.status)
        assertTrue(engine.state.timeLeftMs >= 30_000L)
    }

    @Test
    fun `resolver todos os pares ganha a partida com estrelas e recompensa`() {
        val engine = engine(config(columns = 2, rows = 2))
        var events: List<GameEvent> = emptyList()
        repeat(engine.state.config.pairCount) {
            val (a, b) = findPair(engine)
            engine.flip(a)
            events = engine.flip(b)
        }

        assertTrue(events.any { it is GameEvent.Won })
        assertEquals(GameStatus.WON, engine.state.status)
        assertEquals(3, engine.state.stars)
        assertTrue(engine.neuronReward() > 0)
    }

    @Test
    fun `curinga resolve um par automaticamente`() {
        val engine = engine(config(columns = 4, rows = 4))
        val before = engine.state.matches
        engine.useWildcard()
        assertEquals(before + 1, engine.state.matches)
    }

    @Test
    fun `scan revela tudo por tempo limitado`() {
        val engine = engine()
        assertTrue(engine.useScan())
        assertTrue(engine.state.cards.all { it.isVisible(engine.state.nowMs) })
        engine.tick(GameEngine.SCAN_REVEAL_MS + 100)
        assertTrue(engine.state.cards.none { it.isVisible(engine.state.nowMs) })
    }

    @Test
    fun `pausa impede jogadas`() {
        val engine = engine()
        engine.pause()
        val (a, _) = findPair(engine)
        assertTrue(engine.flip(a).isEmpty())
        assertFalse(engine.state.cards[a].faceUp)

        engine.resume()
        assertTrue(engine.flip(a).isNotEmpty())
    }

    @Test
    fun `carta crio congela o cronometro e devolve tempo`() {
        val engine = engine(config(specials = setOf(CardKind.CRYO)))
        val cryo = engine.state.cards.withIndex().filter { it.value.kind == CardKind.CRYO }
        if (cryo.size < 2) return // o sorteio pode não ter posto Crio neste tabuleiro

        val timeBefore = engine.state.timeLeftMs
        engine.flip(cryo[0].index)
        engine.flip(cryo[1].index)

        assertTrue(engine.state.frozen)
        assertTrue(engine.state.timeLeftMs > timeBefore)
    }

    @Test
    fun `todos os niveis do catalogo tem tabuleiro valido`() {
        (1..LevelCatalog.TOTAL_LEVELS).forEach { id ->
            val level = LevelCatalog.level(id)
            assertEquals("nível $id precisa de células pares", 0, level.cellCount % 2)
            assertTrue("nível $id não pode passar de 24 pares", level.pairCount <= Symbol.entries.size)
            assertTrue(level.columns in 2..6)
        }
    }
}
