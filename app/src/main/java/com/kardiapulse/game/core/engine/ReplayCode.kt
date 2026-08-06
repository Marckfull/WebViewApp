package com.kardiapulse.game.core.engine

import com.kardiapulse.game.core.model.Difficulty
import com.kardiapulse.game.core.model.GameConfig
import com.kardiapulse.game.core.model.GameMode
import com.kardiapulse.game.core.model.GameState
import com.kardiapulse.game.core.model.Modifier
import com.kardiapulse.game.core.model.Move
import com.kardiapulse.game.core.model.Phase
import com.kardiapulse.game.core.model.PowerType

/**
 * Uma partida inteira, gravada.
 *
 * Como o motor é determinístico, não é preciso guardar estado nenhum: a semente mais a lista de
 * jogadas reconstroem o duelo carta por carta. Um duelo típico cabe em pouco mais de cem
 * caracteres — curto o suficiente para mandar por mensagem.
 */
data class Replay(
    val seed: Long,
    val mode: GameMode,
    val difficulty: Difficulty,
    val modifiers: Set<Modifier>,
    val yourHp: Int,
    val foeHp: Int,
    val yourPowers: Map<PowerType, Int>,
    val foePowers: Map<PowerType, Int>,
    val moves: List<Move>
) {
    val config: GameConfig
        get() = GameConfig(
            mode = mode,
            difficulty = difficulty,
            modifiers = modifiers,
            turnSeconds = 0
        )
}

/**
 * Codifica e decodifica replays em texto.
 *
 * O Base64 é implementado aqui à mão de propósito: `java.util.Base64` só existe a partir da API
 * 26 e `android.util.Base64` não roda em teste de unidade na JVM. Trinta linhas resolvem os dois
 * problemas de uma vez.
 */
object ReplayCode {

    private const val VERSION = 1
    private const val ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    private const val MOVE_CONCEDE = 0xFF
    private const val MOVE_POWER_FLAG = 0x80
    private const val MOVE_SIGN_FLAG = 0x40
    private const val MOVE_CARD_MASK = 0x3F

    // ------------------------------------------------------------------ escrita

    fun encode(replay: Replay): String {
        val out = ArrayList<Int>(64)
        out.add(VERSION)
        for (shift in 56 downTo 0 step 8) {
            out.add(((replay.seed ushr shift) and 0xFF).toInt())
        }
        out.add(replay.mode.ordinal)
        out.add(replay.difficulty.ordinal)

        var mask = 0
        for (modifier in replay.modifiers) mask = mask or (1 shl modifier.ordinal)
        out.add(mask and 0xFF)

        out.add(replay.yourHp.coerceIn(0, 255))
        out.add(replay.foeHp.coerceIn(0, 255))
        for (type in PowerType.entries) out.add((replay.yourPowers[type] ?: 0).coerceIn(0, 255))
        for (type in PowerType.entries) out.add((replay.foePowers[type] ?: 0).coerceIn(0, 255))

        for (move in replay.moves) {
            out.add(
                when (move) {
                    is Move.Play ->
                        (move.cardId and MOVE_CARD_MASK) or (if (move.sign > 0) MOVE_SIGN_FLAG else 0)
                    is Move.UsePower -> MOVE_POWER_FLAG or move.power.ordinal
                    Move.Concede -> MOVE_CONCEDE
                }
            )
        }

        // Soma de verificação: pega código digitado errado antes de tentar jogar.
        var sum = 0
        for (byte in out) sum = (sum + byte) and 0xFFFF
        out.add((sum ushr 8) and 0xFF)
        out.add(sum and 0xFF)

        return base64Encode(out)
    }

    // ------------------------------------------------------------------ leitura

