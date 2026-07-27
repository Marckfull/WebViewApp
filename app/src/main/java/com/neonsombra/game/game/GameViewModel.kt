package com.neonsombra.game.game

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.neonsombra.game.audio.Sfx
import com.neonsombra.game.audio.SoundEngine
import com.neonsombra.game.data.Prefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Tempo de dedo parado, em ms, antes da peca comecar a descer depressa. */
private const val HOLD_THRESHOLD_MS = 140L

/** Um toque mais curto do que isto, sem arrastar, gira a peca. */
private const val TAP_MAX_MS = 240L

/** Passo do quadro do jogo (~60 fps). */
private const val FRAME_MS = 16L

/**
 * Liga o motor do Tetris a interface: roda o relogio do jogo, traduz os gestos
 * do jogador e transforma os avisos do motor em som e vibracao.
 */
class GameViewModel(
    private val prefs: Prefs,
    private val sound: SoundEngine,
) : ViewModel() {

    private val engine = TetrisEngine()

    var snapshot by mutableStateOf(engine.snapshot())
        private set

    var isNewRecord by mutableStateOf(false)
        private set

    var highScore by mutableStateOf(prefs.highScore)
        private set

    val playerName: String get() = prefs.playerName.ifBlank { "JOGADOR" }
    val ghostEnabled: Boolean get() = prefs.ghostEnabled

    private var loopJob: Job? = null
    private var pressing = false
    private var pressStartMs = 0L
    private var lastDragMs = 0L
    private var draggedDuringPress = false
    private var softDropStarted = false
    private var gestureConsumed = false
    private var scoreSubmitted = false

    init {
        newGame()
    }

    // ------------------------------------------------------------------ partida

    fun newGame() {
        engine.reset()
        engine.start()
        isNewRecord = false
        scoreSubmitted = false
        highScore = prefs.highScore
        resetTouchState()
        publish()
        startLoop()
    }

    fun pauseGame() {
        engine.pause()
        resetTouchState()
        publish()
    }

    fun resumeGame() {
        if (engine.status == GameStatus.PAUSED) {
            engine.resume()
            publish()
        }
    }

    fun togglePause() {
        if (engine.status == GameStatus.PAUSED) resumeGame() else pauseGame()
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            var previous = SystemClock.elapsedRealtimeNanos()
            while (isActive) {
                delay(FRAME_MS)
                val now = SystemClock.elapsedRealtimeNanos()
                val delta = ((now - previous) / 1_000_000_000f).coerceIn(0f, 0.1f)
                previous = now

                updateSoftDrop()
                engine.update(delta)
                consumeEvents()
                publish()
            }
        }
    }

    private fun publish() {
        snapshot = engine.snapshot()
    }

    // ------------------------------------------------------------------- gestos

    /** Dedo encostou na tela. */
    fun onPressStart() {
        if (engine.status != GameStatus.PLAYING && engine.status != GameStatus.CLEARING) return
        pressing = true
        draggedDuringPress = false
        softDropStarted = false
        gestureConsumed = false
        pressStartMs = SystemClock.uptimeMillis()
        lastDragMs = 0L
    }

    /**
     * Dedo saiu da tela. Se foi um toque rapido, sem arrastar e sem ter virado
     * queda acelerada ou queda instantanea, a peca gira.
     */
    fun onPressEnd() {
        val wasPressing = pressing
        val duration = SystemClock.uptimeMillis() - pressStartMs
        pressing = false
        engine.softDropping = false
        if (wasPressing && !draggedDuringPress && !softDropStarted && !gestureConsumed &&
            duration <= TAP_MAX_MS
        ) {
            engine.rotate()
            consumeEvents()
            publish()
        }
        softDropStarted = false
        gestureConsumed = false
    }

    /** O jogador arrastou o dedo o equivalente a uma coluna. */
    fun onHorizontalStep(direction: Int) {
        if (gestureConsumed) return
        draggedDuringPress = true
        lastDragMs = SystemClock.uptimeMillis()
        engine.softDropping = false
        engine.moveHorizontally(direction)
        consumeEvents()
        publish()
    }

    /** Deslize rapido para baixo: a peca despenca e trava na hora. */
    fun onHardDrop() {
        if (gestureConsumed) return
        gestureConsumed = true
        engine.softDropping = false
        engine.hardDrop()
        consumeEvents()
        publish()
    }

    private fun updateSoftDrop() {
        if (!pressing || gestureConsumed || engine.status != GameStatus.PLAYING) {
            engine.softDropping = false
            return
        }
        val now = SystemClock.uptimeMillis()
        val since = now - maxOf(pressStartMs, lastDragMs)
        val shouldDrop = since >= HOLD_THRESHOLD_MS
        engine.softDropping = shouldDrop
        if (shouldDrop) softDropStarted = true
    }

    private fun resetTouchState() {
        pressing = false
        draggedDuringPress = false
        softDropStarted = false
        engine.softDropping = false
    }

    // -------------------------------------------------------------- som/recorde

    private fun consumeEvents() {
        val events = engine.drainEvents()
        if (events.isEmpty()) return
        events.forEach { event ->
            when (event) {
                GameEvent.Moved -> sound.play(Sfx.MOVE)

                GameEvent.Rotated -> {
                    sound.play(Sfx.ROTATE)
                    sound.vibrate(10)
                }

                GameEvent.Locked -> {
                    sound.play(Sfx.LOCK)
                    sound.vibrate(18)
                }

                is GameEvent.HardDropped -> {
                    sound.play(Sfx.HARD_DROP)
                    // Quanto mais alto o tombo, mais forte o baque.
                    sound.vibrate((20L + event.distance * 2L).coerceAtMost(60L))
                }

                is GameEvent.LinesCleared -> {
                    if (event.count >= 4) {
                        sound.play(Sfx.TETRIS)
                        sound.vibratePattern(longArrayOf(0, 40, 60, 40, 60, 90))
                    } else {
                        sound.play(Sfx.CLEAR)
                        sound.vibrate(30L * event.count)
                    }
                }

                // Combos vibram no compasso da trilha.
                is GameEvent.Combo -> {
                    sound.play(Sfx.COMBO)
                    sound.vibrateComboOnBeat(event.count)
                }

                is GameEvent.ShadowStrike -> {
                    sound.play(Sfx.SHADOW_STRIKE)
                    sound.vibratePattern(longArrayOf(0, 70, 50, 30))
                }

                GameEvent.PurgeStarted -> {
                    sound.play(Sfx.PURGE)
                    sound.vibratePattern(longArrayOf(0, 30, 40, 30, 40, 120))
                }

                GameEvent.PurgeEnded -> sound.play(Sfx.SHADOW_STRIKE)

                GameEvent.LevelUp -> {
                    sound.play(Sfx.LEVEL_UP)
                    sound.vibratePattern(longArrayOf(0, 25, 40, 25))
                }

                GameEvent.GameOver -> {
                    sound.play(Sfx.GAME_OVER)
                    sound.vibratePattern(longArrayOf(0, 120, 80, 220))
                    submitScore()
                }
            }
        }
    }

    private fun submitScore() {
        if (scoreSubmitted) return
        scoreSubmitted = true
        isNewRecord = prefs.submitScore(engine.score, playerName)
        highScore = prefs.highScore
    }

    override fun onCleared() {
        loopJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(prefs: Prefs, sound: SoundEngine): ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel(prefs, sound) }
        }
    }
}
