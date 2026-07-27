package com.chuvadeletras.game

import android.app.Application
import android.content.Context
import com.chuvadeletras.game.ads.RewardedAdHost
import com.chuvadeletras.game.ads.SimulatedRewardedAdHost
import com.chuvadeletras.game.data.PlayerRepository
import com.chuvadeletras.game.data.PuzzleRepository

/**
 * Service locator simples. O app é pequeno o bastante para não precisar de
 * injeção de dependência de verdade.
 */
class AppContainer(context: Context) {
    val playerRepository = PlayerRepository(context.applicationContext)
    val puzzleRepository = PuzzleRepository()

    /**
     * Trocar por `AdMobRewardedAdHost(...)` liga os anúncios de verdade.
     * Passo a passo em `docs/ADMOB.md`.
     */
    val adHost: RewardedAdHost = SimulatedRewardedAdHost()
}

class ChuvaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Atalho para pegar o container a partir de qualquer Context. */
val Context.appContainer: AppContainer
    get() = (applicationContext as ChuvaApp).container
