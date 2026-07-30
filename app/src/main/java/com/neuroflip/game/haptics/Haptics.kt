package com.neuroflip.game.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class Buzz { LIGHT, MEDIUM, HEAVY, SUCCESS, ERROR, COMBO, OVERLOAD, WIN, LOSE }

/**
 * Vibração com padrões diferentes por evento — o jogo "conversa" pelo tato.
 * Degrada com elegância em aparelhos sem amplitude controlável.
 */
class Haptics(context: Context) {

    @Volatile
    var enabled: Boolean = true

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private val hasAmplitudeControl: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator?.hasAmplitudeControl() == true

    fun buzz(kind: Buzz) {
        if (!enabled) return
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        runCatching {
            when (kind) {
                Buzz.LIGHT -> oneShot(12, 60)
                Buzz.MEDIUM -> oneShot(24, 130)
                Buzz.HEAVY -> oneShot(45, 220)
                Buzz.SUCCESS -> pattern(longArrayOf(0, 18, 40, 30), intArrayOf(0, 120, 0, 200))
                Buzz.ERROR -> pattern(longArrayOf(0, 40, 60, 40), intArrayOf(0, 90, 0, 90))
                Buzz.COMBO -> pattern(
                    longArrayOf(0, 14, 30, 14, 30, 22),
                    intArrayOf(0, 110, 0, 160, 0, 220)
                )
                Buzz.OVERLOAD -> pattern(
                    longArrayOf(0, 60, 40, 90),
                    intArrayOf(0, 180, 0, 255)
                )
                Buzz.WIN -> pattern(
                    longArrayOf(0, 30, 60, 30, 60, 70),
                    intArrayOf(0, 140, 0, 190, 0, 255)
                )
                Buzz.LOSE -> pattern(longArrayOf(0, 120, 80, 200), intArrayOf(0, 200, 0, 120))
            }
        }
    }

    private fun oneShot(durationMs: Long, amplitude: Int) {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amp = if (hasAmplitudeControl) amplitude.coerceIn(1, 255) else VibrationEffect.DEFAULT_AMPLITUDE
            vibrate(VibrationEffect.createOneShot(durationMs, amp))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(durationMs)
        }
    }

    private fun pattern(timings: LongArray, amplitudes: IntArray) {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = if (hasAmplitudeControl) {
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            } else {
                VibrationEffect.createWaveform(timings, -1)
            }
            vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(timings, -1)
        }
    }

    private fun vibrate(effect: VibrationEffect) {
        vibrator?.vibrate(effect)
    }

    fun cancel() {
        runCatching { vibrator?.cancel() }
    }
}
