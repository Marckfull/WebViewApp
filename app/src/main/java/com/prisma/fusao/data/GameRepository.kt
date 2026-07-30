package com.prisma.fusao.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prisma.fusao.core.Campaign
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prisma_player")

/**
 * Guarda o estado do jogador como um único JSON. É simples de migrar e evita
 * espalhar dezenas de chaves soltas pelo DataStore.
 */
class GameRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val state: Flow<PlayerState> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_STATE]
        val parsed = if (raw.isNullOrBlank()) {
            PlayerState()
        } else {
            runCatching { json.decodeFromString<PlayerState>(raw) }.getOrElse { PlayerState() }
        }
        parsed.withRegeneratedLives(System.currentTimeMillis())
    }

    suspend fun update(transform: (PlayerState) -> PlayerState) {
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_STATE]
            val current = if (raw.isNullOrBlank()) {
                PlayerState()
            } else {
                runCatching { json.decodeFromString<PlayerState>(raw) }.getOrElse { PlayerState() }
            }
            val regenerated = current.withRegeneratedLives(System.currentTimeMillis())
            prefs[KEY_STATE] = json.encodeToString(transform(regenerated))
        }
    }

    // ------------------------------------------------------------ aceite legal

    suspend fun acceptTerms() = update { it.copy(acceptedTermsVersion = CURRENT_TERMS_VERSION) }

    suspend fun completeTutorial() = update { it.copy(tutorialCompleted = true) }

    // ------------------------------------------------------------------ vidas

    /** Consome uma vida ao iniciar uma fase. Devolve false quando não há vidas. */
    suspend fun consumeLife(): Boolean {
        var ok = false
        update { state ->
            val now = System.currentTimeMillis()
            when {
                state.hasInfiniteLives(now) -> { ok = true; state }
                state.lives > 0 -> {
                    ok = true
                    state.copy(
                        lives = state.lives - 1,
                        lifeRefillStartedAt =
                            if (state.lifeRefillStartedAt == 0L) now else state.lifeRefillStartedAt,
                    )
                }
                else -> state
            }
        }
        return ok
    }

    suspend fun grantLives(amount: Int) = update {
        it.copy(lives = (it.lives + amount).coerceAtMost(PlayerState.MAX_LIVES))
    }

    suspend fun grantInfiniteLives(durationMs: Long) = update {
        val now = System.currentTimeMillis()
        val base = maxOf(now, it.infiniteLivesUntil)
        it.copy(infiniteLivesUntil = base + durationMs, lives = PlayerState.MAX_LIVES)
    }

    // ---------------------------------------------------------------- economia

    suspend fun grantCoins(amount: Int) = update { it.copy(coins = it.coins + amount) }

    /** Gasta moedas se houver saldo. Devolve false quando não dá. */
    suspend fun spendCoins(amount: Int): Boolean {
        var ok = false
        update { state ->
            if (state.coins >= amount) {
                ok = true
                state.copy(coins = state.coins - amount)
            } else {
                state
            }
        }
        return ok
    }

    suspend fun grantBooster(booster: Booster, amount: Int = 1) = update { state ->
        val current = state.boosterCount(booster)
        state.copy(boosters = state.boosters + (booster.id to current + amount))
    }

    /** Consome um booster do inventário. */
    suspend fun consumeBooster(booster: Booster): Boolean {
        var ok = false
        update { state ->
            val current = state.boosterCount(booster)
            if (current > 0) {
                ok = true
                state.copy(boosters = state.boosters + (booster.id to current - 1))
            } else {
                state
            }
        }
        return ok
    }

    suspend fun buyBooster(booster: Booster): Boolean {
        var ok = false
        update { state ->
            if (state.coins >= booster.price) {
                ok = true
                state.copy(
                    coins = state.coins - booster.price,
                    boosters = state.boosters + (booster.id to state.boosterCount(booster) + 1),
                )
            } else {
                state
            }
        }
        return ok
    }

    // ---------------------------------------------------------------- progresso

    /** Registra o fim de uma fase vencida e devolve as moedas ganhas. */
    suspend fun completeLevel(level: Int, score: Int, stars: Int, fusions: Int, gems: Int): Int {
        var reward = 0
        // Tudo dentro de um único update: ler antes e escrever depois abriria espaço
        // para duas telas gravarem por cima uma da outra.
        update { state ->
            val previous = state.levels[level] ?: LevelProgress()
            val previousStars = previous.stars
            reward = 40 + stars * 30 + if (previousStars == 0) 60 else 0
            val updated = LevelProgress(
                stars = maxOf(previous.stars, stars),
                bestScore = maxOf(previous.bestScore, score),
            )
            state.copy(
                levels = state.levels + (level to updated),
                highestUnlocked = maxOf(
                    state.highestUnlocked,
                    (level + 1).coerceAtMost(Campaign.LEVEL_COUNT),
                ),
                coins = state.coins + reward,
                xp = state.xp + score / 10,
                totalFusions = state.totalFusions + fusions,
                totalGemsCleared = state.totalGemsCleared + gems,
            ).let { advanced ->
                advanced.copy(
                    missions = advanceMissions(
                        advanced.missions,
                        mapOf(
                            MissionMetric.LEVELS_WON to 1,
                            MissionMetric.FUSIONS to fusions,
                            MissionMetric.GEMS_CLEARED to gems,
                            MissionMetric.STARS_EARNED to (stars - previousStars).coerceAtLeast(0),
                        ),
                    ),
                )
            }
        }
        return reward
    }

    /** Atualiza missões durante a partida, sem esperar o fim da fase. */
    suspend fun trackMissions(deltas: Map<MissionMetric, Int>) = update {
        it.copy(missions = advanceMissions(it.missions, deltas))
    }

    private fun advanceMissions(
        missions: List<Mission>,
        deltas: Map<MissionMetric, Int>,
    ): List<Mission> = missions.map { mission ->
        val metric = MissionMetric.entries.firstOrNull { it.name == mission.id }
        val delta = metric?.let { deltas[it] } ?: 0
        if (delta <= 0 || mission.complete) mission
        else mission.copy(progress = (mission.progress + delta).coerceAtMost(mission.target))
    }

    // ------------------------------------------------------- missões e diárias

    /** Gera missões novas quando o dia virou. Idempotente dentro do mesmo dia. */
    suspend fun refreshDailyContent() = update { state ->
        val today = epochDay()
        if (state.missionsDay == today) return@update state
        val rng = Random(today)
        state.copy(
            missionsDay = today,
            missions = generateMissions(rng),
            rewardedWatchedToday = if (state.adCountersDay == today) state.rewardedWatchedToday else 0,
            adCountersDay = today,
        )
    }

    private fun generateMissions(rng: Random): List<Mission> {
        val pool = listOf(
            Mission(MissionMetric.LEVELS_WON.name, "Vença 3 fases", 3, reward = 150),
            Mission(MissionMetric.FUSIONS.name, "Faça 8 fusões", 8, reward = 200),
            Mission(MissionMetric.GEMS_CLEARED.name, "Estoure 400 gemas", 400, reward = 180),
            Mission(MissionMetric.STARS_EARNED.name, "Ganhe 4 estrelas", 4, reward = 220),
            Mission(MissionMetric.PRISMS_CREATED.name, "Crie 3 prismas", 3, reward = 250),
        )
        return pool.shuffled(rng).take(3)
    }

    suspend fun claimMission(missionId: String): Int {
        var reward = 0
        update { state ->
            val mission = state.missions.firstOrNull { it.id == missionId }
            if (mission == null || !mission.complete || mission.claimed) return@update state
            reward = mission.reward
            state.copy(
                coins = state.coins + mission.reward,
                missions = state.missions.map {
                    if (it.id == missionId) it.copy(claimed = true) else it
                },
            )
        }
        return reward
    }

    /** Prêmio diário em ciclo de 7 dias; faltar um dia zera a sequência. */
    suspend fun claimDailyReward(): DailyReward? {
        var granted: DailyReward? = null
        update { state ->
            val today = epochDay()
            if (state.lastDailyClaimDay == today) return@update state
            val streak = if (state.lastDailyClaimDay == today - 1) {
                (state.dailyStreak % 7) + 1
            } else {
                1
            }
            val reward = dailyRewardFor(streak)
            granted = reward
            var next = state.copy(
                dailyStreak = streak,
                lastDailyClaimDay = today,
                coins = state.coins + reward.coins,
            )
            reward.booster?.let {
                next = next.copy(boosters = next.boosters + (it.id to next.boosterCount(it) + 1))
            }
            if (reward.infiniteLivesMinutes > 0) {
                val now = System.currentTimeMillis()
                val base = maxOf(now, next.infiniteLivesUntil)
                next = next.copy(
                    infiniteLivesUntil = base + reward.infiniteLivesMinutes * 60_000L,
                    lives = PlayerState.MAX_LIVES,
                )
            }
            next
        }
        return granted
    }

    fun canClaimDaily(state: PlayerState): Boolean = state.lastDailyClaimDay != epochDay()

    // -------------------------------------------------------------- anúncios

    suspend fun registerRewardedWatched() = update { state ->
        val today = epochDay()
        state.copy(
            adCountersDay = today,
            rewardedWatchedToday =
                if (state.adCountersDay == today) state.rewardedWatchedToday + 1 else 1,
        )
    }

    suspend fun registerInterstitialShown() = update {
        it.copy(lastInterstitialAt = System.currentTimeMillis())
    }

    // -------------------------------------------------------------- ajustes

    suspend fun setMusicEnabled(enabled: Boolean) = update { it.copy(musicEnabled = enabled) }

    suspend fun setSfxEnabled(enabled: Boolean) = update { it.copy(sfxEnabled = enabled) }

    suspend fun setVibrationEnabled(enabled: Boolean) = update { it.copy(vibrationEnabled = enabled) }

    /** Apaga tudo — exposto nos ajustes por transparência (LGPD/GDPR). */
    suspend fun eraseAllData() {
        context.dataStore.edit { it.clear() }
    }

    companion object {
        const val CURRENT_TERMS_VERSION = 1
        private val KEY_STATE = stringPreferencesKey("player_state_json")

        fun epochDay(): Long = System.currentTimeMillis() / 86_400_000L

        fun dailyRewardFor(day: Int): DailyReward = when (day) {
            1 -> DailyReward(day, coins = 100)
            2 -> DailyReward(day, coins = 150)
            3 -> DailyReward(day, coins = 0, booster = Booster.HAMMER)
            4 -> DailyReward(day, coins = 250)
            5 -> DailyReward(day, coins = 0, booster = Booster.BOMB)
            6 -> DailyReward(day, coins = 400)
            else -> DailyReward(day, coins = 500, booster = Booster.COLOR_BLAST, infiniteLivesMinutes = 30)
        }
    }
}

data class DailyReward(
    val day: Int,
    val coins: Int,
    val booster: Booster? = null,
    val infiniteLivesMinutes: Int = 0,
)
