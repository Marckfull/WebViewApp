package com.chuvadeletras.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chuvadeletras.game.domain.model.GameMode
import com.chuvadeletras.game.domain.model.MatchSummary
import com.chuvadeletras.game.domain.model.PowerUp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Context.playerDataStore: DataStore<Preferences> by preferencesDataStore(name = "chuva_de_letras")

/** Nível mínimo para cada modo aparecer destravado. */
object ModeUnlocks {
    fun requiredLevel(mode: GameMode): Int = when (mode) {
        GameMode.CLASSICO -> 1
        GameMode.ZEN -> 1
        GameMode.DIARIO -> 2
        GameMode.TEMPESTADE -> 3
        GameMode.DILUVIO -> 5
    }

    fun isUnlocked(mode: GameMode, profile: PlayerProfile): Boolean =
        profile.level >= requiredLevel(mode)
}

/** O que aconteceu de bom logo depois de uma partida — a UI usa para comemorar. */
data class MatchOutcome(
    val summary: MatchSummary,
    val leveledUp: Boolean,
    val newLevel: Int,
    val levelUpCoins: Int,
    val unlockedMode: GameMode?,
    val completedMissions: List<MissionTemplate>,
    val unlockedAchievements: List<Achievement>
)

/**
 * Guarda tudo do jogador em um único JSON dentro do DataStore.
 * Um blob só evita migração de chave a cada campo novo.
 */
class PlayerRepository(private val context: Context) {

    private val key = stringPreferencesKey("profile")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val profile: Flow<PlayerProfile> = context.playerDataStore.data.map { prefs ->
        prefs[key]?.let { raw ->
            runCatching { json.decodeFromString<PlayerProfile>(raw) }.getOrElse { PlayerProfile() }
        } ?: PlayerProfile()
    }

    suspend fun current(): PlayerProfile = profile.first()

    suspend fun update(transform: (PlayerProfile) -> PlayerProfile): PlayerProfile {
        var result = PlayerProfile()
        context.playerDataStore.edit { prefs ->
            val currentProfile = prefs[key]
                ?.let { runCatching { json.decodeFromString<PlayerProfile>(it) }.getOrNull() }
                ?: PlayerProfile()
            result = transform(currentProfile)
            prefs[key] = json.encodeToString(result)
        }
        return result
    }

    // ----------------------------------------------------------------
    // Rotina de abertura do app
    // ----------------------------------------------------------------

    /**
     * Atualiza ofensiva e missões do dia. Deve rodar toda vez que o app abre.
     * Jogou ontem e hoje? Ofensiva sobe. Pulou um dia? Volta para 1.
     */
    suspend fun onAppOpened(): PlayerProfile = update { profile ->
        val today = LocalDate.now()
        val todayIso = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val newMissions = if (profile.missionsDate != todayIso) {
            Missions.forDate(todayIso).map { MissionProgress(it.id) }
        } else {
            profile.missions
        }

        val last = profile.lastPlayedDate.toLocalDateOrNull()
        val newStreak = when {
            last == null -> 1
            last == today -> profile.streak.coerceAtLeast(1)
            last.plusDays(1) == today -> profile.streak + 1
            else -> 1
        }

        // Perdeu um dia? O ciclo do baú recomeça.
        val lastReward = profile.lastRewardDate.toLocalDateOrNull()
        val rewardIndex = when {
            lastReward == null -> 0
            lastReward.plusDays(1) == today -> profile.rewardDayIndex
            lastReward == today -> profile.rewardDayIndex
            else -> 0
        }

        profile.copy(
            streak = newStreak,
            bestStreak = maxOf(profile.bestStreak, newStreak),
            lastPlayedDate = todayIso,
            missions = newMissions,
            missionsDate = todayIso,
            rewardDayIndex = rewardIndex
        )
    }

    // ----------------------------------------------------------------
    // Fim de partida
    // ----------------------------------------------------------------

