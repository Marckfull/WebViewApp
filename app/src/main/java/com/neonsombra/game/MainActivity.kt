package com.neonsombra.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.neonsombra.game.ui.LocalPrefs
import com.neonsombra.game.ui.LocalSound
import com.neonsombra.game.ui.NeonSombraApp
import com.neonsombra.game.ui.theme.NeonSombraTheme

class MainActivity : ComponentActivity() {

    private val neonApplication: NeonSombraApplication
        get() = application as NeonSombraApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        hideSystemBars()

        setContent {
            NeonSombraTheme {
                CompositionLocalProvider(
                    LocalPrefs provides neonApplication.prefs,
                    LocalSound provides neonApplication.sound,
                ) {
                    NeonSombraApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        neonApplication.sound.syncMusicWithPreferences()
    }

    override fun onPause() {
        neonApplication.sound.stopMusic()
        super.onPause()
    }

    /** Tela cheia de verdade: o jogo ocupa tudo e as barras voltam com um deslize. */
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
