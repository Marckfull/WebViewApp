package com.kardiapulse.game.audio

import com.kardiapulse.game.audio.WavSynth.build
import com.kardiapulse.game.audio.WavSynth.midiToHz
import com.kardiapulse.game.audio.WavSynth.pad
import com.kardiapulse.game.audio.WavSynth.pluck
import com.kardiapulse.game.audio.WavSynth.sine
import com.kardiapulse.game.audio.WavSynth.smoothEdges
import com.kardiapulse.game.audio.WavSynth.triangle
import kotlin.math.exp
import kotlin.math.sin

/**
 * As trilhas do jogo, também sintetizadas.
 *
 * Cada trilha é um loop curto e sem emenda: pads sustentados por cima de uma progressão de
 * quatro acordes, com um arpejo por cima e um pulso grave marcando o tempo. O suficiente para
 * dar clima sem cansar em uma sessão longa.
 */
enum class MusicTrack(val fileName: String, val barSeconds: Double, val bars: Int) {

    /** Menu: lento, arejado, quase parado. */
    MENU("music_menu.wav", barSeconds = 4.8, bars = 4),

    /** Duelo: mesma harmonia, mais movimento e um pulso presente. */
    DUELO("music_duel.wav", barSeconds = 3.6, bars = 4);

    val durationSeconds: Double get() = barSeconds * bars

    /** Progressão em lá menor: Am - F - C - G. Raízes em notas MIDI. */
    private val roots: IntArray get() = intArrayOf(45, 41, 48, 43)

    /** Tríades correspondentes, em intervalos a partir da raiz. */
    private val triads: Array<IntArray>
        get() = arrayOf(
            intArrayOf(0, 3, 7),   // menor
            intArrayOf(0, 4, 7),   // maior
            intArrayOf(0, 4, 7),   // maior
            intArrayOf(0, 4, 7)    // maior
        )

    fun render(): ShortArray {
        val isDuel = this == DUELO
        val total = durationSeconds
        val samples = build(total) { t, i ->
            val barIndex = ((t / barSeconds).toInt()) % bars
            val localT = t - barIndex * barSeconds
            val root = roots[barIndex]
            val triad = triads[barIndex]

            // --- Pad: a tríade sustentada, levemente desafinada para ganhar largura.
            var padVoice = 0.0
            for (interval in triad) {
                val hz = midiToHz(root + 24 + interval)
                padVoice += sine(hz * t) * 0.30
                padVoice += sine(hz * 1.004 * t) * 0.22   // detune
            }
            padVoice *= pad(localT, barSeconds, fade = barSeconds * 0.28) * 0.13

            // --- Baixo: a raiz, com um leve vibrato.
            val bassHz = midiToHz(root)
            val bass = (sine(bassHz * t) * 0.5 + triangle(bassHz * t) * 0.12) *
                pad(localT, barSeconds, fade = 0.5) * 0.20

            // --- Arpejo: uma nota da tríade por subdivisão.
            val stepLength = if (isDuel) barSeconds / 8 else barSeconds / 4
            val step = (localT / stepLength).toInt()
            val stepLocal = localT - step * stepLength
            val octave = if (isDuel && step % 4 == 3) 12 else 0
            val arpHz = midiToHz(root + 36 + triad[step % triad.size] + octave)
            val arp = (sine(arpHz * t) * 0.4 + sine(arpHz * 2 * t) * 0.1) *
                pluck(stepLocal, stepLength, attack = 0.01, decay = if (isDuel) 9.0 else 5.0) *
                (if (isDuel) 0.16 else 0.11)

            // --- Pulso grave: só no duelo, marcando o tempo.
            val beat = if (isDuel) {
                val beatLength = barSeconds / 4
                val beatLocal = localT - (localT / beatLength).toInt() * beatLength
                sine(58.0 * t) * 0.5 * exp(-22.0 * beatLocal) * 0.22
            } else 0.0

            // --- Respiração lenta no volume geral, para o loop não soar mecânico.
            val breathe = 0.92 + 0.08 * sin(2.0 * Math.PI * t / total)

            (padVoice + bass + arp + beat) * breathe
        }
        return smoothEdges(samples, fadeSamples = 1200)
    }
}
