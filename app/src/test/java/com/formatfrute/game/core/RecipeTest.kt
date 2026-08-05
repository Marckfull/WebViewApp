package com.formatfrute.game.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * O contrato do Modo Receita: **nenhuma das 60 fases pode nascer impossível**.
 * Como as fases são geradas, e não desenhadas à mão, é o teste que garante isso.
 */
class RecipeTest {

    private fun boardOf(recipe: Recipe) = RecipeBook.board(recipe, Random(recipe.seed))

    @Test
    fun `todas as fases tem pedido valido`() {
        (1..RecipeBook.TOTAL).forEach { n ->
            val recipe = RecipeBook.recipe(n)
            assertTrue("fase $n sem pedido", recipe.orders.isNotEmpty())
            assertTrue("fase $n com jogadas invalidas", recipe.moves > 0)
            recipe.orders.forEach { order ->
                // Cereja é a base da escada: nunca dá para "criar" uma.
                assertTrue("fase $n pede nível ${order.level}", order.level in 1..Fruit.MAX)
                assertTrue("fase $n pede ${order.count}", order.count > 0)
            }
        }
    }

    @Test
    fun `toda fase nasce com materia-prima para cada item do pedido`() {
        (1..RecipeBook.TOTAL).forEach { n ->
            val recipe = RecipeBook.recipe(n)
            val board = boardOf(recipe)

            recipe.orders.forEachIndexed { index, order ->
                val depth = if (index == 0) recipe.mainDepth else 1
                val need = order.count * (1 shl depth)
                val level = (order.level - depth).coerceAtLeast(0)
                val have = board.tiles.count {
                    it.level == level && !it.frozen && it.kind == TileKind.FRUIT
                }
                assertTrue(
                    "fase $n: ${order.fruit.label} x${order.count} precisa de $need " +
                        "peças de nível $level, mas o tabuleiro tem $have",
                    have >= need,
                )
            }
        }
    }

    @Test
    fun `nenhuma fase nasce sem espaco para manobrar`() {
        (1..RecipeBook.TOTAL).forEach { n ->
            val recipe = RecipeBook.recipe(n)
            val board = boardOf(recipe)
            val free = recipe.size * recipe.size - board.tiles.size

            assertTrue("fase $n nasce com só $free casas livres", free >= 4)
            assertTrue("fase $n nasce travada", Engine.canMove(board))
        }
    }

    @Test
    fun `o gelo nunca congela materia-prima`() {
        (1..RecipeBook.TOTAL).forEach { n ->
            val board = boardOf(RecipeBook.recipe(n))
            assertTrue(
                "fase $n congelou uma peça importante",
                board.tiles.none { it.frozen && it.level > 1 },
            )
        }
    }

    @Test
    fun `o tabuleiro nasce valido`() {
        (1..RecipeBook.TOTAL).forEach { n ->
            val recipe = RecipeBook.recipe(n)
            val board = boardOf(recipe)
            val cells = board.tiles.map { it.row to it.col }

            assertEquals("fase $n empilhou frutas", cells.size, cells.toSet().size)
            assertTrue(
                "fase $n colocou fruta fora do tabuleiro",
                board.tiles.all { it.row in 0 until recipe.size && it.col in 0 until recipe.size },
            )
        }
    }

    @Test
    fun `sempre sobra jogada alem do minimo de fusoes`() {
        (1..RecipeBook.TOTAL).forEach { n ->
            val recipe = RecipeBook.recipe(n)
            assertTrue(
                "fase $n dá ${recipe.moves} jogadas para ${recipe.minMerges} fusões",
                recipe.moves > recipe.minMerges,
            )
        }
    }

    @Test
    fun `a mesma fase e igual para todo mundo`() {
        val a = RecipeBook.recipe(27)
        val b = RecipeBook.recipe(27)

        assertEquals(a, b)
        assertEquals(
            boardOf(a).tiles.map { it.level to (it.row to it.col) },
            boardOf(b).tiles.map { it.level to (it.row to it.col) },
        )
    }

    @Test
    fun `a receita do dia e a mesma o dia inteiro e muda amanha`() {
        val hoje = RecipeBook.daily("2026-08-05")
        val denovo = RecipeBook.daily("2026-08-05")
        val amanha = RecipeBook.daily("2026-08-06")

        assertEquals(hoje, denovo)
        assertTrue("a fase do dia não mudou", hoje.seed != amanha.seed)
        assertTrue(hoje.daily)
        assertEquals(RecipeBook.DAILY, hoje.number)
    }

    @Test
    fun `a receita do dia nunca cai nas fases faceis`() {
        (1..400).forEach { dia ->
            val day = "2026-%02d-%02d".format((dia % 12) + 1, (dia % 28) + 1)
            assertTrue("dia $day sorteou fase fácil", RecipeBook.dailyNumber(day) >= 6)
            assertTrue(RecipeBook.dailyNumber(day) <= RecipeBook.TOTAL)
        }
    }

    @Test
    fun `a receita do dia nasce jogavel`() {
        val recipe = RecipeBook.daily("2026-08-05")
        val board = boardOf(recipe)

        assertTrue(board.tiles.isNotEmpty())
        assertTrue(Engine.canMove(board))
    }

    @Test
    fun `estrelas premiam quem economiza jogada`() {
        val recipe = RecipeBook.recipe(10)

        assertEquals(3, recipe.starsFor(recipe.moves))
        assertEquals(3, recipe.starsFor((recipe.moves * 0.4f).toInt() + 1))
        assertEquals(2, recipe.starsFor((recipe.moves * 0.2f).toInt()))
        assertEquals(1, recipe.starsFor(0))
    }

    // ------------------------------------------------------ voz das frutas

    @Test
    fun `so as frutas grandes falam na estreia`() {
        assertNull(FruitVoice.onArrival(Fruit.CEREJA, 1))
        assertNull(FruitVoice.onArrival(Fruit.MORANGO, 1))
        Fruit.entries.filter { it.ordinal >= Fruit.LIMAO.ordinal }.forEach {
            assertNotNull("${it.label} está muda", FruitVoice.onArrival(it, 7))
        }
    }

    @Test
    fun `a fruta do dia e a mesma o dia inteiro`() {
        assertEquals(
            FruitVoice.greetingOfTheDay("2026-08-05"),
            FruitVoice.greetingOfTheDay("2026-08-05"),
        )
        assertTrue(
            FruitVoice.greetingOfTheDay("2026-08-05") != FruitVoice.greetingOfTheDay("2026-08-06"),
        )
    }
}
