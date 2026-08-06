package com.kardiapulse.game.data

import com.kardiapulse.game.core.model.Difficulty
import com.kardiapulse.game.core.model.GameMode

/** Resultado de um duelo, do jeito que a economia precisa enxergar. */
data class MatchOutcome(
    val mode: GameMode,
    val difficulty: Difficulty,
    val won: Boolean,
    val rounds: Int,
    val bestChain: Int,
    val flips: Int,
    val hpLeft: Int,
    val endlessStreak: Int = 0
)

/** As recompensas ganhas em um duelo, prontas para a tela de vitória. */
data class Rewards(
    val xp: Int,
    val shards: Int,
    val crystals: Int,
    val perfect: Boolean
)

/**
 * Todas as contas de recompensa moram aqui, em um lugar só, para o balanceamento ser ajustável
 * sem caçar números espalhados pelo código.
 */
object Economy {

    const val REWARDED_AD_SHARDS = 90
    const val REWARDED_AD_CRYSTALS = 1
    const val DOUBLE_REWARD_MULTIPLIER = 2

    private val difficultyBonus = mapOf(
        Difficulty.FACIL to 1.0,
        Difficulty.NORMAL to 1.35,
        Difficulty.DIFICIL to 1.8,
        Difficulty.MESTRE to 2.5
    )

    private val modeBonus = mapOf(
        GameMode.DUELO to 1.0,
        GameMode.BLITZ to 1.25,
        GameMode.SOBREVIVENCIA to 1.4,
        GameMode.CAOS to 1.5,
        GameMode.DIARIO to 1.6
    )

    fun rewardsFor(outcome: MatchOutcome): Rewards {
        val diff = difficultyBonus[outcome.difficulty] ?: 1.0
        val mode = modeBonus[outcome.mode] ?: 1.0
        val multiplier = diff * mode

        val baseXp = if (outcome.won) 90 else 30
        val chainXp = outcome.bestChain * 6
        val flipXp = outcome.flips * 3
        val xp = ((baseXp + chainXp + flipXp) * multiplier).toInt()

        val baseShards = if (outcome.won) 60 else 18
        val streakShards = outcome.endlessStreak * 15
        val shards = ((baseShards + streakShards + outcome.bestChain * 4) * multiplier).toInt()

        // Vitória sem levar nenhum dano: o "perfeito" que rende cristal.
        val perfect = outcome.won && outcome.hpLeft >= 50
        val crystals = when {
            perfect -> 2
            outcome.won && outcome.difficulty == Difficulty.MESTRE -> 1
            outcome.won && outcome.mode == GameMode.DIARIO -> 1
            else -> 0
        }

        return Rewards(xp = xp, shards = shards, crystals = crystals, perfect = perfect)
    }

    /** Aplica as recompensas e as estatísticas do duelo ao perfil. */
    fun apply(profile: PlayerProfile, outcome: MatchOutcome, rewards: Rewards): PlayerProfile =
        profile.copy(
            xp = profile.xp + rewards.xp,
            shards = profile.shards + rewards.shards,
            crystals = profile.crystals + rewards.crystals,
            duelsPlayed = profile.duelsPlayed + 1,
            duelsWon = profile.duelsWon + if (outcome.won) 1 else 0,
            bestChain = maxOf(profile.bestChain, outcome.bestChain),
            totalFlips = profile.totalFlips + outcome.flips,
            endlessBest = maxOf(profile.endlessBest, outcome.endlessStreak),
            duelsSinceInterstitial = profile.duelsSinceInterstitial + 1
        )
}
