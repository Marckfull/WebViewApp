package com.neuroflip.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neuroflip.game.ads.ConsentManager
import com.neuroflip.game.ui.AppViewModel
import com.neuroflip.game.ui.GameViewModel
import com.neuroflip.game.ui.NeuroFlipRoot
import com.neuroflip.game.ui.theme.NeuroFlipTheme

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()
    private val gameViewModel: GameViewModel by viewModels()
    private var consentManager: ConsentManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash do sistema (ícone) → splash animada em Compose.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val manager = ConsentManager(this)
        consentManager = manager
        manager.gather(this) { canRequestAds ->
            if (canRequestAds) appViewModel.ads.preload()
        }

        setContent {
            val player by appViewModel.player.collectAsStateWithLifecycle()
            NeuroFlipTheme(paletteId = player.themeId) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NeuroFlipRoot(
                        appVm = appViewModel,
                        gameVm = gameViewModel,
                        consent = consentManager
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        neuro.music.pause()
        gameViewModel.pause()
    }

    override fun onResume() {
        super.onResume()
        neuro.music.resume()
    }
}
