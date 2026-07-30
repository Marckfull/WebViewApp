package com.neuroflip.game.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflip.game.audio.MusicTrack
import com.neuroflip.game.audio.Sfx
import com.neuroflip.game.data.PlayerState
import com.neuroflip.game.data.PowerUp
import com.neuroflip.game.haptics.Buzz
import com.neuroflip.game.neuro
import com.neuroflip.game.ui.theme.NeuroThemes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailyReward(
    val available: Boolean,
    val streak: Int,
    val amount: Int
)

/** Estado global: perfil do jogador, economia, áudio e anúncios. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = app.neuro.repository
    private val sound = app.neuro.sound
    private val music = app.neuro.music
    private val haptics = app.neuro.haptics
    val ads = app.neuro.ads

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val player: StateFlow<PlayerState> = repo.state
        .onEach { state ->
            sound.enabled = state.sfxEnabled
            haptics.enabled = state.hapticsEnabled
            music.enabled = state.musicEnabled
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerState())

    init {
        ads.initialize()
    }

    // ------------------------------------------------------------------ áudio

    fun playMenuMusic() = music.play(MusicTrack.MENU)
    fun tap() {
        sound.play(Sfx.TAP, volume = 0.6f)
        haptics.buzz(Buzz.LIGHT)
    }

    // ------------------------------------------------------------------ economia

    fun buyPowerUp(kind: PowerUp) = viewModelScope.launch {
        if (repo.spendNeurons(kind.price)) {
            repo.addPowerUp(kind, 1)
            sound.play(Sfx.COIN)
            haptics.buzz(Buzz.SUCCESS)
            _toast.value = "${kind.label} comprado!"
        } else {
            _toast.value = "Neurônios insuficientes. Assista a um vídeo para ganhar mais."
            haptics.buzz(Buzz.ERROR)
        }
    }

    fun buyTheme(themeId: String) = viewModelScope.launch {
        val theme = NeuroThemes.byId(themeId)
        if (player.value.unlockedThemes.contains(themeId)) {
            repo.setTheme(themeId)
            return@launch
        }
        if (repo.spendNeurons(theme.price)) {
            repo.unlockTheme(themeId)
            repo.setTheme(themeId)
            sound.play(Sfx.COIN)
            haptics.buzz(Buzz.SUCCESS)
            _toast.value = "Tema ${theme.name} liberado!"
        } else {
            _toast.value = "Faltam ${theme.price - player.value.neurons} neurônios."
        }
    }

    fun selectTheme(themeId: String) = viewModelScope.launch { repo.setTheme(themeId) }

    // ------------------------------------------------------------------ bônus diário

    fun dailyReward(nowMs: Long = System.currentTimeMillis()): DailyReward {
        val state = player.value
        val elapsed = nowMs - state.lastDailyClaimMs
        val available = state.lastDailyClaimMs == 0L || elapsed >= COOLDOWN_MS
        val nextStreak = when {
            state.lastDailyClaimMs == 0L -> 1
            elapsed in COOLDOWN_MS..STREAK_WINDOW_MS -> state.dailyStreak + 1
            elapsed > STREAK_WINDOW_MS -> 1
            else -> state.dailyStreak
        }
        val amount = (25 + nextStreak * 10).coerceAtMost(120)
        return DailyReward(available, nextStreak, amount)
    }

    fun claimDaily(doubled: Boolean = false) = viewModelScope.launch {
        val reward = dailyReward()
        if (!reward.available) return@launch
        val amount = if (doubled) reward.amount * 2 else reward.amount
        repo.claimDaily(System.currentTimeMillis(), amount, reward.streak)
        sound.play(Sfx.COIN)
        haptics.buzz(Buzz.SUCCESS)
        _toast.value = "+$amount neurônios (sequência de ${reward.streak} dias)"
    }

    // ------------------------------------------------------------------ anúncios premiados

    /**
     * Mostra o vídeo premiado. Se não houver anúncio disponível (offline, por
     * exemplo), entrega uma recompensa de consolo — o jogador nunca fica preso.
     */
    fun watchForNeurons(activity: Activity, amount: Int = 60) {
        ads.showRewarded(
            activity = activity,
            onReward = {
                viewModelScope.launch {
                    repo.addNeurons(amount)
                    sound.play(Sfx.COIN)
                    haptics.buzz(Buzz.SUCCESS)
                    _toast.value = "+$amount neurônios!"
                }
            },
            onUnavailable = {
                viewModelScope.launch {
                    repo.addNeurons(10)
                    _toast.value = "Vídeo indisponível agora. Toma aí 10 neurônios."
                }
            }
        )
    }

    fun watchForPowerUp(activity: Activity, kind: PowerUp) {
        ads.showRewarded(
            activity = activity,
            onReward = {
                viewModelScope.launch {
                    repo.addPowerUp(kind, 1)
                    sound.play(Sfx.COIN)
                    haptics.buzz(Buzz.SUCCESS)
                    _toast.value = "+1 ${kind.label}!"
                }
            },
            onUnavailable = {
                _toast.value = "Vídeo indisponível. Tente de novo em instantes."
            }
        )
    }

    fun watchForDaily(activity: Activity) {
        ads.showRewarded(
            activity = activity,
            onReward = { claimDaily(doubled = true) },
            onUnavailable = { claimDaily(doubled = false) }
        )
    }

    // ------------------------------------------------------------------ preferências

    fun setMusic(enabled: Boolean) = viewModelScope.launch { repo.setMusic(enabled) }
    fun setSfx(enabled: Boolean) = viewModelScope.launch { repo.setSfx(enabled) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { repo.setHaptics(enabled) }
    fun acceptLegal() = viewModelScope.launch { repo.acceptLegal() }
    fun setTutorialSeen() = viewModelScope.launch { repo.setTutorialSeen() }
    fun consumeToast() {
        _toast.value = null
    }

    override fun onCleared() {
        super.onCleared()
        music.pause()
    }

    companion object {
        private const val COOLDOWN_MS = 20 * 60 * 60 * 1000L      // 20 horas
        private const val STREAK_WINDOW_MS = 48 * 60 * 60 * 1000L // até 48h mantém a sequência
    }
}
