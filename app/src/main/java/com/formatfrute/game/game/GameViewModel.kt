package com.formatfrute.game.game

import android.app.Application
import androidx.annotation.StringRes
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
import com.formatfrute.game.core.FruitVoice
import com.formatfrute.game.core.GameMode
import com.formatfrute.game.core.GameState
import com.formatfrute.game.core.Merge
import com.formatfrute.game.core.Power
import com.formatfrute.game.core.Recipe
import com.formatfrute.game.core.RecipeBook
import com.formatfrute.game.core.TileKind
import com.formatfrute.game.data.Achievement
import com.formatfrute.game.data.Achievements
import com.formatfrute.game.data.GameRepository
import com.formatfrute.game.R
import com.formatfrute.game.data.SavedGame
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

/** Um balãozinho de fala saindo de uma casa do tabuleiro. */
data class Speech(
    val id: Long,
    val text: String,
    val fruit: Fruit?,
    val row: Int,
    val col: Int,
    val villain: Boolean = false,
)

data class GameUi(
    val mode: GameMode = GameMode.POMAR,
    val state: GameState = Engine.empty(4),
    val status: GameStatus = GameStatus.PLAYING,
    val paused: Boolean = false,
    val timeLeft: Int = 0,
    val movesLeft: Int = 0,
    val movesTotal: Int = 0,
    val recipe: Recipe? = null,
    /** Quantas frutas de cada degrau já foram CRIADAS nesta partida. */
    val produced: Map<Int, Int> = emptyMap(),
    val stars: Int = 0,
    val speech: Speech? = null,
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
    /** Ids das frutas acesas pelo Olho Bom. */
    val hintTiles: Set<Long> = emptySet(),
    val floaters: List<Floater> = emptyList(),
    val bursts: List<Burst> = emptyList(),
    /** Tremida de "jogada inválida". */
    val shakeToken: Long = 0,
    /** Baque de fruta grande: a tela inteira sente. */
    val impactToken: Long = 0,
    val impactPower: Float = 0f,
    val hint: String? = null,
    val rewardRequest: RewardRequest? = null,
    val tutorial: TutorialStep? = null,
    val tutorialToken: Long = 0,
    /** Ensino do Modo Receita, mostrado só na primeira fase da vida do jogador. */
    val coach: RecipeCoachStep? = null,
    val canUndo: Boolean = false,
    val reviveUsed: Boolean = false,
    val unlocked: Achievement? = null,
)

/**
 * Retrato completo de uma jogada.
 *
 * Guardar só o tabuleiro no histórico abria brecha: o jogador fundia, o dano
 * no chefe / o tempo ganho / o item do pedido ficavam contabilizados, ele
 * desfazia e fundia de novo. Undo agora volta *tudo* junto.
 */
