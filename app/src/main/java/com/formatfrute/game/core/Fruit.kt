package com.formatfrute.game.core

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.formatfrute.game.R

/**
 * A escada de evolucao do Format Frute. Cada nivel vale o dobro do anterior,
 * exatamente como a mecanica classica, mas quem sobe de nivel e a fruta.
 */
enum class Fruit(
    val label: String,
    @DrawableRes val art: Int,
    val skin: Color,
    val glow: Color,
) {
    CEREJA("Cereja", R.drawable.fruit_cereja, Color(0xFFE01E45), Color(0xFFFF8FA8)),
    MORANGO("Morango", R.drawable.fruit_morango, Color(0xFFF0334A), Color(0xFFFF93A2)),
    UVA("Uva", R.drawable.fruit_uva, Color(0xFF6B4FA8), Color(0xFFB9A3F0)),
    LIMAO("Limão", R.drawable.fruit_limao, Color(0xFFF3C81F), Color(0xFFFFE98A)),
    BANANA("Banana", R.drawable.fruit_banana, Color(0xFFF5C518), Color(0xFFFFE47A)),
    MACA("Maçã", R.drawable.fruit_maca, Color(0xFFE8202A), Color(0xFFFF8A8F)),
    LARANJA("Laranja", R.drawable.fruit_laranja, Color(0xFFF58220), Color(0xFFFFC182)),
    PERA("Pera", R.drawable.fruit_pera, Color(0xFFA8CE4A), Color(0xFFDDF39B)),
    KIWI("Kiwi", R.drawable.fruit_kiwi, Color(0xFF8B5E3C), Color(0xFFD4A87F)),
    ABACAXI("Abacaxi", R.drawable.fruit_abacaxi, Color(0xFFF2A81D), Color(0xFFFFD780)),
    PITAYA("Pitaya", R.drawable.fruit_pitaya, Color(0xFFE0197B), Color(0xFFFF83C0)),
    MELANCIA("Melancia", R.drawable.fruit_melancia, Color(0xFF2E9E4A), Color(0xFF95E5A6));

    /** Valor classico da peca: cereja = 2, morango = 4, uva = 8... melancia = 4096. */
    val value: Int get() = 1 shl (ordinal + 1)

    companion object {
        val MAX = entries.lastIndex

        fun of(level: Int): Fruit = entries[level.coerceIn(0, MAX)]
    }
}
