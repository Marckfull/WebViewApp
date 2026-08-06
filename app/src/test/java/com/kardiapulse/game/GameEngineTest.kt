package com.kardiapulse.game

import com.kardiapulse.game.core.engine.AiPlayer
import com.kardiapulse.game.core.engine.GameEngine
import com.kardiapulse.game.core.engine.PulseRandom
import com.kardiapulse.game.core.model.Card
import com.kardiapulse.game.core.model.Difficulty
import com.kardiapulse.game.core.model.Element
import com.kardiapulse.game.core.model.GameConfig
import com.kardiapulse.game.core.model.GameEvent
import com.kardiapulse.game.core.model.Modifier
import com.kardiapulse.game.core.model.Move
import com.kardiapulse.game.core.model.Phase
import com.kardiapulse.game.core.model.PowerType
import com.kardiapulse.game.core.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Testes do motor de regras. Todos rodam na JVM, sem emulador: `./gradlew test`.
 */
class GameEngineTest {

    private data class Report(
        val finished: Boolean,
        val rounds: Int,
        val winner: Side?,
        val cardsPlayed: Int,
        val flips: Int
    )

    /** Roda um duelo completo IA contra IA, validando as invariantes a cada passo. */
    private fun playMatch(
        config: GameConfig,
        seed: Long,
        d1: Difficulty = Difficulty.NORMAL,
        d2: Difficulty = Difficulty.NORMAL,
        maxTurns: Int = 2000
    ): Report {
        val rng = PulseRandom(seed xor 0x5DEECE66DL)
        var state = GameEngine.newMatch(config, seed)
        var turns = 0

        while (state.phase != Phase.FIM_DE_DUELO && turns < maxTurns) {
            turns++
            if (state.phase == Phase.JOGANDO) {
                assertTrue(
                    "Núcleo fora do limite: ${state.nucleus} > ${state.limitNow}",
                    abs(state.nucleus) <= state.limitNow
                )
            }
            if (state.phase == Phase.FIM_DE_RODADA) {
                state = GameEngine.nextRound(state)
                continue
            }
            val difficulty = if (state.turn == Side.VOCE) d1 else d2
            val move = AiPlayer.chooseMove(state, difficulty, rng)
            assertNotNull("IA não devolveu jogada em ${state.phase}", move)
            val before = state
            state = GameEngine.apply(state, move!!)
            assertTrue("Motor rejeitou a jogada da IA: $move", state !== before)
            state = GameEngine.clearEvents(state)
        }
        return Report(
            finished = state.phase == Phase.FIM_DE_DUELO,
            rounds = state.round,
            winner = state.winner,
            cardsPlayed = state.statsCardsPlayed,
            flips = state.statsFlips
        )
    }

    @Test
    fun `baralho tem 40 cartas com ids unicos`() {
        val deck = GameEngine.buildDeck()
        assertEquals(40, deck.size)
        assertEquals(40, deck.map { it.id }.toSet().size)
        assertEquals(4, deck.count { it.element == Element.ETER })
        assertTrue(deck.all { it.value in 1..9 })
    }

    @Test
    fun `partida comeca centrada e com direcao livre`() {
        val s = GameEngine.newMatch(GameConfig(), seed = 12345L)
        assertEquals(0, s.nucleus)
        assertEquals(0, s.direction)
        assertEquals(5, s.you.hand.size)
        assertEquals(5, s.foe.hand.size)
        assertEquals(30, s.deck.size)
        // Com direção livre e Núcleo em 0, todas as 5 cartas servem nas duas polaridades.
        assertEquals(10, GameEngine.legalPlays(s).size)
    }

    @Test
    fun `primeira carta trava a direcao`() {
        var s = GameEngine.newMatch(GameConfig(), seed = 12345L)
        val card = s.you.hand.first()
        s = GameEngine.apply(s, Move.Play(card.id, 1))
        assertEquals(1, s.direction)
        assertEquals(card.value, s.nucleus)
        assertEquals(Side.RIVAL, s.turn)
        assertEquals(5, s.you.hand.size)
    }

    @Test
    fun `inverter a direcao exige ressonancia`() {
        var s = GameEngine.newMatch(GameConfig(), seed = 999L)
        s = GameEngine.apply(s, Move.Play(s.you.hand.first().id, 1))
        // Nesse ponto nada pode estourar o limite indo para baixo, então a única regra em
        // jogo é a ressonância com o elemento da última carta.
        for (card in s.foe.hand) {
            assertEquals(
                "Carta ${card.element}/${card.value} contra a direção",
                card.resonatesWith(s.lastElement),
                GameEngine.isLegal(s, card, -1)
            )
        }
    }

    @Test
    fun `eter ressoa com qualquer elemento`() {
        for (e in Element.entries) {
            assertTrue(Element.resonates(Element.ETER, e))
            assertTrue(Element.resonates(e, Element.ETER))
        }
    }

    @Test
    fun `colapso encolhe o campo conforme a rodada avanca`() {
        val s = GameEngine.newMatch(GameConfig(), seed = 7L)
        assertEquals(s.config.limit, s.limitNow)
        val late = s.copy(cardsThisRound = 20)
        assertTrue("O Colapso precisa apertar", late.limitNow < s.limitNow)
        val veryLate = s.copy(cardsThisRound = 500)
        assertEquals(s.config.limitFloor, veryLate.limitNow)
    }

