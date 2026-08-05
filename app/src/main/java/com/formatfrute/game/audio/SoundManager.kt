package com.formatfrute.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.annotation.RawRes
import com.formatfrute.game.R
import com.formatfrute.game.data.GameRepository

enum class Sfx(@RawRes val res: Int) {
    SWIPE(R.raw.sfx_swipe),
    MERGE(R.raw.sfx_merge),
    SPAWN(R.raw.sfx_spawn),
    POWER(R.raw.sfx_power),
    COIN(R.raw.sfx_coin),
    BUTTON(R.raw.sfx_button),
    WIN(R.raw.sfx_win),
    LOSE(R.raw.sfx_lose),
    HARVEST(R.raw.sfx_harvest),
    ICE(R.raw.sfx_ice),
    LEVELUP(R.raw.sfx_levelup),
}

enum class Track(@RawRes val res: Int) {
    MENU(R.raw.music_menu),
    GAME(R.raw.music_game),
    BOSS(R.raw.music_boss),
}

/**
 * Som do jogo. SoundPool para os efeitos (curtos, sobrepostos) e MediaPlayer
 * para a musica de fundo, que faz crossfade suave ao trocar de tela.
 */
class SoundManager private constructor(context: Context) {

    private val app = context.applicationContext
    private val repo = GameRepository.get(app)

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = HashMap<Sfx, Int>()
    private val ready = HashSet<Int>()

    private var player: MediaPlayer? = null
    private var playing: Track? = null

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) ready += sampleId
        }
        Sfx.entries.forEach { ids[it] = pool.load(app, it.res, 1) }
    }

    fun play(sfx: Sfx, volume: Float = 1f, rate: Float = 1f) {
        if (!repo.current.sfx) return
        val id = ids[sfx] ?: return
        if (id !in ready) return
        pool.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    /** O som da fusao sobe de tom junto com a fruta. Vicia. */
    fun playMerge(level: Int) {
        play(Sfx.MERGE, volume = 0.9f, rate = 1f + level * 0.055f)
    }

    fun music(track: Track?) {
        if (!repo.current.music || track == null) {
            stopMusic()
            playing = track
            return
        }
        if (playing == track && player?.isPlaying == true) return
        stopMusic()
        playing = track
        runCatching {
            player = MediaPlayer.create(app, track.res)?.apply {
                isLooping = true
                setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
                start()
            }
        }
    }

    /** Rechama a faixa atual — usado quando o jogador liga a musica nos ajustes. */
    fun refreshMusic() {
        val track = playing
        if (repo.current.music) {
            playing = null
            music(track)
        } else {
            stopMusic()
        }
    }

    fun pauseMusic() {
        runCatching { player?.takeIf { it.isPlaying }?.pause() }
    }

    fun resumeMusic() {
        if (!repo.current.music) return
        val current = player
        if (current == null) music(playing) else runCatching { current.start() }
    }

    private fun stopMusic() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
    }

    fun release() {
        stopMusic()
        pool.release()
    }

    companion object {
        private const val MUSIC_VOLUME = 0.42f

        @Volatile
        private var instance: SoundManager? = null

        fun get(context: Context): SoundManager =
            instance ?: synchronized(this) {
                instance ?: SoundManager(context).also { instance = it }
            }
    }
}
