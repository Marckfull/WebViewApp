package com.formatfrute.game.audio

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.formatfrute.game.data.GameRepository

/**
 * Vibracao com intencao: cada evento do jogo tem a sua "assinatura" no dedo.
 */
class Haptics private constructor(context: Context) {

    private val app = context.applicationContext
    private val repo = GameRepository.get(app)

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = app.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private val enabled: Boolean get() = repo.current.vibration && vibrator?.hasVibrator() == true

    fun tick() = oneShot(12, 60)

    fun tap() = oneShot(18, 110)

    fun merge(level: Int) = oneShot(16L + level * 3L, (90 + level * 12).coerceAtMost(255))

    fun power() = pattern(longArrayOf(0, 18, 40, 26), intArrayOf(0, 150, 0, 210))

    fun win() = pattern(
        longArrayOf(0, 40, 60, 40, 60, 120),
        intArrayOf(0, 180, 0, 210, 0, 255),
    )

    fun lose() = pattern(longArrayOf(0, 90, 70, 160), intArrayOf(0, 120, 0, 80))

    fun harvest() = pattern(longArrayOf(0, 30, 30, 30, 30, 90), intArrayOf(0, 200, 0, 220, 0, 255))

    fun error() = oneShot(45, 90)

    private fun oneShot(ms: Long, amplitude: Int) {
        if (!enabled) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                emit(VibrationEffect.createOneShot(ms, amplitude.coerceIn(1, 255)))
            } else {
                legacy(longArrayOf(0, ms))
            }
        }
    }

    private fun pattern(timings: LongArray, amplitudes: IntArray) {
        if (!enabled) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                emit(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                legacy(timings)
            }
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private fun emit(effect: VibrationEffect) {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = app.getSystemService(VibratorManager::class.java)
            if (manager != null) {
                manager.vibrate(CombinedVibration.createParallel(effect))
                return
            }
        }
        vib.vibrate(effect)
    }

    /** Aparelhos anteriores ao Android 8 nao tem controle de amplitude. */
    @Suppress("DEPRECATION")
    private fun legacy(timings: LongArray) {
        vibrator?.vibrate(timings, -1)
    }

    companion object {
        @Volatile
        private var instance: Haptics? = null

        fun get(context: Context): Haptics =
            instance ?: synchronized(this) {
                instance ?: Haptics(context).also { instance = it }
            }
    }
}
