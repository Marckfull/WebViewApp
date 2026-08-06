package com.kardiapulse.game

import com.kardiapulse.game.core.engine.AiPersona
import com.kardiapulse.game.core.engine.AiPlayer
import com.kardiapulse.game.core.engine.GameEngine
import com.kardiapulse.game.core.engine.PulseRandom
import com.kardiapulse.game.core.engine.Replay
import com.kardiapulse.game.core.engine.ReplayCode
import com.kardiapulse.game.core.model.Difficulty
import com.kardiapulse.game.core.model.GameConfig
import com.kardiapulse.game.core.model.GameMode
import com.kardiapulse.game.core.model.Modifier
import com.kardiapulse.game.core.model.Move
import com.kardiapulse.game.core.model.Phase
import com.kardiapulse.game.core.model.PowerType
import com.kardiapulse.game.core.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayCodeTest {

    /** Joga um duelo completo e devolve o replay correspondente. */
    private fun recordMatch(
        seed: Long,
        config: GameConfig = GameConfig(),
        yourPowers: Map<PowerType, Int> = emptyMap()
    ): Pair<Replay, Int> {
        var state = GameEngine.newMatch(config, seed, yourPowers = yourPowers)
        val rng = PulseRandom(seed xor 0x1234L)
        val moves = ArrayList<Move>()
        var guard = 0
        while (state.phase != Phase.FIM_DE_DUELO && guard++ < 2000) {
            if (state.phase == Phase.FIM_DE_RODADA) {
                state = GameEngine.nextRound(state)
                continue
            }
            val move = AiPlayer.chooseMove(state, Difficulty.NORMAL, rng) ?: break
            state = GameEngine.clearEvents(GameEngine.apply(state, move))
            moves.add(move)
        }
        val replay = Replay(
            seed = seed,
            mode = config.mode,
            difficulty = config.difficulty,
            modifiers = config.modifiers,
            yourHp = config.startHp,
            foeHp = config.startHp,
            yourPowers = yourPowers,
            foePowers = emptyMap(),
            moves = moves
        )
        return replay to state.statsCardsPlayed
    }

    @Test
    fun `codificar e decodificar preserva o replay`() {
        val original = Replay(
            seed = -8_123_456_789_012_345L,
            mode = GameMode.CAOS,
            difficulty = Difficulty.MESTRE,
            modifiers = setOf(Modifier.GRAVIDADE, Modifier.ESPELHO),
            yourHp = 37,
            foeHp = 50,
            yourPowers = mapOf(PowerType.DESCARGA to 2, PowerType.ECO to 1),
            foePowers = mapOf(PowerType.ESCUDO to 3),
            moves = listOf(
                Move.Play(0, 1),
                Move.Play(39, -1),
                Move.UsePower(PowerType.INVERSOR),
                Move.Play(17, 1),
                Move.Concede
            )
        )
        val decoded = ReplayCode.decode(ReplayCode.encode(original))
        assertNotNull("O código deveria decodificar", decoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `o replay reproduz o duelo carta por carta`() {
        val (replay, cardsPlayed) = recordMatch(4242L)
        assertTrue("A partida gravada precisa ter jogadas", replay.moves.isNotEmpty())

        val code = ReplayCode.encode(replay)
        val decoded = ReplayCode.decode(code)
        assertNotNull(decoded)

        val timeline = ReplayCode.rebuild(decoded!!)
        val last = timeline.last()
        assertEquals(Phase.FIM_DE_DUELO, last.phase)
        assertEquals(cardsPlayed, last.statsCardsPlayed)
    }

    @Test
    fun `replay de um duelo com poderes tambem reproduz`() {
        val (replay, cards) = recordMatch(
            seed = 909L,
            yourPowers = mapOf(PowerType.DESCARGA to 2, PowerType.ESCUDO to 1)
        )
        val decoded = ReplayCode.decode(ReplayCode.encode(replay))!!
        val timeline = ReplayCode.rebuild(decoded)
        assertEquals(cards, timeline.last().statsCardsPlayed)
    }

    @Test
    fun `codigo corrompido e recusado em vez de virar partida inventada`() {
        val (replay, _) = recordMatch(77L)
        val code = ReplayCode.encode(replay)

        // Troca um caractere do meio: a soma de verificação precisa pegar.
        val index = code.length / 2
        val swapped = if (code[index] == 'A') 'B' else 'A'
        val broken = code.substring(0, index) + swapped + code.substring(index + 1)

        assertNull("Código adulterado não pode decodificar", ReplayCode.decode(broken))
        assertNull("Lixo não pode decodificar", ReplayCode.decode("isso!nao@e#um+codigo"))
        assertNull("Vazio não pode decodificar", ReplayCode.decode(""))
    }

    @Test
    fun `o codigo de um duelo tem tamanho compartilhavel`() {
        val (replay, _) = recordMatch(31337L)
        val code = ReplayCode.encode(replay)
        assertTrue(
            "Código longo demais para compartilhar: ${code.length} caracteres",
            code.length < 400
        )
        // Só caracteres seguros para URL e mensagem.
        assertTrue(code.all { it.isLetterOrDigit() || it == '-' || it == '_' })
    }

    @Test
    fun `personas diferentes produzem partidas diferentes`() {
        // Mesma semente, mesma dificuldade: só o temperamento muda. Se as personas fossem
        // decorativas, as duas sequências de jogadas seriam idênticas.
        fun playWith(persona: AiPersona): List<Move> {
            var state = GameEngine.newMatch(GameConfig(), 5150L)
            val rng = PulseRandom(99L)
            val moves = ArrayList<Move>()
            var guard = 0
            while (state.phase != Phase.FIM_DE_DUELO && guard++ < 400) {
                if (state.phase == Phase.FIM_DE_RODADA) {
                    state = GameEngine.nextRound(state)
                    continue
                }
                // A persona só governa o RIVAL; você é sempre o mesmo jogador de referência.
                val p = if (state.turn == Side.RIVAL) persona else AiPersona.EQUILIBRADO
                val move = AiPlayer.chooseMove(state, Difficulty.DIFICIL, rng, p) ?: break
                state = GameEngine.clearEvents(GameEngine.apply(state, move))
                moves.add(move)
            }
            return moves
        }

        val equilibrado = playWith(AiPersona.EQUILIBRADO)
        val agressivo = playWith(AiPersona.AGRESSIVO)
        val paciente = playWith(AiPersona.PACIENTE)

        assertTrue("Agressivo joga igual ao Equilibrado", agressivo != equilibrado)
        assertTrue("Paciente joga igual ao Equilibrado", paciente != equilibrado)
        assertTrue("Agressivo joga igual ao Paciente", agressivo != paciente)
    }

    @Test
    fun `toda persona consegue jogar um duelo ate o fim`() {
        for (persona in AiPersona.ALL) {
            var state = GameEngine.newMatch(GameConfig(), 2024L)
            val rng = PulseRandom(7L)
            var guard = 0
            while (state.phase != Phase.FIM_DE_DUELO && guard++ < 2000) {
                if (state.phase == Phase.FIM_DE_RODADA) {
                    state = GameEngine.nextRound(state)
                    continue
                }
                val move = AiPlayer.chooseMove(state, Difficulty.NORMAL, rng, persona)
                assertNotNull("$persona não devolveu jogada", move)
                state = GameEngine.clearEvents(GameEngine.apply(state, move!!))
            }
            assertEquals("$persona não terminou o duelo", Phase.FIM_DE_DUELO, state.phase)
        }
    }
}
