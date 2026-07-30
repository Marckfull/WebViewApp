package com.neuroflip.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "neuroflip_player")

data class PlayerState(
    val neurons: Int = 60,
    val unlockedLevel: Int = 1,
    val starsByLevel: Map<Int, Int> = emptyMap(),
    val blitzBest: Int = 0,
    val campaignScore: Int = 0,
    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val themeId: String = "cortex",
    val unlockedThemes: Set<String> = setOf("cortex"),
    val legalAccepted: Boolean = false,
    val personalizedAds: Boolean = true,
    val scanCount: Int = 2,
    val wildCount: Int = 1,
    val timeCount: Int = 1,
    val lastDailyClaimMs: Long = 0L,
    val dailyStreak: Int = 0,
    val tutorialSeen: Boolean = false
) {
    val totalStars: Int get() = starsByLevel.values.sum()
}

class PlayerRepository(private val context: Context) {

    private object Keys {
        val NEURONS = intPreferencesKey("neurons")
        val UNLOCKED_LEVEL = intPreferencesKey("unlocked_level")
        val STARS = stringPreferencesKey("stars_map")
        val BLITZ_BEST = intPreferencesKey("blitz_best")
        val CAMPAIGN_SCORE = intPreferencesKey("campaign_score")
        val MUSIC = booleanPreferencesKey("music")
        val SFX = booleanPreferencesKey("sfx")
        val HAPTICS = booleanPreferencesKey("haptics")
        val THEME = stringPreferencesKey("theme")
        val UNLOCKED_THEMES = stringPreferencesKey("unlocked_themes")
        val LEGAL = booleanPreferencesKey("legal_accepted")
        val PERSONALIZED_ADS = booleanPreferencesKey("personalized_ads")
        val SCAN = intPreferencesKey("pu_scan")
        val WILD = intPreferencesKey("pu_wild")
        val TIME = intPreferencesKey("pu_time")
        val DAILY_MS = longPreferencesKey("daily_ms")
        val STREAK = intPreferencesKey("daily_streak")
        val TUTORIAL = booleanPreferencesKey("tutorial_seen")
    }

    val state: Flow<PlayerState> = context.dataStore.data.map { p ->
        val default = PlayerState()
        PlayerState(
            neurons = p[Keys.NEURONS] ?: default.neurons,
            unlockedLevel = p[Keys.UNLOCKED_LEVEL] ?: default.unlockedLevel,
            starsByLevel = decodeStars(p[Keys.STARS]),
            blitzBest = p[Keys.BLITZ_BEST] ?: 0,
            campaignScore = p[Keys.CAMPAIGN_SCORE] ?: 0,
            musicEnabled = p[Keys.MUSIC] ?: true,
            sfxEnabled = p[Keys.SFX] ?: true,
            hapticsEnabled = p[Keys.HAPTICS] ?: true,
            themeId = p[Keys.THEME] ?: default.themeId,
            unlockedThemes = decodeSet(p[Keys.UNLOCKED_THEMES], default.unlockedThemes),
            legalAccepted = p[Keys.LEGAL] ?: false,
            personalizedAds = p[Keys.PERSONALIZED_ADS] ?: true,
            scanCount = p[Keys.SCAN] ?: default.scanCount,
            wildCount = p[Keys.WILD] ?: default.wildCount,
            timeCount = p[Keys.TIME] ?: default.timeCount,
            lastDailyClaimMs = p[Keys.DAILY_MS] ?: 0L,
            dailyStreak = p[Keys.STREAK] ?: 0,
            tutorialSeen = p[Keys.TUTORIAL] ?: false
        )
    }

    // ------------------------------------------------------------------ mutações

    suspend fun addNeurons(amount: Int) = context.dataStore.edit { p ->
        val current = p[Keys.NEURONS] ?: PlayerState().neurons
        p[Keys.NEURONS] = (current + amount).coerceAtLeast(0)
    }

    /** Gasta neurônios se houver saldo. Devolve false quando não dá. */
    suspend fun spendNeurons(amount: Int): Boolean {
        var ok = false
        context.dataStore.edit { p ->
            val current = p[Keys.NEURONS] ?: PlayerState().neurons
            if (current >= amount) {
                p[Keys.NEURONS] = current - amount
                ok = true
            }
        }
        return ok
    }

