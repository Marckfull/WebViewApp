package com.formatfrute.game.core

import androidx.annotation.StringRes
import com.formatfrute.game.R
import kotlin.random.Random

/** Um item do pedido: quantas frutas daquele degrau o freguês quer. */
data class Order(val level: Int, val count: Int) {
    val fruit: Fruit get() = Fruit.of(level)
}

/**
 * Uma fase do Modo Receita. O tabuleiro já vem com a matéria-prima; o que
 * conta para o pedido são as frutas **criadas por fusão** durante a partida.
 */
data class Recipe(
    val number: Int,
    @StringRes val title: Int,
    val orders: List<Order>,
    val moves: Int,
    val size: Int,
    /**
     * Quantos degraus abaixo nasce a matéria-prima do pedido principal.
     * 1 = basta juntar duas; 2 = é preciso montar as duas antes.
     */
    val mainDepth: Int,
    val iceBlocks: Int,
    /** A cada quantas jogadas cai uma fruta podre; 0 = nenhuma. */
    val rottenEvery: Int,
    /** Semente do tabuleiro: é ela que faz a fase ser igual para todo mundo. */
    val seed: Long,
) {
    /** Fusões no mínimo absoluto, se cada peça caísse no lugar certo. */
    val minMerges: Int
        get() = orders.withIndex().sumOf { (index, order) ->
            val depth = if (index == 0) mainDepth else 1
            order.count * ((1 shl depth) - 1)
        }

    val hardest: Fruit get() = Fruit.of(orders.maxOf { it.level })

    /** Sorteada pela data em vez de escolhida na trilha. */
    val daily: Boolean get() = number == RecipeBook.DAILY

    fun starsFor(movesLeft: Int): Int = when {
        movesLeft >= moves * 0.40f -> 3
        movesLeft >= moves * 0.15f -> 2
        else -> 1
    }
}

/**
 * O caderno de receitas: 60 fases geradas de forma determinística, então a
 * fase 27 é a mesma para todo mundo, em qualquer aparelho, para sempre.
 *
 * A regra de ouro do gerador: **o tabuleiro sempre nasce com matéria-prima
 * suficiente para cada item do pedido**. Se não couber, o pedido encolhe — a
 * fase nunca sai impossível.
 */
object RecipeBook {

    const val TOTAL = 60

    /** Número reservado para a Receita do Dia (nunca é uma fase da trilha). */
    const val DAILY = 0

    /**
     * A Receita do Dia: uma fase da trilha sorteada pela data — a mesma para
     * todo mundo, no mundo inteiro, e diferente amanhã. Nunca sorteia as cinco
     * primeiras, que são fáceis demais para valer prêmio de dia.
     */
    fun dailyNumber(day: String): Int {
        val rng = Random(day.hashCode().toLong() * 8191)
        return 6 + rng.nextInt(TOTAL - 5)
    }

    /** A fase do dia, com tabuleiro próprio (não é a mesma jogada da trilha). */
    fun daily(day: String): Recipe {
        val base = recipe(dailyNumber(day))
        return base.copy(
            number = DAILY,
            title = R.string.recipe_daily,
            seed = day.hashCode().toLong() * 104_729L,
        )
    }

    /**
     * Casas que ficam livres na largada. Sem esse respiro o tabuleiro nasce
     * travado — e tabuleiro travado não é desafio, é bug.
     */
    private fun breathingRoom(size: Int): Int = 2 + size / 2

    private val names = listOf(
        R.string.recipe_1, R.string.recipe_2, R.string.recipe_3, R.string.recipe_4,
        R.string.recipe_5, R.string.recipe_6, R.string.recipe_7, R.string.recipe_8,
        R.string.recipe_9, R.string.recipe_10, R.string.recipe_11, R.string.recipe_12,
        R.string.recipe_13, R.string.recipe_14, R.string.recipe_15, R.string.recipe_16,
        R.string.recipe_17, R.string.recipe_18, R.string.recipe_19, R.string.recipe_20,
    )

    fun recipe(number: Int): Recipe {
        val n = number.coerceIn(1, TOTAL)
        val rng = Random(n * 7717L + 31)

        // A fruta principal sobe devagar: começa no limão e termina no abacaxi.
        val top = (3 + (n - 1) / 5).coerceIn(3, 9)
        val size = if (n >= 16 && (n % 5 == 0 || n % 7 == 0)) 5 else 4
        val mainDepth = if (n >= 25) 2 else 1

        val iceBlocks = when {
            n < 10 -> 0
            n < 30 -> 1
            else -> 2
        }
        // Reserva de casas vazias para manobrar, mais o espaço que o gelo ocupa.
        val capacity = size * size - breathingRoom(size) - iceBlocks

        val wanted = buildList {
            add(Order(top, if (n >= 30) 2 else 1))
            if (n >= 4) add(Order((top - 2).coerceAtLeast(1), 2 + rng.nextInt(2)))
            if (n >= 12) add(Order((top - 4).coerceAtLeast(1), 2 + rng.nextInt(3)))
        }

        // Corta os pedidos até a matéria-prima caber de fato no tabuleiro.
        val orders = ArrayList<Order>()
        var used = 0
        wanted.forEachIndexed { index, order ->
            val perFruit = 1 shl (if (index == 0) mainDepth else 1)
            var count = order.count
            while (count > 0 && used + count * perFruit > capacity) count--
            if (count > 0) {
                orders += Order(order.level, count)
                used += count * perFruit
            }
        }
        if (orders.isEmpty()) orders += Order(top, 1)

        return Recipe(
            number = n,
            title = names[(n - 1) % names.size],
            orders = orders,
            moves = (20 + n / 3).coerceAtMost(42),
            size = size,
            mainDepth = mainDepth,
            iceBlocks = iceBlocks,
            rottenEvery = when {
                n < 20 -> 0
                n < 40 -> 7
                else -> 5
            },
            seed = n * 104_729L,
        )
    }

    /**
     * Monta o tabuleiro inicial: para cada item do pedido entram exatamente as
     * frutas necessárias para montá-lo, mais um punhado de frutinhas soltas
     * para dar o que manobrar. O gelo só congela peça solta, nunca a
     * matéria-prima — senão a fase poderia travar sem solução.
     */
    fun board(recipe: Recipe, rng: Random): GameState {
        var state = Engine.empty(recipe.size)

        recipe.orders.forEachIndexed { index, order ->
            val depth = if (index == 0) recipe.mainDepth else 1
            val level = (order.level - depth).coerceAtLeast(0)
            repeat(order.count * (1 shl depth)) {
                state = Engine.spawn(state, rng, forcedLevel = level)
            }
        }

        // Frutinhas de manobra: ocupam parte do espaço livre, nunca tudo.
        val loose = (Engine.freeCells(state).size - breathingRoom(recipe.size) - recipe.iceBlocks)
            .coerceIn(0, 3)
        repeat(loose) {
            state = Engine.spawn(state, rng, forcedLevel = rng.nextInt(0, 2))
        }

        // Blocos de gelo: obstáculo puro, entram como peça extra congelada.
        repeat(recipe.iceBlocks) {
            if (Engine.freeCells(state).size <= 2) return@repeat
            val before = state.tiles.map { it.id }.toSet()
            state = Engine.spawn(state, rng, forcedLevel = 0)
            val fresh = state.tiles.firstOrNull { it.id !in before } ?: return@repeat
            state = state.copy(
                tiles = state.tiles - fresh + fresh.copy(ice = 1),
            )
        }

        return state.copy(tiles = state.tiles.map { it.copy(spawned = false) })
    }
}
