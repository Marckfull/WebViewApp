package com.kardiapulse.game.core.engine

import com.kardiapulse.game.core.model.Card
import com.kardiapulse.game.core.model.Element
import com.kardiapulse.game.core.model.GameConfig
import com.kardiapulse.game.core.model.GameEvent
import com.kardiapulse.game.core.model.GameState
import com.kardiapulse.game.core.model.LogEntry
import com.kardiapulse.game.core.model.Move
import com.kardiapulse.game.core.model.Phase
import com.kardiapulse.game.core.model.PlayerState
import com.kardiapulse.game.core.model.PowerType
import com.kardiapulse.game.core.model.Side
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * O motor de regras de Kardia Pulse.
 *
 * ## A mecânica
 * Existe um único Núcleo compartilhado com um valor entre -limite e +limite. Cada carta tem
 * uma intensidade (1..9) e um elemento, mas **não tem sinal**: quem joga decide a polaridade.
 *
 * A regra que muda tudo: **a direção trava**. A primeira carta da rodada define para que lado
 * o Núcleo caminha, e todas as jogadas seguintes precisam seguir essa direção. A única forma
 * de inverter é jogar uma carta que **ressoa** com a última carta jogada (mesmo elemento, ou Éter).
 *
 * Ninguém pode jogar uma carta que estoure o limite. Então quem chega no turno sem nenhuma
 * jogada legal **sobrecarrega** e leva dano proporcional a quão longe o Núcleo foi.
 *
 * O resultado: o jogo não é sobre ter cartas altas, é sobre controlar quem fica sem saída.
 * Você quer empurrar o Núcleo para a beirada mantendo uma ressonância guardada na mão.
 */
object GameEngine {

    const val MIN_DAMAGE = 12
    const val MAX_DAMAGE = 34
    const val CHAIN_REWARD_EVERY = 5
    const val MAX_CARDS_PER_ROUND = 60

    /** 40 cartas: 1..9 em quatro elementos, mais quatro Éteres baixos e preciosos. */
    fun buildDeck(): List<Card> {
        val cards = ArrayList<Card>(40)
        var id = 0
        for (element in Element.COMMON) {
            for (value in 1..9) cards.add(Card(id++, value, element))
        }
        for (value in 1..4) cards.add(Card(id++, value, Element.ETER))
        return cards
    }

    fun newMatch(
        config: GameConfig,
        seed: Long,
        yourName: String = "Você",
        foeName: String = "Rival",
        yourPowers: Map<PowerType, Int> = emptyMap(),
        foePowers: Map<PowerType, Int> = emptyMap(),
        yourHp: Int = config.startHp,
        foeHp: Int = config.startHp
    ): GameState {
        val rng = PulseRandom(seed)
        val shuffled = buildDeck().shuffledWith(rng)
        val hs = config.handSize

        val state = GameState(
            config = config,
            you = PlayerState(
                name = yourName,
                hp = yourHp,
                hand = shuffled.take(hs),
                powers = yourPowers
            ),
            foe = PlayerState(
                name = foeName,
                hp = foeHp,
                hand = shuffled.drop(hs).take(hs),
                powers = foePowers
            ),
            deck = shuffled.drop(hs * 2),
            turn = Side.VOCE,
            rngState = rng.state,
            log = listOf(LogEntry("O Núcleo desperta em 0. A direção está livre.", null))
        )
        return startTurn(state)
    }

    // ---------------------------------------------------------------- legalidade

    fun isLegal(state: GameState, card: Card, sign: Int): Boolean {
        if (state.phase != Phase.JOGANDO) return false
        if (sign != 1 && sign != -1) return false
        val after = state.nucleus + sign * card.value
        if (abs(after) > state.limitAfterNextCard) return false
        if (state.direction == 0) return true
        if (sign == state.direction) return true
        if (state.directionUnlockedFor == state.turn) return true
        return card.resonatesWith(state.lastElement)
    }

