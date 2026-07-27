package com.chuvadeletras.game.data

import com.chuvadeletras.game.domain.model.GameMode
import com.chuvadeletras.game.domain.model.MatchSummary
import com.chuvadeletras.game.domain.model.PowerUp
import kotlin.random.Random

// ---------------------------------------------------------------------------
// Missões diárias
// ---------------------------------------------------------------------------

enum class MissionKind {
    WORDS_SOLVED,
    GAMES_WON,
    LETTERS_PLACED,
    PERFECT_WIN,
    DAILY_CHALLENGE,
    STORM_WIN,
    BIG_COMBO
}

data class MissionTemplate(
    val id: String,
    val emoji: String,
    val title: String,
    val kind: MissionKind,
    val target: Int,
    val coins: Int,
    val xp: Int
)

object Missions {

    private val catalog = listOf(
        MissionTemplate("m_words_5", "🔤", "Complete 5 palavras", MissionKind.WORDS_SOLVED, 5, 60, 25),
        MissionTemplate("m_words_12", "📚", "Complete 12 palavras", MissionKind.WORDS_SOLVED, 12, 120, 50),
        MissionTemplate("m_win_1", "🏆", "Vença 1 partida", MissionKind.GAMES_WON, 1, 70, 30),
        MissionTemplate("m_win_3", "🥇", "Vença 3 partidas", MissionKind.GAMES_WON, 3, 160, 70),
        MissionTemplate("m_letters_40", "✍️", "Encaixe 40 letras", MissionKind.LETTERS_PLACED, 40, 80, 30),
        MissionTemplate("m_letters_80", "🖋️", "Encaixe 80 letras", MissionKind.LETTERS_PLACED, 80, 150, 60),
        MissionTemplate("m_perfect", "💎", "Vença sem perder nenhum quadradinho", MissionKind.PERFECT_WIN, 1, 200, 80),
        MissionTemplate("m_daily", "📅", "Conclua o Desafio Diário", MissionKind.DAILY_CHALLENGE, 1, 150, 60),
        MissionTemplate("m_storm", "⛈️", "Vença uma Tempestade", MissionKind.STORM_WIN, 1, 180, 70),
        MissionTemplate("m_combo", "🔥", "Faça um combo de 8 letras seguidas", MissionKind.BIG_COMBO, 8, 130, 50)
    )

    fun byId(id: String): MissionTemplate? = catalog.firstOrNull { it.id == id }

    /** As 3 missões do dia. Mesma data = mesmas missões (para todo mundo). */
    fun forDate(isoDate: String): List<MissionTemplate> {
        val seed = isoDate.filter { it.isDigit() }.toLongOrNull() ?: 0L
        return catalog.shuffled(Random(seed)).take(3)
    }

    /** Traduz o resultado de uma partida em avanço de missão. */
    fun advance(template: MissionTemplate, summary: MatchSummary, current: Int): Int = when (template.kind) {
        MissionKind.WORDS_SOLVED -> current + summary.wordsSolved
        MissionKind.GAMES_WON -> current + if (summary.won) 1 else 0
        MissionKind.LETTERS_PLACED -> current + summary.wordsSolved * 4
        MissionKind.PERFECT_WIN -> maxOf(current, if (summary.perfect) 1 else 0)
        MissionKind.DAILY_CHALLENGE ->
            maxOf(current, if (summary.won && summary.mode == GameMode.DIARIO) 1 else 0)

        MissionKind.STORM_WIN ->
            maxOf(current, if (summary.won && summary.mode == GameMode.TEMPESTADE) 1 else 0)

        MissionKind.BIG_COMBO -> maxOf(current, summary.bestCombo)
    }.coerceAtMost(template.target)
}

// ---------------------------------------------------------------------------
// Conquistas
// ---------------------------------------------------------------------------

data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val target: Int,
    val coins: Int,
    val metric: (PlayerProfile) -> Int
)

object Achievements {

