package com.formatfrute.game.core

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.formatfrute.game.R

/**
 * A escada de evolucao do Format Frute. Cada nivel vale o dobro do anterior,
 * exatamente como a mecanica classica, mas quem sobe de nivel e a fruta.
 *
 * O nome vem de `res/values/strings.xml`: nenhum texto de tela mora no Kotlin,
 * entao traduzir o jogo e copiar um arquivo XML.
 */
enum class Fruit(
    @StringRes val label: Int,
    @DrawableRes val art: Int,
    val skin: Color,
    val glow: Color,
) {
    CEREJA(R.string.fruit_cereja, R.drawable.fruit_cereja, Color(0xFFE01E45), Color(0xFFFF8FA8)),
    MORANGO(R.string.fruit_morango, R.drawable.fruit_morango, Color(0xFFF0334A), Color(0xFFFF93A2)),
    UVA(R.string.fruit_uva, R.drawable.fruit_uva, Color(0xFF6B4FA8), Color(0xFFB9A3F0)),
    LIMAO(R.string.fruit_limao, R.drawable.fruit_limao, Color(0xFFF3C81F), Color(0xFFFFE98A)),
    BANANA(R.string.fruit_banana, R.drawable.fruit_banana, Color(0xFFF5C518), Color(0xFFFFE47A)),
    MACA(R.string.fruit_maca, R.drawable.fruit_maca, Color(0xFFE8202A), Color(0xFFFF8A8F)),
    LARANJA(R.string.fruit_laranja, R.drawable.fruit_laranja, Color(0xFFF58220), Color(0xFFFFC182)),
    PERA(R.string.fruit_pera, R.drawable.fruit_pera, Color(0xFFA8CE4A), Color(0xFFDDF39B)),
    KIWI(R.string.fruit_kiwi, R.drawable.fruit_kiwi, Color(0xFF8B5E3C), Color(0xFFD4A87F)),
    ABACAXI(R.string.fruit_abacaxi, R.drawable.fruit_abacaxi, Color(0xFFF2A81D), Color(0xFFFFD780)),
    PITAYA(R.string.fruit_pitaya, R.drawable.fruit_pitaya, Color(0xFFE0197B), Color(0xFFFF83C0)),
    MELANCIA(R.string.fruit_melancia, R.drawable.fruit_melancia, Color(0xFF2E9E4A), Color(0xFF95E5A6));

    /** Valor classico da peca: cereja = 2, morango = 4, uva = 8... melancia = 4096. */
    val value: Int get() = 1 shl (ordinal + 1)

    companion object {
        val MAX = entries.lastIndex

        fun of(level: Int): Fruit = entries[level.coerceIn(0, MAX)]
    }
}
