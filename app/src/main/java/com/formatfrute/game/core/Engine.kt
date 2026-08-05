package com.formatfrute.game.core

import kotlin.random.Random

/**
 * Motor puro do Format Frute: sem Android, sem Compose, so regra de jogo.
 * Tudo aqui e funcao de estado -> estado, o que deixa a UI livre para animar
 * e o teste unitario livre para conferir a regra.
 */
object Engine {

    fun empty(size: Int) = GameState(size = size)

    /** Tabuleiro inicial com [seeds] frutinhas pequenas. */
    fun start(size: Int, rng: Random, seeds: Int = 2): GameState {
        var state = empty(size)
        repeat(seeds) { state = spawn(state, rng) }
        return state
    }

    // ---------------------------------------------------------------- spawn

    /**
     * Coloca uma peca nova numa celula livre. Sem celula livre, devolve o
     * estado intacto.
     */
    fun spawn(
        state: GameState,
        rng: Random,
        rainbowChance: Float = 0f,
        iceChance: Float = 0f,
        forcedLevel: Int? = null,
        forcedKind: TileKind? = null,
    ): GameState {
        val free = freeCells(state)
        if (free.isEmpty()) return state
        val (row, col) = free[rng.nextInt(free.size)]

        val kind = forcedKind ?: when {
            rng.nextFloat() < rainbowChance -> TileKind.RAINBOW
            else -> TileKind.FRUIT
        }
        val level = forcedLevel ?: if (rng.nextFloat() < 0.86f) 0 else 1
        val ice = if (kind == TileKind.FRUIT && rng.nextFloat() < iceChance) 1 else 0

        val tile = Tile(
            id = state.nextId,
            level = if (kind == TileKind.RAINBOW) 0 else level,
            row = row,
            col = col,
            kind = kind,
            ice = ice,
            spawned = true,
        )
        return state.copy(tiles = state.tiles + tile, nextId = state.nextId + 1)
    }

    /** Coloca uma peca numa celula especifica (usado por poderes e tutorial). */
    fun place(state: GameState, row: Int, col: Int, level: Int, kind: TileKind = TileKind.FRUIT): GameState {
        if (state.at(row, col) != null) return state
        val tile = Tile(
            id = state.nextId,
            level = level,
            row = row,
            col = col,
            kind = kind,
            spawned = true,
        )
        return state.copy(tiles = state.tiles + tile, nextId = state.nextId + 1)
    }

    fun freeCells(state: GameState): List<Pair<Int, Int>> {
        val taken = state.tiles.map { it.row to it.col }.toHashSet()
        val out = ArrayList<Pair<Int, Int>>()
        for (r in 0 until state.size) for (c in 0 until state.size) {
            val cell = r to c
            if (cell !in taken) out += cell
        }
        return out
    }

    // ----------------------------------------------------------------- move

    fun move(state: GameState, dir: Direction): MoveResult {
        val n = state.size
        val grid = HashMap<Pair<Int, Int>, Tile>(n * n)
        state.tiles.forEach { grid[it.row to it.col] = it }

        val out = ArrayList<Tile>(state.tiles.size)
        val dead = ArrayList<Tile>()
        val merges = ArrayList<Merge>()
        var gain = 0
        var harvests = 0

        for (line in lines(n, dir)) {
            var dest = 0
            var lastIndex = -1
            var last: Tile? = null
            var lastOpen = false

            for (i in line.indices) {
                val tile = grid[line[i]] ?: continue

                if (tile.frozen) {
                    out += tile.copy(spawned = false, merged = false)
                    dest = i + 1
                    last = null
                    lastOpen = false
                    continue
                }

                val target = last
                if (target != null && lastOpen && canMerge(target, tile)) {
                    val newLevel = mergeLevel(target, tile)
                    val (row, col) = line[lastIndex]

                    if (newLevel > Fruit.MAX) {
                        // Duas melancias: colheita! As duas somem e o tabuleiro respira.
                        out.remove(target)
                        dead += target.copy(row = row, col = col, spawned = false, merged = false)
                        dead += tile.copy(row = row, col = col, spawned = false, merged = false)
                        merges += Merge(row, col, Fruit.MAX, HARVEST_POINTS, harvest = true)
                        gain += HARVEST_POINTS
                        harvests++
                        dest = lastIndex
                        last = null
                        lastOpen = false
                    } else {
                        out.remove(target)
                        val fused = target.copy(
                            level = newLevel,
                            row = row,
                            col = col,
                            kind = TileKind.FRUIT,
                            spawned = false,
                            merged = true,
                        )
                        out += fused
                        dead += tile.copy(row = row, col = col, spawned = false, merged = false)
                        val value = Fruit.of(newLevel).value
                        merges += Merge(row, col, newLevel, value)
                        gain += value
                        last = fused
                        lastOpen = false
                    }
                } else {
                    val (row, col) = line[dest]
                    val placed = tile.copy(row = row, col = col, spawned = false, merged = false)
                    out += placed
                    lastIndex = dest
                    last = placed
                    lastOpen = true
                    dest += 1
                }
            }
        }

        var tiles: List<Tile> = out
        var brokenIce = 0
        var cleaned = 0

        if (merges.isNotEmpty()) {
            // O impacto da fusao quebra o gelo e espanta a fruta podre ao redor.
            val hot = merges.map { it.row to it.col }.toHashSet()
            val touched = HashSet<Pair<Int, Int>>()
            hot.forEach { (r, c) ->
                touched += (r - 1) to c
                touched += (r + 1) to c
                touched += r to (c - 1)
                touched += r to (c + 1)
            }
            val kept = ArrayList<Tile>(tiles.size)
            tiles.forEach { t ->
                val near = (t.row to t.col) in touched
                when {
                    near && t.kind == TileKind.ROTTEN -> {
                        dead += t
                        cleaned++
                    }
                    near && t.frozen -> {
                        brokenIce++
                        kept += t.copy(ice = t.ice - 1)
                    }
                    else -> kept += t
                }
            }
            tiles = kept
        }

        val changed = merges.isNotEmpty() || tiles.any { t ->
            val before = state.tiles.firstOrNull { it.id == t.id }
            before != null && (before.row != t.row || before.col != t.col)
        }

        val next = state.copy(
            tiles = tiles,
            dying = dead,
            score = state.score + gain,
            moves = if (changed) state.moves + 1 else state.moves,
        )
        return MoveResult(
            state = next,
            changed = changed,
            scoreGain = gain,
            merges = merges,
            brokenIce = brokenIce,
            cleanedRotten = cleaned,
            harvests = harvests,
        )
    }

