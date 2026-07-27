package com.neonsombra.game.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

const val COLS = 10
const val ROWS = 20

/** Quantas pecas futuras o jogador ve no painel lateral. */
const val NEXT_COUNT = 3

private const val LOCK_DELAY = 0.45f
private const val MAX_LOCK_RESETS = 10
private const val CLEAR_ANIMATION = 0.30f
private const val SOFT_DROP_INTERVAL = 0.040f
private const val LINES_PER_LEVEL = 10

enum class GameStatus { READY, PLAYING, CLEARING, PAUSED, GAME_OVER }

/** Avisos que o motor manda para a camada de som/vibracao. */
sealed interface GameEvent {
    data object Moved : GameEvent
    data object Rotated : GameEvent
    data object Locked : GameEvent
    data class LinesCleared(val count: Int) : GameEvent
    data object LevelUp : GameEvent
    data object GameOver : GameEvent
}

/**
 * Retrato imutavel do jogo, entregue ao Compose a cada quadro.
 *
 * [cells] tem ROWS * COLS posicoes; 0 e vazio, qualquer outro valor e o
 * [TetrominoType.code] da peca que ali ficou presa.
 */
data class GameSnapshot(
    val cells: List<Int>,
    val active: ActivePiece?,
    val ghostY: Int,
    val next: List<TetrominoType>,
    val clearingRows: Set<Int>,
    val score: Int,
    val lines: Int,
    val level: Int,
    val status: GameStatus,
    val shadowStrength: Float,
    val softDropping: Boolean,
) {
    fun codeAt(row: Int, col: Int): Int = cells[row * COLS + col]

    /**
     * O diferencial do jogo: a pilha do fundo projeta um espelho invertido na
     * parte de cima do tabuleiro. Devolve o codigo da peca refletida naquela
     * celula, ou 0 se ali nao ha sombra.
     *
     * A sombra e apenas visual -- nao colide com nada, so atrapalha a leitura
     * do tabuleiro. Quanto mais alta a pilha, mais o "lado sombrio" invade a
     * area onde as pecas novas caem.
     */
    fun shadowCodeAt(row: Int, col: Int): Int {
        if (codeAt(row, col) != 0) return 0
        return codeAt(ROWS - 1 - row, col)
    }
}

/**
 * Toda a regra do Tetris vive aqui, sem nenhuma dependencia de Android ou
 * Compose, para poder ser testada na JVM.
 */
class TetrisEngine(private val random: Random = Random.Default) {

    private val grid = IntArray(ROWS * COLS)
    private val bag = ArrayDeque<TetrominoType>()
    private val queue = ArrayDeque<TetrominoType>()
    private val pendingEvents = mutableListOf<GameEvent>()

    private var cachedCells: List<Int> = grid.toList()
    private var gridDirty = false

    private var gravityAccumulator = 0f
    private var lockTimer = 0f
    private var lockResets = 0
    private var clearTimer = 0f
    private var clearingRows: Set<Int> = emptySet()

    var active: ActivePiece? = null
        private set
    var score = 0
        private set
    var lines = 0
        private set
    var level = 1
        private set
    var status = GameStatus.READY
        private set

    /** Ligado enquanto o jogador segura o dedo na tela. */
    var softDropping = false

    init {
        reset()
    }

    // ------------------------------------------------------------------ ciclo

    fun reset() {
        grid.fill(0)
        bag.clear()
        queue.clear()
        pendingEvents.clear()
        gravityAccumulator = 0f
        lockTimer = 0f
        lockResets = 0
        clearTimer = 0f
        clearingRows = emptySet()
        softDropping = false
        score = 0
        lines = 0
        level = 1
        gridDirty = true
        repeat(NEXT_COUNT + 1) { queue.addLast(nextFromBag()) }
        active = null
        status = GameStatus.READY
    }

    fun start() {
        if (status != GameStatus.READY) reset()
        status = GameStatus.PLAYING
        spawn()
    }

    fun pause() {
        if (status == GameStatus.PLAYING || status == GameStatus.CLEARING) {
            status = GameStatus.PAUSED
            softDropping = false
        }
    }

    fun resume() {
        if (status == GameStatus.PAUSED) {
            status = if (clearingRows.isEmpty()) GameStatus.PLAYING else GameStatus.CLEARING
        }
    }