    /** Todas as jogadas legais para quem está com o turno. */
    fun legalPlays(state: GameState): List<Move.Play> {
        if (state.phase != Phase.JOGANDO) return emptyList()
        val hand = state.player(state.turn).hand
        val out = ArrayList<Move.Play>(hand.size * 2)
        for (card in hand) {
            if (isLegal(state, card, 1)) out.add(Move.Play(card.id, 1))
            if (isLegal(state, card, -1)) out.add(Move.Play(card.id, -1))
        }
        return out
    }

    /** Poderes que ainda podem salvar o jogador de uma sobrecarga iminente. */
    fun rescuePowers(state: GameState, side: Side): List<PowerType> {
        val p = state.player(side)
        val out = ArrayList<PowerType>(3)
        if (p.powerCount(PowerType.DESCARGA) > 0) out.add(PowerType.DESCARGA)
        if (p.powerCount(PowerType.INVERSOR) > 0 && state.direction != 0) out.add(PowerType.INVERSOR)
        if (p.powerCount(PowerType.ESCUDO) > 0 && !p.shielded) out.add(PowerType.ESCUDO)
        return out
    }

    // ---------------------------------------------------------------- aplicação

    fun apply(state: GameState, move: Move): GameState {
        if (state.phase != Phase.JOGANDO) return state
        return when (move) {
            is Move.Play -> applyPlay(state, move)
            is Move.UsePower -> applyPower(state, move)
            Move.Concede -> endRound(state.copy(events = emptyList()), state.turn, forceFatal = true)
        }
    }

    private fun applyPlay(state: GameState, move: Move.Play): GameState {
        val side = state.turn
        val actor = state.player(side)
        val card = actor.hand.firstOrNull { it.id == move.cardId } ?: return state
        if (!isLegal(state, card, move.sign)) return state

        val rng = PulseRandom(state.rngState)
        val before = state.nucleus
        val after = before + move.sign * card.value
        val resonated = card.resonatesWith(state.lastElement)
        val flipped = state.direction != 0 && move.sign != state.direction
        val newChain = if (resonated) state.chain + 1 else 1

        // A mão perde a carta e repõe até o tamanho cheio.
        val handAfterPlay = actor.hand.filterNot { it.id == card.id }
        val refill = drawUpTo(handAfterPlay, state.deck, state.discard + card, rng, state.config.handSize)

        var updatedActor = actor.copy(hand = refill.hand)

        // Correntes longas são recompensadas com uma carga de poder aleatória.
        var chainReward: PowerType? = null
        if (newChain >= CHAIN_REWARD_EVERY && newChain % CHAIN_REWARD_EVERY == 0) {
            val gained = PowerType.ALL[rng.nextInt(PowerType.ALL.size)]
            chainReward = gained
            val powers = updatedActor.powers.toMutableMap()
            powers[gained] = (powers[gained] ?: 0) + 1
            updatedActor = updatedActor.copy(powers = powers)
        }

        val nextLastElement =
            if (state.config.hasStorm) rng.pick(Element.entries.toList()) else card.element

        var s = state
            .withPlayer(side, updatedActor)
            .copy(
                nucleus = after,
                direction = move.sign,
                lastElement = nextLastElement,
                chain = newChain,
                maxChainThisRound = max(state.maxChainThisRound, newChain),
                deck = refill.deck,
                discard = refill.discard,
                directionUnlockedFor =
                    if (state.directionUnlockedFor == side) null else state.directionUnlockedFor,
                cardsThisRound = state.cardsThisRound + 1,
                statsCardsPlayed = state.statsCardsPlayed + 1,
                statsFlips = state.statsFlips + if (flipped) 1 else 0,
                statsBestChain = max(state.statsBestChain, newChain),
                rngState = rng.state,
                events = state.events + GameEvent.CardPlayed(
                    by = side,
                    card = card,
                    sign = move.sign,
                    nucleusBefore = before,
                    nucleusAfter = after,
                    flipped = flipped,
                    chain = newChain
                ),
                log = state.log + LogEntry(describePlay(actor.name, card, move.sign, after, flipped, newChain), side)
            )

        if (chainReward != null) {
            s = s.copy(
                log = s.log + LogEntry("Corrente de $newChain! ${actor.name} recebe ${chainReward.ptName}.", side)
            )
        }

        // O Colapso aperta o campo: avisa quando o limite realmente encolhe.
        if (s.limitNow < state.limitNow) {
            s = s.copy(log = s.log + LogEntry("Colapso: o Núcleo agora suporta apenas ±${s.limitNow}.", null))
        }

        // O Eco concede uma jogada extra: o turno não passa.
        val keepsTurn = s.extraTurnFor == side
        s = s.copy(
            extraTurnFor = if (keepsTurn) null else s.extraTurnFor,
            turn = if (keepsTurn) side else side.other,
            turnCounter = s.turnCounter + 1
        )

        return startTurn(s)
    }

