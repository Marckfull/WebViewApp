package com.kardiapulse.game.ui.tutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kardiapulse.game.Services
import com.kardiapulse.game.audio.Sfx
import com.kardiapulse.game.core.engine.AiPlayer
import com.kardiapulse.game.core.engine.GameEngine
import com.kardiapulse.game.core.engine.PulseRandom
import com.kardiapulse.game.core.model.Difficulty
import com.kardiapulse.game.core.model.GameConfig
import com.kardiapulse.game.core.model.GameEvent
import com.kardiapulse.game.core.model.GameMode
import com.kardiapulse.game.core.model.GameState
import com.kardiapulse.game.core.model.Move
import com.kardiapulse.game.core.model.Phase
import com.kardiapulse.game.core.model.PowerType
import com.kardiapulse.game.core.model.Side
import com.kardiapulse.game.data.TutorialGoal
import com.kardiapulse.game.data.TutorialStep
import com.kardiapulse.game.data.TutorialSteps
import com.kardiapulse.game.haptics.HapticPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TutorialUiState(
    val game: GameState? = null,
    val stepIndex: Int = 0,
    val goalMet: Boolean = false,
    val selectedCardId: Int? = null,
    val aiThinking: Boolean = false,
    val finished: Boolean = false,
    val toast: String? = null
) {
    val step: TutorialStep get() = TutorialSteps.ALL[stepIndex.coerceIn(0, TutorialSteps.ALL.lastIndex)]
    val isLastStep: Boolean get() = stepIndex >= TutorialSteps.ALL.lastIndex
    val yourTurn: Boolean get() = game?.turn == Side.VOCE && game?.phase == Phase.JOGANDO
}

/**
 * Conduz o tutorial dentro de um duelo real contra a IA mais fraca.
 *
 * A regra do enunciado é literal: cada etapa só destrava depois que a ação daquela etapa foi
 * executada de verdade. O botão de avançar fica desabilitado até `goalMet` virar verdadeiro,
 * e `goalMet` só é ligado observando o que aconteceu no motor.
 */
class TutorialViewModel(private val services: Services) : ViewModel() {

    private val _ui = MutableStateFlow(TutorialUiState())
    val ui: StateFlow<TutorialUiState> = _ui.asStateFlow()

    private val rng = PulseRandom(System.nanoTime())
    private var aiJob: Job? = null

    init {
        viewModelScope.launch {
            val profile = services.repository.current()
            val startStep = profile.tutorialStep.coerceIn(0, TutorialSteps.ALL.lastIndex)
            startBoard(startStep)
        }
    }

    private fun startBoard(stepIndex: Int) {
        val config = GameConfig(
            mode = GameMode.DUELO,
            difficulty = Difficulty.FACIL,
            startHp = 40
        )
        var game = GameEngine.newMatch(
            config = config,
            seed = 20260806L,
            yourName = "Você",
            foeName = "Treino",
            // O tutorial empresta os poderes: a etapa de poderes precisa ter o que usar.
            yourPowers = mapOf(PowerType.DESCARGA to 2, PowerType.INVERSOR to 1, PowerType.ESCUDO to 1)
        )
        game = GameEngine.clearEvents(game)
        _ui.value = TutorialUiState(
            game = game,
            stepIndex = stepIndex,
            goalMet = TutorialSteps.ALL[stepIndex].goal == TutorialGoal.CONTINUAR
        )
    }

    // ------------------------------------------------------------------ ações

    fun selectCard(cardId: Int) {
        if (!_ui.value.yourTurn) return
        services.audio.play(Sfx.TAP)
        services.haptics.perform(HapticPattern.TAP)
        _ui.value = _ui.value.copy(
            selectedCardId = if (_ui.value.selectedCardId == cardId) null else cardId
        )
    }

    fun play(sign: Int) {
        val state = _ui.value
        val game = state.game ?: return
        val cardId = state.selectedCardId ?: return
        val card = game.you.hand.firstOrNull { it.id == cardId } ?: return

        if (!GameEngine.isLegal(game, card, sign)) {
            services.audio.play(Sfx.ERROR)
            services.haptics.perform(HapticPattern.ERROR)
            _ui.value = state.copy(toast = "Essa jogada não é legal aqui. ${state.step.hint}")
            return
        }

        val before = game
        val next = GameEngine.apply(game, Move.Play(cardId, sign))
        _ui.value = state.copy(game = next, selectedCardId = null, toast = null)
        handleTransition(before, next)
    }

    fun usePower(power: PowerType) {
        val state = _ui.value
        val game = state.game ?: return
        if (!state.yourTurn) return
        val next = GameEngine.apply(game, Move.UsePower(power))
        if (next === game) return
        _ui.value = state.copy(game = next, selectedCardId = null)
        handleTransition(game, next)
    }

    fun dismissToast() {
        _ui.value = _ui.value.copy(toast = null)
    }

