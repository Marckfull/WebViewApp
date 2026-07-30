package com.neuroflip.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class Sfx { FLIP, MATCH, MISMATCH, COMBO, ECO, MUTATION, OVERLOAD, WIN, LOSE, COIN, TAP, TICK }

/**
 * Efeitos sonoros do jogo. Os WAVs são sintetizados uma única vez no cache
 * e carregados num SoundPool (permite sons sobrepostos e variação de pitch).
 */
class SoundEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val soundIds = ConcurrentHashMap<Sfx, Int>()
    private var pool: SoundPool? = null

    @Volatile
    var enabled: Boolean = true

    fun warmUp() {
        if (pool != null) return
        pool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        scope.launch {
            val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
            Sfx.entries.forEach { sfx ->
                runCatching {
                    val file = File(dir, "${sfx.name.lowercase()}_v1.wav")
                    if (!file.exists()) {
                        val samples = render(sfx)
                        Synth.normalize(samples, peak = 0.85f)
                        Synth.writeWav(file, samples)
                    }
                    pool?.load(file.absolutePath, 1)?.let { soundIds[sfx] = it }
                }
            }
        }
    }

    fun play(sfx: Sfx, volume: Float = 1f, rate: Float = 1f) {
        if (!enabled) return
        val id = soundIds[sfx] ?: return
        pool?.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    /** Combo sobe de tom conforme a sequência cresce — reforço positivo audível. */
    fun playCombo(comboLevel: Int) {
        val rate = (1f + (comboLevel - 1) * 0.12f).coerceAtMost(2f)
        play(Sfx.COMBO, volume = 0.9f, rate = rate)
    }

    fun release() {
        pool?.release()
        pool = null
        soundIds.clear()
    }

    // ------------------------------------------------------------------ síntese

    private fun render(sfx: Sfx): FloatArray = when (sfx) {
        // Clique curto e seco com um "whoosh" de carta virando.
        Sfx.FLIP -> buffer(140) { out ->
            Synth.mix(out, 0, Synth.tone(90, 900.0, 1500.0, decay = 9.0, wave = Synth::triangle), 0.5)
            Synth.mix(out, 0, Synth.noiseBurst(120, decay = 18.0, seed = 3), 0.25)
        }

        // Acerto: intervalo de quinta ascendente (dó–sol) bem brilhante.
        Sfx.MATCH -> buffer(420) { out ->
            Synth.mix(out, 0, Synth.tone(260, Synth.note(72), decay = 4.0), 0.45)
            Synth.mix(out, 40 * Synth.SAMPLE_RATE / 1000, Synth.tone(300, Synth.note(79), decay = 4.0), 0.4)
            Synth.mix(out, 90 * Synth.SAMPLE_RATE / 1000, Synth.tone(320, Synth.note(84), decay = 5.0, wave = Synth::triangle), 0.3)
        }

        // Erro: descida curta, grave, sem ser punitiva.
        Sfx.MISMATCH -> buffer(300) { out ->
            Synth.mix(out, 0, Synth.tone(240, 320.0, 150.0, decay = 5.0, wave = Synth::square), 0.3)
            Synth.mix(out, 0, Synth.noiseBurst(160, decay = 14.0, seed = 11), 0.15)
        }

        // Combo: arpejo rápido ascendente.
        Sfx.COMBO -> buffer(420) { out ->
            Synth.mix(out, 0, Synth.arpeggio(listOf(76, 80, 83, 88), stepMs = 65, noteMs = 150), 0.4)
        }

        // Pulso ECO: ping etéreo com vibrato.
        Sfx.ECO -> buffer(360) { out ->
            Synth.mix(out, 0, Synth.tone(340, Synth.note(88), decay = 3.0, vibrato = 0.02), 0.28)
            Synth.mix(out, 60 * Synth.SAMPLE_RATE / 1000, Synth.tone(280, Synth.note(95), decay = 4.0), 0.16)
        }

        // Mutação: sirene curta de "atenção, o tabuleiro mudou".
        Sfx.MUTATION -> buffer(460) { out ->
            Synth.mix(out, 0, Synth.tone(220, 600.0, 300.0, decay = 3.0, wave = Synth::saw), 0.25)
            Synth.mix(out, 200 * Synth.SAMPLE_RATE / 1000, Synth.tone(220, 300.0, 620.0, decay = 3.0, wave = Synth::saw), 0.25)
        }

        // Sobrecarga: acorde grande e energético.
        Sfx.OVERLOAD -> buffer(900) { out ->
            listOf(60, 67, 72, 76, 79).forEachIndexed { i, midi ->
                Synth.mix(
                    out,
                    i * 40 * Synth.SAMPLE_RATE / 1000,
                    Synth.tone(700, Synth.note(midi), decay = 2.5, wave = Synth::triangle),
                    0.22
                )
            }
            Synth.mix(out, 0, Synth.noiseBurst(300, decay = 6.0, seed = 21), 0.12)
        }

        // Vitória: fanfarra curta em modo maior.
        Sfx.WIN -> buffer(1_200) { out ->
            Synth.mix(out, 0, Synth.arpeggio(listOf(72, 76, 79, 84, 88), stepMs = 130, noteMs = 320), 0.42)
            Synth.mix(out, 650 * Synth.SAMPLE_RATE / 1000, Synth.tone(520, Synth.note(91), decay = 3.0), 0.28)
        }

        // Derrota: queda cromática discreta.
        Sfx.LOSE -> buffer(900) { out ->
            Synth.mix(out, 0, Synth.arpeggio(listOf(72, 69, 65, 60), stepMs = 170, noteMs = 300, decay = 4.0), 0.32)
        }

        // Moeda: dois pings metálicos.
        Sfx.COIN -> buffer(300) { out ->
            Synth.mix(out, 0, Synth.tone(120, Synth.note(93), decay = 8.0), 0.35)
            Synth.mix(out, 70 * Synth.SAMPLE_RATE / 1000, Synth.tone(200, Synth.note(100), decay = 6.0), 0.28)
        }

        // Toque de interface.
        Sfx.TAP -> buffer(110) { out ->
            Synth.mix(out, 0, Synth.tone(80, 1_200.0, 900.0, decay = 12.0, wave = Synth::triangle), 0.3)
        }

        // Tique do cronômetro nos últimos segundos.
        Sfx.TICK -> buffer(120) { out ->
            Synth.mix(out, 0, Synth.tone(70, 1_600.0, 1_200.0, decay = 16.0, wave = Synth::square), 0.22)
        }
    }

    private inline fun buffer(durationMs: Int, block: (FloatArray) -> Unit): FloatArray {
        val out = FloatArray(durationMs * Synth.SAMPLE_RATE / 1000)
        block(out)
        return out
    }
}
