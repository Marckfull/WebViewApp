package com.prisma.fusao.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Regras do PRISMA. Nenhuma dependência de Android aqui de propósito: o motor é
 * Kotlin puro, determinístico por semente e coberto por testes.
 */
class GameEngine(
    val spec: LevelSpec,
    seed: Long = spec.seed,
) {
    val rows: Int = spec.rows
    val cols: Int = spec.cols
    val board: Board = Board(rows, cols)

    private val rng = Random(seed)
    private var nextId = 1L
    private val progress = ObjectiveProgress(spec.objectives)

    var score: Int = 0
        private set
    var movesLeft: Int = spec.moves
        private set
    var status: LevelStatus = LevelStatus.PLAYING
        private set
    var fusionCount: Int = 0
        private set

    private var prismoidsDelivered = 0

    init {
        buildBoard()
    }

    // ---------------------------------------------------------------- estado

    fun objectives(): List<ObjectiveState> = progress.snapshot()

    fun stars(): Int = spec.starsFor(score)

    fun cells(): List<List<Cell>> = List(rows) { r -> List(cols) { c -> board[r, c] } }

    // ------------------------------------------------------------ construção

    private fun newId(): Long = nextId++

    private fun randomColor(): GemColor = spec.palette[rng.nextInt(spec.palette.size)]

    private fun newNormal(color: GemColor = randomColor()) =
        Gem(newId(), GemKind.NORMAL, color)

    private fun buildBoard() {
        for (r in 0 until rows) {
            val line = spec.mask.getOrElse(r) { ".".repeat(cols) }
            for (c in 0 until cols) {
                when (line.getOrElse(c) { '.' }) {
                    '#' -> board[r, c] = Cell(playable = false)
                    'X' -> board[r, c] = Cell(gem = Gem(newId(), GemKind.STONE, hp = 2))
                    '1' -> board[r, c] = Cell(ice = 1)
                    '2' -> board[r, c] = Cell(ice = 2)
                    else -> board[r, c] = Cell()
                }
            }
        }
        // A casa jogável mais alta de cada coluna é por onde entram gemas novas.
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                if (board[r, c].playable) {
                    board[r, c] = board[r, c].copy(spawner = true)
                    break
                }
            }
        }
        // Preenche sem deixar nenhum match pronto na largada.
        for (p in board.playablePositions()) {
            if (board[p].gem != null) continue
            var gem = newNormal()
            var guard = 0
            while (createsMatchAt(p, gem.color!!) && guard++ < 24) gem = newNormal()
            board.setGem(p, gem)
        }
        ensurePlayable()
    }

    /** Verifica se colocar [color] em [p] já fecharia um match (usado só no preenchimento). */
    private fun createsMatchAt(p: Pos, color: GemColor): Boolean {
        var run = 1
        var c = p.c - 1
        while (c >= 0 && board.gemAt(p.r, c)?.takeIf { it.matchable }?.color == color) { run++; c-- }
        c = p.c + 1
        while (c < cols && board.gemAt(p.r, c)?.takeIf { it.matchable }?.color == color) { run++; c++ }
        if (run >= 3) return true

        run = 1
        var r = p.r - 1
        while (r >= 0 && board.gemAt(r, p.c)?.takeIf { it.matchable }?.color == color) { run++; r-- }
        r = p.r + 1
        while (r < rows && board.gemAt(r, p.c)?.takeIf { it.matchable }?.color == color) { run++; r++ }
        return run >= 3
    }

    // --------------------------------------------------------------- matches

    private data class Run(val cells: List<Pos>, val color: GemColor, val horizontal: Boolean)

    private data class MatchGroup(
        val cells: Set<Pos>,
        val color: GemColor,
        val runs: List<Run>,
    ) {
        val isCross: Boolean get() = runs.size >= 2
        val longest: Int get() = runs.maxOf { it.cells.size }
    }

    private fun findRuns(): List<Run> {
        val runs = mutableListOf<Run>()
        for (r in 0 until rows) {
            var c = 0
            while (c < cols) {
                val color = board.gemAt(r, c)?.takeIf { it.matchable }?.color
                if (color == null) { c++; continue }
                var end = c
                while (end + 1 < cols &&
                    board.gemAt(r, end + 1)?.takeIf { it.matchable }?.color == color
                ) end++
                if (end - c + 1 >= 3) {
                    runs += Run((c..end).map { Pos(r, it) }, color, horizontal = true)
                }
                c = end + 1
            }
        }
        for (c in 0 until cols) {
            var r = 0
            while (r < rows) {
                val color = board.gemAt(r, c)?.takeIf { it.matchable }?.color
                if (color == null) { r++; continue }
                var end = r
                while (end + 1 < rows &&
                    board.gemAt(end + 1, c)?.takeIf { it.matchable }?.color == color
                ) end++
                if (end - r + 1 >= 3) {
                    runs += Run((r..end).map { Pos(it, c) }, color, horizontal = false)
                }
                r = end + 1
            }
        }
        return runs
    }

    /** Junta sequências que se cruzam para reconhecer formatos em L e em T. */
    private fun findMatchGroups(): List<MatchGroup> {
        val runs = findRuns()
        if (runs.isEmpty()) return emptyList()
        val parent = IntArray(runs.size) { it }
        fun find(x: Int): Int {
            var a = x
            while (parent[a] != a) { parent[a] = parent[parent[a]]; a = parent[a] }
            return a
        }
        fun union(x: Int, y: Int) {
            val rx = find(x); val ry = find(y)
            if (rx != ry) parent[rx] = ry
        }
        for (i in runs.indices) for (j in i + 1 until runs.size) {
            if (runs[i].color != runs[j].color) continue
            if (runs[i].cells.any { it in runs[j].cells }) union(i, j)
        }
        return runs.indices.groupBy { find(it) }.values.map { idx ->
            val group = idx.map { runs[it] }
            MatchGroup(group.flatMap { it.cells }.toSet(), group.first().color, group)
        }
    }

    fun hasMatches(): Boolean = findRuns().isNotEmpty()

    // ------------------------------------------------------------- detonação

    /** Casas atingidas quando uma peça especial detona sozinha. */
    private fun blastCells(pos: Pos, gem: Gem): List<Pos> = when (gem.kind) {
        GemKind.ESSENCE -> square(pos, 1)
        GemKind.PRISM -> prismCells(pos)
        GemKind.SUPERNOVA -> supernovaCells(pos, gem.color)
        else -> emptyList()
    }

    private fun square(center: Pos, radius: Int): List<Pos> {
        val out = mutableListOf<Pos>()
        for (r in center.r - radius..center.r + radius) {
            for (c in center.c - radius..center.c + radius) {
                if (board.inBounds(r, c) && board[r, c].playable) out += Pos(r, c)
            }
        }
        return out
    }

    private fun prismCells(pos: Pos): List<Pos> {
        val out = mutableListOf<Pos>()
        for (c in 0 until cols) if (board[pos.r, c].playable) out += Pos(pos.r, c)
        for (r in 0 until rows) if (board[r, pos.c].playable) out += Pos(r, pos.c)
        // Braços diagonais: a luz refratando pelo prisma.
        for (d in listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)) {
            for (k in 1..2) {
                val p = Pos(pos.r + d.first * k, pos.c + d.second * k)
                if (board.inBounds(p) && board[p].playable) out += p
            }
        }
        return out.distinct()
    }

    private fun supernovaCells(pos: Pos, color: GemColor?): List<Pos> {
        val out = square(pos, 2).toMutableList()
        if (color != null) {
            out += board.playablePositions().filter { board.gemAt(it)?.color == color }
        }
        return out.distinct()
    }

    /** Combinação de duas peças especiais trocadas entre si. */
    private fun megaBlast(a: Pos, ga: Gem, b: Pos, gb: Gem): List<Pos> {
        val kinds = listOf(ga.kind, gb.kind)
        return when {
            kinds.all { it == GemKind.PRISM } -> band(a) + band(b)
            kinds.all { it == GemKind.SUPERNOVA } ->
                square(a, 3) + board.playablePositions()
                    .filter { board.gemAt(it)?.color == ga.color || board.gemAt(it)?.color == gb.color }
            else -> {
                val prism = if (ga.kind == GemKind.PRISM) a else b
                val nova = if (ga.kind == GemKind.PRISM) gb else ga
                band(prism) + board.playablePositions().filter { board.gemAt(it)?.color == nova.color }
            }
        }.distinct()
    }

    /** Três linhas e três colunas em torno de [pos]. */
    private fun band(pos: Pos): List<Pos> {
        val out = mutableListOf<Pos>()
        for (dr in -1..1) {
            val r = pos.r + dr
            if (r in 0 until rows) for (c in 0 until cols) if (board[r, c].playable) out += Pos(r, c)
        }
        for (dc in -1..1) {
            val c = pos.c + dc
            if (c in 0 until cols) for (r in 0 until rows) if (board[r, c].playable) out += Pos(r, c)
        }
        return out
    }

    // ---------------------------------------------------------------- limpeza

    private class ClearAccumulator {
        val cleared = mutableListOf<ClearedGem>()
        val blasts = mutableListOf<Blast>()
        val ice = mutableListOf<IceBreak>()
        val stones = mutableListOf<Pos>()
        var points = 0
    }

    private fun pointsFor(gem: Gem): Int = when (gem.kind) {
        GemKind.NORMAL -> 60
        GemKind.ESSENCE -> 120
        GemKind.PRISM -> 220
        GemKind.SUPERNOVA -> 260
        GemKind.STONE -> 90
        GemKind.PRISMOID -> 0
    }

    /**
     * Remove as casas indicadas, encadeando detonações de peças especiais que forem
     * atingidas. Prismoides são imunes: precisam descer até a base.
     */
    private fun clearCells(seed: Collection<Pos>, cascade: Int): ClearAccumulator {
        val acc = ClearAccumulator()
        val queue = ArrayDeque(seed.distinct())
        val done = mutableSetOf<Pos>()
        val multiplier = 1.0 + 0.5 * cascade

        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            if (!board.inBounds(pos) || !board[pos].playable) continue
            if (!done.add(pos)) continue

            val gem = board.gemAt(pos)

            if (gem != null && gem.kind == GemKind.STONE) {
                // A pedra absorve o estouro: nada abaixo dela é atingido nesta casa.
                damageStone(pos, acc)
                continue
            }

            // O prismoide é imune — precisa descer até a base — mas o estouro ainda
            // trinca o cristal da casa em que ele está.
            if (gem != null && gem.kind != GemKind.PRISMOID) {
                board.setGem(pos, null)
                acc.cleared += ClearedGem(pos, gem)
                acc.points += (pointsFor(gem) * multiplier).toInt()
                if (gem.kind == GemKind.NORMAL) {
                    progress.add(ObjectiveType.COLLECT, 1, gem.color)
                }
                if (gem.isSpecial) {
                    val cells = blastCells(pos, gem)
                    acc.blasts += Blast(pos, gem.kind, gem.color, cells)
                    cells.forEach { if (it !in done) queue.addLast(it) }
                }
            }

            // Cristal fica sob a peça: cada estouro na casa remove uma camada.
            val current = board[pos]
            if (current.ice > 0) {
                val left = current.ice - 1
                board[pos] = current.copy(ice = left)
                acc.ice += IceBreak(pos, left)
                acc.points += (30 * multiplier).toInt()
                progress.add(ObjectiveType.ICE, 1)
            }

            // Pedras vizinhas trincam com a onda de choque.
            for (d in NEIGHBORS) {
                val n = Pos(pos.r + d.first, pos.c + d.second)
                if (board.inBounds(n) && board.gemAt(n)?.kind == GemKind.STONE && n !in done) {
                    damageStone(n, acc)
                }
            }
        }
        return acc
    }

    private fun damageStone(pos: Pos, acc: ClearAccumulator) {
        val gem = board.gemAt(pos) ?: return
        if (gem.kind != GemKind.STONE) return
        acc.stones += pos
        if (gem.hp <= 1) {
            board.setGem(pos, null)
            acc.cleared += ClearedGem(pos, gem)
            acc.points += pointsFor(gem)
            progress.add(ObjectiveType.STONE, 1)
        } else {
            board.setGem(pos, gem.copy(hp = gem.hp - 1))
        }
    }

    // ----------------------------------------------------------------- fusão

    /** Pares de essências encostadas, prontas para fundir. */
    private fun findFusionPairs(): List<Pair<Pos, Pos>> {
        val used = mutableSetOf<Pos>()
        val pairs = mutableListOf<Pair<Pos, Pos>>()
        for (p in board.playablePositions()) {
            if (p in used) continue
            if (board.gemAt(p)?.kind != GemKind.ESSENCE) continue
            for (d in listOf(0 to 1, 1 to 0)) {
                val n = Pos(p.r + d.first, p.c + d.second)
                if (!board.inBounds(n) || n in used) continue
                if (board.gemAt(n)?.kind != GemKind.ESSENCE) continue
                pairs += p to n
                used += p
                used += n
                break
            }
        }
        return pairs
    }

    private fun applyFusions(pairs: List<Pair<Pos, Pos>>): List<FusionEvent> {
        val events = mutableListOf<FusionEvent>()
        for ((a, b) in pairs) {
            val ga = board.gemAt(a) ?: continue
            val gb = board.gemAt(b) ?: continue
            // A peça nasce onde estava a essência mais recente: é a que o jogador acabou de mover.
            val at = if (ga.id >= gb.id) a else b
            val other = if (at == a) b else a
            val result = if (ga.color == gb.color) {
                Gem(newId(), GemKind.SUPERNOVA, ga.color)
            } else {
                Gem(newId(), GemKind.PRISM, null)
            }
            board.setGem(a, null)
            board.setGem(b, null)
            board.setGem(at, result)
            events += FusionEvent(a, b, at, result)
            fusionCount++
            progress.add(ObjectiveType.FUSION, 1)
            score += 400
        }
        return events
    }

    // ------------------------------------------------------------- gravidade

    private class GravityResult(
        val moves: List<GemMove>,
        val spawns: List<GemSpawn>,
    )

    private fun applyGravity(): GravityResult {
        val before = mutableMapOf<Long, Pos>()
        for (p in board.playablePositions()) board.gemAt(p)?.let { before[it.id] = p }

        val spawnOrigin = mutableMapOf<Long, Pos>()
        val spawnDepth = IntArray(cols)

        var guard = 0
        var changed = true
        while (changed && guard++ < rows * cols * 4) {
            changed = false
            for (r in rows - 1 downTo 0) {
                for (c in 0 until cols) {
                    val cell = board[r, c]
                    if (!cell.playable || cell.gem != null) continue

                    val straight = Pos(r - 1, c)
                    if (board.inBounds(straight) &&
                        board[straight].playable &&
                        board.gemAt(straight)?.fallable == true
                    ) {
                        moveGem(straight, Pos(r, c))
                        changed = true
                        continue
                    }

                    val blockedAbove = !board.inBounds(straight) ||
                        !board[straight].playable ||
                        (board.gemAt(straight)?.fallable == false)

                    if (blockedAbove) {
                        var slid = false
                        for (dc in intArrayOf(-1, 1)) {
                            val src = Pos(r - 1, c + dc)
                            if (board.inBounds(src) &&
                                board[src].playable &&
                                board.gemAt(src)?.fallable == true &&
                                !feedsSomethingBelow(src)
                            ) {
                                moveGem(src, Pos(r, c))
                                slid = true
                                changed = true
                                break
                            }
                        }
                        if (slid) continue
                    }

                    if (cell.spawner) {
                        val gem = spawnGem()
                        board.setGem(Pos(r, c), gem)
                        spawnDepth[c]++
                        spawnOrigin[gem.id] = Pos(-spawnDepth[c], c)
                        changed = true
                    }
                }
            }
        }

        val moves = mutableListOf<GemMove>()
        val spawns = mutableListOf<GemSpawn>()
        for (p in board.playablePositions()) {
            val gem = board.gemAt(p) ?: continue
            val from = before[gem.id]
            if (from == null) {
                spawns += GemSpawn(gem, spawnOrigin[gem.id] ?: Pos(-1, p.c), p)
            } else if (from != p) {
                moves += GemMove(gem.id, from, p)
            }
        }
        return GravityResult(moves, spawns)
    }

    /** Evita que uma peça escorregue na diagonal quando ainda tem buraco reto abaixo dela. */
    private fun feedsSomethingBelow(src: Pos): Boolean {
        val below = Pos(src.r + 1, src.c)
        return board.inBounds(below) && board[below].playable && board[below].gem == null
    }

    private fun moveGem(from: Pos, to: Pos) {
        val gem = board.gemAt(from) ?: return
        board.setGem(from, null)
        board.setGem(to, gem)
    }

    private fun spawnGem(): Gem {
        if (spec.prismoidsRequested > 0) {
            val onBoard = board.playablePositions()
                .count { board.gemAt(it)?.kind == GemKind.PRISMOID }
            // A conta é sobre o que falta *entregar*, não sobre o que já nasceu: senão um
            // prismoide preso numa coluna morta tornaria a fase impossível de vencer.
            val faltam = spec.prismoidsRequested - prismoidsDelivered - onBoard
            // Deixa descer em paralelo: um prismoide leva ~30 jogadas para cruzar o
            // tabuleiro, então dois em série não caberiam no orçamento da fase.
            val simultaneos = spec.prismoidsRequested.coerceIn(1, 3)
            if (faltam > 0 && onBoard < simultaneos && rng.nextInt(100) < 25) {
                return Gem(newId(), GemKind.PRISMOID)
            }
        }
        return newNormal()
    }

    /** Prismoides que chegaram à base saem do tabuleiro e contam para o objetivo. */
    private fun collectPrismoids(): List<Pos> {
        val delivered = mutableListOf<Pos>()
        for (c in 0 until cols) {
            val r = board.bottomPlayableRow(c)
            if (r < 0) continue
            if (board.gemAt(r, c)?.kind == GemKind.PRISMOID) {
                board.setGem(Pos(r, c), null)
                delivered += Pos(r, c)
                prismoidsDelivered++
                progress.add(ObjectiveType.PRISMOID, 1)
                score += 500
            }
        }
        return delivered
    }

    // -------------------------------------------------------------- resolução

    /**
     * Roda a cascata até o tabuleiro estabilizar, devolvendo um quadro por etapa.
     * A ordem dentro de cada volta é proposital: fusão antes de match, para que
     * encostar duas essências responda na hora.
     */
    private fun resolve(initialClear: Collection<Pos>? = null): List<ResolveStep> {
        val steps = mutableListOf<ResolveStep>()
        var cascade = 0
        var pendingClear: Collection<Pos>? = initialClear
        var guard = 0

        while (guard++ < 80) {
            val fusions: List<FusionEvent>
            val acc: ClearAccumulator

            if (pendingClear != null) {
                acc = clearCells(pendingClear, cascade)
                fusions = emptyList()
                pendingClear = null
                if (acc.cleared.isEmpty() && acc.ice.isEmpty() && acc.stones.isEmpty()) {
                    // nada aconteceu: continua para as demais checagens
                }
            } else {
                val pairs = findFusionPairs()
                if (pairs.isNotEmpty()) {
                    fusions = applyFusions(pairs)
                    acc = ClearAccumulator()
                } else {
                    val groups = findMatchGroups()
                    if (groups.isEmpty()) break
                    fusions = emptyList()
                    acc = resolveMatchGroups(groups, cascade)
                }
            }

            score += acc.points
            val created = pendingCreations.toList()
            pendingCreations.clear()

            val gravity = applyGravity()
            val delivered = collectPrismoids()
            val afterDelivery = if (delivered.isNotEmpty()) applyGravity() else null

            val step = ResolveStep(
                cascade = cascade,
                cleared = acc.cleared,
                created = created,
                fusions = fusions,
                blasts = acc.blasts,
                iceBroken = acc.ice,
                stonesHit = acc.stones,
                prismoidsDelivered = delivered,
                moves = gravity.moves + (afterDelivery?.moves ?: emptyList()),
                spawns = gravity.spawns + (afterDelivery?.spawns ?: emptyList()),
                scoreGained = acc.points,
                totalScore = score,
            )
            if (step.isEmpty) break
            steps += step
            cascade++
        }

        progress.setScore(score)
        return steps
    }

    private val pendingCreations = mutableListOf<CreatedGem>()

    /** Aplica os grupos de match, decidindo o que cada formato gera. */
    private fun resolveMatchGroups(groups: List<MatchGroup>, cascade: Int): ClearAccumulator {
        val toClear = mutableSetOf<Pos>()
        val essenceSpots = mutableListOf<Pair<Pos, GemColor>>()
        val shockwaves = mutableListOf<Pos>()

        for (group in groups) {
            val origin = pickOrigin(group)
            when {
                group.isCross -> {
                    essenceSpots += origin to group.color
                    shockwaves += origin
                }
                group.longest >= 5 -> {
                    // Cinco em linha condensa duas essências vizinhas: elas se fundem
                    // na volta seguinte da cascata e viram uma Supernova.
                    val run = group.runs.maxBy { it.cells.size }
                    val partner = run.cells.firstOrNull { it != origin && it.isNeighbor(origin) }
                        ?: run.cells.first { it != origin }
                    essenceSpots += origin to group.color
                    essenceSpots += partner to group.color
                }
                group.longest >= 4 -> essenceSpots += origin to group.color
                // Match de 3 formado durante uma cascata também condensa essência.
                // Sem isso a fusão — o coração do jogo — quase nunca acontecia:
                // medindo, um bot fazia 0,5 fusão por fase. É o que transforma
                // reação em cadeia em recurso estratégico.
                cascade >= 1 -> essenceSpots += origin to group.color
            }
            toClear += group.cells
        }

        val acc = clearCells(toClear, cascade)
        for (p in shockwaves) {
            val extra = clearCells(square(p, 1).filter { it !in toClear }, cascade)
            acc.cleared += extra.cleared
            acc.blasts += extra.blasts
            acc.ice += extra.ice
            acc.stones += extra.stones
            acc.points += extra.points
        }
        for ((pos, color) in essenceSpots.distinctBy { it.first }) {
            if (!board[pos].playable || board.gemAt(pos) != null) continue
            val gem = Gem(newId(), GemKind.ESSENCE, color)
            board.setGem(pos, gem)
            pendingCreations += CreatedGem(pos, gem)
        }
        return acc
    }

    /** A peça especial nasce onde o jogador tocou; sem isso, no centro do grupo. */
    private fun pickOrigin(group: MatchGroup): Pos {
        lastSwap?.let { (a, b) ->
            if (a in group.cells) return a
            if (b in group.cells) return b
        }
        if (group.isCross) {
            val h = group.runs.first { it.horizontal }
            val v = group.runs.first { !it.horizontal }
            h.cells.firstOrNull { it in v.cells }?.let { return it }
        }
        val run = group.runs.maxBy { it.cells.size }
        return run.cells[run.cells.size / 2]
    }

    private var lastSwap: Pair<Pos, Pos>? = null

    // ------------------------------------------------------------- jogadas

    /** True quando a troca é permitida sem formar match (regra das peças especiais). */
    fun isFreeSwap(a: Pos, b: Pos): Boolean {
        val ga = board.gemAt(a) ?: return false
        val gb = board.gemAt(b) ?: return false
        return ga.isSpecial || gb.isSpecial
    }

    fun canSwap(a: Pos, b: Pos): Boolean {
        if (status != LevelStatus.PLAYING || movesLeft <= 0) return false
        if (!board.inBounds(a) || !board.inBounds(b)) return false
        if (!a.isNeighbor(b)) return false
        if (!board[a].playable || !board[b].playable) return false
        val ga = board.gemAt(a) ?: return false
        val gb = board.gemAt(b) ?: return false
        if (!ga.swappable || !gb.swappable) return false
        if (ga.isSpecial || gb.isSpecial) return true
        return swapCreatesMatch(a, b)
    }

    private fun swapCreatesMatch(a: Pos, b: Pos): Boolean {
        swapGems(a, b)
        val has = hasMatches()
        swapGems(a, b)
        return has
    }

    private fun swapGems(a: Pos, b: Pos) {
        val ga = board.gemAt(a)
        val gb = board.gemAt(b)
        board.setGem(a, gb)
        board.setGem(b, ga)
    }

    /**
     * Jogada principal. Trocas entre gemas comuns exigem match; qualquer troca
     * envolvendo peça especial é livre. Prismas e supernovas detonam ao serem
     * trocados — essências apenas se reposicionam.
     */
    fun trySwap(a: Pos, b: Pos): MoveOutcome {
        if (!canSwap(a, b)) return MoveOutcome.Rejected(a, b)

        val ga = board.gemAt(a)!!
        val gb = board.gemAt(b)!!
        val free = ga.isSpecial || gb.isSpecial

        swapGems(a, b)
        movesLeft--
        lastSwap = a to b

        val detonators = listOf(GemKind.PRISM, GemKind.SUPERNOVA)
        val initialClear: Collection<Pos>? = when {
            ga.kind in detonators && gb.kind in detonators -> megaBlast(b, ga, a, gb)
            ga.kind in detonators -> blastCells(b, ga) + b
            gb.kind in detonators -> blastCells(a, gb) + a
            else -> null
        }

        val steps = resolve(initialClear)
        lastSwap = null
        updateStatus()
        return MoveOutcome.Accepted(a, b, steps, free, status)
    }

    private fun updateStatus() {
        status = when {
            progress.allComplete() -> LevelStatus.WON
            movesLeft <= 0 -> LevelStatus.LOST
            else -> LevelStatus.PLAYING
        }
    }

    // -------------------------------------------------------------- boosters

    /** Martelo: remove uma peça qualquer sem gastar jogada. */
    fun useHammer(pos: Pos): List<ResolveStep> {
        if (status != LevelStatus.PLAYING) return emptyList()
        if (!board.inBounds(pos) || !board[pos].playable) return emptyList()
        val steps = resolve(listOf(pos))
        updateStatus()
        return steps
    }

    /** Bomba: estoura uma área 3x3. */
    fun useBomb(pos: Pos): List<ResolveStep> {
        if (status != LevelStatus.PLAYING) return emptyList()
        val steps = resolve(square(pos, 1))
        updateStatus()
        return steps
    }

    /** Raio cromático: remove todas as gemas de uma cor. */
    fun useColorBlast(color: GemColor): List<ResolveStep> {
        if (status != LevelStatus.PLAYING) return emptyList()
        val cells = board.playablePositions()
            .filter { board.gemAt(it)?.kind == GemKind.NORMAL && board.gemAt(it)?.color == color }
            .toList()
        val steps = resolve(cells)
        updateStatus()
        return steps
    }

    /** Jogadas extras — usado pelo prêmio de vídeo recompensado. */
    fun grantMoves(extra: Int) {
        movesLeft += extra
        if (status == LevelStatus.LOST && movesLeft > 0) status = LevelStatus.PLAYING
    }

    // --------------------------------------------------------------- embaralhar

    fun hasPossibleMove(): Boolean {
        for (p in board.playablePositions()) {
            val gem = board.gemAt(p) ?: continue
            if (!gem.swappable) continue
            if (gem.isSpecial) {
                for (d in NEIGHBORS) {
                    val n = Pos(p.r + d.first, p.c + d.second)
                    if (board.inBounds(n) && board[n].playable && board.gemAt(n)?.swappable == true) {
                        return true
                    }
                }
                continue
            }
            for (d in listOf(0 to 1, 1 to 0)) {
                val n = Pos(p.r + d.first, p.c + d.second)
                if (!board.inBounds(n) || !board[n].playable) continue
                if (board.gemAt(n)?.swappable != true) continue
                if (swapCreatesMatch(p, n)) return true
            }
        }
        return false
    }

    /** Redistribui as cores até existir jogada possível e nenhum match pronto. */
    fun shuffle(): Boolean {
        val spots = board.playablePositions()
            .filter { board.gemAt(it)?.kind == GemKind.NORMAL }
            .toList()
        if (spots.size < 3) return false
        repeat(60) {
            val colors = spots.map { board.gemAt(it)!!.color!! }.shuffled(rng)
            spots.forEachIndexed { i, p ->
                board.setGem(p, board.gemAt(p)!!.copy(color = colors[i]))
            }
            if (!hasMatches() && hasPossibleMove()) return true
        }
        return hasPossibleMove()
    }

    private fun ensurePlayable() {
        var guard = 0
        while ((hasMatches() || !hasPossibleMove()) && guard++ < 40) {
            if (!shuffle()) break
        }
    }

    /** Distância em casas — usada pelos efeitos visuais para escalonar atrasos. */
    fun distance(a: Pos, b: Pos): Int = max(abs(a.r - b.r), abs(a.c - b.c))

    companion object {
        private val NEIGHBORS = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)

        /** Menor dos dois — atalho legível usado pelos cálculos de área. */
        fun clampIndex(value: Int, limit: Int): Int = min(max(value, 0), limit - 1)
    }
}
