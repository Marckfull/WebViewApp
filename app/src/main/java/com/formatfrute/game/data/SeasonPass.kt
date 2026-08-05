package com.formatfrute.game.data

import com.formatfrute.game.core.Power
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class RewardKind { SEMENTES, PODER, PELE, TITULO }

data class PassReward(
    val kind: RewardKind,
    val amount: Int = 0,
    val powerId: String = "",
    val themeId: String = "",
    val title: String = "",
) {
    val emoji: String
        get() = when (kind) {
            RewardKind.SEMENTES -> "🌱"
            RewardKind.PODER -> Power.byId(powerId)?.emoji ?: "✨"
            RewardKind.PELE -> "🎨"
            RewardKind.TITULO -> "🏅"
        }

    val label: String
        get() = when (kind) {
            RewardKind.SEMENTES -> "$amount sementes"
            RewardKind.PODER -> "${Power.byId(powerId)?.title ?: "Poder"} x$amount"
            RewardKind.PELE -> BoardTheme.byId(themeId).title
            RewardKind.TITULO -> title
        }
}

data class PassTier(val level: Int, val free: PassReward, val premium: PassReward)

/**
 * Passe da Feira: 30 degraus por temporada (uma por mês).
 *
 * A faixa grátis abre só por jogar. A faixa premiada abre degrau a degrau
 * assistindo um vídeo — sem cobrança, do jeito que o resto do jogo funciona.
 */
object SeasonPass {

    const val TIERS = 30
    const val POINTS_PER_TIER = 100

    private val titles = listOf(
        "Feirante do Mês", "Rei da Cesta", "Doutor em Fruta",
        "Mão de Vitamina", "Lenda do Pomar",
    )

    /** Nome da temporada, tirado do mês corrente. */
    fun seasonId(millis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date(millis))

    fun seasonName(id: String = seasonId()): String {
        val months = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
        )
        val month = id.substringAfter('-').toIntOrNull() ?: 1
        return "Temporada de ${months[(month - 1).coerceIn(0, 11)]}"
    }

    /** Dias que faltam para a temporada virar. */
    fun daysLeft(millis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val last = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (last - cal.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)
    }

    fun tier(level: Int): PassTier {
        val n = level.coerceIn(1, TIERS)

        val free = when {
            n % 10 == 0 -> PassReward(RewardKind.PODER, amount = 2, powerId = Power.ARCOIRIS.id)
            n % 5 == 0 -> PassReward(RewardKind.PODER, amount = 1, powerId = powerFor(n))
            else -> PassReward(RewardKind.SEMENTES, amount = 60 + (n / 5) * 20)
        }

        // As três peles do Passe são exclusivas: não existem na loja, então
        // subir degrau é a única forma de tê-las.
        val premium = when (n) {
            10 -> PassReward(RewardKind.PELE, themeId = BoardTheme.NOITE.id)
            20 -> PassReward(RewardKind.PELE, themeId = BoardTheme.JUNINA.id)
            30 -> PassReward(RewardKind.PELE, themeId = BoardTheme.ENCANTADO.id)
            else -> when {
                n % 7 == 0 -> PassReward(
                    RewardKind.TITULO,
                    title = titles[(n / 7 - 1).coerceIn(0, titles.lastIndex)],
                )
                n % 3 == 0 -> PassReward(RewardKind.PODER, amount = 2, powerId = powerFor(n + 2))
                else -> PassReward(RewardKind.SEMENTES, amount = 150 + (n / 3) * 40)
            }
        }

        return PassTier(n, free, premium)
    }

    fun allTiers(): List<PassTier> = (1..TIERS).map { tier(it) }

    private fun powerFor(n: Int): String =
        Power.entries[(n / 5) % Power.entries.size].id

    fun tierOf(points: Int): Int = (points / POINTS_PER_TIER).coerceIn(0, TIERS)

    fun progressInTier(points: Int): Pair<Int, Int> {
        if (tierOf(points) >= TIERS) return POINTS_PER_TIER to POINTS_PER_TIER
        return (points % POINTS_PER_TIER) to POINTS_PER_TIER
    }

    // ------------------------------------------------------- ganho de fichas

    /** Fichas por partida terminada, proporcionais ao que o jogador fez. */
    fun pointsForGame(score: Int, merges: Int, won: Boolean): Int =
        (score / 250) + (merges / 12) + (if (won) 12 else 4)

    /**
     * Fase fechada rende bem; tentativa frustrada rende pouco — o suficiente
     * para não parecer castigo, pouco demais para virar fazenda de fichas.
     */
    fun pointsForRecipe(stars: Int): Int = if (stars <= 0) 4 else 15 + stars * 5

    const val POINTS_PER_MISSION = 12
}
