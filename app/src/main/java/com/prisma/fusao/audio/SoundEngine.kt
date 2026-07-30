package com.prisma.fusao.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Efeitos disponíveis. Cada um é sintetizado na primeira execução do app. */
enum class Sfx {
    SWAP,
    INVALID,
    MATCH,
    ESSENCE,
    FUSION,
    PRISM,
    SUPERNOVA,
    ICE,
    STONE,
    PRISMOID,
    BUTTON,
    COIN,
    STAR,
    WIN,
    LOSE,
}

/**
 * Áudio 100% sintetizado em tempo de execução.
 *
 * A alternativa seria versionar dezenas de .ogg no repositório. Gerar as ondas na
 * primeira execução mantém o APK pequeno, evita assets binários no git e deixa cada
 * som ajustável por parâmetro. Os arquivos vão para o cache e são reaproveitados.
 */
class SoundEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val soundIds = mutableMapOf<Sfx, Int>()
    private var soundPool: SoundPool? = null
    private var musicPlayer: MediaPlayer? = null
    private var ready = false

    var musicEnabled: Boolean = true
        set(value) {
            field = value
            applyMusicState()
        }

    var sfxEnabled: Boolean = true

    fun initialize() {
        if (soundPool != null) return
        soundPool = SoundPool.Builder()
            .setMaxStreams(12)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()

        scope.launch {
            runCatching {
                val dir = File(context.cacheDir, "prisma_audio").apply { mkdirs() }
                for (sfx in Sfx.entries) {
                    val file = File(dir, "${sfx.name.lowercase()}_v1.wav")
                    if (!file.exists()) writeWav(file, synthesize(sfx))
                    soundIds[sfx] = soundPool?.load(file.absolutePath, 1) ?: 0
                }
                val musicFile = File(dir, "trilha_v1.wav")
                if (!musicFile.exists()) writeWav(musicFile, synthesizeMusic())
                prepareMusic(musicFile)
                ready = true
            }.onFailure { Log.w(TAG, "áudio indisponível: ${it.message}") }
        }
    }

    // ------------------------------------------------------------- reprodução

    /**
     * @param pitch multiplicador de frequência — usado para subir o tom a cada
     *              elo da cascata, o que dá a sensação de combo crescente.
     */
    fun play(sfx: Sfx, volume: Float = 1f, pitch: Float = 1f) {
        if (!sfxEnabled || !ready) return
        val pool = soundPool ?: return
        val id = soundIds[sfx] ?: return
        val rate = pitch.coerceIn(0.5f, 2f)
        runCatching { pool.play(id, volume, volume, 1, 0, rate) }
    }

    /** Toca o som da cascata com o tom subindo conforme o encadeamento. */
    fun playCascade(cascade: Int) {
        play(Sfx.MATCH, volume = 0.9f, pitch = 1f + min(cascade, 8) * 0.08f)
    }

    private fun prepareMusic(file: File) {
        runCatching {
            musicPlayer?.release()
            musicPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(file.absolutePath)
                isLooping = true
                setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
                prepare()
            }
            applyMusicState()
        }
    }

    private fun applyMusicState() {
        val player = musicPlayer ?: return
        runCatching {
            if (musicEnabled) {
                if (!player.isPlaying) player.start()
            } else {
                if (player.isPlaying) player.pause()
            }
        }
    }

    fun pauseMusic() {
        runCatching { musicPlayer?.takeIf { it.isPlaying }?.pause() }
    }

    fun resumeMusic() = applyMusicState()

    fun release() {
        runCatching { musicPlayer?.release() }
        runCatching { soundPool?.release() }
        musicPlayer = null
        soundPool = null
        ready = false
    }

    // --------------------------------------------------------------- síntese

    private fun synthesize(sfx: Sfx): FloatArray = when (sfx) {
        Sfx.SWAP -> sweep(520f, 780f, 0.10f, decay = 26f, amp = 0.35f)
        Sfx.INVALID -> square(120f, 0.16f, decay = 12f, amp = 0.30f)
        Sfx.MATCH -> chord(listOf(660f, 880f, 1100f), 0.28f, decay = 11f, amp = 0.30f)
        Sfx.ESSENCE -> arpeggio(listOf(660f, 880f, 1320f, 1760f), 0.09f, amp = 0.32f)
        Sfx.FUSION -> mix(
            sweep(300f, 1500f, 0.45f, decay = 5f, amp = 0.30f),
            chord(listOf(523f, 659f, 784f, 1046f), 0.7f, decay = 4f, amp = 0.30f, delay = 0.18f),
        )
        Sfx.PRISM -> sweep(1900f, 260f, 0.42f, decay = 6f, amp = 0.34f)
        Sfx.SUPERNOVA -> mix(
            noise(0.55f, decay = 7f, amp = 0.30f),
            sweep(180f, 60f, 0.7f, decay = 4f, amp = 0.36f),
        )
        Sfx.ICE -> noise(0.16f, decay = 30f, amp = 0.26f, highpass = true)
        Sfx.STONE -> sweep(180f, 70f, 0.22f, decay = 20f, amp = 0.34f)
        Sfx.PRISMOID -> arpeggio(listOf(1320f, 1046f, 880f, 660f), 0.10f, amp = 0.32f)
        Sfx.BUTTON -> sweep(900f, 1200f, 0.06f, decay = 40f, amp = 0.25f)
        Sfx.COIN -> arpeggio(listOf(1046f, 1568f), 0.07f, amp = 0.28f)
        Sfx.STAR -> chord(listOf(1046f, 1318f, 1568f), 0.45f, decay = 7f, amp = 0.28f)
        Sfx.WIN -> arpeggio(listOf(523f, 659f, 784f, 1046f, 1318f), 0.13f, amp = 0.32f)
        Sfx.LOSE -> arpeggio(listOf(587f, 494f, 415f, 330f), 0.17f, amp = 0.30f)
    }

    /**
     * Trilha em lá menor pentatônica: um baixo em colcheias, um pad em acordes e
     * um arpejo cristalino por cima. Oito compassos que fecham em loop.
     */
    private fun synthesizeMusic(): FloatArray {
        val bpm = 96.0
        val beat = 60.0 / bpm
        val bars = 8
        val totalSeconds = bars * 4 * beat
        val out = FloatArray((totalSeconds * SAMPLE_RATE).toInt())

        // Progressão: Am - F - C - G, dois compassos cada par.
        val roots = listOf(220.0, 174.61, 261.63, 196.0)
        val pentatonic = listOf(220.0, 261.63, 293.66, 329.63, 392.0, 440.0, 523.25, 587.33)
        val rng = Random(7)

        for (bar in 0 until bars) {
            val root = roots[bar % roots.size]
            val barStart = bar * 4 * beat

            // Baixo
            for (b in 0 until 4) {
                addTone(
                    out, barStart + b * beat, beat * 0.9,
                    freq = root / 2, amp = 0.16f, decay = 3.2, wave = Wave.TRIANGLE,
                )
            }
            // Pad (tríade sustentada)
            for (semitone in listOf(0, 3, 7)) {
                addTone(
                    out, barStart, beat * 3.6,
                    freq = root * pow2(semitone / 12.0), amp = 0.055f, decay = 0.5,
                    wave = Wave.SINE,
                )
            }
            // Arpejo cristalino
            for (step in 0 until 8) {
                if (rng.nextInt(100) < 22) continue
                val note = pentatonic[rng.nextInt(pentatonic.size)] * 2
                addTone(
                    out, barStart + step * beat / 2, beat * 0.45,
                    freq = note, amp = 0.075f, decay = 7.0, wave = Wave.SINE,
                )
            }
        }

        // Fade nas bordas para o loop não estalar.
        val fade = (SAMPLE_RATE * 0.04).toInt()
        for (i in 0 until min(fade, out.size)) {
            val g = i / fade.toFloat()
            out[i] *= g
            out[out.size - 1 - i] *= g
        }
        return out
    }

    private enum class Wave { SINE, TRIANGLE, SQUARE }

    private fun addTone(
        buffer: FloatArray,
        startSeconds: Double,
        durationSeconds: Double,
        freq: Double,
        amp: Float,
        decay: Double,
        wave: Wave,
    ) {
        val start = (startSeconds * SAMPLE_RATE).toInt()
        val length = (durationSeconds * SAMPLE_RATE).toInt()
        for (i in 0 until length) {
            val index = start + i
            if (index >= buffer.size) break
            val t = i / SAMPLE_RATE.toDouble()
            val phase = 2 * PI * freq * t
            val raw = when (wave) {
                Wave.SINE -> sin(phase)
                Wave.TRIANGLE -> 2.0 / PI * kotlin.math.asin(sin(phase))
                Wave.SQUARE -> if (sin(phase) >= 0) 0.7 else -0.7
            }
            // Ataque curto evita o estalo do início da nota.
            val attack = min(1.0, t / 0.008)
            buffer[index] += (raw * amp * attack * exp(-decay * t)).toFloat()
        }
    }

    private fun sweep(
        fromHz: Float,
        toHz: Float,
        seconds: Float,
        decay: Float,
        amp: Float,
    ): FloatArray {
        val n = (seconds * SAMPLE_RATE).toInt()
        val out = FloatArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i / SAMPLE_RATE.toDouble()
            val p = i / n.toDouble()
            val freq = fromHz + (toHz - fromHz) * p
            phase += 2 * PI * freq / SAMPLE_RATE
            val attack = min(1.0, t / 0.004)
            out[i] = (sin(phase) * amp * attack * exp(-decay * t)).toFloat()
        }
        return out
    }

    private fun square(freq: Float, seconds: Float, decay: Float, amp: Float): FloatArray {
        val n = (seconds * SAMPLE_RATE).toInt()
        val out = FloatArray(n)
        for (i in 0 until n) {
            val t = i / SAMPLE_RATE.toDouble()
            val v = if (sin(2 * PI * freq * t) >= 0) 1.0 else -1.0
            out[i] = (v * amp * exp(-decay * t)).toFloat()
        }
        return out
    }

    private fun chord(
        freqs: List<Float>,
        seconds: Float,
        decay: Float,
        amp: Float,
        delay: Float = 0f,
    ): FloatArray {
        val n = ((seconds + delay) * SAMPLE_RATE).toInt()
        val out = FloatArray(n)
        val offset = (delay * SAMPLE_RATE).toInt()
        val perVoice = amp / freqs.size
        for (freq in freqs) {
            for (i in 0 until n - offset) {
                val t = i / SAMPLE_RATE.toDouble()
                val attack = min(1.0, t / 0.006)
                out[offset + i] +=
                    (sin(2 * PI * freq * t) * perVoice * attack * exp(-decay * t)).toFloat()
            }
        }
        return out
    }

    private fun arpeggio(freqs: List<Float>, stepSeconds: Float, amp: Float): FloatArray {
        val stepSamples = (stepSeconds * SAMPLE_RATE).toInt()
        val tail = (0.25f * SAMPLE_RATE).toInt()
        val out = FloatArray(stepSamples * freqs.size + tail)
        freqs.forEachIndexed { index, freq ->
            val start = index * stepSamples
            for (i in 0 until stepSamples + tail) {
                val pos = start + i
                if (pos >= out.size) break
                val t = i / SAMPLE_RATE.toDouble()
                val attack = min(1.0, t / 0.005)
                out[pos] += (sin(2 * PI * freq * t) * amp * attack * exp(-9.0 * t)).toFloat()
            }
        }
        return out
    }

    private fun noise(
        seconds: Float,
        decay: Float,
        amp: Float,
        highpass: Boolean = false,
    ): FloatArray {
        val n = (seconds * SAMPLE_RATE).toInt()
        val out = FloatArray(n)
        val rng = Random(11)
        var previous = 0f
        for (i in 0 until n) {
            val t = i / SAMPLE_RATE.toDouble()
            val white = rng.nextFloat() * 2f - 1f
            // Filtro de primeira ordem: agudo para o gelo, cheio para a supernova.
            val filtered = if (highpass) white - previous else (white + previous) * 0.5f
            previous = white
            out[i] = (filtered * amp * exp(-decay * t)).toFloat()
        }
        return out
    }

    private fun mix(vararg parts: FloatArray): FloatArray {
        val out = FloatArray(parts.maxOf { it.size })
        for (part in parts) for (i in part.indices) out[i] += part[i]
        return out
    }

    private fun pow2(exponent: Double): Double = Math.pow(2.0, exponent)

    // ------------------------------------------------------------------ WAV

    private fun writeWav(file: File, samples: FloatArray) {
        val pcm = ShortArray(samples.size) { i ->
            (samples[i].coerceIn(-1f, 1f) * 32_767f).toInt().toShort()
        }
        val dataSize = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)              // tamanho do bloco fmt
            putShort(1)             // PCM
            putShort(1)             // mono
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 2) // bytes por segundo
            putShort(2)             // alinhamento do bloco
            putShort(16)            // bits por amostra
            put("data".toByteArray())
            putInt(dataSize)
        }
        val body = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        pcm.forEach { body.putShort(it) }

        FileOutputStream(file).use { stream ->
            stream.write(header.array())
            stream.write(body.array())
        }
    }

    companion object {
        private const val TAG = "SoundEngine"
        private const val SAMPLE_RATE = 44_100
        private const val MUSIC_VOLUME = 0.34f
    }
}
