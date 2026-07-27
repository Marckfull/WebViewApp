package com.chuvadeletras.game.data

import com.chuvadeletras.game.domain.model.GameMode
import com.chuvadeletras.game.domain.model.PowerUp
import kotlinx.serialization.Serializable

/** Contadores acumulados — alimentam conquistas e missões. */
@Serializable
data class PlayerStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val wordsSolved: Int = 0,
    val lettersPlaced: Int = 0,
    val perfectGames: Int = 0,
    val bestCombo: Int = 0,
    val bestScore: Int = 0,
    val cellsLost: Int = 0,
    val adsWatched: Int = 0,
    val dailiesCompleted: Int = 0,
    val secondsPlayed: Long = 0
)

/** Progresso de uma missão diária. */
@Serializable
data class MissionProgress(
    val id: String,
    val progress: Int = 0,
    val claimed: Boolean = false
)

/** Tudo que o jogador acumula entre partidas. Persiste em DataStore como JSON. */
@Serializable
data class PlayerProfile(
    val coins: Int = 150,
    val xp: Int = 0,
    /** Ofensiva: dias seguidos jogando. */
    val streak: Int = 0,
    val bestStreak: Int = 0,
    /** ISO-8601 (yyyy-MM-dd) do último dia em que jogou. Vazio = nunca. */
    val lastPlayedDate: String = "",
    /** Último dia em que resgatou o baú diário. */
    val lastRewardDate: String = "",
    /** Posição no ciclo de 7 dias do baú. */
    val rewardDayIndex: Int = 0,
    /** Último dia em que concluiu o Desafio Diário. */
    val lastDailyDate: String = "",
    val tutorialDone: Boolean = false,
    val powerUps: Map<PowerUp, Int> = mapOf(
        PowerUp.CONGELAR to 2,
        PowerUp.TROCAR to 1,
        PowerUp.REVELAR to 1,
        PowerUp.REPARO to 1,
        PowerUp.TEMPO to 1
    ),
    val achievements: Set<String> = emptySet(),
    val claimedAchievements: Set<String> = emptySet(),
    val stats: PlayerStats = PlayerStats(),
    val missions: List<MissionProgress> = emptyList(),
    val missionsDate: String = "",
    /** Melhor número de estrelas por nível da campanha (nível -> estrelas). */
    val levelStars: Map<Int, Int> = emptyMap(),
    val unlockedModes: Set<GameMode> = setOf(GameMode.CLASSICO, GameMode.ZEN, GameMode.DIARIO),
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val lastFreeCoinsAt: Long = 0L
) {
    val level: Int get() = LevelCurve.levelFor(xp)
    val xpIntoLevel: Int get() = xp - LevelCurve.xpForLevel(level)
    val xpForNextLevel: Int get() = LevelCurve.xpForLevel(level + 1) - LevelCurve.xpForLevel(level)
    val levelProgress: Float
        get() = if (xpForNextLevel == 0) 0f else xpIntoLevel.toFloat() / xpForNextLevel

    val campaignLevel: Int get() = (levelStars.keys.maxOrNull() ?: 0) + 1
    val totalStars: Int get() = levelStars.values.sum()

    fun powerUpCount(powerUp: PowerUp): Int = powerUps[powerUp] ?: 0
}

/** Curva de XP: cada nível custa um pouco mais que o anterior. */
object LevelCurve {
    fun xpForLevel(level: Int): Int {
        if (level <= 1) return 0
        // 120, 270, 450, 660, ... crescimento suave
        val n = level - 1
        return 60 * n * (n + 3) / 2
    }

    fun levelFor(xp: Int): Int {
        var level = 1
        while (xpForLevel(level + 1) <= xp && level < 200) level++
        return level
    }

    /** Recompensa em gotas por subir de nível. */
    fun rewardForLevel(level: Int): Int = 50 + level * 10
}

/** Apelidos por faixa de nível, só para dar sabor. */
object PlayerRank {
    private val ranks = listOf(
        1 to "Gota Novata",
        3 to "Chuvisco",
        6 to "Aguaceiro",
        10 to "Temporal",
        15 to "Tempestade",
        22 to "Furacão",
        30 to "Lenda da Chuva"
    )

    fun titleFor(level: Int): String =
        ranks.last { level >= it.first }.second
}