    /** Só funciona quando a etapa foi realmente cumprida — é o portão do tutorial. */
    fun advance() {
        val state = _ui.value
        if (!state.goalMet) return

        if (state.isLastStep) {
            finish()
            return
        }
        val nextIndex = state.stepIndex + 1
        val nextStep = TutorialSteps.ALL[nextIndex]
        _ui.value = state.copy(
            stepIndex = nextIndex,
            goalMet = nextStep.goal == TutorialGoal.CONTINUAR
        )
        viewModelScope.launch {
            services.repository.update { it.copy(tutorialStep = nextIndex) }
        }
        services.audio.play(Sfx.TAP)
    }

    private fun finish() {
        viewModelScope.launch {
            services.repository.update {
                it.copy(
                    tutorialStep = TutorialSteps.ALL.size,
                    tutorialDone = true,
                    shards = it.shards + TutorialSteps.COMPLETION_SHARDS,
                    crystals = it.crystals + TutorialSteps.COMPLETION_CRYSTALS
                )
            }
            services.audio.play(Sfx.WIN)
            services.haptics.perform(HapticPattern.REWARD)
            _ui.value = _ui.value.copy(finished = true)
        }
    }

    /** Pula o tutorial inteiro, sem a recompensa de conclusão. */
    fun skip() {
        viewModelScope.launch {
            services.repository.update { it.copy(tutorialDone = true) }
            _ui.value = _ui.value.copy(finished = true)
        }
    }

    // ------------------------------------------------------------------ avaliação

    private fun handleTransition(before: GameState, after: GameState) {
        playFeedback(after)
        evaluateGoal(before, after)

        val cleared = GameEngine.clearEvents(after)
        _ui.value = _ui.value.copy(game = cleared)

        when (cleared.phase) {
            Phase.FIM_DE_RODADA -> {
                // No tutorial a rodada recomeça sozinha; ninguém fica preso em uma tela de fim.
                viewModelScope.launch {
                    delay(1400)
                    val restarted = GameEngine.clearEvents(GameEngine.nextRound(cleared))
                    _ui.value = _ui.value.copy(game = restarted, selectedCardId = null)
                    if (restarted.turn == Side.RIVAL) scheduleAi(restarted)
                }
            }
            Phase.FIM_DE_DUELO -> {
                viewModelScope.launch {
                    delay(1200)
                    startBoard(_ui.value.stepIndex)
                }
            }
            Phase.JOGANDO -> if (cleared.turn == Side.RIVAL) scheduleAi(cleared)
        }
    }

    private fun evaluateGoal(before: GameState, after: GameState) {
        val state = _ui.value
        if (state.goalMet) return

        val played = after.events.filterIsInstance<GameEvent.CardPlayed>()
            .firstOrNull { it.by == Side.VOCE }
        val powerUsed = after.events.filterIsInstance<GameEvent.PowerUsed>()
            .any { it.by == Side.VOCE }

        val met = when (state.step.goal) {
            TutorialGoal.CONTINUAR -> true
            TutorialGoal.JOGAR_CARTA -> played != null
            TutorialGoal.SEGUIR_DIRECAO ->
                played != null && !played.flipped && before.direction != 0
            TutorialGoal.INVERTER -> played != null && played.flipped
            TutorialGoal.CORRENTE -> played != null && played.chain >= 3
            TutorialGoal.USAR_PODER -> powerUsed
            TutorialGoal.VENCER_RODADA ->
                after.phase != Phase.JOGANDO && after.lastRoundLoser == Side.RIVAL
        }

        if (met) {
            services.audio.play(Sfx.COIN)
            services.haptics.perform(HapticPattern.REWARD)
            _ui.value = _ui.value.copy(goalMet = true)
        }
    }

    private fun playFeedback(state: GameState) {
        for (event in state.events) {
            when (event) {
                is GameEvent.CardPlayed -> when {
                    event.chain >= 3 -> services.audio.play(Sfx.CHAIN)
                    event.flipped -> services.audio.play(Sfx.FLIP)
                    else -> services.audio.play(Sfx.CARD_PLAY)
                }
                is GameEvent.PowerUsed -> services.audio.play(Sfx.POWER)
                is GameEvent.Overload -> {
                    services.audio.play(Sfx.OVERLOAD)
                    services.haptics.perform(HapticPattern.OVERLOAD)
                }
                else -> Unit
            }
        }
    }

    private fun scheduleAi(state: GameState) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _ui.value = _ui.value.copy(aiThinking = true)
            delay(650)
            val move = withContext(Dispatchers.Default) {
                AiPlayer.chooseMove(state, Difficulty.FACIL, rng)
            }
            _ui.value = _ui.value.copy(aiThinking = false)
            if (move == null) return@launch
            val next = GameEngine.apply(state, move)
            _ui.value = _ui.value.copy(game = next)
            handleTransition(state, next)
        }
    }

    override fun onCleared() {
        aiJob?.cancel()
        super.onCleared()
    }

    class Factory(private val services: Services) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TutorialViewModel(services) as T
    }
}
