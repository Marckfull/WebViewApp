package com.kardiapulse.game.core.engine

import com.kardiapulse.game.core.model.Difficulty
import com.kardiapulse.game.core.model.GameState
import com.kardiapulse.game.core.model.Move
import com.kardiapulse.game.core.model.Phase
import com.kardiapulse.game.core.model.PowerType
import com.kardiapulse.game.core.model.Side
import kotlin.math.abs

/**
 * A IA de Kardia Pulse.
 *
 * O problema é que a mão do adversário é oculta, então uma busca comum não se aplica. A
 * abordagem usada aqui é **PIMC (Perfect Information Monte Carlo)**: a IA sorteia várias mãos
 * plausíveis para o oponente a partir das cartas que ela ainda não viu, resolve cada mundo
 * desses com busca alfa-beta, e escolhe a jogada que se sai melhor na média dos mundos.
 *
 * Isso produz um adversário que realmente entende a mecânica — ele guarda ressonâncias para
 * inverter na hora certa e empurra o Núcleo para encurralar você — sem nunca trapacear
 * olhando a sua mão.
 */
object AiPlayer {

    private const val WIN_SCORE = 1_000_000
    private const val ROUND_SCORE = 5_000

    /**
     * Escolhe a jogada da IA para quem está com o turno.
     *
     * [difficulty] define o quanto ela enxerga; [persona] define o que ela quer.
     */
    fun chooseMove(
        state: GameState,
        difficulty: Difficulty,
        rng: PulseRandom,
        persona: AiPersona = AiPersona.EQUILIBRADO
    ): Move? {
        if (state.phase != Phase.JOGANDO) return null
        val me = state.turn

        val plays = GameEngine.legalPlays(state)
        if (plays.isEmpty()) {
            // Encurralada: gasta um poder de resgate, na ordem que mais devolve controle.
            val rescue = GameEngine.rescuePowers(state, me)
            val chosen = rescue.firstOrNull { it == PowerType.DESCARGA }
                ?: rescue.firstOrNull { it == PowerType.INVERSOR }
                ?: rescue.firstOrNull()
            return chosen?.let { Move.UsePower(it) }
        }

        // Escudo defensivo: se o estrago previsto for alto e a vida estiver curta, protege.
        val me2 = state.player(me)
        if (!me2.shielded && me2.powerCount(PowerType.ESCUDO) > 0) {
            val projected = abs(state.nucleus).coerceAtLeast(GameEngine.MIN_DAMAGE)
            if (me2.hp <= projected + 4 && state.headroom <= 6) {
                return Move.UsePower(PowerType.ESCUDO)
            }
        }

        if (plays.size == 1) return plays[0]

        // Erro proposital nas dificuldades baixas, para o jogo não parecer impiedoso.
        if (difficulty.blunderChance > 0 && rng.nextInt(100) < difficulty.blunderChance) {
            return plays[rng.nextInt(plays.size)]
        }

        val totals = IntArray(plays.size)
        repeat(difficulty.samples) {
            val world = determinize(state, rng)
            for (i in plays.indices) {
                val next = stripped(GameEngine.apply(world, plays[i]))
                totals[i] += search(
                    next, difficulty.depth - 1, -WIN_SCORE * 2, WIN_SCORE * 2, me, persona
                )
            }
        }

        // O ruído da persona entra DEPOIS da busca, nunca dentro dela: assim a árvore continua
        // consistente e só a escolha final fica imprevisível.
        if (persona.noise > 0) {
            for (i in totals.indices) {
                val jitter = rng.nextInt(persona.noise * 2 + 1) - persona.noise
                totals[i] += jitter * difficulty.samples
            }
        }

        var bestIndex = 0
        for (i in plays.indices) if (totals[i] > totals[bestIndex]) bestIndex = i
        return plays[bestIndex]
    }

    // ------------------------------------------------------------------ busca