    suspend fun registerMatch(summary: MatchSummary, campaignLevel: Int?): MatchOutcome {
        var outcome: MatchOutcome? = null

        update { profile ->
            val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val beforeLevel = profile.level
            val beforeUnlocked = GameMode.entries.filter { ModeUnlocks.isUnlocked(it, profile) }.toSet()

            val stats = profile.stats.copy(
                gamesPlayed = profile.stats.gamesPlayed + 1,
                gamesWon = profile.stats.gamesWon + if (summary.won) 1 else 0,
                wordsSolved = profile.stats.wordsSolved + summary.wordsSolved,
                lettersPlaced = profile.stats.lettersPlaced + summary.wordsSolved * 4,
                perfectGames = profile.stats.perfectGames + if (summary.perfect) 1 else 0,
                bestCombo = maxOf(profile.stats.bestCombo, summary.bestCombo),
                bestScore = maxOf(profile.stats.bestScore, summary.score),
                cellsLost = profile.stats.cellsLost + summary.lockedCells,
                dailiesCompleted = profile.stats.dailiesCompleted +
                    if (summary.won && summary.mode == GameMode.DIARIO) 1 else 0
            )

            val advancedMissions = profile.missions.map { progress ->
                val template = Missions.byId(progress.id) ?: return@map progress
                progress.copy(progress = Missions.advance(template, summary, progress.progress))
            }

            val stars = profile.levelStars.toMutableMap()
            if (campaignLevel != null && summary.won) {
                stars[campaignLevel] = maxOf(stars[campaignLevel] ?: 0, summary.stars)
            }

            var updated = profile.copy(
                coins = profile.coins + summary.coinsEarned,
                xp = profile.xp + summary.xpEarned,
                stats = stats,
                missions = advancedMissions,
                levelStars = stars,
                lastDailyDate = if (summary.won && summary.mode == GameMode.DIARIO) {
                    todayIso
                } else {
                    profile.lastDailyDate
                }
            )

            // Bônus por subir de nível
            val afterLevel = updated.level
            var levelCoins = 0
            if (afterLevel > beforeLevel) {
                for (lvl in (beforeLevel + 1)..afterLevel) levelCoins += LevelCurve.rewardForLevel(lvl)
                updated = updated.copy(coins = updated.coins + levelCoins)
            }

            val afterUnlocked = GameMode.entries.filter { ModeUnlocks.isUnlocked(it, updated) }.toSet()
            val newlyUnlocked = (afterUnlocked - beforeUnlocked).firstOrNull()
            updated = updated.copy(unlockedModes = afterUnlocked)

            val newAchievements = Achievements.all.filter {
                Achievements.isComplete(it, updated) && it.id !in profile.achievements
            }
            updated = updated.copy(achievements = updated.achievements + newAchievements.map { it.id })

            val finishedMissions = advancedMissions.mapNotNull { progress ->
                val template = Missions.byId(progress.id) ?: return@mapNotNull null
                val was = profile.missions.firstOrNull { it.id == progress.id }?.progress ?: 0
                if (progress.progress >= template.target && was < template.target) template else null
            }

            outcome = MatchOutcome(
                summary = summary,
                leveledUp = afterLevel > beforeLevel,
                newLevel = afterLevel,
                levelUpCoins = levelCoins,
                unlockedMode = newlyUnlocked,
                completedMissions = finishedMissions,
                unlockedAchievements = newAchievements
            )
            updated
        }

        return outcome ?: MatchOutcome(summary, false, 1, 0, null, emptyList(), emptyList())
    }

    // ----------------------------------------------------------------
    // Economia
    // ----------------------------------------------------------------

    suspend fun addCoins(amount: Int) = update { it.copy(coins = (it.coins + amount).coerceAtLeast(0)) }

    suspend fun grantPowerUp(powerUp: PowerUp, amount: Int = 1) = update { profile ->
        profile.copy(powerUps = profile.powerUps + (powerUp to profile.powerUpCount(powerUp) + amount))
    }

