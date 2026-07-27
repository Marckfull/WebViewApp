package com.neonsombra.game.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

const val COLS = 10
const val ROWS = 20

/** Quantas pecas futuras o jogador ve no painel lateral. */
const val NEXT_COUNT = 3

/**
 * Codigo do bloco que a sombra solidificou. Nao pertence a nenhuma peca: e o
 * lado sombrio virando obstaculo de verdade.
 */
const val SHADOW_CODE = 99

private const val LOCK_DELAY = 0.45f
private const val MAX_LOCK_RESETS = 10
private const val CLEAR_ANIMATION = 0.30f
private const val SOFT_DROP_INTERVAL = 0.040f
private const val LINES_PER_LEVEL = 10

/** A partir deste nivel a sombra comeca a atacar. */
private const val SHADOW_ATTACK_LEVEL = 5

/** Teto de blocos sombrios vivos ao mesmo tempo, para o tabuleiro nao travar. */
private const val MAX_SOLID_SHADOWS = 8

/** Colunas onde as pecas nascem: a sombra nunca solidifica aqui em cima. */
private val SPAWN_SAFE_COLUMNS = 3..6

/** Quanto tempo a Purga segura a sombra depois de um tetris. */
private const val PURGE_DURATION = 15f

/** Nos ultimos segundos da Purga a sombra volta aparecendo aos poucos. */
private const val PURGE_FADE = 2f

/** Duracao do clarao que marca o ponto onde a sombra atacou. */
private const val STRIKE_FLASH = 0.55f

enum class GameStatus { READY, PLAYING, CLEARING, PAUSED, GAME_OVER }

/** Avisos que o motor manda para a camada de som/vibracao. */
sealed interface GameEvent {
    data object Moved : GameEvent
    data object Rotated : GameEvent
    data object Locked : GameEvent
    data class LinesCleared(val count: Int) : GameEvent
    data class HardDropped(val distance: Int) : GameEvent
    data class Combo(val count: Int) : GameEvent
    data class ShadowStrike(val row: Int, val column: Int) : GameEvent
    data object PurgeStarted : GameEvent
    data object PurgeEnded : GameEvent
    data object LevelUp : GameEvent
    data object GameOver : GameEvent
}

