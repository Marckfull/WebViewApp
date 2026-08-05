package com.formatfrute.game.data

import com.formatfrute.game.core.GameMode
import com.formatfrute.game.core.GameState
import com.formatfrute.game.core.Tile
import com.formatfrute.game.core.TileKind

/**
 * Uma partida interrompida no meio.
 *
 * Em celular, sessão cortada por ligação, notificação ou bateria é regra, não
 * exceção. Perder o tabuleiro nessas horas é o jeito mais rápido de o jogador
 * desinstalar o jogo.
 */
data class SavedGame(
    val mode: GameMode,
    val recipeNumber: Int,
    val recipeDay: String,
    val state: GameState,
    val timeLeft: Int,
    val movesLeft: Int,
    val movesTotal: Int,
    val bossHp: Int,
    val movesSinceAttack: Int,
    val produced: Map<Int, Int>,
    val merges: Int,
    val harvests: Int,
) {
    val isRecipe: Boolean get() = mode.isCampaign

    fun encode(): String = listOf(
        mode.id,
        recipeNumber.toString(),
        recipeDay,
        state.size.toString(),
        state.score.toString(),
        state.moves.toString(),
        state.nextId.toString(),
        timeLeft.toString(),
        movesLeft.toString(),
        movesTotal.toString(),
        bossHp.toString(),
        movesSinceAttack.toString(),
        merges.toString(),
        harvests.toString(),
        produced.entries.joinToString(",") { "${it.key}=${it.value}" },
        state.tiles.joinToString(",") {
            "${it.id}:${it.level}:${it.row}:${it.col}:${it.kind.ordinal}:${it.ice}"
        },
    ).joinToString("|")

    companion object {
        private const val FIELDS = 16

        fun decode(raw: String?): SavedGame? {
            if (raw.isNullOrBlank()) return null
            val parts = raw.split('|')
            if (parts.size != FIELDS) return null

            return runCatching {
                val tiles = parts[15].split(',').filter { it.isNotBlank() }.map { chunk ->
                    val f = chunk.split(':')
                    Tile(
                        id = f[0].toLong(),
                        level = f[1].toInt(),
                        row = f[2].toInt(),
                        col = f[3].toInt(),
                        kind = TileKind.entries[f[4].toInt()],
                        ice = f[5].toInt(),
                    )
                }
                val produced = parts[14].split(',').filter { it.isNotBlank() }.associate {
                    val f = it.split('=')
                    f[0].toInt() to f[1].toInt()
                }

                SavedGame(
                    mode = GameMode.byId(parts[0]),
                    recipeNumber = parts[1].toInt(),
                    recipeDay = parts[2],
                    state = GameState(
                        size = parts[3].toInt(),
                        tiles = tiles,
                        score = parts[4].toInt(),
                        moves = parts[5].toInt(),
                        nextId = parts[6].toLong(),
                    ),
                    timeLeft = parts[7].toInt(),
                    movesLeft = parts[8].toInt(),
                    movesTotal = parts[9].toInt(),
                    bossHp = parts[10].toInt(),
                    movesSinceAttack = parts[11].toInt(),
                    merges = parts[12].toInt(),
                    harvests = parts[13].toInt(),
                    produced = produced,
                )
            }.getOrNull()?.takeIf { it.state.tiles.isNotEmpty() }
        }
    }
}