    /** Avanca o jogo [delta] segundos. */
    fun update(delta: Float) {
        when (status) {
            GameStatus.CLEARING -> {
                clearTimer -= delta
                if (clearTimer <= 0f) finishClearing()
            }

            GameStatus.PLAYING -> {
                val interval = if (softDropping) SOFT_DROP_INTERVAL else gravityInterval()
                gravityAccumulator += delta
                var guard = 0
                while (gravityAccumulator >= interval && status == GameStatus.PLAYING && guard++ < ROWS) {
                    gravityAccumulator -= interval
                    stepDown()
                }
                if (status == GameStatus.PLAYING) {
                    if (isGrounded()) {
                        lockTimer += delta
                        if (lockTimer >= LOCK_DELAY) lockPiece()
                    } else {
                        lockTimer = 0f
                    }
                }
            }

            else -> Unit
        }
    }

    // ----------------------------------------------------------------- acoes

    fun moveHorizontally(direction: Int): Boolean {
        val piece = active ?: return false
        if (status != GameStatus.PLAYING) return false
        val moved = piece.copy(x = piece.x + direction)
        if (collides(moved)) return false
        active = moved
        onPieceTouched()
        pendingEvents += GameEvent.Moved
        return true
    }

    /**
     * Um toque na tela roda a peca. Como cada peca tem quatro estados, o
     * jogador consegue passar por ate tres posicoes diferentes da original
     * antes de voltar ao inicio.
     */
    fun rotate(): Boolean {
        val piece = active ?: return false
        if (status != GameStatus.PLAYING) return false
        val target = piece.copy(rotation = (piece.rotation + 1) % 4)
        for (kick in WALL_KICKS) {
            val candidate = target.copy(x = target.x + kick[0], y = target.y + kick[1])
            if (!collides(candidate)) {
                active = candidate
                onPieceTouched()
                pendingEvents += GameEvent.Rotated
                return true
            }
        }
        return false
    }

    /** Onde a peca atual pousaria: usado para desenhar a peca fantasma. */
    fun ghostY(): Int {
        val piece = active ?: return 0
        var y = piece.y
        while (!collides(piece.copy(y = y + 1))) y++
        return y
    }

    // ---------------------------------------------------------------- estado

    fun snapshot(): GameSnapshot {
        if (gridDirty) {
            cachedCells = grid.toList()
            gridDirty = false
        }
        return GameSnapshot(
            cells = cachedCells,
            active = active,
            ghostY = ghostY(),
            next = queue.take(NEXT_COUNT),
            clearingRows = clearingRows,
            score = score,
            lines = lines,
            level = level,
            status = status,
            shadowStrength = shadowStrength(),
            softDropping = softDropping,
        )
    }

    /** Esvazia a fila de avisos acumulados desde a ultima chamada. */
    fun drainEvents(): List<GameEvent> {
        if (pendingEvents.isEmpty()) return emptyList()
        val copy = pendingEvents.toList()
        pendingEvents.clear()
        return copy
    }

    /** Opacidade do espelho sombrio: sobe com o nivel, ate ficar bem escuro. */
    fun shadowStrength(): Float = min(0.92f, 0.50f + (level - 1) * 0.045f)

    fun gravityInterval(): Float = max(0.07f, 0.90f - (level - 1) * 0.075f)

    // --------------------------------------------------------------- interno

    private fun stepDown() {
        val piece = active ?: return
        val moved = piece.copy(y = piece.y + 1)
        if (collides(moved)) {
            // Encostou: quem decide travar e o temporizador de lock.
            gravityAccumulator = 0f
            return
        }
        active = moved
        lockTimer = 0f
        if (softDropping) score += 1
    }

    private fun isGrounded(): Boolean {
        val piece = active ?: return false
        return collides(piece.copy(y = piece.y + 1))
    }

    /** Mexer ou rodar uma peca encostada da mais um instante antes de travar. */
    private fun onPieceTouched() {
        if (isGrounded() && lockResets < MAX_LOCK_RESETS) {
            lockResets++
            lockTimer = 0f
        }
    }

