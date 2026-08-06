package com.kardiapulse.game

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import com.kardiapulse.game.ads.AdsManager
import com.kardiapulse.game.audio.AudioController
import com.kardiapulse.game.data.PrefsRepository
import com.kardiapulse.game.haptics.Haptics
import com.kardiapulse.game.notifications.Notifier
import com.kardiapulse.game.security.DeviceIntegrity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Serviços compartilhados pelo app inteiro.
 *
 * O projeto não usa injeção de dependência: são quatro objetos de vida longa e um
 * CompositionLocal resolve isso com menos cerimônia e zero processamento de anotação.
 */
class Services(
    val repository: PrefsRepository,
    val audio: AudioController,
    val haptics: Haptics,
    val ads: AdsManager,
    val integrity: DeviceIntegrity.Report
)

val LocalServices = staticCompositionLocalOf<Services> {
    error("Services não foi fornecido. Envolva a árvore com KardiaRoot.")
}

class KardiaApp : Application() {

    lateinit var services: Services
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        val repository = PrefsRepository(this)
        services = Services(
            repository = repository,
            audio = AudioController(this),
            haptics = Haptics(this),
            ads = AdsManager(this),
            integrity = DeviceIntegrity.assess(this)
        )

        Notifier.ensureChannel(this)

        // As preferências salvas mandam nas chaves de som, música e vibração.
        scope.launch {
            runCatching {
                val profile = repository.profile.first()
                services.audio.soundEnabled = profile.soundOn
                services.audio.musicEnabled = profile.musicOn
                services.haptics.enabled = profile.hapticsOn
                services.ads.adsRemoved = profile.removeAds
            }
        }
    }
}