    val all: List<Achievement> = listOf(
        Achievement("a_first_win", "🌱", "Primeira Gota", "Vença sua primeira partida", 1, 50) {
            it.stats.gamesWon
        },
        Achievement("a_win_10", "🌧️", "Chuva Constante", "Vença 10 partidas", 10, 150) {
            it.stats.gamesWon
        },
        Achievement("a_win_50", "⛈️", "Temporal", "Vença 50 partidas", 50, 500) {
            it.stats.gamesWon
        },
        Achievement("a_words_100", "📖", "Vocabulário Molhado", "Complete 100 palavras", 100, 250) {
            it.stats.wordsSolved
        },
        Achievement("a_words_500", "🧠", "Dicionário Ambulante", "Complete 500 palavras", 500, 800) {
            it.stats.wordsSolved
        },
        Achievement("a_perfect_5", "💎", "Mão Firme", "Vença 5 partidas sem perder quadradinho", 5, 400) {
            it.stats.perfectGames
        },
        Achievement("a_combo_15", "🔥", "Sequência Relâmpago", "Faça um combo de 15 letras", 15, 300) {
            it.stats.bestCombo
        },
        Achievement("a_streak_7", "📆", "Semana Cheia", "Jogue 7 dias seguidos", 7, 350) {
            it.bestStreak
        },
        Achievement("a_streak_30", "🗓️", "Mês Inteiro", "Jogue 30 dias seguidos", 30, 1200) {
            it.bestStreak
        },
        Achievement("a_daily_10", "🎯", "Fiel de Carteirinha", "Conclua 10 Desafios Diários", 10, 400) {
            it.stats.dailiesCompleted
        },
        Achievement("a_level_10", "⭐", "Veterano da Chuva", "Chegue ao nível 10", 10, 500) {
            it.level
        },
        Achievement("a_score_5000", "💯", "Pontaria", "Faça 5000 pontos em uma partida", 5000, 450) {
            it.stats.bestScore
        }
    )

    fun byId(id: String): Achievement? = all.firstOrNull { it.id == id }

    fun progressOf(achievement: Achievement, profile: PlayerProfile): Int =
        achievement.metric(profile).coerceAtMost(achievement.target)

    fun isComplete(achievement: Achievement, profile: PlayerProfile): Boolean =
        achievement.metric(profile) >= achievement.target

    /** Conquistas concluídas e ainda não resgatadas. */
    fun claimable(profile: PlayerProfile): List<Achievement> =
        all.filter { isComplete(it, profile) && it.id !in profile.claimedAchievements }
}

// ---------------------------------------------------------------------------
// Baú diário (ciclo de 7 dias)
// ---------------------------------------------------------------------------

data class DailyReward(
    val day: Int,
    val coins: Int,
    val powerUp: PowerUp? = null,
    val powerUpAmount: Int = 0,
    val isBig: Boolean = false
)

object DailyRewards {

    val cycle: List<DailyReward> = listOf(
        DailyReward(1, 50),
        DailyReward(2, 75),
        DailyReward(3, 100, PowerUp.CONGELAR, 1),
        DailyReward(4, 125),
        DailyReward(5, 150, PowerUp.REVELAR, 1),
        DailyReward(6, 200, PowerUp.TROCAR, 1),
        DailyReward(7, 400, PowerUp.REPARO, 2, isBig = true)
    )

    fun rewardFor(dayIndex: Int): DailyReward = cycle[dayIndex.coerceIn(0, cycle.size - 1)]
}

/** Marcos de ofensiva: bônus extra por manter a sequência de dias. */
object StreakBonus {
    private val milestones = mapOf(3 to 100, 7 to 300, 14 to 600, 30 to 1500, 60 to 3000)

    fun bonusFor(streak: Int): Int = milestones[streak] ?: 0

    fun nextMilestone(streak: Int): Int? = milestones.keys.sorted().firstOrNull { it > streak }
}
