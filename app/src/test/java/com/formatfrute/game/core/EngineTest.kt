package com.formatfrute.game.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Testes do motor. Rodam na JVM (sem emulador): Run 'EngineTest' no Android
 * Studio ou `./gradlew test`.
 */
class EngineTest {

    private fun board(size: Int, vararg tiles: Tile) =
        GameState(size = size, tiles = tiles.toList(), nextId = 100)

    private fun tile(
        id: Long,
        level: Int,
        row: Int,
        col: Int,
        kind: TileKind = TileKind.FRUIT,
        ice: Int = 0,
    ) = Tile(id = id, level = level, row = row, col = col, kind = kind, ice = ice)

    // ------------------------------------------------------------ deslize

    @Test
    fun `duas frutas iguais viram a proxima da escada`() {
        val result = Engine.move(board(4, tile(1, 0, 0, 0), tile(2, 0, 0, 1)), Direction.LEFT)

        assertEquals(1, result.state.tiles.size)
        assertEquals(Fruit.MORANGO.ordinal, result.state.tiles[0].level)
        assertEquals(Fruit.MORANGO.value, result.scoreGain)
        assertEquals(0, result.state.tiles[0].col)
        // o id sobrevive a fusao: e o que permite animar o deslize
        assertEquals(1L, result.state.tiles[0].id)
        assertEquals(1, result.state.dying.size)
    }

    @Test
    fun `tres iguais fundem apenas uma vez`() {
        val result = Engine.move(
            board(4, tile(1, 0, 0, 0), tile(2, 0, 0, 1), tile(3, 0, 0, 2)),
            Direction.LEFT,
        )

        assertEquals(2, result.state.tiles.size)
        assertTrue(result.state.tiles.any { it.level == 1 })
        assertTrue(result.state.tiles.any { it.level == 0 })
    }

    @Test
    fun `quatro iguais viram dois pares`() {
        val result = Engine.move(
            board(4, tile(1, 0, 0, 0), tile(2, 0, 0, 1), tile(3, 0, 0, 2), tile(4, 0, 0, 3)),
            Direction.LEFT,
        )

        assertEquals(2, result.state.tiles.size)
        assertEquals(Fruit.MORANGO.value * 2, result.scoreGain)
        assertEquals(listOf(0, 1), result.state.tiles.map { it.col }.sorted())
    }

    @Test
    fun `jogada sem efeito nao conta como jogada`() {
        val result = Engine.move(board(4, tile(1, 0, 0, 0), tile(2, 1, 0, 1)), Direction.LEFT)

        assertFalse(result.changed)
        assertEquals(0, result.state.moves)
    }

    // --------------------------------------------------------------- gelo

    @Test
    fun `fruta congelada nao desliza nem funde`() {
        val result = Engine.move(
            board(4, tile(1, 0, 0, 3, ice = 1), tile(2, 0, 0, 0)),
            Direction.RIGHT,
        )

        assertEquals(3, result.state.tiles.first { it.id == 1L }.col)
        assertEquals(2, result.state.tiles.first { it.id == 2L }.col)
        assertTrue(result.merges.isEmpty())
    }

    @Test
    fun `fusao vizinha racha o gelo`() {
        val result = Engine.move(
            board(4, tile(1, 0, 0, 0), tile(2, 0, 0, 1), tile(3, 0, 1, 0, ice = 2)),
            Direction.LEFT,
        )

        assertEquals(1, result.state.tiles.first { it.id == 3L }.ice)
        assertEquals(1, result.brokenIce)
    }

    @Test
    fun `gelo longe da fusao continua inteiro`() {
        val result = Engine.move(
            board(4, tile(1, 0, 0, 0), tile(2, 0, 0, 1), tile(3, 0, 3, 3, ice = 2)),
            Direction.LEFT,
        )

        assertEquals(2, result.state.tiles.first { it.id == 3L }.ice)
    }

    // -------------------------------------------------------- fruta podre

    @Test
    fun `fruta podre nunca funde`() {
        val result = Engine.move(
            board(4, tile(1, 0, 0, 0, TileKind.ROTTEN), tile(2, 0, 0, 1)),
            Direction.LEFT,
        )

        assertTrue(result.merges.isEmpty())
        assertEquals(2, result.state.tiles.size)
    }

