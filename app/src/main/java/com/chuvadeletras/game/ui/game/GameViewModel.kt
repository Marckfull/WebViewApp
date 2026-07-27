package com.chuvadeletras.game.ui.game

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chuvadeletras.game.ads.AdPlacement
import com.chuvadeletras.game.ads.AdResult
import com.chuvadeletras.game.ads.RewardedAdHost
import com.chuvadeletras.game.data.MatchOutcome
import com.chuvadeletras.game.data.PlayerProfile
import com.chuvadeletras.game.data.PlayerRepository
import com.chuvadeletras.game.data.PuzzleRepository
import com.chuvadeletras.game.domain.engine.GameRules
import com.chuvadeletras.game.domain.model.Difficulty
import com.chuvadeletras.game.domain.model.GameConfig
import com.chuvadeletras.game.domain.model.GameEvent
import com.chuvadeletras.game.domain.model.GameMode
import com.chuvadeletras.game.domain.model.GameState
import com.chuvadeletras.game.domain.model.GameStatus
import com.chuvadeletras.game.domain.model.GridPos
import com.chuvadeletras.game.domain.model.PowerUp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

data class GameUiState(
    val game: GameState? = null,
    val profile: PlayerProfile = PlayerProfile(),
    val outcome: MatchOutcome? = null,
    val paused: Boolean = false,
    /** Power-up esperando o jogador tocar no alvo (letra ou quadradinho). */
    val pendingPowerUp: PowerUp? = null,
    val message: String? = null,
    val tutorialStep: Int = -1,
    val rewardDoubled: Boolean = false,
    val stageIntro: String? = null
) {
    val isTutorial: Boolean get() = tutorialStep >= 0
}