/**
 * Retrato imutavel do jogo, entregue ao Compose a cada quadro.
 *
 * [cells] tem ROWS * COLS posicoes; 0 e vazio, [SHADOW_CODE] e um bloco que a
 * sombra solidificou e qualquer outro valor e o [TetrominoType.code] da peca
 * que ali ficou presa.
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
    val combo: Int,
    val purgeSecondsLeft: Float,
    val shadowAttacksActive: Boolean,
    val strikeRow: Int,
    val strikeColumn: Int,
    val strikeFlash: Float,
) {
    fun codeAt(row: Int, col: Int): Int = cells[row * COLS + col]

    /** True enquanto a Purga estiver segurando o lado sombrio. */
    val purgeActive: Boolean get() = purgeSecondsLeft > 0f

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

    private var strikeTimer = 0f
    private var strikeFlash = 0f
    private var strikeRow = -1
    private var strikeColumn = -1
    private var solidShadows = 0

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

    /** Quantas pecas seguidas limparam linha. */
    var combo = 0
        private set

    /** Segundos restantes da Purga; zero quando a sombra esta solta. */
    var purgeSecondsLeft = 0f
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
        combo = 0
        purgeSecondsLeft = 0f
        solidShadows = 0
        strikeTimer = strikeInterval()
        strikeFlash = 0f
        strikeRow = -1
        strikeColumn = -1
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
        if (strikeFlash > 0f) strikeFlash = max(0f, strikeFlash - delta)

        when (status) {
            GameStatus.CLEARING -> {
                updatePurge(delta)
                clearTimer -= delta
                if (clearTimer <= 0f) finishClearing()
            }

            GameStatus.PLAYING -> {
                updatePurge(delta)
                updateShadowAttack(delta)

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

    /**
     * Deslize rapido para baixo: a peca despenca ate o fundo, rende dois pontos
     * por linha percorrida e trava na hora, sem esperar o lock delay.
     */
    fun hardDrop(): Boolean {
        val piece = active ?: return false
        if (status != GameStatus.PLAYING) return false
        val target = ghostY()
        val distance = target - piece.y
        if (distance > 0) {
            active = piece.copy(y = target)
            score += distance * 2
        }
        pendingEvents += GameEvent.HardDropped(distance)
        lockPiece()
        return true
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
            combo = combo,
            purgeSecondsLeft = purgeSecondsLeft,
            shadowAttacksActive = shadowAttacksActive() && purgeSecondsLeft <= 0f,
            strikeRow = strikeRow,
            strikeColumn = strikeColumn,
            strikeFlash = strikeFlash,
        )
    }

    /** Esvazia a fila de avisos acumulados desde a ultima chamada. */
    fun drainEvents(): List<GameEvent> {
        if (pendingEvents.isEmpty()) return emptyList()
        val copy = pendingEvents.toList()
        pendingEvents.clear()
        return copy
    }

    /**
     * Opacidade do espelho sombrio: sobe com o nivel e cai a zero durante a
     * Purga, voltando aos poucos nos ultimos segundos.
     */
    fun shadowStrength(): Float {
        val base = min(0.92f, 0.50f + (level - 1) * 0.045f)
        if (purgeSecondsLeft <= 0f) return base
        val returning = ((PURGE_FADE - purgeSecondsLeft) / PURGE_FADE).coerceIn(0f, 1f)
        return base * returning
    }

    fun gravityInterval(): Float = max(0.07f, 0.90f - (level - 1) * 0.075f)

    /** De quanto em quanto tempo a sombra tenta solidificar um bloco. */
    fun strikeInterval(): Float = max(4f, 14f - level * 0.8f)

    fun shadowAttacksActive(): Boolean = level >= SHADOW_ATTACK_LEVEL

    // --------------------------------------------------------------- interno

    private fun updatePurge(delta: Float) {
        if (purgeSecondsLeft <= 0f) return
        purgeSecondsLeft = max(0f, purgeSecondsLeft - delta)
        if (purgeSecondsLeft == 0f) {
            pendingEvents += GameEvent.PurgeEnded
            strikeTimer = strikeInterval()
        }
    }

    /**
     * O lado sombrio revidando: de tempos em tempos uma celula da sombra vira
     * bloco de verdade. So acontece do nivel [SHADOW_ATTACK_LEVEL] em diante e
     * fica suspenso enquanto a Purga estiver ativa.
     */
    private fun updateShadowAttack(delta: Float) {
        if (!shadowAttacksActive() || purgeSecondsLeft > 0f) return
        strikeTimer -= delta
        if (strikeTimer > 0f) return
        strikeTimer = strikeInterval()
        solidifyShadowCell()
    }

    private fun solidifyShadowCell() {
        if (solidShadows >= MAX_SOLID_SHADOWS) return
        val piece = active
        val candidates = mutableListOf<Int>()

        for (row in 0 until ROWS) {
            for (column in 0 until COLS) {
                val index = row * COLS + column
                if (grid[index] != 0) continue
                // So vira bloco o que ja e sombra: o reflexo da pilha do fundo.
                if (grid[(ROWS - 1 - row) * COLS + column] == 0) continue
                // Nunca sufoca a area onde as pecas nascem.
                if (row <= 1 && column in SPAWN_SAFE_COLUMNS) continue
                if (piece != null && piece.occupies(column, row)) continue
                candidates += index
            }
        }

        if (candidates.isEmpty()) return
        val index = candidates[random.nextInt(candidates.size)]
        grid[index] = SHADOW_CODE
        solidShadows++
        gridDirty = true
        strikeRow = index / COLS
        strikeColumn = index % COLS
        strikeFlash = STRIKE_FLASH
        pendingEvents += GameEvent.ShadowStrike(strikeRow, strikeColumn)
    }

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
            combo = 0
            spawn()
        } else {
            combo++
            clearingRows = full
            clearTimer = CLEAR_ANIMATION
            status = GameStatus.CLEARING
            pendingEvents += GameEvent.LinesCleared(full.size)
            if (combo >= 2) pendingEvents += GameEvent.Combo(combo)
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
            solidShadows = grid.count { it == SHADOW_CODE }

            score += LINE_SCORES[rows.size] * level
            // Bonus por encadear limpezas seguidas.
            if (combo >= 2) score += COMBO_BONUS * (combo - 1) * level

            lines += rows.size
            val newLevel = 1 + lines / LINES_PER_LEVEL
            if (newLevel > level) {
                level = newLevel
                pendingEvents += GameEvent.LevelUp
            }

            // Quatro linhas de uma vez: a Purga apaga a sombra por um tempo.
            if (rows.size >= 4) {
                purgeSecondsLeft = PURGE_DURATION
                pendingEvents += GameEvent.PurgeStarted
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

    /** Sorteio em sacos de sete: garante variedade sem sequencias crueis. */
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
        solidShadows = 0
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

    /** Pula direto para o nivel pedido. So para testes. */
    internal fun setLevelForTest(target: Int) {
        level = target
        strikeTimer = strikeInterval()
    }

    /** Faz a sombra atacar agora, sem esperar o temporizador. So para testes. */
    internal fun forceShadowStrikeForTest() = solidifyShadowCell()

    companion object {
        private val LINE_SCORES = intArrayOf(0, 100, 300, 500, 800)

        /** Pontos extras por limpeza encadeada, multiplicados pelo nivel. */
        private const val COMBO_BONUS = 50

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