    fun decode(raw: String): Replay? {
        val bytes = base64Decode(raw.trim().replace(" ", "").replace("\n", "")) ?: return null
        val headerSize = 12 + PowerType.entries.size * 2
        if (bytes.size < headerSize + 2) return null

        var sum = 0
        for (i in 0 until bytes.size - 2) sum = (sum + bytes[i]) and 0xFFFF
        val expected = (bytes[bytes.size - 2] shl 8) or bytes[bytes.size - 1]
        if (sum != expected) return null

        if (bytes[0] != VERSION) return null

        var seed = 0L
        for (i in 1..8) seed = (seed shl 8) or bytes[i].toLong()

        val mode = GameMode.entries.getOrNull(bytes[9]) ?: return null
        val difficulty = Difficulty.entries.getOrNull(bytes[10]) ?: return null
        val mask = bytes[11]
        val modifiers = Modifier.entries.filter { mask and (1 shl it.ordinal) != 0 }.toSet()

        val yourHp = bytes[12]
        val foeHp = bytes[13]

        var cursor = 14
        val yourPowers = LinkedHashMap<PowerType, Int>()
        for (type in PowerType.entries) {
            val count = bytes[cursor++]
            if (count > 0) yourPowers[type] = count
        }
        val foePowers = LinkedHashMap<PowerType, Int>()
        for (type in PowerType.entries) {
            val count = bytes[cursor++]
            if (count > 0) foePowers[type] = count
        }

        val moves = ArrayList<Move>()
        while (cursor < bytes.size - 2) {
            val b = bytes[cursor++]
            val move = when {
                b == MOVE_CONCEDE -> Move.Concede
                b and MOVE_POWER_FLAG != 0 ->
                    PowerType.entries.getOrNull(b and 0x0F)?.let { Move.UsePower(it) } ?: return null
                else -> Move.Play(b and MOVE_CARD_MASK, if (b and MOVE_SIGN_FLAG != 0) 1 else -1)
            }
            moves.add(move)
        }

        return Replay(
            seed = seed,
            mode = mode,
            difficulty = difficulty,
            modifiers = modifiers,
            yourHp = yourHp,
            foeHp = foeHp,
            yourPowers = yourPowers,
            foePowers = foePowers,
            moves = moves
        )
    }

    /**
     * Reconstrói a linha do tempo completa do duelo, um estado por jogada.
     *
     * Uma jogada gravada que não seja legal na reconstrução significa código corrompido ou de
     * outra versão das regras: a reprodução para ali em vez de mostrar uma partida inventada.
     */
    fun rebuild(replay: Replay): List<GameState> {
        var state = GameEngine.newMatch(
            config = replay.config,
            seed = replay.seed,
            yourPowers = replay.yourPowers,
            foePowers = replay.foePowers,
            yourHp = replay.yourHp,
            foeHp = replay.foeHp
        )
        val timeline = ArrayList<GameState>(replay.moves.size + 1)
        timeline.add(state)

        for (move in replay.moves) {
            if (state.phase == Phase.FIM_DE_RODADA) {
                state = GameEngine.nextRound(state)
                timeline.add(state)
            }
            if (state.phase != Phase.JOGANDO) break
            val next = GameEngine.apply(state, move)
            if (next === state) break // jogada rejeitada: o código não confere com estas regras
            state = next
            timeline.add(state)
        }
        return timeline
    }

    /** Deixa o código legível, em grupos de seis, para conferir e digitar sem enlouquecer. */
    fun pretty(code: String): String = code.chunked(6).joinToString(" ")

    // ------------------------------------------------------------------ base64

    private fun base64Encode(bytes: List<Int>): String {
        val sb = StringBuilder((bytes.size + 2) / 3 * 4)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i] and 0xFF
            val b1 = if (i + 1 < bytes.size) bytes[i + 1] and 0xFF else 0
            val b2 = if (i + 2 < bytes.size) bytes[i + 2] and 0xFF else 0
            val triple = (b0 shl 16) or (b1 shl 8) or b2

            sb.append(ALPHABET[(triple ushr 18) and 0x3F])
            sb.append(ALPHABET[(triple ushr 12) and 0x3F])
            if (i + 1 < bytes.size) sb.append(ALPHABET[(triple ushr 6) and 0x3F])
            if (i + 2 < bytes.size) sb.append(ALPHABET[triple and 0x3F])
            i += 3
        }
        return sb.toString()
    }

    private fun base64Decode(text: String): List<Int>? {
        if (text.isEmpty()) return null
        val values = IntArray(text.length)
        for (i in text.indices) {
            val index = ALPHABET.indexOf(text[i])
            if (index < 0) return null
            values[i] = index
        }
        val out = ArrayList<Int>(text.length * 3 / 4)
        var i = 0
        while (i < values.size) {
            val remaining = values.size - i
            if (remaining < 2) return null
            val c0 = values[i]
            val c1 = values[i + 1]
            val c2 = if (remaining > 2) values[i + 2] else 0
            val c3 = if (remaining > 3) values[i + 3] else 0
            val triple = (c0 shl 18) or (c1 shl 12) or (c2 shl 6) or c3

            out.add((triple ushr 16) and 0xFF)
            if (remaining > 2) out.add((triple ushr 8) and 0xFF)
            if (remaining > 3) out.add(triple and 0xFF)
            i += 4
        }
        return out
    }
}