class GameViewModel(
    private val mode: GameMode,
    private val requestedDifficulty: Difficulty,
    private val playerRepository: PlayerRepository,
    private val puzzleRepository: PuzzleRepository,
    private val adHost: RewardedAdHost
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    val adState = adHost.state

    private var random = Random(System.nanoTime())
    private var timerJob: Job? = null
    private var autoRoundJob: Job? = null
    private var stage = 1
    private var campaignLevel: Int? = null

    init {
        viewModelScope.launch {
            val profile = playerRepository.current()
            campaignLevel = if (mode == GameMode.CLASSICO) profile.campaignLevel else null
            _state.value = _state.value.copy(
                profile = profile,
                tutorialStep = if (!profile.tutorialDone) 0 else -1
            )
            startMatch(profile, requestedDifficulty)
        }
        viewModelScope.launch {
            playerRepository.profile.collect { profile ->
                _state.value = _state.value.copy(profile = profile)
            }
        }
    }

    // ------------------------------------------------------------------
    // Ciclo de vida da partida
    // ------------------------------------------------------------------

    private fun startMatch(
        profile: PlayerProfile,
        difficulty: Difficulty,
        carryScore: Int = 0,
        carryLives: Int? = null
    ) {
        val puzzle = puzzleRepository.create(mode, difficulty, stage = stage)
        val seed = puzzle.id.hashCode().toLong()
        random = Random(seed xor System.nanoTime())

        val config = GameConfig.forMode(
            mode = mode,
            difficulty = difficulty,
            seed = seed,
            cellCount = puzzle.cellCount,
            assist = _state.value.isTutorial
        )
        val game = GameRules.newGame(
            puzzle = puzzle,
            config = config,
            powerUps = profile.powerUps,
            random = random,
            stageIndex = stage,
            carryOverScore = carryScore,
            carryOverLives = carryLives
        )
        _state.value = _state.value.copy(game = game, outcome = null, pendingPowerUp = null)
        restartTimer()
    }

    private fun restartTimer() {
        timerJob?.cancel()
        val game = _state.value.game ?: return
        if (game.config.secondsPerRound == null) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _state.value
                if (current.paused || current.isTutorial) continue
                val running = current.game ?: continue
                if (running.isOver) return@launch
                val ticked = GameRules.tick(running)
                _state.value = current.copy(game = ticked)
                if (ticked.secondsLeft <= 0) endRound()
            }
        }
    }

    /** Depois de gastar a última jogada, a rodada vira sozinha — com uma pausa
     * para a animação de evaporação acontecer na tela. */
    private fun scheduleAutoRound() {
        autoRoundJob?.cancel()
        autoRoundJob = viewModelScope.launch {
            delay(750)
            val game = _state.value.game ?: return@launch
            if (!game.isOver && game.movesLeft <= 0) endRound()
        }
    }

    // ------------------------------------------------------------------
    // Ações do jogador
    // ------------------------------------------------------------------

    fun onCellTap(pos: GridPos) {
        val current = _state.value
        val game = current.game ?: return

        // Reparo pendente: o toque escolhe qual quadradinho destravar.
        if (current.pendingPowerUp == PowerUp.REPARO) {
            val repaired = GameRules.repairCell(game, pos)
            if (repaired !== game) {
                update(repaired, pendingPowerUp = null, message = "Quadradinho recuperado!")
                syncPowerUps(repaired)
            } else {
                _state.value = current.copy(
                    pendingPowerUp = null,
                    message = "O Reparo só funciona em quadradinho trancado"
                )
            }
            return
        }
        update(GameRules.selectCell(game, pos))
    }

    fun onClueTap(entryId: Int) {
        val game = _state.value.game ?: return
        update(GameRules.selectEntry(game, entryId))
    }

    fun onTileTap(tileId: Long) {
        val current = _state.value
        val game = current.game ?: return

        if (current.pendingPowerUp == PowerUp.CONGELAR) {
            val frozen = GameRules.freezeTile(game, tileId)
            update(frozen, pendingPowerUp = null, message = "Letra congelada para a próxima rodada")
            syncPowerUps(frozen)
            return
        }

        if (game.movesLeft <= 0) {
            _state.value = current.copy(message = "Acabaram as jogadas desta rodada")
            return
        }
        if (game.selected == null || game.cells[game.selected]?.isOpen != true) {
            _state.value = current.copy(message = "Escolha primeiro um quadradinho vazio")
            return
        }

        val played = GameRules.placeTile(game, tileId, random)
        update(played)
        if (played.status == GameStatus.PLAYING && played.movesLeft <= 0) scheduleAutoRound()
        if (played.isOver) finishMatch(played)
    }

    fun onClearCell(pos: GridPos) {
        val game = _state.value.game ?: return
        update(GameRules.clearCell(game, pos))
    }

    fun endRound() {
        autoRoundJob?.cancel()
        val game = _state.value.game ?: return
        if (game.isOver) return
        val next = GameRules.endRound(game, random)
        update(next)
        restartTimer()
        if (next.isOver) finishMatch(next)
    }

    fun consumeEvent() {
        val game = _state.value.game ?: return
        if (game.event != null) _state.value = _state.value.copy(game = GameRules.consumeEvent(game))
    }

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun setPaused(paused: Boolean) {
        _state.value = _state.value.copy(paused = paused)
    }

    // ------------------------------------------------------------------
    // Power-ups
    // ------------------------------------------------------------------

    fun onPowerUp(powerUp: PowerUp) {
        val current = _state.value
        val game = current.game ?: return

        if (game.powerUps[powerUp].orZero() <= 0) {
            _state.value = current.copy(message = "Você não tem ${powerUp.label}. Passe na loja!")
            return
        }

        when (powerUp) {
            PowerUp.CONGELAR, PowerUp.REPARO -> {
                val hint = if (powerUp == PowerUp.CONGELAR) {
                    "Toque na letra que você quer segurar"
                } else {
                    "Toque no quadradinho trancado"
                }
                _state.value = current.copy(pendingPowerUp = powerUp, message = hint)
            }

            PowerUp.TROCAR -> {
                val swapped = GameRules.swapTray(game, random)
                update(swapped, message = "Bandeja trocada!")
                syncPowerUps(swapped)
            }

            PowerUp.REVELAR -> {
                val revealed = GameRules.revealCell(game, random)
                if (revealed === game) {
                    _state.value = current.copy(message = "Escolha um quadradinho vazio primeiro")
                } else {
                    update(revealed)
                    syncPowerUps(revealed)
                    if (revealed.isOver) finishMatch(revealed)
                }
            }

            PowerUp.TEMPO -> {
                if (game.config.secondsPerRound == null) {
                    _state.value = current.copy(message = "Este modo não tem relógio")
                } else {
                    val timed = GameRules.addTime(game)
                    update(timed, message = "+20 segundos!")
                    syncPowerUps(timed)
                }
            }
        }
    }

    fun cancelPendingPowerUp() {
        _state.value = _state.value.copy(pendingPowerUp = null, message = null)
    }

    private fun syncPowerUps(game: GameState) {
        viewModelScope.launch { playerRepository.syncPowerUps(game.powerUps) }
    }

    // ------------------------------------------------------------------
    // Anúncios recompensados
    // ------------------------------------------------------------------

    fun watchAd(activity: Activity?, placement: AdPlacement) {
        viewModelScope.launch {
            when (adHost.show(activity, placement)) {
                AdResult.Rewarded -> {
                    playerRepository.registerAdWatched()
                    applyAdReward(placement)
                }

                AdResult.Dismissed ->
                    _state.value = _state.value.copy(message = "Anúncio fechado antes do fim — sem prêmio")

                is AdResult.Failed ->
                    _state.value = _state.value.copy(message = "Não deu para carregar o anúncio agora")
            }
        }
    }

    private suspend fun applyAdReward(placement: AdPlacement) {
        val game = _state.value.game ?: return
        when (placement) {
            AdPlacement.EXTRA_MOVES -> {
                autoRoundJob?.cancel()
                update(GameRules.grantExtraMoves(game, 3), message = "+3 jogadas!")
            }

            AdPlacement.REVIVE -> {
                val revived = GameRules.revive(game, random)
                _state.value = _state.value.copy(game = revived, outcome = null)
                restartTimer()
            }

            AdPlacement.REPAIR_CELL -> {
                val target = game.lockedCells.firstOrNull()
                if (target == null) {
                    _state.value = _state.value.copy(message = "Nenhum quadradinho trancado por aqui")
                } else {
                    val repaired = GameRules.repairCell(
                        GameRules.grantPowerUp(game, PowerUp.REPARO),
                        target
                    )
                    update(repaired, message = "Quadradinho recuperado!")
                }
            }

            AdPlacement.DOUBLE_MATCH_REWARD -> {
                val outcome = _state.value.outcome ?: return
                if (_state.value.rewardDoubled) return
                playerRepository.addCoins(outcome.summary.coinsEarned)
                _state.value = _state.value.copy(
                    rewardDoubled = true,
                    message = "Prêmio dobrado: +${outcome.summary.coinsEarned} gotas"
                )
            }

            AdPlacement.FREE_COINS -> {
                playerRepository.addCoins(100)
                _state.value = _state.value.copy(message = "+100 gotas!")
            }

            AdPlacement.DOUBLE_DAILY -> Unit
        }
    }

    // ------------------------------------------------------------------
    // Fim de partida
    // ------------------------------------------------------------------

    private fun finishMatch(game: GameState) {
        timerJob?.cancel()
        autoRoundJob?.cancel()

        // No Dilúvio, vencer não termina nada: emenda na próxima grade, maior.
        if (game.status == GameStatus.WON && mode == GameMode.DILUVIO) {
            viewModelScope.launch {
                _state.value = _state.value.copy(
                    stageIntro = "Grade ${stage} concluída! Preparando a próxima…"
                )
                delay(1600)
                stage++
                val profile = playerRepository.profile.first()
                startMatch(
                    profile = profile,
                    difficulty = Difficulty.forLevel(stage * 3),
                    carryScore = game.score,
                    carryLives = game.lives
                )
                _state.value = _state.value.copy(stageIntro = null)
            }
            return
        }

        viewModelScope.launch {
            playerRepository.syncPowerUps(game.powerUps)
            val summary = game.summary ?: GameRules.buildSummary(game, game.status == GameStatus.WON)
            val outcome = playerRepository.registerMatch(summary, campaignLevel)
            _state.value = _state.value.copy(outcome = outcome, rewardDoubled = false)
        }
    }

    fun playAgain() {
        viewModelScope.launch {
            stage = 1
            val profile = playerRepository.current()
            campaignLevel = if (mode == GameMode.CLASSICO) profile.campaignLevel else null
            _state.value = _state.value.copy(outcome = null, rewardDoubled = false, message = null)
            startMatch(profile, requestedDifficulty)
        }
    }

    // ------------------------------------------------------------------
    // Tutorial
    // ------------------------------------------------------------------

    fun nextTutorialStep() {
        val step = _state.value.tutorialStep
        if (step < 0) return
        if (step >= TutorialSteps.all.lastIndex) {
            finishTutorial()
        } else {
            _state.value = _state.value.copy(tutorialStep = step + 1)
        }
    }

    fun finishTutorial() {
        _state.value = _state.value.copy(tutorialStep = -1)
        viewModelScope.launch { playerRepository.markTutorialDone() }
    }

    fun restartTutorial() {
        _state.value = _state.value.copy(tutorialStep = 0)
    }

    private fun update(
        game: GameState,
        pendingPowerUp: PowerUp? = _state.value.pendingPowerUp,
        message: String? = null
    ) {
        _state.value = _state.value.copy(
            game = game,
            pendingPowerUp = pendingPowerUp,
            message = message ?: _state.value.message
        )
    }

    private fun Int?.orZero(): Int = this ?: 0

    override fun onCleared() {
        timerJob?.cancel()
        autoRoundJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val mode: GameMode,
        private val difficulty: Difficulty,
        private val playerRepository: PlayerRepository,
        private val puzzleRepository: PuzzleRepository,
        private val adHost: RewardedAdHost
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GameViewModel(mode, difficulty, playerRepository, puzzleRepository, adHost) as T
    }
}

