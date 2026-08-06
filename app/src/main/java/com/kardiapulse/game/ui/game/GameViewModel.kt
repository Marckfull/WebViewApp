package com.kardiapulse.game.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kardiapulse.game.Services
import com.kardiapulse.game.audio.MusicTrack
import com.kardiapulse.game.audio.Sfx
import com.kardiapulse.game.core.engine.AiPersona
import com.kardiapulse.game.core.engine.AiPlayer
import com.kardiapulse.game.core.engine.GameEngine
import com.kardiapulse.game.core.engine.PulseRandom
import com.kardiapulse.game.core.engine.Replay
import com.kardiapulse.game.core.engine.ReplayCode
import com.kardiapulse.game.core.model.Difficulty
import com.kardiapulse.game.core.model.GameConfig
import com.kardiapulse.game.core.model.GameEvent
import com.kardiapulse.game.core.model.GameMode
import com.kardiapulse.game.core.model.GameState
import com.kardiapulse.game.core.model.Modifier
import com.kardiapulse.game.core.model.Move
import com.kardiapulse.game.core.model.Phase
import com.kardiapulse.game.core.model.PowerType
import com.kardiapulse.game.core.model.Side
import com.kardiapulse.game.data.Achievements
import com.kardiapulse.game.data.DailyPass
import com.kardiapulse.game.data.Economy
import com.kardiapulse.game.data.MatchOutcome
import com.kardiapulse.game.data.PlayerProfile
import com.kardiapulse.game.data.Rewards
import com.kardiapulse.game.haptics.HapticPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** O resultado de um duelo, já com recompensas calculadas, pronto para a tela final. */
data class MatchResult(
    val won: Boolean,
    val rewards: Rewards,
    val newAchievements: List<String>,
    val leveledUp: Boolean,
    val endlessStreak: Int,
    val rewardDoubled: Boolean = false,
    /** O duelo inteiro em uma string curta. Ver [ReplayCode]. */
    val replayCode: String = ""
)

data class GameUiState(
    val game: GameState? = null,
    val profile: PlayerProfile = PlayerProfile(),
    val selectedCardId: Int? = null,
    val aiThinking: Boolean = false,
    val secondsLeft: Int = 0,
    val toast: String? = null,
    val matchResult: MatchResult? = null,
    val endlessStreak: Int = 0,
    val reviveOffered: Boolean = false,
    /** Eventos do último passo, para a UI animar. O motor já os consumiu. */
    val fxEvents: List<GameEvent> = emptyList(),
    val fxTick: Int = 0,
    val canUndo: Boolean = false,
    val rivalPersona: AiPersona = AiPersona.EQUILIBRADO
) {
    val phase: Phase? get() = game?.phase
    val yourTurn: Boolean get() = game?.turn == Side.VOCE && game?.phase == Phase.JOGANDO

    /** Verdadeiro quando o jogador está encurralado e só um poder de resgate resolve. */
    val cornered: Boolean
        get() {
            val g = game ?: return false
            return yourTurn &&
                GameEngine.legalPlays(g).isEmpty() &&
                GameEngine.rescuePowers(g, Side.VOCE).isNotEmpty()
        }
}

