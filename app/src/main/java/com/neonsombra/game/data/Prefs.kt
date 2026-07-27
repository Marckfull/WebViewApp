package com.neonsombra.game.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Guarda tudo o que o jogo precisa lembrar entre sessoes.
 *
 * Os campos sao estados do Compose, entao qualquer tela que os leia se
 * redesenha sozinha quando o valor muda.
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("neon_sombra_prefs", Context.MODE_PRIVATE)

    var playerName: String by stringPref(KEY_NAME, "")
    var highScore: Int by intPref(KEY_HIGH_SCORE, 0)
    var highScoreOwner: String by stringPref(KEY_HIGH_SCORE_OWNER, "")

    var termsAccepted: Boolean by boolPref(KEY_TERMS, false)
    var tutorialSeen: Boolean by boolPref(KEY_TUTORIAL, false)

    var musicEnabled: Boolean by boolPref(KEY_MUSIC, true)
    var soundEnabled: Boolean by boolPref(KEY_SOUND, true)
    var vibrationEnabled: Boolean by boolPref(KEY_VIBRATION, true)
    var ghostEnabled: Boolean by boolPref(KEY_GHOST, true)

    /** Devolve true quando o placar foi batido. */
    fun submitScore(score: Int, player: String): Boolean {
        if (score <= highScore) return false
        highScore = score
        highScoreOwner = player
        return true
    }

    // --------------------------------------------------------------- delegados

    private fun stringPref(key: String, default: String) = object : ReadWriteProperty<Any?, String> {
        private val state = mutableStateOf(sp.getString(key, default) ?: default)
        override fun getValue(thisRef: Any?, property: KProperty<*>): String = state.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            state.value = value
            sp.edit { putString(key, value) }
        }
    }

    private fun intPref(key: String, default: Int) = object : ReadWriteProperty<Any?, Int> {
        private val state = mutableStateOf(sp.getInt(key, default))
        override fun getValue(thisRef: Any?, property: KProperty<*>): Int = state.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            state.value = value
            sp.edit { putInt(key, value) }
        }
    }

    private fun boolPref(key: String, default: Boolean) = object : ReadWriteProperty<Any?, Boolean> {
        private val state = mutableStateOf(sp.getBoolean(key, default))
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = state.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            state.value = value
            sp.edit { putBoolean(key, value) }
        }
    }

    private companion object {
        const val KEY_NAME = "player_name"
        const val KEY_HIGH_SCORE = "high_score"
        const val KEY_HIGH_SCORE_OWNER = "high_score_owner"
        const val KEY_TERMS = "terms_accepted"
        const val KEY_TUTORIAL = "tutorial_seen"
        const val KEY_MUSIC = "music_enabled"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_VIBRATION = "vibration_enabled"
        const val KEY_GHOST = "ghost_enabled"
    }
}