    @Test
    fun `nenhuma partida fica presa e o nucleo nunca estoura`() {
        for (seed in 1L..25L) {
            val r = playMatch(GameConfig(), seed, Difficulty.DIFICIL, Difficulty.NORMAL)
            assertTrue("Semente $seed não terminou", r.finished)
        }
    }

    @Test
    fun `todos os modificadores produzem partidas validas`() {
        for (mod in Modifier.ALL) {
            val config = GameConfig(modifiers = setOf(mod))
            for (seed in 100L..106L) {
                assertTrue(
                    "Modificador $mod travou na semente $seed",
                    playMatch(config, seed).finished
                )
            }
        }
        val all = GameConfig(modifiers = Modifier.ALL.toSet())
        for (seed in 200L..206L) {
            assertTrue("Combo de modificadores travou", playMatch(all, seed).finished)
        }
    }

    @Test
    fun `mesma semente produz exatamente a mesma partida`() {
        val a = playMatch(GameConfig(), 777L, Difficulty.MESTRE, Difficulty.MESTRE)
        val b = playMatch(GameConfig(), 777L, Difficulty.MESTRE, Difficulty.MESTRE)
        assertEquals(a.winner, b.winner)
        assertEquals(a.rounds, b.rounds)
        assertEquals(a.cardsPlayed, b.cardsPlayed)
    }

    @Test
    fun `a IA mestre vence a IA facil com folga`() {
        var wins = 0
        val total = 20
        for (seed in 1000L until 1000L + total) {
            // Alterna os lados para anular qualquer vantagem de começar jogando.
            val masterIsYou = seed % 2 == 0L
            val r = playMatch(
                GameConfig(),
                seed,
                if (masterIsYou) Difficulty.MESTRE else Difficulty.FACIL,
                if (masterIsYou) Difficulty.FACIL else Difficulty.MESTRE
            )
            if (r.winner == (if (masterIsYou) Side.VOCE else Side.RIVAL)) wins++
        }
        assertTrue("Mestre venceu apenas $wins de $total", wins * 100 / total >= 65)
    }

    @Test
    fun `o duelo tem ritmo de jogo mobile`() {
        var rounds = 0
        var cards = 0
        var flips = 0
        val total = 12
        for (seed in 3000L until 3000L + total) {
            val r = playMatch(GameConfig(), seed, Difficulty.DIFICIL, Difficulty.DIFICIL)
            rounds += r.rounds
            cards += r.cardsPlayed
            flips += r.flips
        }
        val avgRounds = rounds.toDouble() / total
        val cardsPerRound = cards.toDouble() / rounds
        assertTrue("Duelo com $avgRounds rodadas", avgRounds in 2.0..9.0)
        assertTrue("Rodada com $cardsPerRound cartas", cardsPerRound < 30.0)
        assertTrue("Inversões precisam acontecer", flips.toDouble() / total >= 1.0)
    }

    @Test
    fun `sobrecarga aplica dano dentro dos limites`() {
        var s = GameEngine.newMatch(GameConfig(startHp = 20), seed = 55L)
        var sawOverload = false
        var guard = 0
        while (guard++ < 500 && s.phase == Phase.JOGANDO) {
            val move = AiPlayer.chooseMove(s, Difficulty.NORMAL, PulseRandom(guard.toLong())) ?: break
            s = GameEngine.apply(s, move)
            val overload = s.events.filterIsInstance<GameEvent.Overload>().firstOrNull()
            if (overload != null) {
                sawOverload = true
                assertTrue(
                    "Dano fora da faixa: ${overload.damage}",
                    overload.blockedByShield ||
                        overload.damage in GameEngine.MIN_DAMAGE..GameEngine.MAX_DAMAGE
                )
                break
            }
            s = GameEngine.clearEvents(s)
        }
        assertTrue("Nenhuma sobrecarga aconteceu", sawOverload)
    }

    @Test
    fun `o escudo anula por completo o dano de uma sobrecarga`() {
        val base = GameEngine.newMatch(GameConfig(), seed = 11L)
        // Núcleo encostado no limite, sem nenhuma carta capaz de continuar na direção travada.
        val cornered = base.copy(
            nucleus = 20,
            direction = 1,
            lastElement = Element.FOGO,
            you = base.you.copy(
                hand = listOf(Card(90, 9, Element.AGUA)),
                shielded = true,
                // Visão não é poder de resgate: gastá-la não desfaz o beco sem saída.
                powers = mapOf(PowerType.VISAO to 1)
            ),
            turn = Side.VOCE
        )
        assertTrue("A posição precisa ser sem saída", GameEngine.legalPlays(cornered).isEmpty())
        val hpBefore = cornered.you.hp
        val resolved = GameEngine.apply(cornered, Move.UsePower(PowerType.VISAO))
        // Sem poder de resgate disponível, o motor encerra a rodada com o escudo absorvendo tudo.
        val overload = resolved.events.filterIsInstance<GameEvent.Overload>().firstOrNull()
        assertNotNull("Deveria ter sobrecarregado", overload)
        assertTrue("O escudo deveria ter bloqueado", overload!!.blockedByShield)
        assertEquals(hpBefore, resolved.you.hp)
    }
}
