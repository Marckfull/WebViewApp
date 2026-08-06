package com.kardiapulse.game.security

import com.kardiapulse.game.data.PlayerProfile
import kotlin.math.abs

/**
 * Verificação de plausibilidade do progresso, aplicada em **toda** gravação.
 *
 * O save já é assinado com HMAC ([SaveGuard]), o que resolve a edição do arquivo em disco. Esta
 * camada resolve o caso seguinte: um editor de memória que altera o valor dentro do app, entre a
 * leitura e a gravação — aí a assinatura seria recalculada em cima do valor adulterado e bateria
 * direitinho.
 *
 * A regra é comparar o que está sendo gravado com o que estava lá antes. Nenhuma jogada legítima
 * do mundo dobra os Fragmentos de 300 para 999.999.999 de uma vez. Quando a variação é
 * impossível, o **delta é rejeitado** (o valor anterior é mantido) e o perfil fica marcado.
 *
 * Os tetos são propositalmente folgados: entre punir um jogador honesto por causa de um caso raro
 * e deixar passar um trapaceiro num jogo de um jogador só, a escolha certa é sempre não punir o
 * honesto. Isto **eleva a barreira**, não é proteção absoluta — em cliente offline isso não existe.
 */
object IntegrityRules {

    // Variação máxima aceitável em uma única gravação.
    const val MAX_DELTA_SHARDS = 20_000
    const val MAX_DELTA_XP = 20_000
    const val MAX_DELTA_CRYSTALS = 100

    // Tetos absolutos. Acima disso é lixo, não progresso.
    const val MAX_SHARDS = 9_999_999
    const val MAX_XP = 99_999_999
    const val MAX_CRYSTALS = 99_999
    const val MAX_POWER_STACK = 99
    const val MAX_UNDO = 99
    const val MAX_CHAIN = 60
    const val MAX_ENDLESS = 9_999
    const val MAX_STREAK = 3_650

    data class Result(
        val profile: PlayerProfile,
        val violations: List<String>
    ) {
        val clean: Boolean get() = violations.isEmpty()
    }

    /**
     * Confere [candidate] contra [previous] e devolve uma versão segura para gravar.
     */
    fun validate(previous: PlayerProfile, candidate: PlayerProfile): Result {
        val violations = ArrayList<String>(4)
        var safe = candidate

        safe = safe.copy(
            shards = guard(
                previous.shards, candidate.shards,
                MAX_DELTA_SHARDS, MAX_SHARDS, "shards", violations
            ),
            xp = guard(
                previous.xp, candidate.xp,
                MAX_DELTA_XP, MAX_XP, "xp", violations
            ),
            crystals = guard(
                previous.crystals, candidate.crystals,
                MAX_DELTA_CRYSTALS, MAX_CRYSTALS, "crystals", violations
            )
        )

        // Uma gravação registra no máximo um duelo, e o contador nunca anda para trás.
        if (candidate.duelsPlayed !in previous.duelsPlayed..(previous.duelsPlayed + 1)) {
            violations.add("duelsPlayed")
            safe = safe.copy(duelsPlayed = previous.duelsPlayed)
        }
        if (safe.duelsWon > safe.duelsPlayed) {
            violations.add("duelsWon")
            safe = safe.copy(duelsWon = safe.duelsPlayed)
        }
        if (safe.duelsWon < 0) safe = safe.copy(duelsWon = 0)

        if (candidate.bestChain !in 0..MAX_CHAIN) {
            violations.add("bestChain")
            safe = safe.copy(bestChain = previous.bestChain)
        }
        if (candidate.endlessBest !in 0..MAX_ENDLESS) {
            violations.add("endlessBest")
            safe = safe.copy(endlessBest = previous.endlessBest)
        }
        if (candidate.streakDays !in 0..MAX_STREAK) {
            violations.add("streakDays")
            safe = safe.copy(streakDays = previous.streakDays)
        }
        if (candidate.totalFlips < 0) {
            violations.add("totalFlips")
            safe = safe.copy(totalFlips = previous.totalFlips)
        }

        // Estoques: sempre dá para aparar sem prejudicar ninguém.
        if (candidate.undoCharges !in 0..MAX_UNDO) {
            violations.add("undoCharges")
            safe = safe.copy(undoCharges = candidate.undoCharges.coerceIn(0, MAX_UNDO))
        }
        val badPowers = candidate.powers.filterValues { it !in 1..MAX_POWER_STACK }
        if (badPowers.isNotEmpty()) {
            violations.add("powers")
            safe = safe.copy(
                powers = candidate.powers
                    .mapValues { (_, v) -> v.coerceIn(0, MAX_POWER_STACK) }
                    .filterValues { it > 0 }
            )
        }

        // Cosméticos precisam existir de verdade e o equipado precisa estar entre os comprados.
        if (safe.activeCardBack !in safe.cosmetics) {
            safe = safe.copy(activeCardBack = "dorso_padrao")
        }
        if (safe.activeBoard !in safe.cosmetics) {
            safe = safe.copy(activeBoard = "mesa_padrao")
        }

        if (violations.isNotEmpty()) safe = safe.copy(suspicious = true)
        return Result(safe, violations)
    }

    private fun guard(
        previous: Int,
        candidate: Int,
        maxDelta: Int,
        absoluteMax: Int,
        field: String,
        violations: MutableList<String>
    ): Int {
        if (candidate < 0 || candidate > absoluteMax || abs(candidate - previous) > maxDelta) {
            violations.add(field)
            return previous.coerceIn(0, absoluteMax)
        }
        return candidate
    }
}
