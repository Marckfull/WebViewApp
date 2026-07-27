package com.chuvadeletras.game.ui.home

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chuvadeletras.game.ads.AdPlacement
import com.chuvadeletras.game.ads.AdResult
import com.chuvadeletras.game.ads.RewardedAdHost
import com.chuvadeletras.game.data.DailyReward
import com.chuvadeletras.game.data.PlayerProfile
import com.chuvadeletras.game.data.PlayerRepository
import com.chuvadeletras.game.domain.model.PowerUp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val FREE_COINS_COOLDOWN_MS = 4 * 60 * 60 * 1000L

data class HomeUiState(
    val profile: PlayerProfile = PlayerProfile(),
    val loading: Boolean = true,
    val canClaimDaily: Boolean = false,
    val dailyChallengeDone: Boolean = false,
    val claimedReward: DailyReward? = null,
    val message: String? = null
) {
    val freeCoinsReady: Boolean
        get() = System.currentTimeMillis() - profile.lastFreeCoinsAt > FREE_COINS_COOLDOWN_MS
}

/**
 * ViewModel compartilhado pelas telas fora da partida: início, loja, missões,
 * conquistas e ajustes. Todas leem o mesmo perfil, então um ViewModel só evita
 * estados divergentes.
 */
class HomeViewModel(
    private val repository: PlayerRepository,
    private val adHost: RewardedAdHost
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val adState = adHost.state

    init {
        viewModelScope.launch {
            repository.onAppOpened()
        }
        viewModelScope.launch {
            repository.profile.collect { profile ->
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                _state.value = _state.value.copy(
                    profile = profile,
                    loading = false,
                    canClaimDaily = repository.canClaimDaily(profile),
                    dailyChallengeDone = profile.lastDailyDate == today
                )
            }
        }
    }

    fun claimDaily(doubled: Boolean, activity: Activity?) {
        viewModelScope.launch {
            if (doubled) {
                if (adHost.show(activity, AdPlacement.DOUBLE_DAILY) != AdResult.Rewarded) {
                    _state.value = _state.value.copy(
                        message = "Sem problema — resgate normal continua disponível"
                    )
                    return@launch
                }
            }
            val reward = repository.claimDailyReward(doubled)
            _state.value = _state.value.copy(
                claimedReward = reward,
                message = if (reward == null) "O baú de hoje já foi aberto" else null
            )
        }
    }

    fun dismissClaimedReward() {
        _state.value = _state.value.copy(claimedReward = null)
    }

    fun buy(powerUp: PowerUp) {
        viewModelScope.launch {
            val bought = repository.buyPowerUp(powerUp)
            _state.value = _state.value.copy(
                message = if (bought) {
                    "${powerUp.emoji} ${powerUp.label} comprado!"
                } else {
                    "Gotas insuficientes — assista a um vídeo para ganhar mais"
                }
            )
        }
    }

    fun watchForCoins(activity: Activity?) {
        viewModelScope.launch {
            if (!_state.value.freeCoinsReady) {
                _state.value = _state.value.copy(message = "As gotas grátis voltam daqui a pouco")
                return@launch
            }
            if (adHost.show(activity, AdPlacement.FREE_COINS) == AdResult.Rewarded) {
                repository.addCoins(100)
                repository.markFreeCoinsCollected()
                repository.registerAdWatched()
                _state.value = _state.value.copy(message = "+100 gotas! 💧")
            }
        }
    }

    fun claimMission(missionId: String) {
        viewModelScope.launch {
            val mission = repository.claimMission(missionId)
            _state.value = _state.value.copy(
                message = mission?.let { "+${it.coins} gotas e +${it.xp} XP!" }
            )
        }
    }

    fun claimAchievement(achievementId: String) {
        viewModelScope.launch {
            val achievement = repository.claimAchievement(achievementId)
            _state.value = _state.value.copy(
                message = achievement?.let { "${it.emoji} +${it.coins} gotas!" }
            )
        }
    }

    fun setSound(enabled: Boolean) {
        viewModelScope.launch { repository.setSound(enabled) }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch { repository.setVibration(enabled) }
    }

    fun replayTutorial() {
        viewModelScope.launch { repository.resetTutorial() }
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    class Factory(
        private val repository: PlayerRepository,
        private val adHost: RewardedAdHost
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository, adHost) as T
    }
}
