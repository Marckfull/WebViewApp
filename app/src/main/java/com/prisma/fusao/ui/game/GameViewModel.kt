package com.prisma.fusao.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prisma.fusao.PrismaApplication
import com.prisma.fusao.audio.Sfx
import com.prisma.fusao.core.Campaign
import com.prisma.fusao.core.Cell
import com.prisma.fusao.core.GameEngine
import com.prisma.fusao.core.GemKind
import com.prisma.fusao.core.LevelSpec
import com.prisma.fusao.core.LevelStatus
import com.prisma.fusao.core.MoveOutcome
import com.prisma.fusao.core.ObjectiveState
import com.prisma.fusao.core.Pos
import com.prisma.fusao.core.ResolveStep
import com.prisma.fusao.data.Booster
import com.prisma.fusao.data.MissionMetric
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Comandos que a tela precisa reproduzir em ordem. */
sealed interface BoardEvent {
    /** Some/nasce/funde: a primeira metade do quadro. */
    data class Clear(val step: ResolveStep) : BoardEvent

    /** A gravidade puxa: a segunda metade do quadro. */
    data class Fall(val step: ResolveStep) : BoardEvent

    /** Troca inválida: sacode as duas peças. */
    data class Reject(val a: Pos, val b: Pos) : BoardEvent

    /** Realinha a tela com o tabuleiro real ao fim da jogada. */
    data class Settle(val cells: List<List<Cell>>) : BoardEvent

    /** Recomeço/embaralhamento: refaz tudo. */
    data class Reset(val cells: List<List<Cell>>) : BoardEvent
}

data class GameUiState(
    val level: LevelSpec = Campaign.level(1),
    val score: Int = 0,
    val movesLeft: Int = 0,
    val objectives: List<ObjectiveState> = emptyList(),
    val stars: Int = 0,
    val status: LevelStatus = LevelStatus.PLAYING,
    /** True enquanto uma jogada está animando: bloqueia novos toques. */
    val busy: Boolean = true,
    val armedBooster: Booster? = null,
    val outOfLives: Boolean = false,
    val fusionsThisLevel: Int = 0,
    val gemsClearedThisLevel: Int = 0,
    val coinsAwarded: Int = 0,
    /** Mensagem curta e passageira ("Cascata x3!", "Fusão!"). */
    val toast: String? = null,
)

