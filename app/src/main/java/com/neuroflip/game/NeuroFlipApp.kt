package com.neuroflip.game

import android.app.Application
import android.content.Context
import com.neuroflip.game.ads.AdsManager
import com.neuroflip.game.audio.MusicEngine
import com.neuroflip.game.audio.SoundEngine
import com.neuroflip.game.data.PlayerRepository
import com.neuroflip.game.haptics.Haptics

/**
 * Ponto único de criação das dependências (service locator enxuto — o projeto
 * não precisa de um framework de injeção para cinco objetos).
 */
class NeuroFlipApp : Application() {

    val repository: PlayerRepository by lazy { PlayerRepository(this) }
    val sound: SoundEngine by lazy { SoundEngine(this) }
    val music: MusicEngine by lazy { MusicEngine(this) }
    val haptics: Haptics by lazy { Haptics(this) }
    val ads: AdsManager by lazy { AdsManager(this) }

    override fun onCreate() {
        super.onCreate()
        sound.warmUp()
    }

    override fun onTerminate() {
        super.onTerminate()
        sound.release()
        music.release()
    }
}

val Context.neuro: NeuroFlipApp
    get() = applicationContext as NeuroFlipApp