    @Test
    fun `fusao ao lado espanta a fruta podre`() {
        val result = Engine.move(
            board(4, tile(1, 0, 1, 0), tile(2, 0, 1, 1), tile(3, 0, 0, 0, TileKind.ROTTEN)),
            Direction.LEFT,
        )

        assertTrue(result.state.tiles.none { it.kind == TileKind.ROTTEN })
        assertEquals(1, result.cleanedRotten)
    }

    // ----------------------------------------------------------- coringa

    @Test
    fun `coringa combina com qualquer fruta`() {
        val result = Engine.move(
            board(4, tile(1, 5, 0, 0), tile(2, 0, 0, 1, TileKind.RAINBOW)),
            Direction.LEFT,
        )

        assertEquals(1, result.state.tiles.size)
        assertEquals(6, result.state.tiles[0].level)
        assertEquals(TileKind.FRUIT, result.state.tiles[0].kind)
    }

    // ---------------------------------------------------------- colheita

    @Test
    fun `duas melancias viram colheita`() {
        val result = Engine.move(
            board(4, tile(1, Fruit.MAX, 0, 0), tile(2, Fruit.MAX, 0, 1)),
            Direction.LEFT,
        )

        assertTrue(result.state.tiles.isEmpty())
        assertEquals(1, result.harvests)
        assertEquals(Engine.HARVEST_POINTS, result.scoreGain)
    }

    @Test
    fun `casa liberada pela colheita recebe quem vem atras`() {
        val result = Engine.move(
            board(4, tile(1, Fruit.MAX, 0, 0), tile(2, Fruit.MAX, 0, 1), tile(3, 0, 0, 3)),
            Direction.LEFT,
        )

        assertEquals(0, result.state.tiles.single().col)
    }

    // -------------------------------------------------------- fim de jogo

    @Test
    fun `tabuleiro cheio sem par adjacente encerra`() {
        val tiles = listOf(0, 1, 1, 0).mapIndexed { i, level ->
            tile(i.toLong(), level, i / 2, i % 2)
        }
        assertFalse(Engine.canMove(GameState(size = 2, tiles = tiles)))
    }

    @Test
    fun `tabuleiro cheio com par continua`() {
        val tiles = listOf(0, 0, 1, 2).mapIndexed { i, level ->
            tile(i.toLong(), level, i / 2, i % 2)
        }
        assertTrue(Engine.canMove(GameState(size = 2, tiles = tiles)))
    }

    // ------------------------------------------------------------ poderes

    @Test
    fun `martelo e adubo fazem o combinado`() {
        val state = board(4, tile(1, 3, 2, 2))

        assertTrue(Engine.smash(state, 2, 2).tiles.isEmpty())
        assertEquals(4, Engine.grow(state, 2, 2).tiles[0].level)
        assertEquals(3, Engine.grow(state, 0, 0).tiles[0].level)
    }

    @Test
    fun `peneira embaralha sem empilhar frutas`() {
        val state = Engine.start(4, Random(7), seeds = 6)
        val shuffled = Engine.shuffle(state, Random(3))

        assertEquals(state.tiles.size, shuffled.tiles.size)
        val cells = shuffled.tiles.map { it.row to it.col }
        assertEquals(cells.size, cells.toSet().size)
    }

    // ----------------------------------------------------------- spawn

    @Test
    fun `spawn respeita as casas livres`() {
        var state = GameState(size = 2)
        val rng = Random(42)
        repeat(4) { state = Engine.spawn(state, rng) }

        assertEquals(4, state.tiles.size)
        assertEquals(4, state.tiles.map { it.id }.toSet().size)
        assertEquals(4, Engine.spawn(state, rng).tiles.size)
    }

    // ------------------------------------------------------------- fuzz

    @Test
    fun `duzentas partidas aleatorias sem estado invalido`() {
        val rng = Random(2024)
        repeat(200) {
            var state = Engine.start(4, rng)
            var guard = 0
            while (Engine.canMove(state) && guard++ < 600) {
                val result = Engine.move(state, Direction.entries[rng.nextInt(4)])
                state = result.state
                if (result.changed) {
                    state = Engine.spawn(state, rng, rainbowChance = 0.03f, iceChance = 0.1f)
                }
                val cells = state.tiles.map { it.row to it.col }
                assertEquals("frutas empilhadas na mesma casa", cells.size, cells.toSet().size)
                assertTrue(
                    "fruta fora do tabuleiro",
                    state.tiles.all { it.row in 0..3 && it.col in 0..3 },
                )
            }
        }
    }
}
