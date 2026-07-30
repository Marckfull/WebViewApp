package com.neuroflip.game.audio

import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sintetizador simples em Kotlin puro.
 *
 * Todo o áudio do NeuroFlip (efeitos e trilha) é GERADO em tempo de execução e
 * gravado como WAV no cache do app. Assim não há nenhum arquivo de áudio
 * licenciado no repositório — som 100% original e gratuito.
 */
object Synth {

    const val SAMPLE_RATE = 22_050

    // ------------------------------------------------------------------ osciladores

    fun sine(phase: Double) = sin(phase * 2 * PI)

    fun triangle(phase: Double): Double {
        val t = phase % 1.0
        return 4.0 * kotlin.math.abs(t - 0.5) - 1.0
    }

    fun square(phase: Double, duty: Double = 0.5) = if ((phase % 1.0) < duty) 1.0 else -1.0

    fun saw(phase: Double) = 2.0 * (phase % 1.0) - 1.0

    // ------------------------------------------------------------------ envelopes

    /** Envelope percussivo (pluck): ataque muito curto e decaimento exponencial. */
    fun pluck(t: Double, duration: Double, decay: Double = 6.0): Double {
        if (t < 0 || t > duration) return 0.0
        val attack = 0.005
        val a = if (t < attack) t / attack else 1.0
        return a * exp(-decay * t / duration)
    }

    fun adsr(
        t: Double,
        duration: Double,
        attack: Double = 0.02,
        release: Double = 0.15
    ): Double {
        if (t < 0 || t > duration) return 0.0
        return when {
            t < attack -> t / attack
            t > duration - release -> ((duration - t) / release).coerceIn(0.0, 1.0)
            else -> 1.0
        }
    }

    // ------------------------------------------------------------------ utilidades

    /** Frequência de uma nota MIDI (69 = A4 = 440Hz). */
    fun note(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    fun mix(target: FloatArray, offsetSamples: Int, source: DoubleArray, gain: Double) {
        for (i in source.indices) {
            val idx = offsetSamples + i
            if (idx < 0 || idx >= target.size) continue
            target[idx] = (target[idx] + source[i] * gain).toFloat()
        }
    }

    fun normalize(samples: FloatArray, peak: Float = 0.92f) {
        var maxAbs = 0f
        for (s in samples) maxAbs = max(maxAbs, kotlin.math.abs(s))
        if (maxAbs <= 0.0001f) return
        val factor = peak / maxAbs
        for (i in samples.indices) samples[i] *= factor
    }

    /** Filtro passa-baixa de 1 polo — deixa os timbres menos agressivos. */
    fun lowPass(samples: DoubleArray, cutoffHz: Double, sampleRate: Int = SAMPLE_RATE): DoubleArray {
        val rc = 1.0 / (2 * PI * cutoffHz)
        val dt = 1.0 / sampleRate
        val alpha = dt / (rc + dt)
        var last = 0.0
        val out = DoubleArray(samples.size)
        for (i in samples.indices) {
            last += alpha * (samples[i] - last)
            out[i] = last
        }
        return out
    }

    // ------------------------------------------------------------------ blocos sonoros

    /** Tom com glissando opcional (usado nos efeitos de virar/acertar carta). */
    fun tone(
        durationMs: Int,
        fromHz: Double,
        toHz: Double = fromHz,
        decay: Double = 5.0,
        wave: (Double) -> Double = ::sine,
        vibrato: Double = 0.0
    ): DoubleArray {
        val n = durationMs * SAMPLE_RATE / 1000
        val duration = durationMs / 1000.0
        val out = DoubleArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val p = t / duration
            var freq = fromHz + (toHz - fromHz) * p
            if (vibrato > 0) freq *= 1.0 + vibrato * sin(2 * PI * 6.0 * t)
            phase += freq / SAMPLE_RATE
            out[i] = wave(phase) * pluck(t, duration, decay)
        }
        return out
    }

    fun noiseBurst(durationMs: Int, decay: Double = 12.0, seed: Int = 7): DoubleArray {
        val n = durationMs * SAMPLE_RATE / 1000
        val duration = durationMs / 1000.0
        val rnd = Random(seed)
        val raw = DoubleArray(n) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            (rnd.nextDouble() * 2 - 1) * pluck(t, duration, decay)
        }
        return lowPass(raw, 4_000.0)
    }

    fun arpeggio(
        midiNotes: List<Int>,
        stepMs: Int,
        noteMs: Int = stepMs,
        wave: (Double) -> Double = ::triangle,
        decay: Double = 5.0
    ): DoubleArray {
        val totalMs = stepMs * midiNotes.size + noteMs
        val out = DoubleArray(totalMs * SAMPLE_RATE / 1000)
        midiNotes.forEachIndexed { index, midi ->
            val partial = tone(noteMs, note(midi), decay = decay, wave = wave)
            val offset = index * stepMs * SAMPLE_RATE / 1000
            for (i in partial.indices) {
                val idx = offset + i
                if (idx < out.size) out[idx] += partial[i] * 0.7
            }
        }
        return out
    }

    // ------------------------------------------------------------------ gravação WAV

    fun writeWav(file: File, samples: FloatArray, sampleRate: Int = SAMPLE_RATE) {
        val byteCount = samples.size * 2
        FileOutputStream(file).use { out ->
            out.write("RIFF".toByteArray())
            out.write(intLe(36 + byteCount))
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            out.write(intLe(16))              // tamanho do bloco fmt
            out.write(shortLe(1))             // PCM
            out.write(shortLe(1))             // mono
            out.write(intLe(sampleRate))
            out.write(intLe(sampleRate * 2))  // byte rate
            out.write(shortLe(2))             // block align
            out.write(shortLe(16))            // bits por amostra
            out.write("data".toByteArray())
            out.write(intLe(byteCount))

            val buffer = ByteArray(byteCount)
            for (i in samples.indices) {
                val v = (min(1f, max(-1f, samples[i])) * 32_767f).toInt()
                buffer[i * 2] = (v and 0xFF).toByte()
                buffer[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
            }
            out.write(buffer)
        }
    }

    private fun intLe(value: Int) = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte()
    )

    private fun shortLe(value: Int) = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte()
    )
}
