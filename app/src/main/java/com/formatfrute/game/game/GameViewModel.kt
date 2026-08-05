package com.formatfrute.game.game

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.formatfrute.game.ads.AdsManager
import com.formatfrute.game.ads.RewardReason
import com.formatfrute.game.audio.Haptics
import com.formatfrute.game.audio.Sfx
import com.formatfrute.game.audio.SoundManager
import com.formatfrute.game.audio.Track
import com.formatfrute.game.core.Direction
import com.formatfrute.game.core.Engine
import com.formatfrute.game.core.Fruit
import com.formatfrute.game.core.GameMode
import com.formatfrute.game.core.GameState
import com.formatfrute.game.core.Power
import com.formatfrute.game.core.TileKind
import com.formatfrute.game.data.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

enum class GameStatus { PLAYING, WON, LOST }

data class Floater(
    val id: Long,
    val text: String,
    val row: Int,
    val col: Int,
    val color: Color,
)

data class Burst(
    val id: Long,
    val row: Int,
    val col: Int,
    val color: Color,
    val big: Boolean,
)

data class RewardRequest(val reason: RewardReason, val power: Power? = null)

data class GameUi(
    val mode: GameMode = GameMode.POMAR,
    val state: GameState = Engine.empty(4),
    val status: GameStatus = GameStatus.PLAYING,
    val paused: Boolean = false,
    val timeLeft: Int = 0,
    val movesLeft: Int = 0,
    val combo: Int = 0,
    val comboToken: Long = 0,
    val bossHp: Int = 0,
    val bossMaxHp: Int = 1,
    val bossAngry: Boolean = false,
    val bossHitToken: Long = 0,
    val goal: String = "",
    val goalReached: Boolean = false,
    val coinsEarned: Int = 0,
    val newRecord: Boolean = false,
    val pendingPower: Power? = null,
    val floaters: List<Floater> = emptyList(),
    val bursts: List<Burst> = emptyList(),
    val shakeToken: Long = 0,
    val hint: String? = null,
    val rewardRequest: RewardRequest? = null,
    val tutorial: TutorialStep? = null,
    val tutorialToken: Long = 0,
    val canUndo: Boolean = false,
    val reviveUsed: Boolean = false,
)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GameRepository.get(app)
    private val sound = SoundManager.get(app)
    private val haptics = Haptics.get(app)

    private val _ui = MutableStateFlow(GameUi())
    val ui: StateFlow<GameUi> = _ui.asStateFlow()

    private var rng: Random = Random(System.nanoTime())
    private var timerJob: Job? = null
    private var effectId = 1L
    private var history = ArrayDeque<GameState>()
    private var mergesThisGame = 0
    private var harvestsThisGame = 0
    private var movesSinceAttack = 0
    private var comboJob: Job? = null
    private var tutorialActive = false

    // ------------------------------------------------------------- partida

    fun start(mode: GameMode, tutorial: Boolean = false) {
        timerJob?.cancel()
        comboJob?.cancel()
        history = ArrayDeque()
        mergesThisGame = 0
        harvestsThisGame = 0
        movesSinceAttack = 0
        tutorialActive = tutorial

        rng = if (mode == GameMode.CESTA) Random(dailySeed()) else Random(System.nanoTime())

        val state = when {
            tutorial -> tutorialBoard()
            mode == GameMode.CESTA -> dailyBoard(mode)
            else -> Engine.start(mode.gridSize, rng, seeds = if (mode.gridSize >= 5) 3 else 2)
        }

        _ui.value = GameUi(
            mode = mode,
            state = state,
            timeLeft = mode.timeLimit,
            movesLeft = mode.moveLimit,
            bossHp = BOSS_HP,
            bossMaxHp = BOSS_HP,
            goal = goalText(mode),
            tutorial = if (tutorial) TutorialStep.entries.first() else null,
            tutorialToken = 1,
        )

        sound.music(if (mode.boss) Track.BOSS else Track.GAME)
        repo.markLastPlayed()
        if (mode.hasClock) startClock()
    }

    private fun goalText(mode: GameMode): String = when (mode) {
        GameMode.POMAR -> "Chegue na ${Fruit.of(mode.goalLevel).label}"
        GameMode.VITAMINA -> "Faça o máximo de pontos em ${mode.timeLimit}s"
        GameMode.GELEIA -> "Derreta o gelo e chegue na ${Fruit.of(mode.goalLevel).label}"
        GameMode.CESTA -> "Chegue na ${Fruit.of(dailyGoalLevel()).label} em ${mode.moveLimit} jogadas"
        GameMode.ZEN -> "Relaxe. O pomar cuida do resto."
        GameMode.BATALHA -> "Derrote o Monstro Azedo"
    }

    private fun startClock() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val ui = _ui.value
                if (ui.status != GameStatus.PLAYING || ui.paused) continue
                val left = ui.timeLeft - 1
                _ui.value = ui.copy(timeLeft = max(0, left))
                if (left <= 5 && left > 0) haptics.tick()
                if (left <= 0) {
                    finish(won = false)
                    break
                }
            }
        }
    }

    // ---------------------------------------------------------------- jogada

    fun swipe(dir: Direction) {
        val ui = _ui.value
        if (ui.status != GameStatus.PLAYING || ui.paused || ui.pendingPower != null) return

        if (tutorialActive && !tutorialAllows(dir)) {
            _ui.value = ui.copy(shakeToken = ui.shakeToken + 1, hint = ui.tutorial?.blockHint)
            haptics.error()
            return
        }

        val result = Engine.move(ui.state, dir)
        if (!result.changed) {
            _ui.value = ui.copy(shakeToken = ui.shakeToken + 1)
            haptics.error()
            return
        }

        history.addLast(ui.state)
        if (history.size > 6) history.removeFirst()

        var state = result.state
        mergesThisGame += result.merges.size
        harvestsThisGame += result.harvests

        // Nasce fruta nova (no puzzle diario o tabuleiro e fechado: nada nasce).
        if (ui.mode != GameMode.CESTA || Engine.freeCells(state).size > 4) {
            state = Engine.spawn(
                state,
                rng,
                rainbowChance = ui.mode.rainbowChance,
                iceChance = ui.mode.iceChance,
            )
        }

        val newFloaters = ArrayList(ui.floaters)
        val newBursts = ArrayList(ui.bursts)
        result.merges.forEach { merge ->
            val fruit = Fruit.of(merge.level)
            newFloaters += Floater(
                id = effectId++,
                text = if (merge.harvest) "COLHEITA!" else "+${merge.value}",
                row = merge.row,
                col = merge.col,
                color = if (merge.harvest) Color(0xFFFFD54F) else fruit.glow,
            )
            newBursts += Burst(effectId++, merge.row, merge.col, fruit.glow, merge.harvest || merge.level >= 7)
        }

        val combo = if (result.merges.isNotEmpty()) ui.combo + result.merges.size else 0
        var bonus = 0
        var timeLeft = ui.timeLeft

        if (result.merges.isNotEmpty()) {
            sound.playMerge(result.merges.maxOf { it.level })
            haptics.merge(result.merges.maxOf { it.level })
            if (combo >= 3) {
                bonus = combo * 25
                newFloaters += Floater(effectId++, "COMBO x$combo", 0, ui.mode.gridSize / 2, Color(0xFFFFD54F))
            }
            if (ui.mode.hasClock) {
                timeLeft = (timeLeft + result.merges.sumOf { 1 + it.level / 3 }).coerceAtMost(120)
            }
        } else {
            sound.play(Sfx.SWIPE, volume = 0.5f)
            haptics.tick()
        }
        if (result.brokenIce > 0) sound.play(Sfx.ICE)
        if (result.harvests > 0) {
            sound.play(Sfx.HARVEST)
            haptics.harvest()
        }

        state = state.copy(score = state.score + bonus)

        // Monstro Azedo: dano nas fusoes e contra-ataque a cada tantas jogadas.
        var bossHp = ui.bossHp
        var bossHit = ui.bossHitToken
        if (ui.mode.boss) {
            val damage = result.merges.sumOf { it.value }
            if (damage > 0) {
                bossHp = (bossHp - damage).coerceAtLeast(0)
                bossHit++
            }
            movesSinceAttack++
            val angry = bossHp < BOSS_HP * 0.3f
            if (movesSinceAttack >= (if (angry) 2 else 3) && bossHp > 0) {
                movesSinceAttack = 0
                state = Engine.spawn(state, rng, forcedKind = TileKind.ROTTEN, forcedLevel = 0)
                newFloaters += Floater(effectId++, "Fruta podre!", 0, 0, Color(0xFF9CCC65))
                haptics.error()
            }
        }

        val movesLeft = if (ui.mode.moveLimit > 0) (ui.movesLeft - 1).coerceAtLeast(0) else 0

        _ui.value = ui.copy(
            state = state,
            combo = combo,
            comboToken = if (combo >= 3) ui.comboToken + 1 else ui.comboToken,
            timeLeft = timeLeft,
            movesLeft = movesLeft,
            bossHp = bossHp,
            bossHitToken = bossHit,
            bossAngry = ui.mode.boss && bossHp < BOSS_HP * 0.3f,
            floaters = newFloaters,
            bursts = newBursts,
            canUndo = history.isNotEmpty(),
            hint = null,
        )

        scheduleCleanup()
        resetComboLater()
        advanceTutorial(dir, result.merges.isNotEmpty(), state)
        checkEnd()
    }

    private fun scheduleCleanup() {
        viewModelScope.launch {
            delay(190)
            _ui.value = _ui.value.let { it.copy(state = it.state.copy(dying = emptyList())) }
            delay(700)
            _ui.value = _ui.value.copy(floaters = emptyList(), bursts = emptyList())
        }
    }

    private fun resetComboLater() {
        comboJob?.cancel()
        comboJob = viewModelScope.launch {
            delay(2600)
            _ui.value = _ui.value.copy(combo = 0)
        }
    }

    private fun checkEnd() {
        val ui = _ui.value
        val mode = ui.mode

        if (mode.boss && ui.bossHp <= 0) {
            finish(won = true)
            return
        }

        val goalLevel = if (mode == GameMode.CESTA) dailyGoalLevel() else mode.goalLevel
        val reached = ui.state.highestLevel >= goalLevel
        if (reached && !ui.goalReached && !mode.endless && !mode.boss) {
            _ui.value = ui.copy(goalReached = true)
            if (mode == GameMode.CESTA) repo.markCestaCleared()
            finish(won = true)
            return
        }

        if (mode.moveLimit > 0 && ui.movesLeft <= 0) {
            finish(won = false)
            return
        }

        if (!Engine.canMove(ui.state)) {
            if (mode.endless) {
                // Zen nunca perde: o pomar colhe as menores e a vida segue.
                val cleaned = Engine.harvestSmallest(ui.state, 3)
                _ui.value = ui.copy(
                    state = cleaned,
                    floaters = ui.floaters + Floater(
                        effectId++, "O pomar respirou!", 0, mode.gridSize / 2, Color(0xFF2FBF71),
                    ),
                )
                sound.play(Sfx.HARVEST)
                haptics.harvest()
                scheduleCleanup()
            } else {
                finish(won = false)
            }
        }
    }

    private fun finish(won: Boolean) {
        timerJob?.cancel()
        val ui = _ui.value
        if (ui.status != GameStatus.PLAYING) return

        val coins = (ui.state.score / 90) + harvestsThisGame * 60 + if (won) 120 else 0
        val record = repo.registerGame(
            mode = ui.mode,
            score = ui.state.score,
            highestFruit = ui.state.highestLevel,
            merges = mergesThisGame,
            harvests = harvestsThisGame,
        )
        repo.addCoins(coins)
        val leveled = repo.addXp(ui.state.score / 40 + if (won) 80 else 20)
        AdsManager.onGameFinished()

        _ui.value = ui.copy(
            status = if (won) GameStatus.WON else GameStatus.LOST,
            coinsEarned = coins,
            newRecord = record,
        )

        sound.play(if (won) Sfx.WIN else Sfx.LOSE)
        if (leveled) sound.play(Sfx.LEVELUP)
        if (won) haptics.win() else haptics.lose()
    }

    // --------------------------------------------------------------- poderes

    /** Ativa o poder se houver no inventario; senao pede o anuncio premiado. */
    fun requestPower(power: Power) {
        val ui = _ui.value
        if (ui.status != GameStatus.PLAYING) return
        if (power == Power.RELOGIO && !ui.mode.hasClock) {
            _ui.value = ui.copy(hint = "Esse poder só funciona nos modos com relógio.")
            haptics.error()
            return
        }
        if (repo.consumePower(power)) {
            activate(power)
        } else {
            _ui.value = ui.copy(rewardRequest = RewardRequest(RewardReason.PODER, power))
        }
    }

    fun buyPowerWithCoins(power: Power) {
        if (repo.buyPower(power)) {
            sound.play(Sfx.COIN)
            haptics.tap()
            requestPower(power)
        } else {
            _ui.value = _ui.value.copy(hint = "Sementes insuficientes. Assista um vídeo!")
            haptics.error()
        }
    }

    fun dismissReward() {
        _ui.value = _ui.value.copy(rewardRequest = null)
    }

    /** Chamado pela tela depois que o anuncio premiado termina. */
    fun onRewardResult(granted: Boolean) {
        val request = _ui.value.rewardRequest
        _ui.value = _ui.value.copy(rewardRequest = null)
        if (!granted) {
            _ui.value = _ui.value.copy(hint = "O vídeo não carregou. Tente de novo em instantes.")
            return
        }
        when (request?.reason) {
            RewardReason.PODER -> request.power?.let { activate(it) }
            RewardReason.REVIVER -> revive()
            RewardReason.TEMPO -> addTime(15)
            RewardReason.DOBRAR -> {
                repo.addCoins(_ui.value.coinsEarned)
                _ui.value = _ui.value.copy(coinsEarned = _ui.value.coinsEarned * 2)
                sound.play(Sfx.COIN)
            }
            else -> Unit
        }
    }

    private fun activate(power: Power) {
        val ui = _ui.value
        sound.play(Sfx.POWER)
        haptics.power()
        when (power) {
            Power.MARTELO, Power.ADUBO -> {
                _ui.value = ui.copy(
                    pendingPower = power,
                    hint = if (power == Power.MARTELO) "Toque na fruta que vai pro chão."
                    else "Toque na fruta que vai crescer.",
                )
            }
            Power.VOLTAR -> undo()
            Power.PENEIRA -> {
                _ui.value = ui.copy(state = Engine.shuffle(ui.state, rng), hint = null)
            }
            Power.RELOGIO -> addTime(15)
            Power.ARCOIRIS -> {
                val state = Engine.spawn(ui.state, rng, forcedKind = TileKind.RAINBOW)
                _ui.value = ui.copy(state = state, hint = null)
            }
        }
        // Poder com alvo so conta depois que o jogador escolhe a fruta.
        if (!power.needsTarget) markPowerUsed()
    }

    private fun markPowerUsed() {
        if (tutorialActive && _ui.value.tutorial == TutorialStep.PODER) {
            completeTutorialStep()
        }
    }

    fun tapCell(row: Int, col: Int) {
        val ui = _ui.value
        val power = ui.pendingPower ?: return
        val target = ui.state.at(row, col)
        if (target == null) {
            _ui.value = ui.copy(hint = "Escolha uma casa com fruta.")
            haptics.error()
            return
        }
        val state = when (power) {
            Power.MARTELO -> Engine.smash(ui.state, row, col)
            Power.ADUBO -> Engine.grow(ui.state, row, col)
            else -> ui.state
        }
        _ui.value = ui.copy(
            state = state,
            pendingPower = null,
            hint = null,
            bursts = ui.bursts + Burst(effectId++, row, col, target.fruit.glow, true),
        )
        sound.play(Sfx.POWER)
        haptics.power()
        scheduleCleanup()
        markPowerUsed()
    }

    fun cancelPendingPower() {
        _ui.value = _ui.value.copy(pendingPower = null, hint = null)
    }

    private fun undo() {
        val previous = history.removeLastOrNull()
        if (previous == null) {
            _ui.value = _ui.value.copy(hint = "Não tem jogada pra desfazer ainda.")
            return
        }
        _ui.value = _ui.value.copy(
            state = previous.copy(dying = emptyList()),
            canUndo = history.isNotEmpty(),
            hint = null,
        )
    }

    private fun addTime(seconds: Int) {
        val ui = _ui.value
        if (!ui.mode.hasClock) return
        _ui.value = ui.copy(timeLeft = ui.timeLeft + seconds, hint = null)
    }

    // ------------------------------------------------------------- reviver

    fun offerRevive() {
        if (_ui.value.reviveUsed) return
        _ui.value = _ui.value.copy(rewardRequest = RewardRequest(RewardReason.REVIVER))
    }

    fun offerDoubleCoins() {
        _ui.value = _ui.value.copy(rewardRequest = RewardRequest(RewardReason.DOBRAR))
    }

    private fun revive() {
        val ui = _ui.value
        val cleaned = Engine.harvestSmallest(ui.state, 4)
        _ui.value = ui.copy(
            state = cleaned,
            status = GameStatus.PLAYING,
            reviveUsed = true,
            timeLeft = if (ui.mode.hasClock) max(ui.timeLeft, 20) else ui.timeLeft,
            movesLeft = if (ui.mode.moveLimit > 0) ui.movesLeft + 5 else ui.movesLeft,
        )
        if (ui.mode.hasClock) startClock()
        sound.play(Sfx.POWER)
        haptics.win()
        scheduleCleanup()
    }

    // ------------------------------------------------------------ tutorial

    private fun tutorialAllows(dir: Direction): Boolean {
        val step = _ui.value.tutorial ?: return true
        return step.requiredDirection == null || step.requiredDirection == dir
    }

    private fun advanceTutorial(dir: Direction, merged: Boolean, state: GameState) {
        if (!tutorialActive) return
        val step = _ui.value.tutorial ?: return
        val done = when (step) {
            TutorialStep.ARRASTAR -> step.requiredDirection == dir
            TutorialStep.FUNDIR -> merged
            TutorialStep.EVOLUIR -> state.highestLevel >= 2
            TutorialStep.PODER -> false
            TutorialStep.PREMIADO -> false
            TutorialStep.FIM -> false
        }
        if (done) completeTutorialStep()
    }

    /** Passos que terminam por toque na UI (poder, anuncio, "entendi"). */
    fun completeTutorialStep() {
        val ui = _ui.value
        val step = ui.tutorial ?: return
        val next = TutorialStep.entries.getOrNull(step.ordinal + 1)
        sound.play(Sfx.BUTTON)
        haptics.tap()
        if (next == null) {
            finishTutorial()
            return
        }
        // Emprestamos um martelo para o passo do poder funcionar de primeira.
        if (next == TutorialStep.PODER && repo.current.powerCount(Power.MARTELO) == 0) {
            repo.addPower(Power.MARTELO)
        }
        _ui.value = ui.copy(tutorial = next, tutorialToken = ui.tutorialToken + 1, hint = null)
    }

    fun finishTutorial() {
        tutorialActive = false
        repo.setTutorialDone(true)
        repo.addCoins(TUTORIAL_REWARD)
        repo.addPower(Power.MARTELO)
        repo.addPower(Power.VOLTAR)
        _ui.value = _ui.value.copy(tutorial = null, hint = null)
        sound.play(Sfx.COIN)
        haptics.win()
    }

    fun skipTutorial() {
        tutorialActive = false
        repo.setTutorialDone(true)
        _ui.value = _ui.value.copy(tutorial = null, hint = null)
    }

    /**
     * Tabuleiro do tutorial montado a mao: o primeiro arraste e so deslize
     * (as cerejas estao em linhas diferentes), a fusao vem no passo seguinte e
     * o morango deixa a uva ao alcance de poucas jogadas.
     */
    private fun tutorialBoard(): GameState {
        var state = Engine.empty(4)
        state = Engine.place(state, row = 0, col = 0, level = 0)
        state = Engine.place(state, row = 2, col = 3, level = 0)
        state = Engine.place(state, row = 3, col = 1, level = 1)
        return state
    }

    // -------------------------------------------------------------- geral

    fun setPaused(paused: Boolean) {
        _ui.value = _ui.value.copy(paused = paused)
        if (paused) sound.pauseMusic() else sound.resumeMusic()
    }

    fun clearHint() {
        _ui.value = _ui.value.copy(hint = null)
    }

    fun restart() {
        start(_ui.value.mode, tutorial = false)
    }

    fun leave() {
        timerJob?.cancel()
        comboJob?.cancel()
        sound.music(Track.MENU)
    }

    override fun onCleared() {
        timerJob?.cancel()
        comboJob?.cancel()
        super.onCleared()
    }

    // ----------------------------------------------------- cesta do dia

    private fun dailySeed(): Long = GameRepository.today().hashCode().toLong() * 977L

    fun dailyGoalLevel(): Int {
        val rng = Random(dailySeed() + 13)
        return rng.nextInt(6, 9)
    }

    private fun dailyBoard(mode: GameMode): GameState {
        val seeded = Random(dailySeed())
        var state = Engine.empty(mode.gridSize)
        val pieces = seeded.nextInt(5, 8)
        repeat(pieces) {
            state = Engine.spawn(state, seeded, forcedLevel = seeded.nextInt(0, 4))
        }
        // uma pedra no caminho pra cesta nao ser passeio
        if (seeded.nextFloat() < 0.6f) {
            state = Engine.spawn(state, seeded, forcedKind = TileKind.ROTTEN, forcedLevel = 0)
        }
        return state
    }

    companion object {
        const val BOSS_HP = 1400
        const val TUTORIAL_REWARD = 150
    }
}
