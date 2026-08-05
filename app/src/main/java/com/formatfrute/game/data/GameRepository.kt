package com.formatfrute.game.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.formatfrute.game.core.GameMode
import com.formatfrute.game.core.Power
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        coins = readGuarded(K_COINS, 250),
        xp = readGuarded(K_XP, 0),
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
        dailyRecipeDay = prefs.getString(K_DAILY_RECIPE, "") ?: "",
        legalAccepted = prefs.getBoolean(K_LEGAL, false),
        achievementsClaimed = prefs.getStringSet(K_ACHIEVEMENTS, null) ?: emptySet(),
        recipeStars = readCounts(K_RECIPE_STARS),
        passSeason = prefs.getString(K_PASS_SEASON, "") ?: "",
        passPoints = readGuarded(K_PASS_POINTS, 0),
        passClaimedFree = prefs.getStringSet(K_PASS_FREE, null) ?: emptySet(),
        passClaimedPremium = prefs.getStringSet(K_PASS_PREMIUM, null) ?: emptySet(),
        passTitle = prefs.getString(K_PASS_TITLE, "") ?: "",
    )

    // Os valores que alguém teria vontade de editar passam pelo Vault.
    private fun readCounts(key: String): Map<String, Int> {
        val raw = Vault.decode(prefs.getString(key, "")).orEmpty()
        if (raw.isBlank()) return emptyMap()
        return raw.split('|').mapNotNull {
            val parts = it.split(':')
            if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
        }.toMap()
    }

    private fun writeCounts(key: String, map: Map<String, Int>) {
        val raw = map.entries.joinToString("|") { "${it.key}:${it.value}" }
        prefs.edit { putString(key, Vault.encode(raw)) }
    }

    private fun readGuarded(key: String, fallback: Int): Int =
        Vault.decode(prefs.getString(key, null))?.toIntOrNull() ?: fallback

    private fun writeGuarded(key: String, value: Int) {
        prefs.edit { putString(key, Vault.encode(value.toString())) }
    }

    private fun update(block: (Profile) -> Profile) {
        _profile.value = block(_profile.value)
    }

    // ---------------------------------------------------------- economia

    fun addCoins(amount: Int) {
        if (amount == 0) return
        val next = (current.coins + amount).coerceAtLeast(0)
        writeGuarded(K_COINS, next)
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
        writeGuarded(K_XP, next)
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
            putStringSet(K_PASS_FREE, emptySet())
            putStringSet(K_PASS_PREMIUM, emptySet())
        }
        writeGuarded(K_PASS_POINTS, 0)
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
        writeGuarded(K_PASS_POINTS, next)
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

    fun markDailyRecipeCleared() {
        val day = today()
        prefs.edit { putString(K_DAILY_RECIPE, day) }
        update { it.copy(dailyRecipeDay = day) }
    }

    val dailyRecipeDone: Boolean get() = current.dailyRecipeDay == today()

    // ------------------------------------------------------- conquistas

    fun claimAchievement(achievement: Achievement): Int {
        if (!achievement.isDone(current)) return 0
        if (achievement.id in current.achievementsClaimed) return 0
        val claimed = current.achievementsClaimed + achievement.id
        prefs.edit { putStringSet(K_ACHIEVEMENTS, claimed) }
        update { it.copy(achievementsClaimed = claimed) }
        addCoins(achievement.reward)
        addXp(achievement.reward / 3)
        return achievement.reward
    }

    // -------------------------------------------------- partida salva

    fun saveResume(game: SavedGame) {
        prefs.edit { putString(K_RESUME, game.encode()) }
    }

    fun loadResume(): SavedGame? = SavedGame.decode(prefs.getString(K_RESUME, null))

    fun clearResume() {
        prefs.edit { remove(K_RESUME) }
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
        private const val K_DAILY_RECIPE = "daily_recipe_day"
        private const val K_LEGAL = "legal"
        private const val K_ACHIEVEMENTS = "achievements"
        private const val K_RESUME = "resume"
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
