package com.chuvadeletras.game.ads

import android.app.Activity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Onde cada anúncio recompensado entra no jogo. Todo ponto aqui é opt-in:
 * o jogador escolhe assistir, nunca é obrigado.
 */
enum class AdPlacement(
    val emoji: String,
    val title: String,
    val rewardLabel: String,
    val cta: String
) {
    DOUBLE_DAILY(
        "🎁",
        "Dobrar o baú do dia",
        "Recompensa em dobro",
        "Assistir e dobrar"
    ),
    EXTRA_MOVES(
        "➕",
        "Jogadas extras",
        "+3 jogadas nesta rodada",
        "Assistir e ganhar"
    ),
    REVIVE(
        "❤️",
        "Continuar jogando",
        "Volta com os quadradinhos reparados",
        "Assistir e continuar"
    ),
    REPAIR_CELL(
        "🔧",
        "Reparar quadradinho",
        "Destrava o quadradinho perdido",
        "Assistir e reparar"
    ),
    FREE_COINS(
        "💧",
        "Gotas grátis",
        "+100 gotas",
        "Assistir e receber"
    ),
    DOUBLE_MATCH_REWARD(
        "✨",
        "Dobrar prêmio da partida",
        "Gotas e XP em dobro",
        "Assistir e dobrar"
    )
}

sealed interface AdResult {
    /** Assistiu até o fim: pode entregar o prêmio. */
    data object Rewarded : AdResult

    /** Fechou antes da hora — sem prêmio, sem punição. */
    data object Dismissed : AdResult

    data class Failed(val reason: String) : AdResult
}

/** Estado do anúncio, para a UI desenhar o overlay. */
sealed interface AdState {
    data object Idle : AdState
    data class Loading(val placement: AdPlacement) : AdState
    data class Playing(val placement: AdPlacement, val secondsLeft: Int) : AdState
}

/**
 * Contrato de anúncio recompensado. O jogo inteiro fala só com esta interface,
 * então trocar o simulador pelo AdMob de verdade não encosta em nenhuma tela.
 * Veja `docs/ADMOB.md` para a implementação real.
 */
interface RewardedAdHost {
    val state: StateFlow<AdState>
    fun isReady(): Boolean
    suspend fun show(activity: Activity?, placement: AdPlacement): AdResult
}

/**
 * Implementação padrão: finge um anúncio de 5 segundos.
 *
 * Serve para desenvolver e testar o loop de recompensa inteiro sem depender de
 * conta de anúncio, e é o que roda enquanto o AdMob não estiver configurado.
 */
class SimulatedRewardedAdHost(
    private val durationSeconds: Int = 5
) : RewardedAdHost {

    private val _state = MutableStateFlow<AdState>(AdState.Idle)
    override val state: StateFlow<AdState> = _state.asStateFlow()

    override fun isReady(): Boolean = _state.value is AdState.Idle

    override suspend fun show(activity: Activity?, placement: AdPlacement): AdResult {
        if (_state.value !is AdState.Idle) return AdResult.Failed("Já existe um anúncio em exibição")
        return try {
            _state.value = AdState.Loading(placement)
            delay(600)
            for (second in durationSeconds downTo 1) {
                _state.value = AdState.Playing(placement, second)
                delay(1000)
            }
            AdResult.Rewarded
        } finally {
            _state.value = AdState.Idle
        }
    }
}