/** Roteiro do tutorial de primeira partida. */
data class TutorialStep(
    val title: String,
    val body: String,
    val anchor: String?
)

object TutorialSteps {
    const val ANCHOR_GRID = "grid"
    const val ANCHOR_TRAY = "tray"
    const val ANCHOR_CLUE = "clue"
    const val ANCHOR_MOVES = "moves"
    const val ANCHOR_POWERUPS = "powerups"

    val all = listOf(
        TutorialStep(
            "Bem-vindo à Chuva de Letras! 🌧️",
            "Aqui a cruzadinha tem uma reviravolta: as letras caem do céu e não " +
                "esperam por você. Vou te mostrar em 30 segundos.",
            null
        ),
        TutorialStep(
            "1. A dica manda no jogo",
            "Leia a dica e toque nela — ou toque direto num quadradinho. " +
                "A palavra escolhida fica destacada na grade.",
            ANCHOR_CLUE
        ),
        TutorialStep(
            "2. A grade é o seu tabuleiro",
            "O quadradinho azul é o que está selecionado. Tocar de novo no mesmo " +
                "quadradinho alterna entre a palavra deitada e a em pé.",
            ANCHOR_GRID
        ),
        TutorialStep(
            "3. A bandeja é a sua chuva",
            "Estas são as letras da rodada. Toque em uma para encaixá-la no " +
                "quadradinho selecionado.",
            ANCHOR_TRAY
        ),
        TutorialStep(
            "4. Jogadas são limitadas",
            "Cada rodada te dá poucas jogadas. Quando elas acabam, a bandeja é " +
                "trocada e vem chuva nova.",
            ANCHOR_MOVES
        ),
        TutorialStep(
            "5. Aqui está a pegadinha ⚠️",
            "Letra que sobra evapora. E se você virar a rodada com jogada sobrando, " +
                "um quadradinho que precisava daquela letra TRANCA — você perde " +
                "aquele espaço. Use todas as suas jogadas!",
            ANCHOR_GRID
        ),
        TutorialStep(
            "6. Socorro em forma de item",
            "Congele uma letra, troque a bandeja, revele uma letra ou repare um " +
                "quadradinho trancado. Ganhe itens jogando todo dia.",
            ANCHOR_POWERUPS
        ),
        TutorialStep(
            "Pronto, agora é com você! 💧",
            "Complete a grade antes de acabarem as rodadas. Terminar sem nenhum " +
                "quadradinho trancado vale 3 estrelas. Boa sorte!",
            null
        )
    )
}