/**
 * Orquestra a partida: aplica a jogada no motor, reproduz o resultado em quadros
 * (som + animação) e mantém o estado da HUD.
 *
 * O motor resolve a jogada inteira de uma vez; a espera existe só para o jogador
 * conseguir ver o que aconteceu.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<PrismaApplication>()
    private val repository get() = app.repository
    private val sound get() = app.soundEngine

    private var engine: GameEngine? = null
    private var levelIndex: Int = 1

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private val eventChannel = Channel<BoardEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    /** Quando true, a fase é do tutorial: não gasta vida nem grava progresso. */
    private var tutorialMode = false

    fun startLevel(index: Int, tutorial: Boolean = false) {
        tutorialMode = tutorial
        levelIndex = index
        viewModelScope.launch {
            if (!tutorial && !repository.consumeLife()) {
                _state.value = _state.value.copy(outOfLives = true, busy = false)
                return@launch
            }
            val spec = Campaign.level(index)
            val created = GameEngine(spec)
            engine = created
            _state.value = GameUiState(
                level = spec,
                score = 0,
                movesLeft = created.movesLeft,
                objectives = created.objectives(),
                stars = 0,
                status = LevelStatus.PLAYING,
                busy = false,
            )
            eventChannel.send(BoardEvent.Reset(created.cells()))
        }
    }

    /** Reinicia a fase atual — cobra uma vida, como uma partida nova. */
    fun restart() = startLevel(levelIndex, tutorialMode)

    // ------------------------------------------------------------------ jogada

    fun onSwipe(a: Pos, b: Pos) {
        val current = engine ?: return
        if (_state.value.busy || _state.value.status != LevelStatus.PLAYING) return
        if (_state.value.armedBooster != null) return

        viewModelScope.launch {
            when (val outcome = current.trySwap(a, b)) {
                is MoveOutcome.Rejected -> {
                    sound.play(Sfx.INVALID, volume = 0.6f)
                    eventChannel.send(BoardEvent.Reject(outcome.a, outcome.b))
                }

                is MoveOutcome.Accepted -> {
                    sound.play(Sfx.SWAP, volume = 0.7f)
                    _state.value = _state.value.copy(busy = true, movesLeft = current.movesLeft)
                    playSteps(outcome.steps)
                    finishMove()
                }
            }
        }
    }

    fun onTap(pos: Pos) {
        val booster = _state.value.armedBooster
        if (booster != null) {
            useArmedBooster(booster, pos)
            return
        }
        // Sem booster armado, o toque serve como seleção: tocar em duas casas
        // vizinhas equivale a arrastar. É mais confortável em telas grandes.
        val current = _state.value
        if (current.busy) return
        val selected = selectedCell
        if (selected == null) {
            selectedCell = pos
            selectionChannelSend(pos)
        } else if (selected == pos) {
            selectedCell = null
            selectionChannelSend(null)
        } else if (selected.isNeighbor(pos)) {
            selectedCell = null
            selectionChannelSend(null)
            onSwipe(selected, pos)
        } else {
            selectedCell = pos
            selectionChannelSend(pos)
        }
    }

    private var selectedCell: Pos? = null

    private val _selection = MutableStateFlow<Pos?>(null)
    val selection: StateFlow<Pos?> = _selection.asStateFlow()

    private fun selectionChannelSend(pos: Pos?) {
        _selection.value = pos
    }

    /**
     * Reproduz a linha do tempo de uma jogada. Cada quadro tem duas metades para
     * o olho conseguir separar "o que estourou" de "o que caiu".
     */
    private suspend fun playSteps(steps: List<ResolveStep>) {
        val current = engine ?: return
        var fusions = 0
        var gems = 0

        for (step in steps) {
            playStepSounds(step)
            eventChannel.send(BoardEvent.Clear(step))

            fusions += step.fusions.size
            gems += step.cleared.count { it.gem.kind == GemKind.NORMAL }

            if (step.fusions.any { it.result.kind == GemKind.NOVA }) {
                _state.value = _state.value.copy(toast = "NOVA CROMÁTICA!")
            } else if (step.fusions.isNotEmpty()) {
                _state.value = _state.value.copy(toast = "FUSÃO!")
            } else if (step.cascade >= 2 && step.cleared.isNotEmpty()) {
                _state.value = _state.value.copy(toast = "CASCATA x${step.cascade}")
            }

            delay(CLEAR_MS)
            eventChannel.send(BoardEvent.Fall(step))

            _state.value = _state.value.copy(
                score = current.score,
                movesLeft = current.movesLeft,
                objectives = current.objectives(),
                stars = current.stars(),
                fusionsThisLevel = _state.value.fusionsThisLevel + step.fusions.size,
                gemsClearedThisLevel = _state.value.gemsClearedThisLevel +
                    step.cleared.count { it.gem.kind == GemKind.NORMAL },
            )
            delay(fallDuration(step))
        }

        if (fusions > 0 || gems > 0) {
            repository.trackMissions(
                mapOf(
                    MissionMetric.FUSIONS to fusions,
                    MissionMetric.GEMS_CLEARED to gems,
                    MissionMetric.PRISMS_CREATED to steps.sumOf { step ->
                        step.fusions.count { it.result.kind == GemKind.PRISM }
                    },
                ),
            )
        }
        eventChannel.send(BoardEvent.Settle(current.cells()))
        _state.value = _state.value.copy(toast = null)
    }

    private fun playStepSounds(step: ResolveStep) {
        when {
            step.fusions.any { it.result.kind == GemKind.NOVA } -> sound.play(Sfx.NOVA)
            step.blasts.any { it.kind == GemKind.NOVA } -> sound.play(Sfx.NOVA)
            step.fusions.isNotEmpty() -> sound.play(Sfx.FUSION)
            step.blasts.any { it.kind == GemKind.SUPERNOVA } -> sound.play(Sfx.SUPERNOVA)
            step.blasts.any { it.kind == GemKind.PRISM } -> sound.play(Sfx.PRISM)
            step.created.isNotEmpty() -> sound.play(Sfx.ESSENCE)
            step.cleared.isNotEmpty() -> sound.playCascade(step.cascade)
        }
        if (step.iceBroken.isNotEmpty()) sound.play(Sfx.ICE, volume = 0.7f)
        if (step.stonesHit.isNotEmpty()) sound.play(Sfx.STONE, volume = 0.7f)
        if (step.prismoidsDelivered.isNotEmpty()) sound.play(Sfx.PRISMOID)
    }

    /** Quanto maior a queda, mais tempo a animação precisa. */
    private fun fallDuration(step: ResolveStep): Long {
        val drop = (step.moves.maxOfOrNull { it.to.r - it.from.r } ?: 0)
            .coerceAtLeast(step.spawns.maxOfOrNull { it.to.r - it.from.r } ?: 0)
        return (FALL_BASE_MS + drop * 22L).coerceAtMost(520L)
    }

    /** Fecha a jogada: confere fim de fase e destrava o tabuleiro se seguir jogando. */
    private suspend fun finishMove() {
        val current = engine ?: return

        if (current.status == LevelStatus.PLAYING && !current.hasPossibleMove()) {
            _state.value = _state.value.copy(toast = "Sem jogadas — embaralhando")
            delay(700)
            current.shuffle()
            eventChannel.send(BoardEvent.Reset(current.cells()))
            _state.value = _state.value.copy(toast = null)
        }

        when (current.status) {
            LevelStatus.WON -> {
                sound.play(Sfx.WIN)
                val coins = if (tutorialMode) 0 else repository.completeLevel(
                    level = levelIndex,
                    score = current.score,
                    stars = current.stars(),
                    fusions = _state.value.fusionsThisLevel,
                    gems = _state.value.gemsClearedThisLevel,
                )
                _state.value = _state.value.copy(
                    busy = false,
                    status = LevelStatus.WON,
                    stars = current.stars(),
                    coinsAwarded = coins,
                )
            }

            LevelStatus.LOST -> {
                sound.play(Sfx.LOSE)
                _state.value = _state.value.copy(busy = false, status = LevelStatus.LOST)
            }

            LevelStatus.PLAYING -> _state.value = _state.value.copy(busy = false)
        }
    }

    // ---------------------------------------------------------------- boosters

    fun armBooster(booster: Booster) {
        if (_state.value.busy || _state.value.status != LevelStatus.PLAYING) return
        _state.value = _state.value.copy(
            armedBooster = if (_state.value.armedBooster == booster) null else booster,
        )
        if (booster == Booster.SHUFFLE) useShuffle()
    }

    fun cancelBooster() {
        _state.value = _state.value.copy(armedBooster = null)
    }

    private fun useShuffle() {
        val current = engine ?: return
        viewModelScope.launch {
            if (!repository.consumeBooster(Booster.SHUFFLE)) {
                _state.value = _state.value.copy(armedBooster = null, toast = "Você não tem esse item")
                delay(1200)
                _state.value = _state.value.copy(toast = null)
                return@launch
            }
            _state.value = _state.value.copy(busy = true, armedBooster = null)
            current.shuffle()
            sound.play(Sfx.SWAP)
            eventChannel.send(BoardEvent.Reset(current.cells()))
            delay(320)
            _state.value = _state.value.copy(busy = false)
        }
    }

    private fun useArmedBooster(booster: Booster, pos: Pos) {
        val current = engine ?: return
        if (_state.value.busy) return
        viewModelScope.launch {
            if (!repository.consumeBooster(booster)) {
                _state.value = _state.value.copy(armedBooster = null, toast = "Você não tem esse item")
                delay(1200)
                _state.value = _state.value.copy(toast = null)
                return@launch
            }
            _state.value = _state.value.copy(busy = true, armedBooster = null)
            val steps = when (booster) {
                Booster.HAMMER -> current.useHammer(pos)
                Booster.BOMB -> current.useBomb(pos)
                Booster.COLOR_BLAST -> {
                    val color = current.board.gemAt(pos)?.color
                    if (color != null) current.useColorBlast(color) else emptyList()
                }
                else -> emptyList()
            }
            playSteps(steps)
            finishMove()
        }
    }

    /** Prêmio do vídeo recompensado: devolve a fase perdida com jogadas extras. */
    fun grantExtraMoves(amount: Int = 5) {
        val current = engine ?: return
        current.grantMoves(amount)
        _state.value = _state.value.copy(
            movesLeft = current.movesLeft,
            status = current.status,
            busy = false,
        )
    }

    fun consumeExtraMovesBooster(onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.consumeBooster(Booster.EXTRA_MOVES)
            if (ok) grantExtraMoves()
            onDone(ok)
        }
    }

    /** Dobra as moedas da fase; usado pelo vídeo recompensado da tela de vitória. */
    fun doubleCoins(onDone: () -> Unit = {}) {
        val extra = _state.value.coinsAwarded
        if (extra <= 0) {
            onDone()
            return
        }
        viewModelScope.launch {
            repository.grantCoins(extra)
            sound.play(Sfx.COIN)
            _state.value = _state.value.copy(coinsAwarded = extra * 2)
            onDone()
        }
    }

    fun currentEngine(): GameEngine? = engine

    companion object {
        private const val CLEAR_MS = 240L
        private const val FALL_BASE_MS = 200L
    }
}
