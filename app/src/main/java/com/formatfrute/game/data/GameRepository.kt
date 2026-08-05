package com.formatfrute.game.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.formatfrute.game.core.GameMode
import com.formatfrute.game.core.Power
import com.formatfrute.game.core.RecipeBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Retrato do jogador, observado pela UI inteira. */
data class Profile(
    val coins: Int = 250,
    val xp: Int = 0,
    val powers: Map<String, Int> = emptyMap(),
    val best: Map<String, Int> = emptyMap(),
    val highestFruit: Int = 0,
    val theme: String = BoardTheme.FEIRA.id,
    val unlockedThemes: Set<String> = setOf(BoardTheme.FEIRA.id),
    val music: Boolean = true,
    val sfx: Boolean = true,
    val vibration: Boolean = true,
    val notifications: Boolean = true,
    val tutorialDone: Boolean = false,
    val streak: Int = 0,
    val lastClaimDay: String = "",
    val totalMerges: Int = 0,
    val totalGames: Int = 0,
    val totalHarvests: Int = 0,
    val powersUsed: Int = 0,
    val missionsDay: String = "",
    val missionProgress: Map<String, Int> = emptyMap(),
    val missionsClaimed: Set<String> = emptySet(),
    val cestaClearedDay: String = "",
    val legalAccepted: Boolean = false,
    /** Estrelas por fase do Modo Receita: "12" -> 3. */
    val recipeStars: Map<String, Int> = emptyMap(),
    val passSeason: String = "",
    val passPoints: Int = 0,
    val passClaimedFree: Set<String> = emptySet(),
    val passClaimedPremium: Set<String> = emptySet(),
    val passTitle: String = "",
) {
    val level: Int get() = Ranks.levelFor(xp)
    val rank: String get() = Ranks.title(level)
    val boardTheme: BoardTheme get() = BoardTheme.byId(theme)

    fun powerCount(power: Power): Int = powers[power.id] ?: 0
    fun bestOf(mode: GameMode): Int = best[mode.id] ?: 0

    fun starsOf(recipe: Int): Int = recipeStars[recipe.toString()] ?: 0

    /** A fase 1 está sempre aberta; as outras pedem a anterior concluída. */
    fun isRecipeUnlocked(recipe: Int): Boolean = recipe <= 1 || starsOf(recipe - 1) > 0

    val recipeCleared: Int get() = recipeStars.count { it.value > 0 }
    val recipeStarTotal: Int get() = recipeStars.values.sum()

    /** Próxima fase a jogar — é nela que o mapa abre. */
    val nextRecipe: Int
        get() = (1..RecipeBook.TOTAL).firstOrNull { starsOf(it) == 0 } ?: RecipeBook.TOTAL

    val passTier: Int get() = SeasonPass.tierOf(passPoints)
}

/**
 * Toda a persistencia do jogo num lugar so. E de proposito sincrono e
 * pequeno: sao dezenas de bytes, e o jogo nunca pode travar por causa disso.
 */
class GameRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("format_frute", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(load())
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    val current: Profile get() = _profile.value

    // ------------------------------------------------------------ carga

    private fun load(): Profile = Profile(
        coins = prefs.getInt(K_COINS, 250),
        xp = prefs.getInt(K_XP, 0),
        powers = readCounts(K_POWERS),
        best = readCounts(K_BEST),
        highestFruit = prefs.getInt(K_HIGHEST, 0),
        theme = prefs.getString(K_THEME, BoardTheme.FEIRA.id) ?: BoardTheme.FEIRA.id,
        unlockedThemes = prefs.getStringSet(K_THEMES, null) ?: setOf(BoardTheme.FEIRA.id),
        music = prefs.getBoolean(K_MUSIC, true),
        sfx = prefs.getBoolean(K_SFX, true),
        vibration = prefs.getBoolean(K_VIBRATION, true),
        notifications = prefs.getBoolean(K_NOTIF, true),
        tutorialDone = prefs.getBoolean(K_TUTORIAL, false),
        streak = prefs.getInt(K_STREAK, 0),
        lastClaimDay = prefs.getString(K_LAST_CLAIM, "") ?: "",
        totalMerges = prefs.getInt(K_MERGES, 0),
        totalGames = prefs.getInt(K_GAMES, 0),
        totalHarvests = prefs.getInt(K_HARVESTS, 0),
        powersUsed = prefs.getInt(K_POWERS_USED, 0),
        missionsDay = prefs.getString(K_MISSION_DAY, "") ?: "",
        missionProgress = readCounts(K_MISSION_PROGRESS),
        missionsClaimed = prefs.getStringSet(K_MISSION_CLAIMED, null) ?: emptySet(),
        cestaClearedDay = prefs.getString(K_CESTA_DAY, "") ?: "",
        legalAccepted = prefs.getBoolean(K_LEGAL, false),
        recipeStars = readCounts(K_RECIPE_STARS),
        passSeason = prefs.getString(K_PASS_SEASON, "") ?: "",
        passPoints = prefs.getInt(K_PASS_POINTS, 0),
        passClaimedFree = prefs.getStringSet(K_PASS_FREE, null) ?: emptySet(),
        passClaimedPremium = prefs.getStringSet(K_PASS_PREMIUM, null) ?: emptySet(),
        passTitle = prefs.getString(K_PASS_TITLE, "") ?: "",
    )

    private fun readCounts(key: String): Map<String, Int> {
        val raw = prefs.getString(key, "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        return raw.split('|').mapNotNull {
            val parts = it.split(':')
            if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
        }.toMap()
    }

    private fun writeCounts(key: String, map: Map<String, Int>) {
        prefs.edit { putString(key, map.entries.joinToString("|") { "${it.key}:${it.value}" }) }
    }

    private fun update(block: (Profile) -> Profile) {
        _profile.value = block(_profile.value)
    }

    // ---------------------------------------------------------- economia

    fun addCoins(amount: Int) {
        if (amount == 0) return
        val next = (current.coins + amount).coerceAtLeast(0)
        prefs.edit { putInt(K_COINS, next) }
        update { it.copy(coins = next) }
    }

    fun spendCoins(amount: Int): Boolean {
        if (current.coins < amount) return false
        addCoins(-amount)
        return true
    }

    fun addXp(amount: Int): Boolean {
        val before = current.level
        val next = current.xp + amount
        prefs.edit { putInt(K_XP, next) }
        update { it.copy(xp = next) }
        return current.level > before
    }

    // ----------------------------------------------------------- poderes

    fun addPower(power: Power, amount: Int = 1) {
        val map = current.powers.toMutableMap()
        map[power.id] = (map[power.id] ?: 0) + amount
        writeCounts(K_POWERS, map)
        update { it.copy(powers = map) }
    }

    fun consumePower(power: Power): Boolean {
        val have = current.powerCount(power)
        if (have <= 0) return false
        val map = current.powers.toMutableMap()
        map[power.id] = have - 1
        writeCounts(K_POWERS, map)
        val used = current.powersUsed + 1
        prefs.edit { putInt(K_POWERS_USED, used) }
        update { it.copy(powers = map, powersUsed = used) }
        bumpMission(MissionKind.PODERES, 1)
        return true
    }

    fun buyPower(power: Power): Boolean {
        if (!spendCoins(power.price)) return false
        addPower(power)
        return true
    }

    // ------------------------------------------------------------ skins

    fun buyTheme(theme: BoardTheme): Boolean {
        if (theme.id in current.unlockedThemes) return true
        if (!spendCoins(theme.price)) return false
        unlockTheme(theme)
        return true
    }

    fun unlockTheme(theme: BoardTheme) {
        val set = current.unlockedThemes + theme.id
        prefs.edit { putStringSet(K_THEMES, set) }
        update { it.copy(unlockedThemes = set) }
    }

    fun selectTheme(theme: BoardTheme) {
        if (theme.id !in current.unlockedThemes) return
        prefs.edit { putString(K_THEME, theme.id) }
        update { it.copy(theme = theme.id) }
    }

    // --------------------------------------------------------- partidas

    fun registerGame(
        mode: GameMode,
        score: Int,
        highestFruit: Int,
        merges: Int,
        harvests: Int,
        won: Boolean,
    ): Boolean {
        var record = false
        if (score > current.bestOf(mode)) {
            val map = current.best.toMutableMap()
            map[mode.id] = score
            writeCounts(K_BEST, map)
            update { it.copy(best = map) }
            record = true
        }
        if (highestFruit > current.highestFruit) {
            prefs.edit { putInt(K_HIGHEST, highestFruit) }
            update { it.copy(highestFruit = highestFruit) }
        }
        val games = current.totalGames + 1
        val allMerges = current.totalMerges + merges
        val allHarvests = current.totalHarvests + harvests
        prefs.edit {
            putInt(K_GAMES, games)
            putInt(K_MERGES, allMerges)
            putInt(K_HARVESTS, allHarvests)
        }
        update { it.copy(totalGames = games, totalMerges = allMerges, totalHarvests = allHarvests) }

        bumpMission(MissionKind.PARTIDAS, 1)
        bumpMission(MissionKind.FUSOES, merges)
        bumpMission(MissionKind.COLHEITA, harvests)
        bumpMissionMax(MissionKind.PONTOS, score)
        bumpMissionMax(MissionKind.CHEGAR_NA_FRUTA, highestFruit)
        bumpMissionMode(mode)
        addPassPoints(SeasonPass.pointsForGame(score, merges, won))
        return record
    }

    // --------------------------------------------------------- modo receita

    /** Guarda a fase concluída. Só sobe estrela, nunca desce. */
    fun registerRecipe(number: Int, stars: Int): Boolean {
        val key = number.toString()
        val before = current.starsOf(number)
        if (stars <= before) {
            addPassPoints(SeasonPass.pointsForRecipe(stars))
            return false
        }
        val map = current.recipeStars.toMutableMap()
        map[key] = stars
        writeCounts(K_RECIPE_STARS, map)
        update { it.copy(recipeStars = map) }
        addPassPoints(SeasonPass.pointsForRecipe(stars))
        return before == 0
    }

    // ---------------------------------------------------- passe da feira

    /** Vira a temporada quando o mês muda: fichas e resgates recomeçam. */
    fun ensureSeasonFresh() {
        val season = SeasonPass.seasonId()
        if (current.passSeason == season) return
        prefs.edit {
            putString(K_PASS_SEASON, season)
            putInt(K_PASS_POINTS, 0)
            putStringSet(K_PASS_FREE, emptySet())
            putStringSet(K_PASS_PREMIUM, emptySet())
        }
        update {
            it.copy(
                passSeason = season,
                passPoints = 0,
                passClaimedFree = emptySet(),
                passClaimedPremium = emptySet(),
            )
        }
    }

    fun addPassPoints(points: Int) {
        if (points <= 0) return
        ensureSeasonFresh()
        val next = current.passPoints + points
        prefs.edit { putInt(K_PASS_POINTS, next) }
        update { it.copy(passPoints = next) }
    }

    fun isTierClaimed(tier: Int, premium: Boolean): Boolean {
        val key = tier.toString()
        return key in if (premium) current.passClaimedPremium else current.passClaimedFree
    }

    fun canClaimTier(tier: Int, premium: Boolean): Boolean =
        current.passTier >= tier && !isTierClaimed(tier, premium)

    /** Entrega a recompensa do degrau. Devolve false se não podia resgatar. */
    fun claimTier(tier: Int, premium: Boolean): Boolean {
        if (!canClaimTier(tier, premium)) return false
        val entry = SeasonPass.tier(tier)
        val reward = if (premium) entry.premium else entry.free

        when (reward.kind) {
            RewardKind.SEMENTES -> addCoins(reward.amount)
            RewardKind.PODER -> Power.byId(reward.powerId)?.let { addPower(it, reward.amount) }
            RewardKind.PELE -> unlockTheme(BoardTheme.byId(reward.themeId))
            RewardKind.TITULO -> {
                prefs.edit { putString(K_PASS_TITLE, reward.title) }
                update { it.copy(passTitle = reward.title) }
            }
        }

        val key = tier.toString()
        if (premium) {
            val set = current.passClaimedPremium + key
            prefs.edit { putStringSet(K_PASS_PREMIUM, set) }
            update { it.copy(passClaimedPremium = set) }
        } else {
            val set = current.passClaimedFree + key
            prefs.edit { putStringSet(K_PASS_FREE, set) }
            update { it.copy(passClaimedFree = set) }
        }
        return true
    }

    fun markCestaCleared() {
        val day = today()
        prefs.edit { putString(K_CESTA_DAY, day) }
        update { it.copy(cestaClearedDay = day) }
    }

    // --------------------------------------------------------- missoes

    fun missions(): List<Mission> = MissionFactory.daily(today())

    fun ensureMissionsFresh() {
        val day = today()
        if (current.missionsDay == day) return
        prefs.edit {
            putString(K_MISSION_DAY, day)
            putString(K_MISSION_PROGRESS, "")
            putStringSet(K_MISSION_CLAIMED, emptySet())
        }
        update { it.copy(missionsDay = day, missionProgress = emptyMap(), missionsClaimed = emptySet()) }
    }

    fun missionProgress(mission: Mission): Int = current.missionProgress[mission.key] ?: 0

    fun isMissionDone(mission: Mission): Boolean = missionProgress(mission) >= mission.target

    fun isMissionClaimed(mission: Mission): Boolean = mission.key in current.missionsClaimed

    fun claimMission(mission: Mission): Int {
        if (!isMissionDone(mission) || isMissionClaimed(mission)) return 0
        val claimed = current.missionsClaimed + mission.key
        prefs.edit { putStringSet(K_MISSION_CLAIMED, claimed) }
        update { it.copy(missionsClaimed = claimed) }
        addCoins(mission.reward)
        addXp(mission.reward / 2)
        addPassPoints(SeasonPass.POINTS_PER_MISSION)
        return mission.reward
    }

    private fun setMissionProgress(mission: Mission, value: Int) {
        val map = current.missionProgress.toMutableMap()
        map[mission.key] = value.coerceAtMost(mission.target)
        writeCounts(K_MISSION_PROGRESS, map)
        update { it.copy(missionProgress = map) }
    }

    fun bumpMission(kind: MissionKind, amount: Int) {
        if (amount <= 0) return
        ensureMissionsFresh()
        missions().filter { it.kind == kind }.forEach {
            setMissionProgress(it, missionProgress(it) + amount)
        }
    }

    private fun bumpMissionMax(kind: MissionKind, value: Int) {
        ensureMissionsFresh()
        missions().filter { it.kind == kind }.forEach {
            if (value > missionProgress(it)) setMissionProgress(it, value)
        }
    }

    private fun bumpMissionMode(mode: GameMode) {
        ensureMissionsFresh()
        missions().filter { it.kind == MissionKind.MODO && it.extra == mode.id }.forEach {
            setMissionProgress(it, it.target)
        }
    }

    // -------------------------------------------------- premio diario

    /** Quantos dias seguidos, ja considerando a virada de hoje. */
    fun pendingDailyStreak(): Int {
        val day = today()
        if (current.lastClaimDay == day) return 0
        val yesterday = dayString(System.currentTimeMillis() - DAY_MS)
        return if (current.lastClaimDay == yesterday) {
            (current.streak % DailyRewards.table.size) + 1
        } else {
            1
        }
    }

    fun claimDaily(): Int {
        val streakDay = pendingDailyStreak()
        if (streakDay == 0) return 0
        val reward = DailyRewards.rewardFor(streakDay)
        val day = today()
        prefs.edit {
            putString(K_LAST_CLAIM, day)
            putInt(K_STREAK, streakDay)
        }
        update { it.copy(lastClaimDay = day, streak = streakDay) }
        addCoins(reward)
        return reward
    }

    // ------------------------------------------------------ ajustes

    fun setMusic(on: Boolean) {
        prefs.edit { putBoolean(K_MUSIC, on) }
        update { it.copy(music = on) }
    }

    fun setSfx(on: Boolean) {
        prefs.edit { putBoolean(K_SFX, on) }
        update { it.copy(sfx = on) }
    }

    fun setVibration(on: Boolean) {
        prefs.edit { putBoolean(K_VIBRATION, on) }
        update { it.copy(vibration = on) }
    }

    fun setNotifications(on: Boolean) {
        prefs.edit { putBoolean(K_NOTIF, on) }
        update { it.copy(notifications = on) }
    }

    fun setTutorialDone(done: Boolean) {
        prefs.edit { putBoolean(K_TUTORIAL, done) }
        update { it.copy(tutorialDone = done) }
    }

    fun acceptLegal() {
        prefs.edit { putBoolean(K_LEGAL, true) }
        update { it.copy(legalAccepted = true) }
    }

    fun markLastPlayed() {
        prefs.edit { putLong(K_LAST_PLAYED, System.currentTimeMillis()) }
    }

    fun lastPlayed(): Long = prefs.getLong(K_LAST_PLAYED, 0L)

    companion object {
        private const val K_COINS = "coins"
        private const val K_XP = "xp"
        private const val K_POWERS = "powers"
        private const val K_BEST = "best"
        private const val K_HIGHEST = "highest"
        private const val K_THEME = "theme"
        private const val K_THEMES = "themes"
        private const val K_MUSIC = "music"
        private const val K_SFX = "sfx"
        private const val K_VIBRATION = "vibration"
        private const val K_NOTIF = "notifications"
        private const val K_TUTORIAL = "tutorial"
        private const val K_STREAK = "streak"
        private const val K_LAST_CLAIM = "last_claim"
        private const val K_MERGES = "merges"
        private const val K_GAMES = "games"
        private const val K_HARVESTS = "harvests"
        private const val K_POWERS_USED = "powers_used"
        private const val K_MISSION_DAY = "mission_day"
        private const val K_MISSION_PROGRESS = "mission_progress"
        private const val K_MISSION_CLAIMED = "mission_claimed"
        private const val K_CESTA_DAY = "cesta_day"
        private const val K_LEGAL = "legal"
        private const val K_LAST_PLAYED = "last_played"
        private const val K_RECIPE_STARS = "recipe_stars"
        private const val K_PASS_SEASON = "pass_season"
        private const val K_PASS_POINTS = "pass_points"
        private const val K_PASS_FREE = "pass_free"
        private const val K_PASS_PREMIUM = "pass_premium"
        private const val K_PASS_TITLE = "pass_title"

        private const val DAY_MS = 24 * 60 * 60 * 1000L

        @Volatile
        private var instance: GameRepository? = null

        fun get(context: Context): GameRepository =
            instance ?: synchronized(this) {
                instance ?: GameRepository(context).also { instance = it }
            }

        fun dayString(millis: Long): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

        fun today(): String = dayString(System.currentTimeMillis())
    }
}