    private fun lockPiece() {
        val piece = active ?: return
        piece.forEachCell { cx, cy ->
            if (cy in 0 until ROWS && cx in 0 until COLS) {
                grid[cy * COLS + cx] = piece.type.code
            }
        }
        gridDirty = true
        active = null
        lockTimer = 0f
        lockResets = 0
        gravityAccumulator = 0f
        pendingEvents += GameEvent.Locked

        val full = (0 until ROWS).filter { row ->
            (0 until COLS).all { col -> grid[row * COLS + col] != 0 }
        }.toSet()

        if (full.isEmpty()) {
            spawn()
        } else {
            clearingRows = full
            clearTimer = CLEAR_ANIMATION
            status = GameStatus.CLEARING
            pendingEvents += GameEvent.LinesCleared(full.size)
        }
    }

    private fun finishClearing() {
        val rows = clearingRows
        if (rows.isNotEmpty()) {
            val kept = (0 until ROWS).filter { it !in rows }
            val rebuilt = IntArray(ROWS * COLS)
            var target = ROWS - 1
            for (row in kept.reversed()) {
                System.arraycopy(grid, row * COLS, rebuilt, target * COLS, COLS)
                target--
            }
            System.arraycopy(rebuilt, 0, grid, 0, grid.size)
            gridDirty = true

            score += LINE_SCORES[rows.size] * level
            lines += rows.size
            val newLevel = 1 + lines / LINES_PER_LEVEL
            if (newLevel > level) {
                level = newLevel
                pendingEvents += GameEvent.LevelUp
            }
        }
        clearingRows = emptySet()
        status = GameStatus.PLAYING
        spawn()
    }

    private fun spawn() {
        val type = queue.removeFirst()
        queue.addLast(nextFromBag())
        val piece = ActivePiece(
            type = type,
            rotation = 0,
            x = (COLS - type.boxSize) / 2,
            y = 0,
        )
        if (collides(piece)) {
            active = piece
            status = GameStatus.GAME_OVER
            softDropping = false
            pendingEvents += GameEvent.GameOver
            return
        }
        active = piece
        lockTimer = 0f
        lockResets = 0
        gravityAccumulator = 0f
    }

    private fun collides(piece: ActivePiece): Boolean {
        for (cell in piece.type.cells(piece.rotation)) {
            val cx = piece.x + cell[0]
            val cy = piece.y + cell[1]
            if (cx < 0 || cx >= COLS || cy >= ROWS) return true
            if (cy >= 0 && grid[cy * COLS + cx] != 0) return true
        }
        return false
    }

    /** Sorteio em sacos de sete: garante variedade sem sequencias cruéis. */
    private fun nextFromBag(): TetrominoType {
        if (bag.isEmpty()) {
            TetrominoType.entries.shuffled(random).forEach { bag.addLast(it) }
        }
        return bag.removeFirst()
    }

    // ----------------------------------------------------------------- testes

    /**
     * Monta o tabuleiro a partir de um desenho, uma string por linha, do topo
     * para o fundo: '.' e celula vazia e qualquer outro caractere e bloco.
     * Existe so para os testes conseguirem preparar situacoes especificas.
     */
    internal fun loadForTest(rows: List<String>) {
        require(rows.size <= ROWS) { "o desenho tem mais linhas do que o tabuleiro" }
        grid.fill(0)
        val offset = ROWS - rows.size
        rows.forEachIndexed { index, line ->
            require(line.length == COLS) { "cada linha precisa de $COLS colunas" }
            line.forEachIndexed { column, char ->
                grid[(offset + index) * COLS + column] = if (char == '.') 0 else TetrominoType.O.code
            }
        }
        gridDirty = true
    }

    /** Forca a peca em queda numa posicao conhecida. So para testes. */
    internal fun placeForTest(piece: ActivePiece) {
        active = piece
        lockTimer = 0f
        lockResets = 0
        gravityAccumulator = 0f
        status = GameStatus.PLAYING
    }

    companion object {
        private val LINE_SCORES = intArrayOf(0, 100, 300, 500, 800)

        /** Empurroes tentados quando a rotacao esbarra numa parede ou peca. */
        private val WALL_KICKS = arrayOf(
            intArrayOf(0, 0),
            intArrayOf(-1, 0),
            intArrayOf(1, 0),
            intArrayOf(-2, 0),
            intArrayOf(2, 0),
            intArrayOf(0, -1),
        )
    }
}
