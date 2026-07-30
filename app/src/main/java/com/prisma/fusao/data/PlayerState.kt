package com.prisma.fusao.data

import kotlinx.serialization.Serializable

/** Itens que o jogador pode acumular e gastar dentro de uma fase. */
enum class Booster(val id: String, val price: Int) {
    /** Remove uma peça qualquer. */
    HAMMER("martelo", 300),

    /** Estoura uma área 3x3. */
    BOMB("bomba", 450),

    /** Remove todas as gemas de uma cor. */
    COLOR_BLAST("raio", 600),

    /** Redistribui o tabuleiro. */
    SHUFFLE("embaralhar", 200),

    /** +5 jogadas, usável quando as jogadas acabam. */
    EXTRA_MOVES("jogadas", 500),
}

@Serializable
data class LevelProgress(
    val stars: Int = 0,
    val bestScore: Int = 0,
)

/** Uma missão diária. O progresso zera junto com o dia. */
@Serializable
data class Mission(
    val id: String,
    val description: String,
    val target: Int,
    val progress: Int = 0,
    val reward: Int,
    val claimed: Boolean = false,
) {
    val complete: Boolean get() = progress >= target
}

/** Tipos de evento que alimentam as missões. */
enum class MissionMetric { LEVELS_WON, FUSIONS, GEMS_CLEARED, STARS_EARNED, PRISMS_CREATED }

/**
 * Tudo que sobrevive ao fechamento do app. Um único objeto serializado em JSON
 * mantém a persistência simples e permite migração por versão.
 */
@Serializable
data class PlayerState(
    /** Versão dos termos que o jogador aceitou. Se subir, pedimos aceite de novo. */
    val acceptedTermsVersion: Int = 0,
    val tutorialCompleted: Boolean = false,

    val highestUnlocked: Int = 1,
    val levels: Map<Int, LevelProgress> = emptyMap(),

    val coins: Int = 300,
    val lives: Int = MAX_LIVES,
    /** Momento (epoch ms) em que a contagem da próxima vida começou. */
    val lifeRefillStartedAt: Long = 0L,
    val infiniteLivesUntil: Long = 0L,

    val boosters: Map<String, Int> = mapOf(
        Booster.HAMMER.id to 2,
        Booster.SHUFFLE.id to 2,
        Booster.BOMB.id to 1,
    ),

    val xp: Int = 0,
    val totalFusions: Int = 0,
    val totalPrisms: Int = 0,
    val totalGemsCleared: Int = 0,

    val dailyStreak: Int = 0,
    /** Dia (epoch day) do último prêmio diário resgatado. */
    val lastDailyClaimDay: Long = -1L,
    val missions: List<Mission> = emptyList(),
    val missionsDay: Long = -1L,

    /** Contadores de anúncio do dia, para respeitar os limites que definimos. */
    val rewardedWatchedToday: Int = 0,
    val adCountersDay: Long = -1L,
    val lastInterstitialAt: Long = 0L,

    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val removeAdsPurchased: Boolean = false,
) {
    fun stars(level: Int): Int = levels[level]?.stars ?: 0

    fun bestScore(level: Int): Int = levels[level]?.bestScore ?: 0

    val totalStars: Int get() = levels.values.sumOf { it.stars }

    fun boosterCount(booster: Booster): Int = boosters[booster.id] ?: 0

    /** Nível do jogador: cresce cada vez mais devagar, como manda a tradição. */
    val playerLevel: Int get() = playerLevelFor(xp)

    val xpIntoLevel: Int get() = xp - xpForLevel(playerLevel)

    val xpForNextLevel: Int get() = xpForLevel(playerLevel + 1) - xpForLevel(playerLevel)

    fun hasInfiniteLives(now: Long): Boolean = infiniteLivesUntil > now

    /**
     * Vidas regeneram com o relógio, então o cálculo acontece na leitura em vez de
     * depender de um timer rodando.
     */
    fun withRegeneratedLives(now: Long): PlayerState {
        if (hasInfiniteLives(now)) return this
        if (lives >= MAX_LIVES) {
            return if (lifeRefillStartedAt == 0L) this else copy(lifeRefillStartedAt = 0L)
        }
        val start = if (lifeRefillStartedAt == 0L) now else lifeRefillStartedAt
        val elapsed = now - start
        if (elapsed < LIFE_REFILL_MS) {
            return if (lifeRefillStartedAt == start) this else copy(lifeRefillStartedAt = start)
        }
        val recovered = (elapsed / LIFE_REFILL_MS).toInt()
        val newLives = (lives + recovered).coerceAtMost(MAX_LIVES)
        val leftover = elapsed % LIFE_REFILL_MS
        return copy(
            lives = newLives,
            lifeRefillStartedAt = if (newLives >= MAX_LIVES) 0L else now - leftover,
        )
    }

    /** Milissegundos até a próxima vida, ou null quando não há espera. */
    fun millisToNextLife(now: Long): Long? {
        if (hasInfiniteLives(now) || lives >= MAX_LIVES) return null
        val start = if (lifeRefillStartedAt == 0L) now else lifeRefillStartedAt
        return (LIFE_REFILL_MS - (now - start)).coerceAtLeast(0L)
    }

    companion object {
        const val MAX_LIVES = 5
        const val LIFE_REFILL_MS = 20 * 60 * 1000L

        /** Limite diário de vídeos recompensados — ver docs/ANUNCIOS.md. */
        const val MAX_REWARDED_PER_DAY = 12

        /** Intervalo mínimo entre intersticiais. */
        const val INTERSTITIAL_MIN_INTERVAL_MS = 3 * 60 * 1000L

        /** Antes desta fase nenhum intersticial é exibido. */
        const val INTERSTITIAL_FIRST_LEVEL = 6

        fun xpForLevel(level: Int): Int {
            var total = 0
            for (l in 1 until level) total += 500 + (l - 1) * 250
            return total
        }

        fun playerLevelFor(xp: Int): Int {
            var level = 1
            while (xpForLevel(level + 1) <= xp) level++
            return level
        }
    }
}
