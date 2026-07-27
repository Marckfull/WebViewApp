package com.neonsombra.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.neonsombra.game.data.Prefs
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/** Efeitos curtos disparados pelo jogo. */
enum class Sfx { MOVE, ROTATE, LOCK, CLEAR, TETRIS, LEVEL_UP, GAME_OVER, UI_CLICK }

private enum class Wave { SQUARE, TRIANGLE, NOISE }

private data class Note(
    val freq: Float,
    val durationMs: Int,
    val endFreq: Float = freq,
    val wave: Wave = Wave.SQUARE,
    val volume: Float = 1f,
)

/**
 * Trilha e efeitos sonoros sintetizados em tempo de execucao.
 *
 * O jogo nao carrega nenhum arquivo de audio: as ondas quadradas do chiptune
 * sao geradas em PCM e tocadas por [AudioTrack]. Assim o projeto continua leve
 * e sem binarios no repositorio.
 *
 * Musica, efeitos e vibracao respeitam os interruptores de [Prefs] a cada
 * chamada, entao desligar nas configuracoes tem efeito imediato.
 */
class SoundEngine(context: Context, private val prefs: Prefs) {

    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private val sfxTracks = ConcurrentHashMap<Sfx, AudioTrack>()

    @Volatile
    private var musicTrack: AudioTrack? = null

    @Volatile
    private var musicWanted = false

