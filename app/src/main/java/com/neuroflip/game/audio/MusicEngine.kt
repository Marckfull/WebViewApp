package com.neuroflip.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class MusicTrack { MENU, GAME, TENSE }

/**
 * Trilha sonora procedural. Cada faixa é composta por código (acordes, arpejos,
 * baixo e percussão sintetizada), gravada como WAV no cache e tocada em loop.
 *
 * Vantagens: nenhum arquivo de música com direitos autorais no projeto,
 * APK pequeno e trilha original.
 */
class MusicEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var player: MediaPlayer? = null
    private var currentTrack: MusicTrack? = null

    @Volatile
    var enabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                pause()
            } else {
                currentTrack?.let { play(it) }
            }
        }

    var volume: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            runCatching { player?.setVolume(field, field) }
        }

    fun play(track: MusicTrack) {
        if (!enabled) {
            currentTrack = track
            return
        }
        if (currentTrack == track && player?.isPlaying == true) return
        currentTrack = track
        scope.launch {
            val file = ensureFile(track) ?: return@launch
            withContext(Dispatchers.Main) {
                if (currentTrack != track) return@withContext
                runCatching {
                    release()
                    player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        setDataSource(file.absolutePath)
                        isLooping = true
                        setVolume(this@MusicEngine.volume, this@MusicEngine.volume)
                        setOnPreparedListener { it.start() }
                        prepareAsync()
                    }
                }
            }
        }
    }

    fun pause() {
        runCatching { if (player?.isPlaying == true) player?.pause() }
    }

    fun resume() {
        if (!enabled) return
        val p = player
        if (p == null) currentTrack?.let { play(it) } else runCatching { p.start() }
    }

    fun release() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
    }

    // ------------------------------------------------------------------ composição

    private suspend fun ensureFile(track: MusicTrack): File? = withContext(Dispatchers.Default) {
        runCatching {
            val dir = File(context.cacheDir, "music").apply { mkdirs() }
            val file = File(dir, "${track.name.lowercase()}_v1.wav")
            if (!file.exists()) {
                val samples = when (track) {
                    MusicTrack.MENU -> renderMenu()
                    MusicTrack.GAME -> renderGame()
                    MusicTrack.TENSE -> renderTense()
                }
                Synth.normalize(samples, peak = 0.72f)
                Synth.writeWav(file, samples)
            }
            file
        }.getOrNull()
    }

    /** Pad sustentado com duas ondas levemente desafinadas (som "quente"). */
    private fun pad(durationMs: Int, freq: Double): DoubleArray {
        val n = durationMs * Synth.SAMPLE_RATE / 1000
        val duration = durationMs / 1000.0
        val out = DoubleArray(n)
        var p1 = 0.0
        var p2 = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / Synth.SAMPLE_RATE
            p1 += freq / Synth.SAMPLE_RATE
            p2 += freq * 1.005 / Synth.SAMPLE_RATE
            val env = Synth.adsr(t, duration, attack = 0.55, release = 0.9)
            out[i] = (Synth.sine(p1) * 0.6 + Synth.triangle(p2) * 0.4) * env
        }
        return Synth.lowPass(out, 1_700.0)
    }

    private fun kick(): DoubleArray = Synth.tone(190, 130.0, 45.0, decay = 5.0)

    private fun hat(seed: Int): DoubleArray = Synth.noiseBurst(70, decay = 26.0, seed = seed)

    /** MENU — Am7 / Fmaj7 / Cmaj7 / G: calmo, espacial, 16 segundos. */
    private fun renderMenu(): FloatArray {
        val barMs = 4_000
        val chords = listOf(
            listOf(57, 60, 64, 67), // Am7
            listOf(53, 57, 60, 64), // Fmaj7
            listOf(48, 52, 55, 59), // Cmaj7
            listOf(55, 59, 62, 66)  // G
        )
        val out = FloatArray(barMs * chords.size * Synth.SAMPLE_RATE / 1000)
        chords.forEachIndexed { bar, chord ->
            val offset = bar * barMs * Synth.SAMPLE_RATE / 1000
            chord.forEach { midi -> Synth.mix(out, offset, pad(barMs, Synth.note(midi)), 0.11) }
            Synth.mix(out, offset, Synth.tone(1_400, Synth.note(chord[0] - 12), decay = 2.0), 0.24)

            val arp = listOf(
                chord[0] + 12, chord[1] + 12, chord[2] + 12, chord[3] + 12,
                chord[2] + 12, chord[3] + 12, chord[1] + 12, chord[2] + 24
            )
            arp.forEachIndexed { i, midi ->
                Synth.mix(
                    out,
                    offset + i * 500 * Synth.SAMPLE_RATE / 1000,
                    Synth.tone(470, Synth.note(midi), decay = 6.0, wave = Synth::triangle),
                    0.13
                )
            }
        }
        return out
    }

    /** GAME — Am / Em / F / G a 120bpm: pulso constante para manter o foco. */
    private fun renderGame(): FloatArray {
        val barMs = 2_000
        val chords = listOf(
            listOf(57, 60, 64),
            listOf(52, 55, 59),
            listOf(53, 57, 60),
            listOf(55, 59, 62)
        )
        val out = FloatArray(barMs * chords.size * Synth.SAMPLE_RATE / 1000)
        chords.forEachIndexed { bar, chord ->
            val offset = bar * barMs * Synth.SAMPLE_RATE / 1000
            chord.forEach { midi -> Synth.mix(out, offset, pad(barMs, Synth.note(midi)), 0.07) }

            // Baixo em oitavas (dois tempos por compasso)
            repeat(2) { beat ->
                Synth.mix(
                    out,
                    offset + beat * 1_000 * Synth.SAMPLE_RATE / 1000,
                    Synth.tone(560, Synth.note(chord[0] - 24), decay = 3.5, wave = Synth::square),
                    0.15
                )
            }
            // Bateria: bumbo nos tempos, hi-hat nos contratempos
            repeat(4) { beat ->
                val beatOffset = offset + beat * 500 * Synth.SAMPLE_RATE / 1000
                Synth.mix(out, beatOffset, kick(), 0.30)
                Synth.mix(out, beatOffset + 250 * Synth.SAMPLE_RATE / 1000, hat(beat + bar * 4), 0.10)
            }
            // Arpejo de 16 avos
            val arp = List(8) { chord[it % chord.size] + 12 + if (it >= 4) 12 else 0 }
            arp.forEachIndexed { i, midi ->
                Synth.mix(
                    out,
                    offset + i * 250 * Synth.SAMPLE_RATE / 1000,
                    Synth.tone(230, Synth.note(midi), decay = 7.0, wave = Synth::triangle),
                    0.11
                )
            }
        }
        return out
    }

    /** TENSE — usado nos últimos 15 segundos: mais escuro e apertado. */
    private fun renderTense(): FloatArray {
        val barMs = 1_500
        val roots = listOf(45, 44, 43, 44)
        val out = FloatArray(barMs * roots.size * Synth.SAMPLE_RATE / 1000)
        roots.forEachIndexed { bar, root ->
            val offset = bar * barMs * Synth.SAMPLE_RATE / 1000
            Synth.mix(out, offset, pad(barMs, Synth.note(root)), 0.16)
            Synth.mix(out, offset, pad(barMs, Synth.note(root + 6)), 0.06) // trítono: tensão
            repeat(3) { beat ->
                val beatOffset = offset + beat * 500 * Synth.SAMPLE_RATE / 1000
                Synth.mix(out, beatOffset, kick(), 0.34)
                Synth.mix(out, beatOffset, Synth.tone(120, 1_500.0, 1_100.0, decay = 14.0, wave = Synth::square), 0.10)
            }
        }
        return out
    }
}
