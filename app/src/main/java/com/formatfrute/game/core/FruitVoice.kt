package com.formatfrute.game.core

import androidx.annotation.StringRes
import com.formatfrute.game.R
import kotlin.random.Random

/**
 * A voz das frutas. A arte já é expressiva — aqui ela ganha texto.
 *
 * Devolve o recurso, não a frase: quem resolve é quem tem Context (o
 * ViewModel), e assim traduzir o jogo não passa por aqui.
 */
object FruitVoice {

    private val arrival: Map<Fruit, IntArray> = mapOf(
        Fruit.CEREJA to intArrayOf(R.string.voice_cereja_1, R.string.voice_cereja_2),
        Fruit.MORANGO to intArrayOf(R.string.voice_morango_1, R.string.voice_morango_2),
        Fruit.UVA to intArrayOf(R.string.voice_uva_1, R.string.voice_uva_2),
        Fruit.LIMAO to intArrayOf(R.string.voice_limao_1, R.string.voice_limao_2),
        Fruit.BANANA to intArrayOf(R.string.voice_banana_1, R.string.voice_banana_2),
        Fruit.MACA to intArrayOf(R.string.voice_maca_1, R.string.voice_maca_2),
        Fruit.LARANJA to intArrayOf(R.string.voice_laranja_1, R.string.voice_laranja_2),
        Fruit.PERA to intArrayOf(R.string.voice_pera_1, R.string.voice_pera_2),
        Fruit.KIWI to intArrayOf(R.string.voice_kiwi_1, R.string.voice_kiwi_2),
        Fruit.ABACAXI to intArrayOf(R.string.voice_abacaxi_1, R.string.voice_abacaxi_2),
        Fruit.PITAYA to intArrayOf(R.string.voice_pitaya_1, R.string.voice_pitaya_2),
        Fruit.MELANCIA to intArrayOf(
            R.string.voice_melancia_1,
            R.string.voice_melancia_2,
            R.string.voice_melancia_3,
        ),
    )

    private val combo = intArrayOf(
        R.string.voice_combo_1, R.string.voice_combo_2, R.string.voice_combo_3,
        R.string.voice_combo_4, R.string.voice_combo_5,
    )

    private val boss = intArrayOf(
        R.string.voice_boss_1, R.string.voice_boss_2, R.string.voice_boss_3,
        R.string.voice_boss_4, R.string.voice_boss_5,
    )

    private val greeting = intArrayOf(
        R.string.voice_hello_1, R.string.voice_hello_2, R.string.voice_hello_3,
        R.string.voice_hello_4, R.string.voice_hello_5, R.string.voice_hello_6,
    )

    private fun pick(list: IntArray, seed: Long): Int = list[Random(seed).nextInt(list.size)]

    /** Fala de estreia da fruta na partida. Só vale a pena do limão pra cima. */
    @StringRes
    fun onArrival(fruit: Fruit, seed: Long): Int? {
        if (fruit.ordinal < Fruit.LIMAO.ordinal) return null
        val lines = arrival[fruit] ?: return null
        return pick(lines, seed)
    }

    @StringRes
    fun onCombo(seed: Long): Int = pick(combo, seed)

    @StringRes
    fun onBoss(seed: Long): Int = pick(boss, seed)

    /** A "fruta do dia" que cumprimenta o jogador na tela inicial. */
    fun greetingOfTheDay(day: String): Pair<Fruit, Int> {
        val seed = day.hashCode().toLong()
        val rng = Random(seed)
        val fruit = Fruit.entries[rng.nextInt(Fruit.entries.size)]
        return fruit to greeting[rng.nextInt(greeting.size)]
    }
}