private data class Snapshot(
    val state: GameState,
    val produced: Map<Int, Int>,
    val bossHp: Int,
    val timeLeft: Int,
    val movesLeft: Int,
    val movesSinceAttack: Int,
    val merges: Int,
    val harvests: Int,
    val seen: Set<Int>,
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
    private var history = ArrayDeque<Snapshot>()
    private var mergesThisGame = 0
    private var harvestsThisGame = 0
    private var movesSinceAttack = 0
    private var comboJob: Job? = null
    private var speechJob: Job? = null
    private var hintJob: Job? = null
    private var tutorialActive = false
    private var seenFruits = HashSet<Int>()
    private var achievementsBefore = emptySet<String>()
    private var recipeDay = ""

    /** Resolve texto de `strings.xml`. O ViewModel tem Application, então pode. */
    private fun str(@StringRes id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    // ------------------------------------------------------------- partida

    fun start(mode: GameMode, tutorial: Boolean = false) {
        if (mode.isCampaign) {
            startRecipe(1)
            return
        }
        reset(tutorial)
        rng = Random(System.nanoTime())

        val state = if (tutorial) {
            tutorialBoard()
        } else {
            Engine.start(mode.gridSize, rng, seeds = if (mode.gridSize >= 5) 3 else 2)
        }

        _ui.value = GameUi(
            mode = mode,
            state = state,
            timeLeft = mode.timeLimit,
            movesLeft = mode.moveLimit,
            movesTotal = mode.moveLimit,
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

    /**
     * Abre uma fase do Modo Receita. [number] igual a [RecipeBook.DAILY] abre a
     * Receita do Dia, sorteada pela data.
     */
    fun startRecipe(number: Int) {
        reset(tutorial = false)
        recipeDay = GameRepository.today()
        val recipe = if (number == RecipeBook.DAILY) {
            RecipeBook.daily(recipeDay)
        } else {
            RecipeBook.recipe(number)
        }
        val state = RecipeBook.board(recipe, Random(recipe.seed))
        rng = Random(System.nanoTime())

        _ui.value = GameUi(
            mode = GameMode.RECEITA,
            state = state,
            recipe = recipe,
            movesLeft = recipe.moves,
            movesTotal = recipe.moves,
            goal = recipeGoal(recipe),
            coach = if (repo.current.recipeCoachDone) null else RecipeCoachStep.entries.first(),
        )

        sound.music(Track.GAME)
        repo.markLastPlayed()
    }

    /** Retoma a partida guardada; sem ela, começa um Pomar novo. */
    fun resumeSaved() {
        val saved = repo.loadResume()
        if (saved == null) start(GameMode.POMAR) else resume(saved)
    }

    /** Retoma a partida interrompida — o tabuleiro volta exatamente como estava. */
    fun resume(saved: SavedGame) {
        reset(tutorial = false)
        recipeDay = saved.recipeDay
        rng = Random(System.nanoTime())
        mergesThisGame = saved.merges
        harvestsThisGame = saved.harvests
        movesSinceAttack = saved.movesSinceAttack

        val recipe = when {
            !saved.isRecipe -> null
            saved.recipeNumber == RecipeBook.DAILY -> RecipeBook.daily(saved.recipeDay)
            else -> RecipeBook.recipe(saved.recipeNumber)
        }

        _ui.value = GameUi(
            mode = saved.mode,
            state = saved.state,
            recipe = recipe,
            produced = saved.produced,
            timeLeft = saved.timeLeft,
            movesLeft = saved.movesLeft,
            movesTotal = saved.movesTotal,
            bossHp = saved.bossHp,
            bossMaxHp = BOSS_HP,
            bossAngry = saved.mode.boss && saved.bossHp < BOSS_HP * 0.3f,
            goal = recipe?.let { recipeGoal(it) } ?: goalText(saved.mode),
        )

        sound.music(if (saved.mode.boss) Track.BOSS else Track.GAME)
        repo.markLastPlayed()
        if (saved.mode.hasClock) startClock()
    }

    private fun reset(tutorial: Boolean) {
        timerJob?.cancel()
        comboJob?.cancel()
        speechJob?.cancel()
        hintJob?.cancel()
        history = ArrayDeque()
        mergesThisGame = 0
        harvestsThisGame = 0
        movesSinceAttack = 0
        seenFruits = HashSet()
        tutorialActive = tutorial
        achievementsBefore = repo.current.achievementsClaimed +
            Achievements.all.filter { it.isDone(repo.current) }.map { it.id }
    }

    private fun goalText(mode: GameMode): String = when (mode) {
        GameMode.RECEITA -> str(R.string.goal_recipe)
        GameMode.POMAR -> str(R.string.goal_reach, str(Fruit.of(mode.goalLevel).label))
        GameMode.VITAMINA -> str(R.string.goal_score_time, mode.timeLimit)
        GameMode.GELEIA -> str(R.string.goal_melt_ice, str(Fruit.of(mode.goalLevel).label))
        GameMode.ZEN -> str(R.string.goal_zen)
        GameMode.BATALHA -> str(R.string.goal_boss)
    }

    private fun recipeGoal(recipe: Recipe): String =
        if (recipe.daily) {
            str(R.string.recipe_daily)
        } else {
            str(R.string.recipe_stage, recipe.number, str(recipe.title))
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

        // Enquanto o cartão do ensino pede um toque, o tabuleiro fica quieto.
        if (ui.coach?.manual == true) {
            _ui.value = ui.copy(shakeToken = ui.shakeToken + 1)
            haptics.error()
            return
        }

        val result = Engine.move(ui.state, dir)
        if (!result.changed) {
            _ui.value = ui.copy(shakeToken = ui.shakeToken + 1)
            haptics.error()
            return
        }

        pushHistory(ui)

        var state = result.state
        mergesThisGame += result.merges.size
        harvestsThisGame += result.harvests

        state = Engine.spawn(
            state,
            rng,
            rainbowChance = ui.mode.rainbowChance,
            iceChance = ui.mode.iceChance,
        )

        // Modo Receita: o pedido conta o que foi CRIADO, nao o que ja estava la.
        val produced = ui.produced.toMutableMap()
        result.merges.forEach { merge ->
            produced[merge.level] = (produced[merge.level] ?: 0) + 1
        }

        val newFloaters = ArrayList(ui.floaters)
        val newBursts = ArrayList(ui.bursts)
        result.merges.forEach { merge ->
            val fruit = Fruit.of(merge.level)
            newFloaters += Floater(
                id = effectId++,
                text = if (merge.harvest) str(R.string.float_harvest) else "+${merge.value}",
                row = merge.row,
                col = merge.col,
                color = if (merge.harvest) Color(0xFFFFD54F) else fruit.glow,
            )
            newBursts += Burst(effectId++, merge.row, merge.col, fruit.glow, merge.harvest || merge.level >= 7)
        }

        val combo = if (result.merges.isNotEmpty()) ui.combo + result.merges.size else 0
        var bonus = 0
        var timeLeft = ui.timeLeft
        var impact = ui.impactToken
        var impactPower = 0f

        if (result.merges.isNotEmpty()) {
            val loudest = result.merges.maxOf { it.level }
            sound.playMerge(loudest)
            haptics.merge(loudest)
            if (loudest >= BIG_FRUIT || result.harvests > 0) {
                impact++
                impactPower = if (result.harvests > 0) 1f else 0.45f + (loudest - BIG_FRUIT) * 0.14f
            }
            if (combo >= 3) {
                bonus = combo * 25
                newFloaters += Floater(
                    effectId++,
                    str(R.string.float_combo, combo),
                    0,
                    ui.mode.gridSize / 2,
                    Color(0xFFFFD54F),
                )
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
                newFloaters += Floater(effectId++, str(R.string.float_rotten), 0, 0, Color(0xFF9CCC65))
                haptics.error()
                say(str(FruitVoice.onBoss(effectId++)), null, 0, ui.state.size - 1, villain = true)
            }
        }

        // A fruta podre da receita cai sozinha, sem monstro nenhum.
        val rottenEvery = ui.recipe?.rottenEvery ?: 0
        if (rottenEvery > 0 && !ui.mode.boss) {
            movesSinceAttack++
            if (movesSinceAttack >= rottenEvery) {
                movesSinceAttack = 0
                state = Engine.spawn(state, rng, forcedKind = TileKind.ROTTEN, forcedLevel = 0)
                haptics.error()
            }
        }

        val movesLeft = if (ui.movesTotal > 0) (ui.movesLeft - 1).coerceAtLeast(0) else 0

        _ui.value = ui.copy(
            state = state,
            produced = produced,
            combo = combo,
            comboToken = if (combo >= 3) ui.comboToken + 1 else ui.comboToken,
            timeLeft = timeLeft,
            movesLeft = movesLeft,
            bossHp = bossHp,
            bossHitToken = bossHit,
            bossAngry = ui.mode.boss && bossHp < BOSS_HP * 0.3f,
            floaters = newFloaters,
            bursts = newBursts,
            impactToken = impact,
            impactPower = impactPower,
            hintTiles = emptySet(),
            canUndo = history.isNotEmpty(),
            hint = null,
        )

        voiceOf(result.merges, combo)
        scheduleCleanup()
        resetComboLater()
        advanceTutorial(dir, result.merges.isNotEmpty(), state)
        // O passo "só vale o que você criar" fecha quando a fusão acontece —
        // é ver o contador andar que ensina, não o texto.
        if (ui.coach == RecipeCoachStep.CRIAR && result.merges.isNotEmpty()) {
            advanceCoach()
        }
        checkEnd()
    }

    // ----------------------------------------------- ensino da Receita

    fun advanceCoach() {
        val current = _ui.value.coach ?: return
        val next = RecipeCoachStep.entries.getOrNull(current.ordinal + 1)
        sound.play(Sfx.BUTTON)
        haptics.tap()
        if (next == null) {
            repo.setRecipeCoachDone(true)
            _ui.value = _ui.value.copy(coach = null)
        } else {
            _ui.value = _ui.value.copy(coach = next)
        }
    }

    fun skipCoach() {
        repo.setRecipeCoachDone(true)
        _ui.value = _ui.value.copy(coach = null)
    }

    private fun pushHistory(ui: GameUi) {
        history.addLast(
            Snapshot(
                state = ui.state,
                produced = ui.produced,
                bossHp = ui.bossHp,
                timeLeft = ui.timeLeft,
                movesLeft = ui.movesLeft,
                movesSinceAttack = movesSinceAttack,
                merges = mergesThisGame,
                harvests = harvestsThisGame,
                seen = seenFruits.toSet(),
            )
        )
        if (history.size > 6) history.removeFirst()
    }

    // ------------------------------------------------------ voz das frutas

    /**
     * As frutas falam com parcimônia: estreia de fruta grande e combo alto.
     * Falar demais vira ruído e o jogador para de ler.
     */
    private fun voiceOf(merges: List<Merge>, combo: Int) {
        val debut = merges
            .filter { !it.harvest && seenFruits.add(it.level) }
            .maxByOrNull { it.level }

        if (debut != null) {
            val fruit = Fruit.of(debut.level)
            val line = FruitVoice.onArrival(fruit, effectId + debut.level)
            if (line != null) {
                say(str(line), fruit, debut.row, debut.col)
                return
            }
        }
        if (combo >= 5 && merges.isNotEmpty()) {
            val loudest = merges.maxByOrNull { it.level } ?: return
            say(
                str(FruitVoice.onCombo(effectId + combo)),
                Fruit.of(loudest.level),
                loudest.row,
                loudest.col,
            )
        }
    }

    private fun say(text: String, fruit: Fruit?, row: Int, col: Int, villain: Boolean = false) {
        val speech = Speech(effectId++, text, fruit, row, col, villain)
        _ui.value = _ui.value.copy(speech = speech)
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            delay(2400)
            if (_ui.value.speech?.id == speech.id) {
                _ui.value = _ui.value.copy(speech = null)
            }
        }
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

        val recipe = ui.recipe
        if (recipe != null) {
            if (ordersDone(recipe, ui.produced)) {
                finish(won = true)
                return
            }
            if (ui.movesLeft <= 0 || !Engine.canMove(ui.state)) {
                finish(won = false)
            }
            return
        }

        val reached = ui.state.highestLevel >= mode.goalLevel
        if (reached && !ui.goalReached && !mode.endless && !mode.boss) {
            _ui.value = ui.copy(goalReached = true)
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
                        effectId++, str(R.string.float_breathe), 0, mode.gridSize / 2, Color(0xFF2FBF71),
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

    /** O pedido está completo? */
    fun ordersDone(recipe: Recipe, produced: Map<Int, Int>): Boolean =
        recipe.orders.all { (produced[it.level] ?: 0) >= it.count }

    private fun finish(won: Boolean) {
        timerJob?.cancel()
        speechJob?.cancel()
        hintJob?.cancel()
        val ui = _ui.value
        if (ui.status != GameStatus.PLAYING) return

        repo.clearResume()

        val recipe = ui.recipe
        val stars = if (recipe != null && won) recipe.starsFor(ui.movesLeft) else 0

        var coins = (ui.state.score / 90) + harvestsThisGame * 60 + if (won) 120 else 0
        var record: Boolean

        if (recipe != null) {
            coins += stars * 40
            if (recipe.daily) {
                if (won) {
                    repo.markDailyRecipeCleared()
                    coins += DAILY_BONUS
                }
                repo.addPassPoints(if (won) 25 else 4)
                record = won
            } else {
                record = repo.registerRecipe(recipe.number, stars)
            }
        } else {
            record = repo.registerGame(
                mode = ui.mode,
                score = ui.state.score,
                highestFruit = ui.state.highestLevel,
                merges = mergesThisGame,
                harvests = harvestsThisGame,
                won = won,
            )
        }

        repo.addCoins(coins)
        val leveled = repo.addXp(ui.state.score / 40 + if (won) 80 else 20)
        AdsManager.onGameFinished()

        _ui.value = ui.copy(
            status = if (won) GameStatus.WON else GameStatus.LOST,
            coinsEarned = coins,
            newRecord = record,
            stars = stars,
            speech = null,
            hintTiles = emptySet(),
            unlocked = freshAchievement(),
        )

        sound.play(if (won) Sfx.WIN else Sfx.LOSE)
        if (leveled) sound.play(Sfx.LEVELUP)
        if (won) haptics.win() else haptics.lose()
    }

    /** Conquista que ficou pronta *nesta* partida — vira comemoração na hora. */
    private fun freshAchievement(): Achievement? =
        Achievements.all.firstOrNull { it.isDone(repo.current) && it.id !in achievementsBefore }

    fun dismissAchievement() {
        val shown = _ui.value.unlocked ?: return
        achievementsBefore = achievementsBefore + shown.id
        _ui.value = _ui.value.copy(unlocked = freshAchievement())
    }

    /** Avança para a próxima fase da campanha. */
    fun nextRecipe() {
        val current = _ui.value.recipe ?: return
        if (current.daily) return
        startRecipe((current.number + 1).coerceAtMost(RecipeBook.TOTAL))
    }

    val hasNextRecipe: Boolean
        get() = _ui.value.recipe?.let { !it.daily && it.number < RecipeBook.TOTAL } ?: false

    // --------------------------------------------------------------- poderes

    /** Ativa o poder se houver no inventario; senao pede o anuncio premiado. */
    fun requestPower(power: Power) {
        val ui = _ui.value
        if (ui.status != GameStatus.PLAYING) return
        if (power == Power.RELOGIO && !ui.mode.hasClock) {
            _ui.value = ui.copy(hint = str(R.string.hint_clock_only))
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
            _ui.value = _ui.value.copy(hint = str(R.string.hint_no_coins))
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
            _ui.value = _ui.value.copy(hint = str(R.string.hint_no_video))
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
                    hint = str(
                        if (power == Power.MARTELO) R.string.hint_pick_smash
                        else R.string.hint_pick_grow
                    ),
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
            Power.DICA -> showHint()
        }
        // Poder com alvo so conta depois que o jogador escolhe a fruta.
        if (!power.needsTarget) markPowerUsed()
    }

    private fun showHint() {
        val ui = _ui.value
        val pair = Engine.findHint(ui.state)
        if (pair == null) {
            _ui.value = ui.copy(hint = str(R.string.hint_no_merge))
            return
        }
        _ui.value = ui.copy(
            hintTiles = setOf(pair.first.id, pair.second.id),
            hint = str(R.string.hint_look_here),
        )
        hintJob?.cancel()
        hintJob = viewModelScope.launch {
            delay(5000)
            _ui.value = _ui.value.copy(hintTiles = emptySet())
        }
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
            _ui.value = ui.copy(hint = str(R.string.hint_pick_any))
            haptics.error()
            return
        }
        pushHistory(ui)

        val state = when (power) {
            Power.MARTELO -> Engine.smash(ui.state, row, col)
            Power.ADUBO -> Engine.grow(ui.state, row, col)
            else -> ui.state
        }

        // O Adubo cria uma fruta de verdade, entao ele conta para o pedido —
        // e o que o jogador espera, e e o que da sentido a usar poder na Receita.
        val produced = ui.produced.toMutableMap()
        if (power == Power.ADUBO && target.kind != TileKind.ROTTEN && target.level < Fruit.MAX) {
            val born = target.level + 1
            produced[born] = (produced[born] ?: 0) + 1
        }

        _ui.value = ui.copy(
            state = state,
            produced = produced,
            pendingPower = null,
            hint = null,
            hintTiles = emptySet(),
            canUndo = history.isNotEmpty(),
            bursts = ui.bursts + Burst(effectId++, row, col, target.fruit.glow, true),
        )
        sound.play(Sfx.POWER)
        haptics.power()
        scheduleCleanup()
        markPowerUsed()
        checkEnd()
    }

    fun cancelPendingPower() {
        _ui.value = _ui.value.copy(pendingPower = null, hint = null)
    }

    private fun undo() {
        val previous = history.removeLastOrNull()
        if (previous == null) {
            _ui.value = _ui.value.copy(hint = str(R.string.hint_no_undo))
            return
        }
        movesSinceAttack = previous.movesSinceAttack
        mergesThisGame = previous.merges
        harvestsThisGame = previous.harvests
        seenFruits = HashSet(previous.seen)

        _ui.value = _ui.value.copy(
            state = previous.state.copy(dying = emptyList()),
            produced = previous.produced,
            bossHp = previous.bossHp,
            bossAngry = _ui.value.mode.boss && previous.bossHp < BOSS_HP * 0.3f,
            timeLeft = previous.timeLeft,
            movesLeft = previous.movesLeft,
            combo = 0,
            hintTiles = emptySet(),
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
            unlocked = null,
            timeLeft = if (ui.mode.hasClock) max(ui.timeLeft, 20) else ui.timeLeft,
            movesLeft = if (ui.movesTotal > 0) ui.movesLeft + 5 else ui.movesLeft,
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
        val recipe = _ui.value.recipe
        if (recipe != null) {
            startRecipe(if (recipe.daily) RecipeBook.DAILY else recipe.number)
        } else {
            start(_ui.value.mode, tutorial = false)
        }
    }

    /**
     * Guarda a partida em andamento. Chamado quando o app vai para o fundo:
     * ligação, notificação e bateria não podem custar o tabuleiro do jogador.
     */
    fun saveProgress() {
        val ui = _ui.value
        if (ui.status != GameStatus.PLAYING || tutorialActive) return
        if (ui.state.tiles.isEmpty()) return

        repo.saveResume(
            SavedGame(
                mode = ui.mode,
                recipeNumber = ui.recipe?.let { if (it.daily) RecipeBook.DAILY else it.number } ?: -1,
                recipeDay = recipeDay,
                state = ui.state.copy(dying = emptyList()),
                timeLeft = ui.timeLeft,
                movesLeft = ui.movesLeft,
                movesTotal = ui.movesTotal,
                bossHp = ui.bossHp,
                movesSinceAttack = movesSinceAttack,
                produced = ui.produced,
                merges = mergesThisGame,
                harvests = harvestsThisGame,
            )
        )
    }

    fun leave() {
        saveProgress()
        timerJob?.cancel()
        comboJob?.cancel()
        speechJob?.cancel()
        hintJob?.cancel()
        sound.music(Track.MENU)
    }

    override fun onCleared() {
        timerJob?.cancel()
        comboJob?.cancel()
        speechJob?.cancel()
        hintJob?.cancel()
        super.onCleared()
    }

    companion object {
        const val BOSS_HP = 1400
        const val TUTORIAL_REWARD = 150
        const val DAILY_BONUS = 250

        /** A partir da Pera o baque da fusão sacode a tela. */
        private const val BIG_FRUIT = 7
    }
}
