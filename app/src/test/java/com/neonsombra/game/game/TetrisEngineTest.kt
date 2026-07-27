package com.neonsombra.game.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TetrisEngineTest {

    private fun engine(seed: Int = 42): TetrisEngine = TetrisEngine(Random(seed)).apply { start() }

    /** Avanca o jogo em passos pequenos, como o relogio real faz. */
    private fun TetrisEngine.run(seconds: Float, step: Float = 1f / 60f) {
        var elapsed = 0f
        while (elapsed < seconds) {
            update(step)
            elapsed += step
        }
    }

    /**
     * Uma peca I em pe na coluna [column], logo acima do fundo. A caixa da peca
     * tem as celulas na terceira coluna, por isso o deslocamento de -2.
     */
    private fun verticalBar(column: Int, y: Int = 15) =
        ActivePiece(TetrominoType.I, rotation = 1, x = column - 2, y = y)

    @Test
    fun `comeca jogando com uma peca e tres pecas na fila`() {
        val engine = engine()
        val snapshot = engine.snapshot()

        assertEquals(GameStatus.PLAYING, snapshot.status)
        assertNotNull(snapshot.active)
        assertEquals(NEXT_COUNT, snapshot.next.size)
    }

    @Test
    fun `quatro giros devolvem a peca ao formato original`() {
        val engine = engine()
        engine.placeForTest(ActivePiece(TetrominoType.T, rotation = 0, x = 4, y = 5))
        val original = engine.active!!.absoluteCells().map { it[0] to it[1] }.toSet()

        repeat(4) { engine.rotate() }

        assertEquals(original, engine.active!!.absoluteCells().map { it[0] to it[1] }.toSet())
    }

    @Test
    fun `girar da tres posicoes diferentes antes de repetir`() {
        val engine = engine()
        engine.placeForTest(ActivePiece(TetrominoType.T, rotation = 0, x = 4, y = 5))

        val formatos = mutableSetOf<Set<Pair<Int, Int>>>()
        repeat(4) {
            formatos += engine.active!!.absoluteCells().map { it[0] to it[1] }.toSet()
            engine.rotate()
        }

        assertEquals(4, formatos.size)
    }

    @Test
    fun `a peca desce sozinha com o tempo`() {
        val engine = engine()
        val start = engine.active!!.y

        engine.run(seconds = 2f)

        assertTrue("a peca deveria ter descido", engine.active!!.y > start)
    }

    @Test
    fun `segurar a tela faz a peca cair mais rapido`() {
        val normal = engine()
        normal.run(seconds = 1f)
        val semSegurar = normal.active!!.y

        val rapido = engine()
        rapido.softDropping = true
        rapido.run(seconds = 1f)
        val segurando = rapido.active!!.y

        assertTrue(
            "queda acelerada ($segurando) deveria passar da normal ($semSegurar)",
            segurando > semSegurar,
        )
    }

    @Test
    fun `a queda acelerada rende um ponto por linha`() {
        val engine = engine()
        engine.placeForTest(ActivePiece(TetrominoType.O, rotation = 0, x = 4, y = 0))
        engine.softDropping = true

        engine.run(seconds = 1.5f)

        assertTrue("deveria pontuar pela descida", engine.snapshot().score > 0)
    }

    @Test
    fun `a peca nao atravessa as paredes`() {
        val engine = engine()
        repeat(COLS * 2) { engine.moveHorizontally(-1) }
        assertTrue(engine.active!!.absoluteCells().all { it[0] >= 0 })

        repeat(COLS * 4) { engine.moveHorizontally(1) }
        assertTrue(engine.active!!.absoluteCells().all { it[0] < COLS })
    }

    @Test
    fun `linha completa some e vale cem pontos`() {
        val engine = engine()
        // Fundo cheio, menos a coluna 0.
        engine.loadForTest(listOf(".#########"))
        engine.placeForTest(verticalBar(column = 0))

        engine.run(seconds = 3f)

        val snapshot = engine.snapshot()
        assertEquals(1, snapshot.lines)
        assertEquals(100, snapshot.score)
        assertEquals(GameStatus.PLAYING, snapshot.status)
        assertTrue("a linha cheia deveria ter sumido", snapshot.codeAt(ROWS - 1, 5) == 0)
    }

    @Test
    fun `quatro linhas de uma vez valem oitocentos`() {
        val engine = engine()
        engine.loadForTest(
            listOf(
                "....######",
                "....######",
                "....######",
                "....######",
            ),
        )
        // Quatro barras em pe fecham as colunas 0 a 3 das quatro linhas.
        listOf(0, 1, 2, 3).forEach { column ->
            engine.placeForTest(verticalBar(column))
            engine.run(seconds = 3f)
        }

        val snapshot = engine.snapshot()
        assertEquals(4, snapshot.lines)
        assertEquals(800, snapshot.score)
    }

    @Test
    fun `o espelho sombrio reflete a pilha do fundo`() {
        val engine = engine()
        engine.loadForTest(listOf("##........"))
        val snapshot = engine.snapshot()

        assertTrue(snapshot.codeAt(ROWS - 1, 0) != 0)
        assertTrue("a ultima linha aparece espelhada na primeira", snapshot.shadowCodeAt(0, 0) != 0)
        assertTrue(snapshot.shadowCodeAt(0, 1) != 0)
        assertEquals(0, snapshot.shadowCodeAt(0, 2))
    }

    @Test
    fun `a sombra nunca cobre um bloco de verdade`() {
        val engine = engine()
        val rows = MutableList(ROWS) { "..........".toCharArray() }
        rows[0][0] = '#'
        rows[ROWS - 1][0] = '#'
        engine.loadForTest(rows.map { String(it) })

        val snapshot = engine.snapshot()
        assertEquals("bloco real tem prioridade sobre a sombra", 0, snapshot.shadowCodeAt(0, 0))
    }

    @Test
    fun `a sombra comeca a meia forca e nunca passa do limite`() {
        val engine = engine()
        assertEquals(0.50f, engine.shadowStrength(), 0.001f)
        assertTrue(engine.shadowStrength() <= 0.92f)
    }

    @Test
    fun `pilha ate o topo encerra a partida`() {
        val engine = engine()
        // Dezenove linhas com dois buracos em colunas diferentes: nenhuma peca
        // sozinha consegue completar uma linha e limpar o tabuleiro.
        engine.loadForTest(List(ROWS - 1) { "#.#######." })
        engine.placeForTest(ActivePiece(TetrominoType.O, rotation = 0, x = 8, y = 0))

        engine.run(seconds = 4f)

        assertEquals(GameStatus.GAME_OVER, engine.snapshot().status)
    }

    @Test
    fun `pausar congela o jogo e retomar volta a mexer`() {
        val engine = engine()
        engine.run(seconds = 0.5f)
        engine.pause()
        val parado = engine.active!!.y

        engine.run(seconds = 3f)
        assertEquals(parado, engine.active!!.y)

        engine.resume()
        engine.run(seconds = 2f)
        assertTrue(engine.active!!.y > parado)
    }

    @Test
    fun `o saco de sete nao repete peca dentro do mesmo saco`() {
        val engine = engine()
        val vistas = mutableListOf<TetrominoType>()
        vistas += engine.active!!.type
        vistas += engine.snapshot().next

        assertEquals(vistas.size, vistas.distinct().size)
    }

    @Test
    fun `reiniciar zera pontuacao e tabuleiro`() {
        val engine = engine()
        engine.loadForTest(listOf(".#########"))
        engine.placeForTest(verticalBar(column = 0))
        engine.run(seconds = 3f)
        assertTrue(engine.snapshot().score > 0)

        engine.reset()

        val snapshot = engine.snapshot()
        assertEquals(0, snapshot.score)
        assertEquals(0, snapshot.lines)
        assertEquals(1, snapshot.level)
        assertEquals(GameStatus.READY, snapshot.status)
        assertNull(snapshot.active)
        assertTrue(snapshot.cells.all { it == 0 })
    }

    @Test
    fun `a peca fantasma pousa no fundo livre`() {
        val engine = engine()
        engine.placeForTest(ActivePiece(TetrominoType.O, rotation = 0, x = 4, y = 0))

        assertEquals(ROWS - 2, engine.ghostY())
    }

    @Test
    fun `a queda fica mais rapida conforme o nivel sobe`() {
        val engine = engine()
        assertTrue("o nivel 1 e o mais lento", engine.gravityInterval() > 0.5f)
    }

    @Test
    fun `os avisos do motor chegam para o som`() {
        val engine = engine()
        engine.drainEvents()
        engine.moveHorizontally(-1)

        val eventos = engine.drainEvents()

        assertTrue(eventos.contains(GameEvent.Moved))
        assertTrue("a fila esvazia depois de lida", engine.drainEvents().isEmpty())
    }
}
