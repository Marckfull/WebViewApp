package com.kardiapulse.game.audio

import com.kardiapulse.game.audio.WavSynth.build
import com.kardiapulse.game.audio.WavSynth.midiToHz
import com.kardiapulse.game.audio.WavSynth.noise
import com.kardiapulse.game.audio.WavSynth.pluck
import com.kardiapulse.game.audio.WavSynth.saw
import com.kardiapulse.game.audio.WavSynth.sine
import com.kardiapulse.game.audio.WavSynth.square
import com.kardiapulse.game.audio.WavSynth.triangle
import kotlin.math.exp
import kotlin.math.ln

/**
 * A paleta sonora do jogo. Cada efeito é uma receita curta de síntese.
 */
enum class Sfx(val fileName: String) {
    TAP("sfx_tap.wav"),
    CARD_PLAY("sfx_card.wav"),
    FLIP("sfx_flip.wav"),
    CHAIN("sfx_chain.wav"),
    POWER("sfx_power.wav"),
    OVERLOAD("sfx_overload.wav"),
    WIN("sfx_win.wav"),
    LOSE("sfx_lose.wav"),
    COIN("sfx_coin.wav"),
    ERROR("sfx_error.wav"),
    TICK("sfx_tick.wav"),
    COLLAPSE("sfx_collapse.wav");

    /** Gera as amostras deste efeito. */
    fun render(): ShortArray = when (this) {

        // Toque seco de interface.
        TAP -> build(0.06) { t, _ ->
            sine(880.0 * t) * pluck(t, 0.06, attack = 0.002, decay = 60.0) * 0.5
        }

        // Carta caindo no Núcleo: uma subida curta e cheia.
        CARD_PLAY -> build(0.18) { t, i ->
            val f = sweep(t, 0.18, 330.0, 620.0)
            val body = sine(f * t) * 0.6 + triangle(f * 2 * t) * 0.2
            val air = noise(i) * 0.08 * exp(-40.0 * t)
            (body + air) * pluck(t, 0.18, attack = 0.004, decay = 14.0)
        }

        // Inversão de direção: o som mais dramático do jogo comum.
        FLIP -> build(0.34) { t, _ ->
            val f = sweep(t, 0.34, 220.0, 990.0)
            val body = sine(f * t) * 0.55 + square(f * 0.5 * t) * 0.18
            val shimmer = sine(f * 3.01 * t) * 0.12
            (body + shimmer) * pluck(t, 0.34, attack = 0.006, decay = 7.0)
        }

        // Corrente: arpejo ascendente de três notas.
        CHAIN -> build(0.42) { t, _ ->
            val step = (t / 0.12).toInt().coerceAtMost(2)
            val local = t - step * 0.12
            val hz = midiToHz(72 + step * 4)
            (sine(hz * t) * 0.5 + sine(hz * 2 * t) * 0.15) * pluck(local, 0.14, decay = 16.0)
        }

        // Poder ativado: brilho subindo com um corpo grave.
        POWER -> build(0.5) { t, i ->
            val f = sweep(t, 0.5, 180.0, 1400.0)
            val body = sine(f * t) * 0.4 + sine(f * 1.5 * t) * 0.2
            val sparkle = sine(2600.0 * t + sine(9.0 * t) * 4) * 0.15 * exp(-5.0 * t)
            val air = noise(i) * 0.05 * exp(-9.0 * t)
            (body + sparkle + air) * pluck(t, 0.5, attack = 0.01, decay = 5.0)
        }

        // Sobrecarga: descida grave com estouro.
        OVERLOAD -> build(0.7) { t, i ->
            val f = sweep(t, 0.7, 420.0, 60.0)
            val body = saw(f * t) * 0.35 + sine(f * 0.5 * t) * 0.35
            val burst = noise(i) * 0.4 * exp(-14.0 * t)
            (body + burst) * pluck(t, 0.7, attack = 0.003, decay = 4.0)
        }

        // Vitória: acorde maior arpejado.
        WIN -> build(0.9) { t, _ ->
            val notes = intArrayOf(60, 64, 67, 72)
            var acc = 0.0
            for ((index, note) in notes.withIndex()) {
                val start = index * 0.09
                val local = t - start
                if (local > 0) {
                    val hz = midiToHz(note)
                    acc += (sine(hz * t) * 0.4 + sine(hz * 2 * t) * 0.12) *
                        pluck(local, 0.9, attack = 0.008, decay = 3.2)
                }
            }
            acc * 0.55
        }

        // Derrota: mesma ideia, descendo e em menor.
        LOSE -> build(0.9) { t, _ ->
            val notes = intArrayOf(67, 63, 60, 55)
            var acc = 0.0
            for ((index, note) in notes.withIndex()) {
                val start = index * 0.11
                val local = t - start
                if (local > 0) {
                    val hz = midiToHz(note)
                    acc += (sine(hz * t) * 0.4 + triangle(hz * t) * 0.1) *
                        pluck(local, 0.9, attack = 0.01, decay = 3.0)
                }
            }
            acc * 0.5
        }

        // Moeda / recompensa.
        COIN -> build(0.28) { t, _ ->
            val step = if (t < 0.07) 0 else 1
            val hz = if (step == 0) midiToHz(88) else midiToHz(93)
            val local = t - step * 0.07
            sine(hz * t) * 0.45 * pluck(local, 0.22, decay = 18.0)
        }

        // Jogada inválida.
        ERROR -> build(0.16) { t, _ ->
            val gate = if ((t * 26).toInt() % 2 == 0) 1.0 else 0.25
            square(150.0 * t) * 0.32 * gate * pluck(t, 0.16, attack = 0.002, decay = 12.0)
        }

        // Relógio do modo Blitz.
        TICK -> build(0.04) { t, _ ->
            sine(1500.0 * t) * 0.35 * pluck(t, 0.04, attack = 0.001, decay = 90.0)
        }

        // O campo apertando.
        COLLAPSE -> build(0.45) { t, i ->
            val f = sweep(t, 0.45, 700.0, 300.0)
            val body = sine(f * t) * 0.3 + sine(f * 1.33 * t) * 0.18
            val air = noise(i) * 0.12 * exp(-6.0 * t)
            (body + air) * pluck(t, 0.45, attack = 0.02, decay = 5.5)
        }
    }

    /**
     * Varredura exponencial de frequência. Devolve a frequência *integrada* dividida por t,
     * que é o que a fase precisa para o glissando soar contínuo em vez de picotado.
     */
    private fun sweep(t: Double, duration: Double, from: Double, to: Double): Double {
        if (t <= 0.0) return from
        val k = ln(to / from) / duration
        return from * (exp(k * t) - 1.0) / (k * t)
    }
}
