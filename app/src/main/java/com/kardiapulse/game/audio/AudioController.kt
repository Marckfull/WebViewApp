package com.kardiapulse.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Ponto único de áudio do jogo: efeitos via [SoundPool] e trilha via [MediaPlayer].
 *
 * Na primeira execução os WAVs são sintetizados no cache em background. Enquanto isso não
 * termina, `play` simplesmente não faz nada — o jogo nunca trava esperando som.
 */
class AudioController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val loadMutex = Mutex()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = HashMap<Sfx, Int>()
    private val loaded = HashSet<Int>()

    private var musicPlayer: MediaPlayer? = null
    private var currentTrack: MusicTrack? = null

    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) stopMusic() else currentTrack?.let { play(it) }
        }

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }
        scope.launch { prepare() }
    }

    /** Sintetiza (uma vez) e carrega todos os efeitos. Seguro chamar várias vezes. */
    private suspend fun prepare() = loadMutex.withLock {
        if (soundIds.isNotEmpty()) return@withLock
        val dir = File(context.cacheDir, "audio").apply { mkdirs() }
        for (sfx in Sfx.entries) {
            try {
                val file = File(dir, sfx.fileName)
                if (!file.exists() || file.length() < 64) {
                    WavSynth.writeWav(file, sfx.render())
                }
                soundIds[sfx] = soundPool.load(file.absolutePath, 1)
            } catch (t: Throwable) {
                Log.w(TAG, "Falha ao preparar ${sfx.name}", t)
            }
        }
    }

    fun play(sfx: Sfx, volume: Float = 1f, rate: Float = 1f) {
        if (!soundEnabled) return
        val id = soundIds[sfx] ?: return
        if (id !in loaded) return
        val v = volume.coerceIn(0f, 1f)
        try {
            soundPool.play(id, v, v, 1, 0, rate.coerceIn(0.5f, 2f))
        } catch (t: Throwable) {
            Log.w(TAG, "Falha ao tocar ${sfx.name}", t)
        }
    }

    /** Toca (ou troca para) uma trilha em loop. Sintetiza na primeira vez. */
    fun play(track: MusicTrack) {
        currentTrack = track
        if (!musicEnabled) return
        scope.launch {
            try {
                val dir = File(context.cacheDir, "audio").apply { mkdirs() }
                val file = File(dir, track.fileName)
                if (!file.exists() || file.length() < 1024) {
                    WavSynth.writeWav(file, track.render())
                }
                synchronized(this@AudioController) {
                    if (currentTrack != track || !musicEnabled) return@synchronized
                    releasePlayer()
                    musicPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        setDataSource(file.absolutePath)
                        isLooping = true
                        setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
                        setOnErrorListener { _, _, _ -> true }
                        prepare()
                        start()
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Falha ao tocar a trilha ${track.name}", t)
            }
        }
    }

    fun stopMusic() {
        synchronized(this) { releasePlayer() }
    }

    fun pauseMusic() {
        synchronized(this) {
            runCatching { musicPlayer?.takeIf { it.isPlaying }?.pause() }
        }
    }

    fun resumeMusic() {
        synchronized(this) {
            if (!musicEnabled) return
            val player = musicPlayer
            if (player == null) currentTrack?.let { play(it) }
            else runCatching { if (!player.isPlaying) player.start() }
        }
    }

    private fun releasePlayer() {
        runCatching { musicPlayer?.stop() }
        runCatching { musicPlayer?.release() }
        musicPlayer = null
    }

    fun release() {
        stopMusic()
        runCatching { soundPool.release() }
    }

    private companion object {
        const val TAG = "AudioController"
        const val MUSIC_VOLUME = 0.42f
    }
}