    @Volatile
    private var released = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        executor.execute { prepareSfx() }
    }

    // ------------------------------------------------------------------ efeitos

    fun play(sfx: Sfx) {
        if (released || !prefs.soundEnabled) return
        val track = sfxTracks[sfx] ?: return
        runCatching {
            if (track.playState != AudioTrack.PLAYSTATE_STOPPED) track.stop()
            track.reloadStaticData()
            track.play()
        }
    }

    fun click() = play(Sfx.UI_CLICK)

    // ------------------------------------------------------------------ musica

    fun startMusic() {
        if (released) return
        musicWanted = true
        executor.execute {
            if (released || !musicWanted || !prefs.musicEnabled) return@execute
            val track = musicTrack ?: buildMusicTrack()?.also { musicTrack = it } ?: return@execute
            runCatching {
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) track.play()
            }
        }
    }

    fun stopMusic() {
        musicWanted = false
        executor.execute {
            runCatching { musicTrack?.takeIf { it.playState == AudioTrack.PLAYSTATE_PLAYING }?.pause() }
        }
    }

    /** Chamada depois de mexer no interruptor de musica nas configuracoes. */
    fun syncMusicWithPreferences() {
        if (prefs.musicEnabled) startMusic() else stopMusic()
    }

    // --------------------------------------------------------------- vibracao

    @Suppress("DEPRECATION")
    fun vibrate(durationMs: Long) {
        val device = enabledVibrator() ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                device.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE),
                )
            } else {
                device.vibrate(durationMs)
            }
        }
    }

    @Suppress("DEPRECATION")
    fun vibratePattern(pattern: LongArray) {
        val device = enabledVibrator() ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                device.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                device.vibrate(pattern, -1)
            }
        }
    }

    private fun enabledVibrator(): Vibrator? {
        if (released || !prefs.vibrationEnabled) return null
        return vibrator?.takeIf { it.hasVibrator() }
    }

    fun release() {
        released = true
        musicWanted = false
        executor.execute {
            runCatching { musicTrack?.stop() }
            runCatching { musicTrack?.release() }
            musicTrack = null
            sfxTracks.values.forEach { track ->
                runCatching { track.stop() }
                runCatching { track.release() }
            }
            sfxTracks.clear()
        }
        executor.shutdown()
    }

    // ------------------------------------------------------------------ sintese

    private fun prepareSfx() {
        val definitions = mapOf(
            Sfx.MOVE to listOf(
                Note(freq = 620f, durationMs = 35, volume = 0.45f),
            ),
            Sfx.ROTATE to listOf(
                Note(freq = 880f, durationMs = 30, endFreq = 1320f, volume = 0.5f),
            ),
            Sfx.LOCK to listOf(
                Note(freq = 240f, durationMs = 60, endFreq = 130f, volume = 0.6f),
            ),
            Sfx.CLEAR to listOf(
                Note(freq = 660f, durationMs = 60, volume = 0.55f),
                Note(freq = 880f, durationMs = 60, volume = 0.55f),
                Note(freq = 1180f, durationMs = 90, volume = 0.55f),
            ),
            Sfx.TETRIS to listOf(
                Note(freq = 523f, durationMs = 60, volume = 0.6f),
                Note(freq = 659f, durationMs = 60, volume = 0.6f),
                Note(freq = 784f, durationMs = 60, volume = 0.6f),
                Note(freq = 1047f, durationMs = 70, volume = 0.6f),
                Note(freq = 1319f, durationMs = 140, volume = 0.6f),
            ),
            Sfx.LEVEL_UP to listOf(
                Note(freq = 440f, durationMs = 70, volume = 0.55f),
                Note(freq = 660f, durationMs = 70, volume = 0.55f),
                Note(freq = 990f, durationMs = 160, endFreq = 1200f, volume = 0.55f),
            ),
            Sfx.GAME_OVER to listOf(
                Note(freq = 400f, durationMs = 180, endFreq = 300f, volume = 0.6f),
                Note(freq = 300f, durationMs = 180, endFreq = 220f, volume = 0.6f),
                Note(freq = 220f, durationMs = 420, endFreq = 90f, volume = 0.6f),
            ),
            Sfx.UI_CLICK to listOf(
                Note(freq = 1100f, durationMs = 25, wave = Wave.TRIANGLE, volume = 0.4f),
            ),
        )

        definitions.forEach { (sfx, notes) ->
            if (released) return
            val pcm = renderNotes(notes)
            buildStaticTrack(pcm, volume = 0.75f, loop = false)?.let { sfxTracks[sfx] = it }
        }
    }

    private fun buildMusicTrack(): AudioTrack? {
        val pcm = buildMusicLoop()
        val track = buildStaticTrack(pcm, volume = 0.32f, loop = true) ?: return null
        runCatching { track.setLoopPoints(0, pcm.size, -1) }
        return track
    }

    private fun buildStaticTrack(pcm: ShortArray, volume: Float, loop: Boolean): AudioTrack? =
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(
                            if (loop) AudioAttributes.CONTENT_TYPE_MUSIC
                            else AudioAttributes.CONTENT_TYPE_SONIFICATION,
                        )
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.setVolume(volume)
            track
        }.getOrNull()

    /**
     * Monta o loop da trilha: baixo em onda quadrada, arpejo por cima e um
     * chimbal de ruido, sobre a progressao Am - F - C - G.
     */
    private fun buildMusicLoop(): ShortArray {
        val stepMs = (60_000f / BPM / 4f).toInt()
        val stepSamples = stepMs * SAMPLE_RATE / 1000
        val mix = FloatArray(STEPS * stepSamples)

        val bassRoots = floatArrayOf(semitone(-24f), semitone(-28f), semitone(-21f), semitone(-26f))
        val chords = arrayOf(
            floatArrayOf(0f, 3f, 7f),
            floatArrayOf(-4f, 0f, 5f),
            floatArrayOf(3f, 7f, 10f),
            floatArrayOf(-2f, 2f, 5f),
        )
        val arpPattern = intArrayOf(0, 1, 2, 1, 0, 1, 2, 1, 0, 2, 1, 2, 0, 1, 2, 1)
        val bassSteps = intArrayOf(0, 3, 6, 8, 11, 14)
        val random = Random(7)

        for (step in 0 until STEPS) {
            val bar = step / 16
            val inBar = step % 16
            val offset = step * stepSamples

            if (inBar in bassSteps) {
                val note = Note(bassRoots[bar], stepMs * 2, volume = 0.55f)
                mixInto(mix, offset, renderNote(note, random))
            }

            val chord = chords[bar]
            val degree = chord[arpPattern[inBar]]
            val lift = if (inBar >= 8) 12f else 0f
            val lead = Note(semitone(degree + lift), (stepMs * 0.9f).toInt(), volume = 0.30f)
            mixInto(mix, offset, renderNote(lead, random))

            if (inBar % 2 == 1) {
                val hat = Note(freq = 8000f, durationMs = 28, wave = Wave.NOISE, volume = 0.12f)
                mixInto(mix, offset, renderNote(hat, random))
            }
        }

        return ShortArray(mix.size) { index ->
            (mix[index].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun renderNotes(notes: List<Note>): ShortArray {
        val random = Random(3)
        val rendered = notes.map { renderNote(it, random) }
        val out = FloatArray(rendered.sumOf { it.size })
        var offset = 0
        for (chunk in rendered) {
            chunk.copyInto(out, offset)
            offset += chunk.size
        }
        return ShortArray(out.size) { index ->
            (out[index].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun renderNote(note: Note, random: Random): FloatArray {
        val length = (note.durationMs * SAMPLE_RATE / 1000).coerceAtLeast(1)
        val out = FloatArray(length)
        var phase = 0f
        for (index in 0 until length) {
            val progress = index.toFloat() / length
            val freq = note.freq + (note.endFreq - note.freq) * progress
            phase += freq / SAMPLE_RATE
            if (phase >= 1f) phase -= phase.toInt()
            val raw = when (note.wave) {
                Wave.SQUARE -> if (phase < 0.5f) 1f else -1f
                Wave.TRIANGLE -> 4f * kotlin.math.abs(phase - 0.5f) - 1f
                Wave.NOISE -> random.nextFloat() * 2f - 1f
            }
            out[index] = raw * envelope(index, length) * note.volume
        }
        return out
    }

    /** Ataque rapido, queda exponencial e um fade curto no fim. */
    private fun envelope(index: Int, length: Int): Float {
        val attack = min(length / 8, SAMPLE_RATE / 400).coerceAtLeast(1)
        val attackGain = if (index < attack) index.toFloat() / attack else 1f
        val decay = exp(-3.2f * index.toFloat() / length)
        val tail = length / 8
        val releaseGain = if (index > length - tail) (length - index).toFloat() / tail else 1f
        return attackGain * decay * releaseGain
    }

    private fun mixInto(target: FloatArray, offset: Int, source: FloatArray) {
        val count = min(source.size, target.size - offset)
        for (index in 0 until count) {
            target[offset + index] += source[index]
        }
    }

    private fun semitone(steps: Float): Float = 440f * 2f.pow(steps / 12f)

    private companion object {
        const val SAMPLE_RATE = 22050
        const val BPM = 128f
        const val STEPS = 64
    }
}