class GameViewModel(
    private val services: Services,
    private val mode: GameMode,
    private val startDifficulty: Difficulty
) : ViewModel() {

    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private var aiJob: Job? = null
    private var timerJob: Job? = null
    private var rng = PulseRandom(System.nanoTime())
    private var difficulty = startDifficulty
    private var carriedHp = 0
    private var persona = AiPersona.EQUILIBRADO

    /** Uma entrada por ação sua, para o Recuo saber exatamente até onde voltar. */
    private data class UndoPoint(
        val state: GameState,
        val moveCount: Int,
        val refundPower: PowerType?
    )

    private val history = ArrayDeque<UndoPoint>()
    private val recordedMoves = ArrayList<Move>()
    private var replaySeed = 0L
    private var replayYourHp = 0
    private var replayFoeHp = 0
    private var replayYourPowers: Map<PowerType, Int> = emptyMap()
    private var replayFoePowers: Map<PowerType, Int> = emptyMap()

    init {
        viewModelScope.launch {
            val profile = services.repository.current()
            _ui.value = _ui.value.copy(profile = profile)
            services.audio.play(MusicTrack.DUELO)
            startMatch(profile)
        }
    }

    // ------------------------------------------------------------------ montagem

    private fun buildConfig(): GameConfig {
        val modifiers: Set<Modifier> = when (mode) {
            GameMode.CAOS -> Modifier.ALL.shuffled().take(2).toSet()
            GameMode.DIARIO -> {
                val day = DailyPass.todayEpochDay()
                val pool = Modifier.ALL
                setOf(pool[(day % pool.size).toInt()])
            }
            else -> emptySet()
        }
        return GameConfig(
            mode = mode,
            difficulty = difficulty,
            modifiers = modifiers,
            turnSeconds = if (mode == GameMode.BLITZ) 12 else 0
        )
    }

    private fun seedFor(): Long = when (mode) {
        // O desafio do dia precisa ser idêntico para todo mundo: a semente é a data.
        GameMode.DIARIO -> DailyPass.todayEpochDay() * 8191L + 104729L
        else -> System.nanoTime()
    }

    private fun startMatch(profile: PlayerProfile) {
        val config = buildConfig()
        val seed = seedFor()
        rng = PulseRandom(seed xor System.nanoTime())
        persona = pickPersona()

        val yourHp = if (mode == GameMode.SOBREVIVENCIA && carriedHp > 0) carriedHp else config.startHp
        val foePowers = rivalPowers()

        val game = GameEngine.newMatch(
            config = config,
            seed = seed,
            yourName = "Você",
            foeName = rivalName(),
            yourPowers = profile.powers,
            foePowers = foePowers,
            yourHp = yourHp,
            foeHp = config.startHp
        )

        // Cabeçalho do replay: com isto mais a lista de jogadas, o duelo é reconstruível.
        history.clear()
        recordedMoves.clear()
        replaySeed = seed
        replayYourHp = yourHp
        replayFoeHp = config.startHp
        replayYourPowers = profile.powers
        replayFoePowers = foePowers

        _ui.value = _ui.value.copy(
            game = game,
            selectedCardId = null,
            matchResult = null,
            reviveOffered = false,
            toast = null,
            canUndo = false,
            rivalPersona = persona
        )
        afterStateChange(game)
    }

    /**
     * O temperamento do rival. Na Sobrevivência ele é estável por posição — o quarto adversário
     * é sempre o mesmo sujeito, com o mesmo jeito de jogar.
     */
    private fun pickPersona(): AiPersona = when (mode) {
        GameMode.SOBREVIVENCIA -> AiPersona.forIndex(_ui.value.endlessStreak)
        GameMode.DIARIO -> AiPersona.forIndex(DailyPass.todayEpochDay().toInt())
        else -> AiPersona.ALL[rng.nextInt(AiPersona.ALL.size)]
    }

    private fun rivalName(): String {
        val names = listOf(
            "Vex", "Mira", "Órion", "Talia", "Nero", "Sova", "Kael", "Íris", "Dorn", "Lyra"
        )
        return if (mode == GameMode.SOBREVIVENCIA) {
            names[_ui.value.endlessStreak % names.size]
        } else {
            names[rng.nextInt(names.size)]
        }
    }

    /** A partir de certa altura da Sobrevivência o rival também entra com poderes. */
    private fun rivalPowers(): Map<PowerType, Int> {
        val streak = _ui.value.endlessStreak
        if (mode != GameMode.SOBREVIVENCIA || streak < 3) return emptyMap()
        val count = (streak / 3).coerceAtMost(3)
        return buildMap {
            repeat(count) {
                val type = PowerType.ALL[rng.nextInt(PowerType.ALL.size)]
                put(type, (get(type) ?: 0) + 1)
            }
        }
    }

    // ------------------------------------------------------------------ ações

    fun selectCard(cardId: Int) {
        val game = _ui.value.game ?: return
        if (!_ui.value.yourTurn) return
        services.audio.play(Sfx.TAP)
        services.haptics.perform(HapticPattern.TAP)
        _ui.value = _ui.value.copy(
            selectedCardId = if (_ui.value.selectedCardId == cardId) null else cardId
        )
        // Mantém o seletor de polaridade só quando a carta tem alguma jogada legal.
        val id = _ui.value.selectedCardId ?: return
        val card = game.you.hand.firstOrNull { it.id == id } ?: return
        if (!GameEngine.isLegal(game, card, 1) && !GameEngine.isLegal(game, card, -1)) {
            services.audio.play(Sfx.ERROR)
            services.haptics.perform(HapticPattern.ERROR)
            _ui.value = _ui.value.copy(
                selectedCardId = null,
                toast = "Essa carta não tem jogada legal agora."
            )
        }
    }

    fun playSelected(sign: Int) {
        val state = _ui.value
        val game = state.game ?: return
        val cardId = state.selectedCardId ?: return
        val card = game.you.hand.firstOrNull { it.id == cardId } ?: return

        if (!GameEngine.isLegal(game, card, sign)) {
            services.audio.play(Sfx.ERROR)
            services.haptics.perform(HapticPattern.ERROR)
            _ui.value = state.copy(toast = illegalReason(game, card, sign))
            return
        }

        timerJob?.cancel()
        pushUndoPoint(game, refundPower = null)
        val move = Move.Play(cardId, sign)
        recordedMoves.add(move)
        val next = GameEngine.apply(game, move)
        _ui.value = state.copy(game = next, selectedCardId = null, toast = null)
        afterStateChange(next)
    }

    fun usePower(power: PowerType) {
        val state = _ui.value
        val game = state.game ?: return
        if (!state.yourTurn) return
        if (game.you.powerCount(power) <= 0) return

        val move = Move.UsePower(power)
        val next = GameEngine.apply(game, move)
        if (next === game) return

        pushUndoPoint(game, refundPower = power)
        recordedMoves.add(move)

        // O poder gasto no duelo sai do inventário permanente.
        viewModelScope.launch {
            val updated = services.repository.update { it.withPower(power, -1) }
            _ui.value = _ui.value.copy(profile = updated)
        }

        _ui.value = state.copy(game = next, selectedCardId = null)
        afterStateChange(next)
    }

    private fun pushUndoPoint(state: GameState, refundPower: PowerType?) {
        history.addLast(UndoPoint(state, recordedMoves.size, refundPower))
        // Um duelo longo não precisa de memória infinita para um recurso que se compra por unidade.
        while (history.size > MAX_UNDO_DEPTH) history.removeFirst()
    }

    /**
     * O Recuo: desfaz a sua última ação e tudo que o rival respondeu depois dela.
     *
     * Como o estado do jogo é imutável, "desfazer" é literalmente voltar a apontar para o estado
     * anterior — não existe reversão de mutação para dar errado. Se a ação desfeita gastou um
     * poder, ele volta para o inventário.
     */
    fun undo() {
        val state = _ui.value
        if (state.profile.undoCharges <= 0) {
            _ui.value = state.copy(toast = "Sem cargas de Recuo. Dá para comprar na Loja.")
            return
        }
        if (state.game?.phase != Phase.JOGANDO) return
        val point = history.removeLastOrNull() ?: return

        aiJob?.cancel()
        timerJob?.cancel()
        while (recordedMoves.size > point.moveCount) recordedMoves.removeAt(recordedMoves.size - 1)

        viewModelScope.launch {
            val updated = services.repository.update { profile ->
                val refunded = point.refundPower?.let { profile.withPower(it, 1) } ?: profile
                refunded.copy(undoCharges = (refunded.undoCharges - 1).coerceAtLeast(0))
            }
            _ui.value = _ui.value.copy(profile = updated)
        }

        services.audio.play(Sfx.POWER)
        services.haptics.perform(HapticPattern.POWER)
        _ui.value = _ui.value.copy(
            game = point.state,
            selectedCardId = null,
            toast = "Recuo: o Núcleo voltou ao que era.",
            matchResult = null,
            canUndo = history.isNotEmpty()
        )
        // Volta a contar o tempo do seu turno, se o modo tiver relógio.
        if (point.state.turn == Side.VOCE) startTurnTimer(point.state)
    }

    fun continueToNextRound() {
        val game = _ui.value.game ?: return
        if (game.phase != Phase.FIM_DE_RODADA) return
        val next = GameEngine.nextRound(game)
        _ui.value = _ui.value.copy(game = next, selectedCardId = null)
        afterStateChange(next)
    }

    fun concede() {
        val game = _ui.value.game ?: return
        if (game.phase != Phase.JOGANDO) return
        val next = GameEngine.apply(game, Move.Concede)
        _ui.value = _ui.value.copy(game = next)
        afterStateChange(next)
    }

    fun dismissToast() {
        _ui.value = _ui.value.copy(toast = null)
    }

    // ------------------------------------------------------------------ ciclo

    /** Consome eventos (som, vibração), depois decide se a IA joga ou se o duelo acabou. */
    private fun afterStateChange(state: GameState) {
        consumeEvents(state)
        val cleared = GameEngine.clearEvents(state)
        // Os eventos seguem para a UI animar; o motor já não precisa deles.
        _ui.value = _ui.value.copy(
            game = cleared,
            fxEvents = state.events,
            fxTick = _ui.value.fxTick + 1,
            canUndo = history.isNotEmpty() && cleared.phase == Phase.JOGANDO
        )

        when (cleared.phase) {
            Phase.FIM_DE_DUELO -> finishMatch(cleared)
            Phase.FIM_DE_RODADA -> {
                aiJob?.cancel()
                timerJob?.cancel()
            }
            Phase.JOGANDO -> {
                if (cleared.turn == Side.RIVAL) scheduleAiTurn(cleared)
                else startTurnTimer(cleared)
            }
        }
    }

    private fun consumeEvents(state: GameState) {
        for (event in state.events) {
            when (event) {
                is GameEvent.CardPlayed -> {
                    when {
                        event.chain >= 3 -> {
                            services.audio.play(Sfx.CHAIN, rate = 1f + (event.chain - 3) * 0.06f)
                            services.haptics.perform(HapticPattern.CHAIN)
                        }
                        event.flipped -> {
                            services.audio.play(Sfx.FLIP)
                            services.haptics.perform(HapticPattern.FLIP)
                        }
                        else -> {
                            services.audio.play(Sfx.CARD_PLAY, rate = 0.92f + event.card.value * 0.02f)
                            if (event.by == Side.VOCE) services.haptics.perform(HapticPattern.CARD)
                        }
                    }
                }
                is GameEvent.PowerUsed -> {
                    services.audio.play(Sfx.POWER)
                    services.haptics.perform(HapticPattern.POWER)
                }
                is GameEvent.Gravity -> {
                    services.audio.play(Sfx.COLLAPSE, volume = 0.5f)
                }
                is GameEvent.Overload -> {
                    services.audio.play(Sfx.OVERLOAD)
                    services.haptics.perform(HapticPattern.OVERLOAD)
                }
                is GameEvent.MatchOver -> {
                    if (event.winner == Side.VOCE) {
                        services.audio.play(Sfx.WIN)
                        services.haptics.perform(HapticPattern.WIN)
                    } else {
                        services.audio.play(Sfx.LOSE)
                        services.haptics.perform(HapticPattern.LOSE)
                    }
                }
            }
        }
    }

    private fun scheduleAiTurn(state: GameState) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _ui.value = _ui.value.copy(aiThinking = true)
            // Uma pausa curta antes de jogar: sem isso a IA responde rápido demais e o duelo
            // fica ilegível.
            delay(420L + rng.nextInt(360).toLong())
            val move = withContext(Dispatchers.Default) {
                AiPlayer.chooseMove(state, difficulty, rng, persona)
            }
            _ui.value = _ui.value.copy(aiThinking = false)
            if (move == null) return@launch
            recordedMoves.add(move)
            val next = GameEngine.apply(state, move)
            _ui.value = _ui.value.copy(game = next)
            afterStateChange(next)
        }
    }

    /** Relógio do modo Blitz. Se o tempo acabar, o motor joga por você. */
    private fun startTurnTimer(state: GameState) {
        timerJob?.cancel()
        val seconds = state.config.turnSeconds
        if (seconds <= 0) {
            _ui.value = _ui.value.copy(secondsLeft = 0)
            return
        }
        timerJob = viewModelScope.launch {
            var left = seconds
            _ui.value = _ui.value.copy(secondsLeft = left)
            while (isActive && left > 0) {
                delay(1000)
                left--
                _ui.value = _ui.value.copy(secondsLeft = left)
                if (left in 1..3) services.audio.play(Sfx.TICK)
            }
            if (!isActive) return@launch
            val current = _ui.value.game ?: return@launch
            if (current.phase != Phase.JOGANDO || current.turn != Side.VOCE) return@launch
            val fallback = GameEngine.legalPlays(current).randomOrNull()
            if (fallback != null) {
                _ui.value = _ui.value.copy(toast = "O tempo acabou! O Núcleo jogou por você.")
                pushUndoPoint(current, refundPower = null)
                recordedMoves.add(fallback)
                val next = GameEngine.apply(current, fallback)
                _ui.value = _ui.value.copy(game = next, selectedCardId = null)
                afterStateChange(next)
            }
        }
    }

    // ------------------------------------------------------------------ fim de duelo

    private fun finishMatch(state: GameState) {
        aiJob?.cancel()
        timerJob?.cancel()
        val won = state.winner == Side.VOCE

        viewModelScope.launch {
            val streak = if (won && mode == GameMode.SOBREVIVENCIA) {
                _ui.value.endlessStreak + 1
            } else {
                _ui.value.endlessStreak
            }

            val outcome = MatchOutcome(
                mode = mode,
                difficulty = difficulty,
                won = won,
                rounds = state.round,
                bestChain = state.statsBestChain,
                flips = state.statsFlips,
                hpLeft = state.you.hp,
                endlessStreak = streak
            )
            val rewards = Economy.rewardsFor(outcome)

            val before = services.repository.current()
            var newAchievements: List<String> = emptyList()
            val after = services.repository.update { profile ->
                var updated = Economy.apply(profile, outcome, rewards)
                if (mode == GameMode.DIARIO && won) {
                    updated = updated.copy(
                        dailyChallengeDay = DailyPass.todayEpochDay(),
                        dailyChallengeWon = true
                    )
                }
                val (withAchievements, unlocked) = Achievements.claimNewly(updated)
                newAchievements = unlocked.map { it.name }
                withAchievements
            }

            if (newAchievements.isNotEmpty()) {
                services.audio.play(Sfx.COIN)
                services.haptics.perform(HapticPattern.REWARD)
            }

            carriedHp = if (won && mode == GameMode.SOBREVIVENCIA) {
                // Sobrevivência recupera um pouco de vida entre rivais, mas nunca tudo.
                (state.you.hp + 12).coerceAtMost(state.config.startHp)
            } else 0

            _ui.value = _ui.value.copy(
                profile = after,
                endlessStreak = streak,
                matchResult = MatchResult(
                    won = won,
                    rewards = rewards,
                    newAchievements = newAchievements,
                    leveledUp = after.level > before.level,
                    endlessStreak = streak,
                    replayCode = buildReplayCode()
                ),
                reviveOffered = !won && mode == GameMode.SOBREVIVENCIA && streak > 0
            )
        }
    }

    /** Zera o contador de duelos depois que um intersticial foi realmente exibido. */
    fun noteInterstitialShown() {
        viewModelScope.launch {
            val updated = services.repository.update { it.copy(duelsSinceInterstitial = 0) }
            _ui.value = _ui.value.copy(profile = updated)
        }
    }

    /** Chamado quando o anúncio premiado de dobrar recompensa foi assistido até o fim. */
    fun applyDoubledReward() {
        val result = _ui.value.matchResult ?: return
        if (result.rewardDoubled) return
        viewModelScope.launch {
            val bonus = result.rewards
            val updated = services.repository.update {
                it.copy(
                    xp = it.xp + bonus.xp,
                    shards = it.shards + bonus.shards,
                    crystals = it.crystals + bonus.crystals
                )
            }
            services.audio.play(Sfx.COIN)
            services.haptics.perform(HapticPattern.REWARD)
            _ui.value = _ui.value.copy(
                profile = updated,
                matchResult = result.copy(rewardDoubled = true)
            )
        }
    }

    /** Continuar na Sobrevivência depois de um anúncio premiado. */
    fun revive() {
        val game = _ui.value.game ?: return
        carriedHp = (game.config.startHp / 2).coerceAtLeast(15)
        _ui.value = _ui.value.copy(reviveOffered = false, matchResult = null)
        viewModelScope.launch {
            val profile = services.repository.current()
            _ui.value = _ui.value.copy(profile = profile)
            startMatch(profile)
        }
    }

    /** Próximo rival da Sobrevivência, ou uma revanche nos outros modos. */
    fun playAgain() {
        if (mode == GameMode.SOBREVIVENCIA && _ui.value.matchResult?.won == true) {
            difficulty = escalate(difficulty, _ui.value.endlessStreak)
        } else if (mode == GameMode.SOBREVIVENCIA) {
            carriedHp = 0
            difficulty = startDifficulty
            _ui.value = _ui.value.copy(endlessStreak = 0)
        }
        viewModelScope.launch {
            val profile = services.repository.current()
            _ui.value = _ui.value.copy(profile = profile, matchResult = null, reviveOffered = false)
            startMatch(profile)
        }
    }

    /** Empacota o duelo que acabou de terminar em um código compartilhável. */
    private fun buildReplayCode(): String = runCatching {
        val game = _ui.value.game ?: return@runCatching ""
        ReplayCode.encode(
            Replay(
                seed = replaySeed,
                mode = game.config.mode,
                difficulty = game.config.difficulty,
                modifiers = game.config.modifiers,
                yourHp = replayYourHp,
                foeHp = replayFoeHp,
                yourPowers = replayYourPowers,
                foePowers = replayFoePowers,
                moves = recordedMoves.toList()
            )
        )
    }.getOrDefault("")

    private fun escalate(current: Difficulty, streak: Int): Difficulty = when {
        streak >= 9 -> Difficulty.MESTRE
        streak >= 5 -> Difficulty.DIFICIL
        streak >= 2 -> Difficulty.NORMAL
        else -> current
    }

    override fun onCleared() {
        aiJob?.cancel()
        timerJob?.cancel()
        super.onCleared()
    }

    private fun illegalReason(game: GameState, card: com.kardiapulse.game.core.model.Card, sign: Int): String {
        val after = game.nucleus + sign * card.value
        return when {
            kotlin.math.abs(after) > game.limitAfterNextCard ->
                "Isso levaria o Núcleo a $after e o limite é ±${game.limitAfterNextCard}. Sobrecarga."
            game.direction != 0 && sign != game.direction ->
                "A direção está travada. Só uma carta de ${game.lastElement?.ptName ?: "qualquer elemento"} (ou Éter) inverte."
            else -> "Jogada inválida."
        }
    }

    private companion object {
        /** Profundidade máxima do histórico de Recuo. */
        const val MAX_UNDO_DEPTH = 40
    }

    class Factory(
        private val services: Services,
        private val mode: GameMode,
        private val difficulty: Difficulty
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GameViewModel(services, mode, difficulty) as T
    }
}
