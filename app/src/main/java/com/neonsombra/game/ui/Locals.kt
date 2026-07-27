package com.neonsombra.game.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.neonsombra.game.audio.SoundEngine
import com.neonsombra.game.data.Prefs

val LocalPrefs = staticCompositionLocalOf<Prefs> {
    error("Prefs nao foi fornecido a arvore do Compose")
}

val LocalSound = staticCompositionLocalOf<SoundEngine> {
    error("SoundEngine nao foi fornecido a arvore do Compose")
}
