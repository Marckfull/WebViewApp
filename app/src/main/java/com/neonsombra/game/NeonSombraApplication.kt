package com.neonsombra.game

import android.app.Application
import com.neonsombra.game.audio.SoundEngine
import com.neonsombra.game.data.Prefs

/**
 * Mantem as preferencias e o motor de som vivos enquanto o processo existir,
 * para que a trilha nao reinicie a cada troca de tela.
 */
class NeonSombraApplication : Application() {

    lateinit var prefs: Prefs
        private set

    lateinit var sound: SoundEngine
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        sound = SoundEngine(this, prefs)
    }

    override fun onTerminate() {
        sound.release()
        super.onTerminate()
    }
}
