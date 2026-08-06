package com.kardiapulse.game.haptics

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Vibração. Cada gesto do jogo tem um toque próprio, o que faz muita diferença na sensação
 * de peso das cartas.
 *
 * Em aparelhos com API 26+ usamos amplitude controlada; abaixo disso cai no padrão de duração.
 */
class Haptics(context: Context) {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    var enabled: Boolean = true

    private val available: Boolean get() = vibrator?.hasVibrator() == true

    fun perform(pattern: HapticPattern) {
        if (!enabled || !available) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, -1)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    vibrator?.vibrate(effect)
                } else {
                    vibrator?.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern.timings, -1)
            }
        }
    }

    fun cancel() {
        runCatching { vibrator?.cancel() }
    }
}

/**
 * Os padrões de vibração. `timings` alterna espera/vibração começando por uma espera de 0.
 */
enum class HapticPattern(val timings: LongArray, val amplitudes: IntArray) {

    /** Toque leve de interface. */
    TAP(longArrayOf(0, 12), intArrayOf(0, 70)),

    /** Carta encostando no Núcleo. */
    CARD(longArrayOf(0, 18), intArrayOf(0, 130)),

    /** Inversão de direção: dois toques, o segundo mais forte. */
    FLIP(longArrayOf(0, 14, 40, 26), intArrayOf(0, 110, 0, 200)),

    /** Corrente crescendo. */
    CHAIN(longArrayOf(0, 10, 30, 10, 30, 20), intArrayOf(0, 90, 0, 130, 0, 180)),

    /** Poder ativado. */
    POWER(longArrayOf(0, 30, 25, 45), intArrayOf(0, 150, 0, 220)),

    /** Sobrecarga: o baque. */
    OVERLOAD(longArrayOf(0, 60, 45, 110), intArrayOf(0, 200, 0, 255)),

    /** Vitória. */
    WIN(longArrayOf(0, 22, 45, 22, 45, 70), intArrayOf(0, 140, 0, 180, 0, 230)),

    /** Derrota. */
    LOSE(longArrayOf(0, 140), intArrayOf(0, 190)),

    /** Jogada inválida. */
    ERROR(longArrayOf(0, 26, 50, 26), intArrayOf(0, 180, 0, 180)),

    /** O campo apertando. */
    COLLAPSE(longArrayOf(0, 8, 20, 8, 20, 8), intArrayOf(0, 60, 0, 90, 0, 120)),

    /** Recompensa recebida. */
    REWARD(longArrayOf(0, 16, 30, 16, 30, 16, 30, 40), intArrayOf(0, 100, 0, 140, 0, 180, 0, 220))
}