    /** Compra na loja. Devolve false se faltar gota. */
    suspend fun buyPowerUp(powerUp: PowerUp): Boolean {
        var bought = false
        update { profile ->
            if (profile.coins < powerUp.price) {
                profile
            } else {
                bought = true
                profile.copy(
                    coins = profile.coins - powerUp.price,
                    powerUps = profile.powerUps + (powerUp to profile.powerUpCount(powerUp) + 1)
                )
            }
        }
        return bought
    }

    /** Sincroniza o estoque de power-ups depois de uma partida que gastou itens. */
    suspend fun syncPowerUps(remaining: Map<PowerUp, Int>) = update { profile ->
        profile.copy(powerUps = profile.powerUps + remaining)
    }

    // ----------------------------------------------------------------
    // Baú diário, missões e conquistas
    // ----------------------------------------------------------------

    fun canClaimDaily(profile: PlayerProfile): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return profile.lastRewardDate != today
    }

    /**
     * Resgata o baú do dia. [doubled] vem de anúncio recompensado.
     * Devolve null se o baú de hoje já tinha sido aberto.
     */
    suspend fun claimDailyReward(doubled: Boolean): DailyReward? {
        var claimed: DailyReward? = null
        update { profile ->
            if (!canClaimDaily(profile)) return@update profile

            val reward = DailyRewards.rewardFor(profile.rewardDayIndex)
            val multiplier = if (doubled) 2 else 1
            val streakExtra = StreakBonus.bonusFor(profile.streak)
            claimed = reward

            val powerUps = reward.powerUp?.let { powerUp ->
                profile.powerUps + (powerUp to profile.powerUpCount(powerUp) + reward.powerUpAmount * multiplier)
            } ?: profile.powerUps

            profile.copy(
                coins = profile.coins + reward.coins * multiplier + streakExtra,
                powerUps = powerUps,
                lastRewardDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                rewardDayIndex = (profile.rewardDayIndex + 1) % DailyRewards.cycle.size,
                stats = if (doubled) {
                    profile.stats.copy(adsWatched = profile.stats.adsWatched + 1)
                } else {
                    profile.stats
                }
            )
        }
        return claimed
    }

    suspend fun claimMission(missionId: String): MissionTemplate? {
        val template = Missions.byId(missionId) ?: return null
        var granted: MissionTemplate? = null
        update { profile ->
            val progress = profile.missions.firstOrNull { it.id == missionId } ?: return@update profile
            if (progress.claimed || progress.progress < template.target) return@update profile
            granted = template
            profile.copy(
                coins = profile.coins + template.coins,
                xp = profile.xp + template.xp,
                missions = profile.missions.map {
                    if (it.id == missionId) it.copy(claimed = true) else it
                }
            )
        }
        return granted
    }

    suspend fun claimAchievement(achievementId: String): Achievement? {
        val achievement = Achievements.byId(achievementId) ?: return null
        var granted: Achievement? = null
        update { profile ->
            if (!Achievements.isComplete(achievement, profile)) return@update profile
            if (achievementId in profile.claimedAchievements) return@update profile
            granted = achievement
            profile.copy(
                coins = profile.coins + achievement.coins,
                claimedAchievements = profile.claimedAchievements + achievementId
            )
        }
        return granted
    }

    suspend fun registerAdWatched() = update { profile ->
        profile.copy(stats = profile.stats.copy(adsWatched = profile.stats.adsWatched + 1))
    }

    // ----------------------------------------------------------------
    // Preferências
    // ----------------------------------------------------------------

    suspend fun markTutorialDone() = update { it.copy(tutorialDone = true) }

    suspend fun resetTutorial() = update { it.copy(tutorialDone = false) }

    suspend fun setSound(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }

    suspend fun setVibration(enabled: Boolean) = update { it.copy(vibrationEnabled = enabled) }

    suspend fun markFreeCoinsCollected() =
        update { it.copy(lastFreeCoinsAt = System.currentTimeMillis()) }

    private fun String.toLocalDateOrNull(): LocalDate? =
        if (isBlank()) null else runCatching { LocalDate.parse(this) }.getOrNull()
}