    private fun applyPower(state: GameState, move: Move.UsePower): GameState {
        val side = state.turn
        val actor = state.player(side)
        if (actor.powerCount(move.power) <= 0) return state

        var s = state.withPlayer(side, actor.withPowerConsumed(move.power))
        s = when (move.power) {
            PowerType.INVERSOR -> s.copy(directionUnlockedFor = side)
            PowerType.DESCARGA -> s.copy(nucleus = 0)
            PowerType.VISAO -> s.copy(revealFoeUntilTurn = s.turnCounter + 2)
            PowerType.ECO -> s.copy(extraTurnFor = side)
            PowerType.ESCUDO -> s.withPlayer(side, s.player(side).copy(shielded = true))
        }
        s = s.copy(
            events = s.events + GameEvent.PowerUsed(side, move.power),
            log = s.log + LogEntry("${actor.name} ativa ${move.power.ptName}.", side)
        )

        // Usar um poder não passa o turno, mas pode ter sido a última cartada de quem estava preso.
        return checkStuck(s)
    }

    /**
     * Início do turno de quem acabou de receber a vez: aplica Gravidade e verifica sobrecarga.
     */
    private fun startTurn(state: GameState): GameState {
        if (state.phase != Phase.JOGANDO) return state
        var s = state
        if (s.config.hasGravity && s.direction != 0) {
            val drifted = s.nucleus + s.direction
            s = s.copy(
                nucleus = drifted,
                events = s.events + GameEvent.Gravity(s.turn, drifted),
                log = s.log + LogEntry("Gravidade empurra o Núcleo para $drifted.", null)
            )
            if (abs(drifted) > s.limitNow) return endRound(s, s.turn)
        }
        return checkStuck(s)
    }

    /** Se quem está com o turno não tem jogada legal nem poder de resgate, sobrecarrega. */
    private fun checkStuck(state: GameState): GameState {
        if (state.phase != Phase.JOGANDO) return state
        // Trava de segurança: o Colapso praticamente sempre resolve a rodada muito antes disso,
        // mas nenhuma rodada pode se arrastar para sempre.
        if (state.cardsThisRound >= MAX_CARDS_PER_ROUND) return endRound(state, state.turn)
        if (legalPlays(state).isNotEmpty()) return state
        if (rescuePowers(state, state.turn).isNotEmpty()) return state
        return endRound(state, state.turn)
    }