    /** Existe alguma jogada possivel? */
    fun canMove(state: GameState): Boolean {
        if (state.tiles.size < state.size * state.size) return true
        for (r in 0 until state.size) for (c in 0 until state.size) {
            val t = state.at(r, c) ?: return true
            if (t.frozen) continue
            val right = state.at(r, c + 1)
            val down = state.at(r + 1, c)
            if (right != null && !right.frozen && canMerge(t, right)) return true
            if (down != null && !down.frozen && canMerge(t, down)) return true
        }
        return false
    }

    fun canMerge(a: Tile, b: Tile): Boolean {
        if (a.frozen || b.frozen) return false
        if (a.kind == TileKind.ROTTEN || b.kind == TileKind.ROTTEN) return false
        if (a.kind == TileKind.RAINBOW || b.kind == TileKind.RAINBOW) return true
        return a.level == b.level
    }

    private fun mergeLevel(a: Tile, b: Tile): Int = when {
        a.kind == TileKind.RAINBOW && b.kind == TileKind.RAINBOW -> 1
        a.kind == TileKind.RAINBOW -> b.level + 1
        b.kind == TileKind.RAINBOW -> a.level + 1
        else -> a.level + 1
    }

    private fun lines(n: Int, dir: Direction): List<List<Pair<Int, Int>>> = when (dir) {
        Direction.LEFT -> (0 until n).map { r -> (0 until n).map { c -> r to c } }
        Direction.RIGHT -> (0 until n).map { r -> (n - 1 downTo 0).map { c -> r to c } }
        Direction.UP -> (0 until n).map { c -> (0 until n).map { r -> r to c } }
        Direction.DOWN -> (0 until n).map { c -> (n - 1 downTo 0).map { r -> r to c } }
    }

    // -------------------------------------------------------------- poderes

    /** Martelo: quebra a fruta da celula. */
    fun smash(state: GameState, row: Int, col: Int): GameState {
        val target = state.at(row, col) ?: return state
        return state.copy(tiles = state.tiles - target, dying = listOf(target))
    }

    /** Adubo: evolui uma fruta em um nivel (e derrete o gelo dela). */
    fun grow(state: GameState, row: Int, col: Int): GameState {
        val target = state.at(row, col) ?: return state
        if (target.kind == TileKind.ROTTEN) return smash(state, row, col)
        if (target.level >= Fruit.MAX) return state
        val grown = target.copy(
            level = target.level + 1,
            ice = 0,
            kind = TileKind.FRUIT,
            merged = true,
        )
        return state.copy(tiles = state.tiles - target + grown, dying = emptyList())
    }

    /** Peneira: embaralha as posicoes mantendo as frutas. */
    fun shuffle(state: GameState, rng: Random): GameState {
        val cells = ArrayList<Pair<Int, Int>>()
        for (r in 0 until state.size) for (c in 0 until state.size) cells += r to c
        cells.shuffle(rng)
        val tiles = state.tiles.mapIndexed { i, t ->
            val (r, c) = cells[i]
            t.copy(row = r, col = c, spawned = false, merged = false)
        }
        return state.copy(tiles = tiles, dying = emptyList())
    }

    /** Colheita de emergencia: tira as [count] menores frutas do tabuleiro. */
    fun harvestSmallest(state: GameState, count: Int): GameState {
        val victims = state.tiles
            .filter { it.kind == TileKind.FRUIT && !it.frozen }
            .sortedBy { it.level }
            .take(count)
        if (victims.isEmpty()) return state
        return state.copy(tiles = state.tiles - victims.toSet(), dying = victims)
    }

    const val HARVEST_POINTS = 20_000
}
