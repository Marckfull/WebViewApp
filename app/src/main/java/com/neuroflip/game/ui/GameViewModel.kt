package com.neuroflip.game.ui

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neuroflip.game.audio.MusicTrack
import com.neuroflip.game.audio.Sfx
import com.neuroflip.game.data.PowerUp
import com.neuroflip.game.domain.GameEngine
import com.neuroflip.game.domain.GameEvent
import com.neuroflip.game.domain.GameMode
import com.neuroflip.game.domain.GameState
import com.neuroflip.game.domain.GameStatus
import com.neuroflip.game.domain.LevelCatalog
import com.neuroflip.game.haptics.Buzz
import com.neuroflip.game.neuro
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GameResult(
    val won: Boolean,
    val levelId: Int,
    val mode: GameMode,
    val score: Int,
    val stars: Int,
    val bestCombo: Int,
    val neurons: Int,
    val rewardDoubled: Boolean = false,
    val canContinue: Boolean = false
)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = app.neuro.repository
    private val sound = app.neuro.sound
    private val music = app.neuro.music
    private val haptics = app.neuro.haptics
    private val ads = app.neuro.ads

    private var engine: GameEngine? = null
    private var loopJob: Job? = null
    private var continuesUsed = 0
    private var tenseMusicOn = false
    private var lastTickSecond = -1

    private val _state = MutableStateFlow<GameState?>(null)
    val state: StateFlow<GameState?> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    private val _result = MutableStateFlow<GameResult?>(null)
    val result: StateFlow<GameResult?> = _result.asStateFlow()

    private val _hint = MutableStateFlow<String?>(null)
    val hint: StateFlow<String?> = _hint.asStateFlow()

    var mode: GameMode = GameMode.CAMPAIGN
        private set

    // ------------------------------------------------------------------ ciclo

    fun startLevel(mode: GameMode, levelId: Int) {
        this.mode = mode
        continuesUsed = 0
        tenseMusicOn = false
        lastTickSecond = -1
        _result.value = null

        val config = when (mode) {
            GameMode.CAMPAIGN -> LevelCatalog.level(levelId)
            GameMode.BLITZ -> LevelCatalog.blitz()
            GameMode.ZEN -> LevelCatalog.zen()
        }
        _hint.value = if (mode == GameMode.CAMPAIGN) LevelCatalog.unlockHint(levelId) else null

        val newEngine = GameEngine(config)
        engine = newEngine
        newEngine.start()
        _state.value = newEngine.state
        music.play(MusicTrack.GAME)
        startLoop()
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(FRAME_MS)
                val now = SystemClock.elapsedRealtime()
                val delta = (now - last).coerceAtMost(250L)
                last = now
                val current = engine ?: continue
                val events = current.tick(delta)
                _state.value = current.state
                dispatch(events)
                handleTimerFeedback(current.state)
            }
        }
    }

    private fun handleTimerFeedback(state: GameState) {
        if (state.config.timeLimitMs <= 0 || state.status != GameStatus.RUNNING) return
        val secondsLeft = (state.timeLeftMs / 1000L).toInt()

        if (!tenseMusicOn && secondsLeft in 1..15) {
            tenseMusicOn = true
            music.play(MusicTrack.TENSE)
        }
        if (secondsLeft in 1..5 && secondsLeft != lastTickSecond) {
            lastTickSecond = secondsLeft
            sound.play(Sfx.TICK, volume = 0.8f)
            haptics.buzz(Buzz.LIGHT)
        }
    }

    fun pause() {
        engine?.pause()
        _state.value = engine?.state
        music.pause()
    }

    fun resume() {
        engine?.resume()
        _state.value = engine?.state
        music.resume()
    }

    fun restart() {
        val current = engine ?: return
        startLevel(mode, current.state.config.id)
    }

    fun quit() {
        loopJob?.cancel()
        loopJob = null
        engine = null
        _state.value = null
        music.play(MusicTrack.MENU)
    }

    // ------------------------------------------------------------------ jogadas

    fun flip(index: Int) {
        val current = engine ?: return
        val events = current.flip(index)
        _state.value = current.state
        dispatch(events)
    }

    fun activateOverload() {
        val current = engine ?: return
        val events = current.activateOverload()
        _state.value = current.state
        dispatch(events)
    }

    fun usePowerUp(kind: PowerUp) = viewModelScope.launch {
        val current = engine ?: return@launch
        if (!repo.consumePowerUp(kind)) {
            return@launch
        }
        val events = when (kind) {
            PowerUp.SCAN -> {
                current.useScan()
                sound.play(Sfx.ECO)
                emptyList()
            }
            PowerUp.WILD -> current.useWildcard()
            PowerUp.TIME -> current.addTime(20_000L)
        }
        haptics.buzz(Buzz.MEDIUM)
        _state.value = current.state
        dispatch(events)
    }

    fun dismissHint() {
        _hint.value = null
        viewModelScope.launch { repo.setTutorialSeen() }
    }

    // ------------------------------------------------------------------ eventos

    private fun dispatch(events: List<GameEvent>) {
        events.forEach { event ->
            _events.tryEmit(event)
            when (event) {
                is GameEvent.Flipped -> {
                    sound.play(Sfx.FLIP, volume = 0.7f)
                    haptics.buzz(Buzz.LIGHT)
                }
                is GameEvent.Matched -> {
                    sound.play(Sfx.MATCH)
                    haptics.buzz(Buzz.SUCCESS)
                }
                is GameEvent.Mismatched -> {
                    sound.play(Sfx.MISMATCH, volume = 0.7f)
                    haptics.buzz(Buzz.ERROR)
                }
                is GameEvent.Combo -> {
                    sound.playCombo(event.level)
                    haptics.buzz(Buzz.COMBO)
                }
                is GameEvent.EcoPulse -> sound.play(Sfx.ECO, volume = 0.55f)
                is GameEvent.Mutation -> {
                    sound.play(Sfx.MUTATION, volume = 0.75f)
                    haptics.buzz(Buzz.MEDIUM)
                }
                GameEvent.OverloadReady -> sound.play(Sfx.COIN, volume = 0.6f)
                GameEvent.OverloadStarted -> {
                    sound.play(Sfx.OVERLOAD)
                    haptics.buzz(Buzz.OVERLOAD)
                }
                is GameEvent.Won -> onWin(event.score, event.stars)
                GameEvent.Lost -> onLose()
                else -> Unit
            }
        }
    }

    private fun onWin(score: Int, stars: Int) {
        val current = engine ?: return
        loopJob?.cancel()
        sound.play(Sfx.WIN)
        haptics.buzz(Buzz.WIN)
        music.play(MusicTrack.MENU)
        ads.onLevelFinished()

        val reward = current.neuronReward()
        val config = current.state.config
        viewModelScope.launch {
            repo.addNeurons(reward)
            when (mode) {
                GameMode.CAMPAIGN -> repo.recordLevelResult(config.id, stars, score)
                GameMode.BLITZ -> repo.recordBlitz(score)
                GameMode.ZEN -> Unit
            }
        }
        _result.value = GameResult(
            won = true,
            levelId = config.id,
            mode = mode,
            score = score,
            stars = stars,
            bestCombo = current.state.bestCombo,
            neurons = reward
        )
    }

    private fun onLose() {
        val current = engine ?: return
        loopJob?.cancel()
        sound.play(Sfx.LOSE)
        haptics.buzz(Buzz.LOSE)
        music.play(MusicTrack.MENU)
        _result.value = GameResult(
            won = false,
            levelId = current.state.config.id,
            mode = mode,
            score = current.state.score,
            stars = 0,
            bestCombo = current.state.bestCombo,
            neurons = 0,
            canContinue = continuesUsed == 0
        )
    }

    // ------------------------------------------------------------------ anúncios premiados

    /** Vídeo premiado: dobra os neurônios ganhos no nível. */
    fun doubleRewardWithAd(activity: Activity) {
        val result = _result.value ?: return
        if (!result.won || result.rewardDoubled) return
        ads.showRewarded(
            activity = activity,
            onReward = {
                viewModelScope.launch {
                    repo.addNeurons(result.neurons)
                    sound.play(Sfx.COIN)
                    haptics.buzz(Buzz.SUCCESS)
                    _result.value = result.copy(
                        neurons = result.neurons * 2,
                        rewardDoubled = true
                    )
                }
            }
        )
    }

    /** Vídeo premiado: ganha 1 unidade do power-up direto na partida. */
    fun usePowerUpFromAd(activity: Activity, kind: PowerUp) {
        ads.showRewarded(
            activity = activity,
            onReward = {
                viewModelScope.launch {
                    repo.addPowerUp(kind, 1)
                    sound.play(Sfx.COIN)
                    haptics.buzz(Buzz.SUCCESS)
                }
            }
        )
    }

    /** Vídeo premiado: +30s e a partida continua de onde parou. */
    fun continueWithAd(activity: Activity) {
        val current = engine ?: return
        if (continuesUsed > 0) return
        ads.showRewarded(
            activity = activity,
            onReward = {
                continuesUsed++
                tenseMusicOn = false
                _result.value = null
                current.addTime(30_000L)
                current.resume()
                _state.value = current.state
                music.play(MusicTrack.GAME)
                startLoop()
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        loopJob?.cancel()
    }

    companion object {
        private const val FRAME_MS = 50L
    }
}
