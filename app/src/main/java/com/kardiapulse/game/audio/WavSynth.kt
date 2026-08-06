package com.kardiapulse.game.audio

import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Sintetizador de áudio.
 *
 * O jogo não carrega nenhum arquivo de som pronto: todos os efeitos e a trilha são calculados
 * na primeira execução e gravados como WAV no cache. Isso mantém o repositório sem binários,
 * o APK menor, e permite ajustar um som mudando uma linha de matemática em vez de reexportar
 * um arquivo.
 */
object WavSynth {

    const val SAMPLE_RATE = 22050

    // ------------------------------------------------------------------ osciladores

    fun sine(phase: Double): Double = sin(2.0 * PI * phase)

    fun square(phase: Double): Double = if (phase % 1.0 < 0.5) 1.0 else -1.0

    fun saw(phase: Double): Double = 2.0 * (phase % 1.0) - 1.0

    fun triangle(phase: Double): Double {
        val p = phase % 1.0
        return if (p < 0.5) 4.0 * p - 1.0 else 3.0 - 4.0 * p
    }

    /** Ruído determinístico: mesmo som a cada geração, sem depender de Random. */
    fun noise(index: Int): Double {
        var x = index * 1103515245 + 12345
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        return ((x and 0xFFFF) / 32768.0) - 1.0
    }

    // ------------------------------------------------------------------ envelopes

    /** Ataque rápido seguido de decaimento exponencial. O envelope de quase tudo aqui. */
    fun pluck(t: Double, duration: Double, attack: Double = 0.005, decay: Double = 6.0): Double {
        if (t < 0 || t > duration) return 0.0
        val a = if (t < attack) t / attack else 1.0
        return a * exp(-decay * t)
    }

    /** Envelope suave para pads: sobe, sustenta, desce. */
    fun pad(t: Double, duration: Double, fade: Double = 0.35): Double {
        if (t < 0 || t > duration) return 0.0
        val up = (t / fade).coerceAtMost(1.0)
        val down = ((duration - t) / fade).coerceAtMost(1.0)
        return (up * down).coerceIn(0.0, 1.0)
    }

    // ------------------------------------------------------------------ construção

    /**
     * Constrói um buffer PCM. [block] recebe o tempo em segundos e o índice da amostra, e
     * devolve um valor entre -1 e 1.
     */
    fun build(durationSeconds: Double, block: (t: Double, index: Int) -> Double): ShortArray {
        val count = (durationSeconds * SAMPLE_RATE).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE
            val v = block(t, i).coerceIn(-1.0, 1.0)
            out[i] = (v * 32000).toInt().toShort()
        }
        return out
    }

    /** Suaviza os dois extremos para o loop da música não estalar na emenda. */
    fun smoothEdges(samples: ShortArray, fadeSamples: Int = 512): ShortArray {
        val n = minOf(fadeSamples, samples.size / 2)
        for (i in 0 until n) {
            val gain = i.toDouble() / n
            samples[i] = (samples[i] * gain).toInt().toShort()
            samples[samples.size - 1 - i] = (samples[samples.size - 1 - i] * gain).toInt().toShort()
        }
        return samples
    }

    /** Converte um nome de nota MIDI em frequência. 69 = lá 440 Hz. */
    fun midiToHz(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    // ------------------------------------------------------------------ arquivo

    fun writeWav(file: File, samples: ShortArray, sampleRate: Int = SAMPLE_RATE) {
        val dataSize = samples.size * 2
        val header = ByteArray(44)
        fun putAscii(offset: Int, text: String) {
            for (i in text.indices) header[offset + i] = text[i].code.toByte()
        }
        fun putInt(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
            header[offset + 2] = ((value shr 16) and 0xFF).toByte()
            header[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }
        fun putShort(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }

        putAscii(0, "RIFF")
        putInt(4, 36 + dataSize)
        putAscii(8, "WAVE")
        putAscii(12, "fmt ")
        putInt(16, 16)          // tamanho do bloco fmt
        putShort(20, 1)         // PCM
        putShort(22, 1)         // mono
        putInt(24, sampleRate)
        putInt(28, sampleRate * 2)
        putShort(32, 2)         // block align
        putShort(34, 16)        // bits por amostra
        putAscii(36, "data")
        putInt(40, dataSize)

        val body = ByteArray(dataSize)
        for (i in samples.indices) {
            val v = samples[i].toInt()
            body[i * 2] = (v and 0xFF).toByte()
            body[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }

        FileOutputStream(file).use { stream ->
            stream.write(header)
            stream.write(body)
        }
    }
}