    private fun search(
        state: GameState,
        depth: Int,
        alphaIn: Int,
        betaIn: Int,
        root: Side,
        persona: AiPersona
    ): Int {
        if (state.phase == Phase.FIM_DE_DUELO) {
            return if (state.winner == root) WIN_SCORE else -WIN_SCORE
        }
        if (state.phase == Phase.FIM_DE_RODADA) {
            val sign = if (state.lastRoundLoser == root) -1 else 1
            return sign * ROUND_SCORE + evaluate(state, root, persona)
        }
        if (depth <= 0) return evaluate(state, root, persona)

        val moves = GameEngine.legalPlays(state)
        if (moves.isEmpty()) return evaluate(state, root, persona)

        val maximizing = state.turn == root
        var alpha = alphaIn
        var beta = betaIn
        var best = if (maximizing) Int.MIN_VALUE else Int.MAX_VALUE

        for (move in moves) {
            val child = stripped(GameEngine.apply(state, move))
            val score = search(child, depth - 1, alpha, beta, root, persona)
            if (maximizing) {
                if (score > best) best = score
                if (best > alpha) alpha = best
            } else {
                if (score < best) best = score
                if (best < beta) beta = best
            }
            if (beta <= alpha) break
        }
        return best
    }

    /**
     * Avaliação posicional. As três coisas que importam em Kardia:
     * vida, quem está sem folga, e quem tem ressonância guardada para inverter.
     */
    private fun evaluate(state: GameState, root: Side, persona: AiPersona): Int {
        val other = root.other
        val mine = state.player(root)
        val theirs = state.player(other)

        var score = (mine.hp - theirs.hp) * persona.hpWeight

        // Aperto = quanto de campo já se perdeu, contando o Colapso. Ruim para quem joga agora.
        val squeeze = (state.config.limit - state.headroom).coerceAtLeast(0)
        score += if (state.turn == root) -squeeze * persona.squeezeWeight
        else squeeze * persona.squeezeWeight

        val options = GameEngine.legalPlays(state).size
        score += if (state.turn == root) options * persona.optionsWeight
        else -options * persona.optionsWeight

        score += resonanceCount(state, root) * persona.resonanceWeight
        score -= resonanceCount(state, other) * persona.denyWeight

        // Cartas pequenas dão sobrevida quando a folga aperta; cartas grandes pressionam cedo.
        score += flexibility(state, root) * persona.flexWeight
        score -= flexibility(state, other) * persona.flexWeight

        score += mine.powers.values.sum() * persona.powerWeight
        score -= theirs.powers.values.sum() * persona.powerWeight
        if (mine.shielded) score += 200
        if (theirs.shielded) score -= 200

        return score
    }

    /** Quantas cartas da mão poderiam inverter a direção agora. */
    private fun resonanceCount(state: GameState, side: Side): Int {
        val last = state.lastElement ?: return 0
        return state.player(side).hand.count { it.resonatesWith(last) }
    }

    /** Quantas cartas ainda cabem na folga atual. Mede o quanto o jogador consegue respirar. */
    private fun flexibility(state: GameState, side: Side): Int {
        val room = state.limitNow - abs(state.nucleus)
        return state.player(side).hand.count { it.value <= room }
    }

    // ------------------------------------------------------------ determinização

    /**
     * Sorteia um mundo possível: a mão do oponente é reconstruída a partir das cartas que a IA
     * ainda não viu (tudo que não está na própria mão nem no descarte).
     */
    private fun determinize(state: GameState, rng: PulseRandom): GameState {
        val me = state.turn
        val opponent = me.other
        val seen = HashSet<Int>()
        state.player(me).hand.forEach { seen.add(it.id) }
        state.discard.forEach { seen.add(it.id) }

        val unseen = GameEngine.buildDeck().filterNot { seen.contains(it.id) }.shuffledWith(rng)
        val handSize = state.player(opponent).hand.size.coerceAtMost(unseen.size)

        return stripped(
            state
                .withPlayer(opponent, state.player(opponent).copy(hand = unseen.take(handSize)))
                .copy(deck = unseen.drop(handSize), rngState = rng.nextLong())
        )
    }

    /** Remove log e eventos dos nós de busca: nada disso importa e só custa alocação. */
    private fun stripped(state: GameState): GameState =
        if (state.events.isEmpty() && state.log.isEmpty()) state
        else state.copy(events = emptyList(), log = emptyList())
}