    suspend fun recordLevelResult(level: Int, stars: Int, score: Int) =
        context.dataStore.edit { p ->
            val stored = decodeStars(p[Keys.STARS]).toMutableMap()
            val best = maxOf(stored[level] ?: 0, stars)
            stored[level] = best
            p[Keys.STARS] = encodeStars(stored)
            p[Keys.CAMPAIGN_SCORE] = (p[Keys.CAMPAIGN_SCORE] ?: 0) + score
            val unlocked = p[Keys.UNLOCKED_LEVEL] ?: 1
            if (level + 1 > unlocked) {
                p[Keys.UNLOCKED_LEVEL] = minOf(level + 1, 30)
            }
        }

    suspend fun recordBlitz(score: Int) = context.dataStore.edit { p ->
        if (score > (p[Keys.BLITZ_BEST] ?: 0)) p[Keys.BLITZ_BEST] = score
    }

    suspend fun setMusic(enabled: Boolean) = context.dataStore.edit { it[Keys.MUSIC] = enabled }
    suspend fun setSfx(enabled: Boolean) = context.dataStore.edit { it[Keys.SFX] = enabled }
    suspend fun setHaptics(enabled: Boolean) = context.dataStore.edit { it[Keys.HAPTICS] = enabled }
    suspend fun setTheme(id: String) = context.dataStore.edit { it[Keys.THEME] = id }
    suspend fun acceptLegal() = context.dataStore.edit { it[Keys.LEGAL] = true }
    suspend fun setTutorialSeen() = context.dataStore.edit { it[Keys.TUTORIAL] = true }
    suspend fun setPersonalizedAds(enabled: Boolean) =
        context.dataStore.edit { it[Keys.PERSONALIZED_ADS] = enabled }

    suspend fun unlockTheme(id: String) = context.dataStore.edit { p ->
        val current = decodeSet(p[Keys.UNLOCKED_THEMES], setOf("cortex"))
        p[Keys.UNLOCKED_THEMES] = (current + id).joinToString(",")
    }

    suspend fun addPowerUp(kind: PowerUp, amount: Int) = context.dataStore.edit { p ->
        val key = kind.key()
        // Sem a chave gravada, o jogador ainda tem a quantidade inicial de brinde.
        val current = p[key] ?: kind.startingAmount()
        p[key] = (current + amount).coerceAtLeast(0)
    }

    suspend fun consumePowerUp(kind: PowerUp): Boolean {
        var ok = false
        context.dataStore.edit { p ->
            val key = kind.key()
            val current = p[key] ?: kind.startingAmount()
            if (current > 0) {
                p[key] = current - 1
                ok = true
            } else {
                p[key] = 0
            }
        }
        return ok
    }

    private fun PowerUp.startingAmount(): Int = PlayerState().let {
        when (this) {
            PowerUp.SCAN -> it.scanCount
            PowerUp.WILD -> it.wildCount
            PowerUp.TIME -> it.timeCount
        }
    }

    suspend fun claimDaily(nowMs: Long, reward: Int, streak: Int) =
        context.dataStore.edit { p ->
            p[Keys.DAILY_MS] = nowMs
            p[Keys.STREAK] = streak
            val current = p[Keys.NEURONS] ?: PlayerState().neurons
            p[Keys.NEURONS] = current + reward
        }

    private fun PowerUp.key() = when (this) {
        PowerUp.SCAN -> Keys.SCAN
        PowerUp.WILD -> Keys.WILD
        PowerUp.TIME -> Keys.TIME
    }

    // ------------------------------------------------------------------ codecs

    private fun encodeStars(map: Map<Int, Int>) =
        map.entries.joinToString(",") { "${it.key}:${it.value}" }

    private fun decodeStars(raw: String?): Map<Int, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            val level = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val stars = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            level to stars
        }.toMap()
    }

    private fun decodeSet(raw: String?, fallback: Set<String>): Set<String> {
        if (raw.isNullOrBlank()) return fallback
        return raw.split(",").filter { it.isNotBlank() }.toSet().ifEmpty { fallback }
    }
}

enum class PowerUp(val label: String, val glyph: String, val price: Int, val description: String) {
    SCAN("Scan", "🔍", 40, "Revela o tabuleiro inteiro por 2 segundos."),
    WILD("Curinga", "⧉", 70, "Resolve um par automaticamente."),
    TIME("Tempo", "⏱", 50, "+20 segundos no cronômetro.")
}
