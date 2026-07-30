package com.neuroflip.game.domain

import kotlin.math.max
import kotlin.random.Random

/**
 * Motor de regras do NeuroFlip.
 *
 * Sem dependências de Android: o tempo entra por [tick], então o motor é 100% testável.
 * A UI só lê [state] e reage aos [GameEvent] devolvidos por cada chamada.
 *
 * Mecânicas exclusivas:
 *  - ECO: ao acertar um par, um pulso revela por instantes as cartas vizinhas.
 *  - MUTAÇÃO: de tempos em tempos duas cartas trocam de lugar (dá para rastrear).
 *  - SINAPSE / SOBRECARGA: acertos em sequência carregam uma barra; cheia, revela tudo.
 *  - Cartas especiais (Crio, Dourada, Espelho, Fantasma, Instável).
 */
class GameEngine(
    config: LevelConfig,
    private val random: Random = Random.Default
) {

    companion object {
        const val ECO_REVEAL_MS = 900L
        const val OVERLOAD_REVEAL_MS = 1_500L
        const val OVERLOAD_DURATION_MS = 10_000L
        const val SCAN_REVEAL_MS = 2_000L
        const val FREEZE_MS = 6_000L
        const val CRYO_TIME_BONUS_MS = 8_000L
        const val MISMATCH_HOLD_MS = 850L
        const val BASE_MATCH_POINTS = 100
        const val ECO_ASSIST_BONUS = 60
        const val UNSTABLE_FUSE_MOVES = 3
    }

    var state: GameState = GameState(config = config, timeLeftMs = config.timeLimitMs)
        private set

    private var movesSinceMutation = 0
    private var mismatchAtMs = 0L
    /** índices revelados pelo último pulso ECO — dão bônus se você usar a informação. */
    private var ecoAssist = mutableMapOf<Int, Int>() // index -> move em que foi revelado
    /** pares instáveis já vistos: pairId -> move em que foi visto. */
    private val unstableFuse = mutableMapOf<Int, Int>()

    init {
        state = state.copy(cards = buildBoard(config), status = GameStatus.READY)
    }

    // ------------------------------------------------------------------ tabuleiro

    private fun buildBoard(config: LevelConfig): List<Card> {
        val pairs = config.pairCount
        val symbols = Symbol.entries.shuffled(random).take(pairs).let { picked ->
            if (picked.size >= pairs) picked
            else picked + List(pairs - picked.size) { Symbol.entries[it % Symbol.entries.size] }
        }

        // Distribui cartas especiais: ~1 par especial a cada 5 pares.
        val specialSlots = if (config.specials.isEmpty()) 0 else max(1, pairs / 5)
        val specialPairIndices = (0 until pairs).shuffled(random).take(specialSlots).toSet()
        val specialPool = config.specials.toList()

        val cards = ArrayList<Card>(config.cellCount)
        var id = 0
        symbols.forEachIndexed { pairId, symbol ->
            val kind = if (pairId in specialPairIndices && specialPool.isNotEmpty()) {
                specialPool[random.nextInt(specialPool.size)]
            } else {
                CardKind.NORMAL
            }
            repeat(2) {
                cards += Card(id = id++, pairId = pairId, symbol = symbol, kind = kind)
            }
        }
        return cards.shuffled(random)
    }

    // ------------------------------------------------------------------ ciclo de vida

    fun start(): List<GameEvent> {
        state = state.copy(status = GameStatus.RUNNING)
        return emptyList()
    }

    fun pause() {
        if (state.status == GameStatus.RUNNING) state = state.copy(status = GameStatus.PAUSED)
    }

    fun resume() {
        if (state.status == GameStatus.PAUSED) state = state.copy(status = GameStatus.RUNNING)
    }

    /** Avança o relógio interno. Chamado pelo ViewModel a cada frame lógico. */
    fun tick(deltaMs: Long): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        val now = state.nowMs + deltaMs
        var newState = state.copy(nowMs = now)

        if (state.status == GameStatus.RUNNING) {
            newState = newState.copy(elapsedMs = newState.elapsedMs + deltaMs)

            if (newState.config.timeLimitMs > 0 && !newState.frozen) {
                val left = (newState.timeLeftMs - deltaMs).coerceAtLeast(0L)
                newState = newState.copy(timeLeftMs = left)
                if (left == 0L) {
                    newState = newState.copy(status = GameStatus.LOST)
                    events += GameEvent.Lost
                }
            }
        }

        state = newState

        // Desvira automaticamente o par errado depois do tempo de leitura.
        if (state.selection.size == 2 && mismatchAtMs > 0 && state.nowMs >= mismatchAtMs) {
            events += resolveMismatch()
        }
        return events
    }

    // ------------------------------------------------------------------ jogada

    fun flip(index: Int): List<GameEvent> {
        if (state.status != GameStatus.RUNNING) return emptyList()
        val events = mutableListOf<GameEvent>()

        // Toque rápido: resolve o par errado antes de aceitar a nova carta.
        if (state.selection.size == 2) {
            events += resolveMismatch()
        }

        val card = state.cards.getOrNull(index) ?: return events
        if (card.matched || card.faceUp) return events

        val cards = state.cards.toMutableList()
        cards[index] = card.copy(faceUp = true)
        val selection = state.selection + index
        state = state.copy(cards = cards, selection = selection)
        events += GameEvent.Flipped(index)

        if (card.kind == CardKind.UNSTABLE) {
            unstableFuse.putIfAbsent(card.pairId, state.moves)
        }

        if (selection.size == 2) {
            events += evaluatePair(selection[0], selection[1])
        }
        return events
    }

    private fun evaluatePair(a: Int, b: Int): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        val cardA = state.cards[a]
        val cardB = state.cards[b]
        state = state.copy(moves = state.moves + 1)
        movesSinceMutation++

        if (cardA.pairId == cardB.pairId) {
            events += applyMatch(a, b, cardA.kind)
        } else {
            mismatchAtMs = state.nowMs + MISMATCH_HOLD_MS
            state = state.copy(
                combo = 0,
                synapse = (state.synapse - 0.15f).coerceAtLeast(0f)
            )
            events += GameEvent.Mismatched(a, b)
            events += checkUnstableFuse(listOf(cardA, cardB))
        }

        events += maybeMutate()
        events += checkWin()
        return events
    }

    private fun applyMatch(a: Int, b: Int, kind: CardKind): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        val cards = state.cards.toMutableList()
        cards[a] = cards[a].copy(matched = true, faceUp = true, previewUntilMs = 0L)
        cards[b] = cards[b].copy(matched = true, faceUp = true, previewUntilMs = 0L)

        val combo = state.combo + 1
        val multiplier = (1f + combo * 0.25f).coerceAtMost(4f)

        // Bônus ECO: você usou uma dica do pulso anterior nesta jogada.
        val usedEco = (ecoAssist.containsKey(a) || ecoAssist.containsKey(b))
        val kindMultiplier = if (kind == CardKind.GOLDEN) 3 else 1
        val timeBonus = if (state.config.timeLimitMs > 0) (state.timeLeftMs / 1000L).toInt() else 0

        var points = ((BASE_MATCH_POINTS + timeBonus) * multiplier).toInt() * kindMultiplier
        if (usedEco) points += ECO_ASSIST_BONUS
        if (state.overloadActive) points *= 2

        val synapse = (state.synapse + 0.2f).coerceAtMost(1f)
        val wasReady = state.overloadReady

        state = state.copy(
            cards = cards,
            selection = emptyList(),
            matches = state.matches + 1,
            score = state.score + points,
            combo = combo,
            bestCombo = max(state.bestCombo, combo),
            synapse = synapse
        )
        mismatchAtMs = 0L
        ecoAssist.clear()
        unstableFuse.remove(cards[a].pairId)

        events += GameEvent.Matched(a, b, kind, points)
        if (combo >= 2) events += GameEvent.Combo(combo)
        if (!wasReady && state.overloadReady) events += GameEvent.OverloadReady

        events += applyKindEffect(kind, a, b)
        if (state.config.ecoEnabled) events += firePulse(listOf(a, b), radius = 1)
        return events
    }

    private fun applyKindEffect(kind: CardKind, a: Int, b: Int): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        when (kind) {
            CardKind.CRYO -> {
                state = state.copy(
                    frozenUntilMs = state.nowMs + FREEZE_MS,
                    timeLeftMs = if (state.config.timeLimitMs > 0) {
                        state.timeLeftMs + CRYO_TIME_BONUS_MS
                    } else {
                        state.timeLeftMs
                    }
                )
                events += GameEvent.Frozen
                if (state.config.timeLimitMs > 0) events += GameEvent.TimeGained(CRYO_TIME_BONUS_MS)
            }

            CardKind.MIRROR -> {
                // Marca permanentemente um par ainda escondido.
                val hidden = state.cards
                    .filter { !it.matched && !it.tagged }
                    .groupBy { it.pairId }
                    .keys
                    .toList()
                if (hidden.isNotEmpty()) {
                    val target = hidden[random.nextInt(hidden.size)]
                    val cards = state.cards.map {
                        if (it.pairId == target) it.copy(tagged = true) else it
                    }
                    state = state.copy(cards = cards)
                    events += GameEvent.Tagged(
                        cards.withIndex().filter { it.value.pairId == target }.map { it.index }
                    )
                }
            }

            else -> Unit
        }
        return events
    }

    /** Pulso ECO: revela por um instante os vizinhos ortogonais das cartas resolvidas. */
    private fun firePulse(origins: List<Int>, radius: Int): List<GameEvent> {
        val columns = state.config.columns
        val revealed = mutableSetOf<Int>()
        origins.forEach { origin ->
            val ox = origin % columns
            val oy = origin / columns
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    if (dx == 0 && dy == 0) continue
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > radius) continue
                    val x = ox + dx
                    val y = oy + dy
                    if (x < 0 || x >= columns || y < 0) continue
                    val idx = y * columns + x
                    val card = state.cards.getOrNull(idx) ?: continue
                    if (!card.matched && !card.faceUp) revealed += idx
                }
            }
        }
        if (revealed.isEmpty()) return emptyList()

        val until = state.nowMs + ECO_REVEAL_MS
        val cards = state.cards.mapIndexed { i, c ->
            if (i in revealed) c.copy(previewUntilMs = until) else c
        }
        state = state.copy(cards = cards)
        revealed.forEach { ecoAssist[it] = state.moves }
        return listOf(GameEvent.EcoPulse(origins, revealed.toList()))
    }

    private fun resolveMismatch(): List<GameEvent> {
        if (state.selection.size < 2) return emptyList()
        val cards = state.cards.toMutableList()
        var hadPhantom = false
        state.selection.forEach { i ->
            if (!cards[i].matched) {
                cards[i] = cards[i].copy(faceUp = false)
                if (cards[i].kind == CardKind.PHANTOM) hadPhantom = true
            }
        }
        state = state.copy(cards = cards, selection = emptyList())
        mismatchAtMs = 0L
        // A carta Fantasma só foge depois de ser escondida de novo.
        return if (hadPhantom) forceMutation() else emptyList()
    }

    /** MUTAÇÃO: duas cartas escondidas trocam de lugar, com animação rastreável. */
    private fun maybeMutate(): List<GameEvent> {
        val every = state.config.mutationEveryMoves
        if (every <= 0 || movesSinceMutation < every) return emptyList()
        movesSinceMutation = 0
        return forceMutation()
    }

    fun forceMutation(): List<GameEvent> {
        val candidates = state.cards.withIndex()
            .filter { !it.value.matched && it.index !in state.selection }
            .map { it.index }
        if (candidates.size < 2) return emptyList()

        val from = candidates[random.nextInt(candidates.size)]
        var to = from
        while (to == from) to = candidates[random.nextInt(candidates.size)]

        val cards = state.cards.toMutableList()
        val tmp = cards[from]
        cards[from] = cards[to]
        cards[to] = tmp
        state = state.copy(cards = cards)
        ecoAssist.clear()
        return listOf(GameEvent.Mutation(from, to))
    }

    /** Par instável esquecido por muitas jogadas sacode o tabuleiro. */
    private fun checkUnstableFuse(seen: List<Card>): List<GameEvent> {
        seen.filter { it.kind == CardKind.UNSTABLE }
            .forEach { unstableFuse.putIfAbsent(it.pairId, state.moves) }

        val expired = unstableFuse.filter { (_, move) ->
            state.moves - move >= UNSTABLE_FUSE_MOVES
        }.keys
        if (expired.isEmpty()) return emptyList()
        expired.forEach { unstableFuse.remove(it) }
        return forceMutation() + forceMutation()
    }

    // ------------------------------------------------------------------ poderes

    fun activateOverload(): List<GameEvent> {
        if (!state.overloadReady || state.status != GameStatus.RUNNING) return emptyList()
        val until = state.nowMs + OVERLOAD_REVEAL_MS
        val cards = state.cards.map {
            if (!it.matched) it.copy(previewUntilMs = until) else it
        }
        state = state.copy(
            cards = cards,
            synapse = 0f,
            overloadUntilMs = state.nowMs + OVERLOAD_DURATION_MS
        )
        return listOf(GameEvent.OverloadStarted)
    }

    /** Power-up SCAN: revela o tabuleiro por 2s. */
    fun useScan(): Boolean {
        if (state.status != GameStatus.RUNNING) return false
        val until = state.nowMs + SCAN_REVEAL_MS
        val cards = state.cards.map { if (!it.matched) it.copy(previewUntilMs = until) else it }
        state = state.copy(cards = cards)
        return true
    }

    /** Power-up CURINGA: resolve automaticamente um par que ainda falta. */
    fun useWildcard(): List<GameEvent> {
        if (state.status != GameStatus.RUNNING) return emptyList()
        val remaining = state.cards.withIndex().filter { !it.value.matched }
        val pairId = remaining.firstOrNull()?.value?.pairId ?: return emptyList()
        val indices = remaining.filter { it.value.pairId == pairId }.map { it.index }
        if (indices.size < 2) return emptyList()
        resolveMismatch()
        return applyMatch(indices[0], indices[1], state.cards[indices[0]].kind) + checkWin()
    }

    /** Tempo extra — comprado com neurônios ou ganho vendo um vídeo premiado. */
    fun addTime(ms: Long): List<GameEvent> {
        if (state.config.timeLimitMs <= 0) return emptyList()
        state = state.copy(timeLeftMs = state.timeLeftMs + ms)
        if (state.status == GameStatus.LOST) state = state.copy(status = GameStatus.RUNNING)
        return listOf(GameEvent.TimeGained(ms))
    }

    // ------------------------------------------------------------------ fim de partida

    private fun checkWin(): List<GameEvent> {
        if (state.matches < state.config.pairCount) return emptyList()
        val stars = computeStars()
        val timeBonus = if (state.config.timeLimitMs > 0) {
            (state.timeLeftMs / 100L).toInt()
        } else {
            0
        }
        val comboBonus = state.bestCombo * 50
        state = state.copy(
            status = GameStatus.WON,
            score = state.score + timeBonus + comboBonus,
            stars = stars
        )
        return listOf(GameEvent.Won(state.score, stars))
    }

    private fun computeStars(): Int {
        val par = state.config.parMoves
        if (par <= 0) return 3
        return when {
            state.moves <= par -> 3
            state.moves <= (par * 1.35f).toInt() -> 2
            else -> 1
        }
    }

    /** Recompensa em neurônios da partida. */
    fun neuronReward(): Int {
        if (state.status != GameStatus.WON) return 0
        return 20 + state.stars * 15 + state.bestCombo * 3
    }
}