    private fun endRound(state: GameState, loser: Side, forceFatal: Boolean = false): GameState {
        val winnerSide = loser.other
        val loserPlayer = state.player(loser)
        val blocked = loserPlayer.shielded && !forceFatal

        val rawDamage = max(MIN_DAMAGE, abs(state.nucleus)) + (state.maxChainThisRound - 1) * 3
        val damage = if (forceFatal) loserPlayer.hp else min(MAX_DAMAGE, rawDamage)
        val newHp = if (blocked) loserPlayer.hp else (loserPlayer.hp - damage).coerceAtLeast(0)

        var s = state
            .withPlayer(loser, loserPlayer.copy(hp = newHp, shielded = false))
            .let { st ->
                val w = st.player(winnerSide)
                st.withPlayer(winnerSide, w.copy(roundsWon = w.roundsWon + 1))
            }
            .copy(
                phase = Phase.FIM_DE_RODADA,
                lastRoundLoser = loser,
                events = state.events + GameEvent.Overload(loser, state.nucleus, if (blocked) 0 else damage, blocked),
                log = state.log + LogEntry(
                    if (blocked) "${loserPlayer.name} sobrecarregou, mas o Escudo absorveu tudo."
                    else "${loserPlayer.name} sobrecarregou em ${state.nucleus} e perdeu $damage de vida.",
                    loser
                )
            )

        if (newHp <= 0) {
            s = s.copy(
                phase = Phase.FIM_DE_DUELO,
                winner = winnerSide,
                events = s.events + GameEvent.MatchOver(winnerSide),
                log = s.log + LogEntry("${s.player(winnerSide).name} venceu o duelo.", winnerSide)
            )
        }
        return s
    }

    /** Prepara a próxima rodada. Quem perdeu começa — abrir a rodada é uma desvantagem sutil. */
    fun nextRound(state: GameState): GameState {
        if (state.phase != Phase.FIM_DE_RODADA) return state
        val rng = PulseRandom(state.rngState)
        val shuffled = buildDeck().shuffledWith(rng)
        val hs = state.config.handSize

        val s = state.copy(
            nucleus = 0,
            direction = 0,
            lastElement = null,
            chain = 0,
            maxChainThisRound = 0,
            cardsThisRound = 0,
            you = state.you.copy(hand = shuffled.take(hs)),
            foe = state.foe.copy(hand = shuffled.drop(hs).take(hs)),
            deck = shuffled.drop(hs * 2),
            discard = emptyList(),
            round = state.round + 1,
            phase = Phase.JOGANDO,
            turn = state.lastRoundLoser ?: Side.VOCE,
            extraTurnFor = null,
            directionUnlockedFor = null,
            revealFoeUntilTurn = -1,
            rngState = rng.state,
            events = emptyList(),
            log = state.log + LogEntry("Rodada ${state.round + 1}. O Núcleo volta a 0.", null)
        )
        return startTurn(s)
    }

    /** A UI consome os eventos e depois os limpa para não reproduzir a mesma animação duas vezes. */
    fun clearEvents(state: GameState): GameState =
        if (state.events.isEmpty()) state else state.copy(events = emptyList())

    // ---------------------------------------------------------------- utilidades

    private data class Refill(val hand: List<Card>, val deck: List<Card>, val discard: List<Card>)

    private fun drawUpTo(
        hand: List<Card>,
        deck: List<Card>,
        discard: List<Card>,
        rng: PulseRandom,
        target: Int
    ): Refill {
        val newHand = hand.toMutableList()
        var pile = deck.toMutableList()
        var used = discard.toMutableList()
        while (newHand.size < target) {
            if (pile.isEmpty()) {
                if (used.isEmpty()) break
                pile = used.shuffledWith(rng).toMutableList()
                used = mutableListOf()
            }
            newHand.add(pile.removeAt(0))
        }
        return Refill(newHand, pile, used)
    }

    private fun describePlay(
        who: String,
        card: Card,
        sign: Int,
        after: Int,
        flipped: Boolean,
        chain: Int
    ): String {
        val signText = if (sign > 0) "+" else "-"
        val base = "$who joga ${card.element.ptName} ${card.value} ($signText). Núcleo: $after."
        return when {
            flipped && chain > 1 -> "$base Ressonância inverte a direção! Corrente $chain."
            flipped -> "$base Inversão!"
            chain > 1 -> "$base Corrente $chain."
            else -> base
        }
    }
}
