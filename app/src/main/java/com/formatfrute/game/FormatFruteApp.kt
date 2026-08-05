package com.formatfrute.game

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.formatfrute.game.audio.SoundManager
import com.formatfrute.game.data.GameRepository
import com.formatfrute.game.notify.Notifier
import com.formatfrute.game.notify.ReminderScheduler

class FormatFruteApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val repo = GameRepository.get(this)
        repo.ensureMissionsFresh()

        Notifier.ensureChannel(this)
        ReminderScheduler.scheduleAll(this)

        // A musica acompanha o app: some quando ele vai pro fundo, volta ao abrir.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                SoundManager.get(this@FormatFruteApp).pauseMusic()
                ReminderScheduler.refreshComebacks(this@FormatFruteApp)
            }

            override fun onStart(owner: LifecycleOwner) {
                SoundManager.get(this@FormatFruteApp).resumeMusic()
            }
        })
    }
}
