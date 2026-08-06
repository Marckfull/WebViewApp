package com.kardiapulse.game.data

import com.kardiapulse.game.core.model.PowerType
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.sqrt

/** Patentes por nível. O nome que aparece embaixo do avatar. */
enum class Rank(val ptName: String, val minLevel: Int, val glyph: String) {
    FAISCA("Faísca", 1, "·"),
    BRASA("Brasa", 5, "▲"),
    CORRENTE("Corrente", 10, "≡"),
    PRISMA("Prisma", 18, "◈"),
    ECLIPSE("Eclipse", 28, "◉"),
    NUCLEO("Núcleo", 40, "✦");

    companion object {
        fun forLevel(level: Int): Rank = entries.last { level >= it.minLevel }
        fun next(level: Int): Rank? = entries.firstOrNull { it.minLevel > level }
    }
}

/**
 * Tudo que o jogador acumula. É serializado como um único JSON e assinado — ver [PrefsRepository].
 */
data class PlayerProfile(
    val xp: Int = 0,
    val shards: Int = 300,
    val crystals: Int = 5,
    val powers: Map<PowerType, Int> = mapOf(PowerType.DESCARGA to 1, PowerType.VISAO to 1),
    /** Cargas de Recuo. Não é um poder do motor: desfaz a última jogada fora das regras. */
    val undoCharges: Int = 1,

    val duelsPlayed: Int = 0,
    val duelsWon: Int = 0,
    val bestChain: Int = 0,
    val totalFlips: Int = 0,
    val endlessBest: Int = 0,

    val streakDays: Int = 0,
    val lastClaimEpochDay: Long = -1,
    val passDayIndex: Int = 0,

    val tutorialStep: Int = 0,
    val tutorialDone: Boolean = false,

    val achievements: Set<String> = emptySet(),
    val cosmetics: Set<String> = setOf("dorso_padrao", "mesa_padrao"),
    val activeCardBack: String = "dorso_padrao",
    val activeBoard: String = "mesa_padrao",

    val soundOn: Boolean = true,
    val musicOn: Boolean = true,
    val hapticsOn: Boolean = true,
    val notificationsOn: Boolean = true,

    val legalAcceptedVersion: Int = 0,
    val dailyChallengeDay: Long = -1,
    val dailyChallengeWon: Boolean = false,
    val removeAds: Boolean = false,
    val duelsSinceInterstitial: Int = 0,
    val tampered: Boolean = false,
    /** Ligado quando uma gravação implausível foi barrada. Ver [com.kardiapulse.game.security.IntegrityRules]. */
    val suspicious: Boolean = false
) {
    /** Curva de nível suave: cada nível custa um pouco mais que o anterior. */
    val level: Int get() = 1 + sqrt(xp / 120.0).toInt()

    val rank: Rank get() = Rank.forLevel(level)

    val xpForCurrentLevel: Int get() = xpForLevel(level)
    val xpForNextLevel: Int get() = xpForLevel(level + 1)

    /** Progresso 0..1 dentro do nível atual. */
    val levelProgress: Float
        get() {
            val span = (xpForNextLevel - xpForCurrentLevel).coerceAtLeast(1)
            return ((xp - xpForCurrentLevel).toFloat() / span).coerceIn(0f, 1f)
        }

    val winRate: Int
        get() = if (duelsPlayed == 0) 0 else duelsWon * 100 / duelsPlayed

    fun powerCount(type: PowerType): Int = powers[type] ?: 0

    fun withPower(type: PowerType, delta: Int): PlayerProfile {
        val next = powers.toMutableMap()
        val value = (powerCount(type) + delta).coerceAtLeast(0)
        if (value == 0) next.remove(type) else next[type] = min(value, 99)
        return copy(powers = next)
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("v", SCHEMA_VERSION)
        json.put("xp", xp)
        json.put("shards", shards)
        json.put("crystals", crystals)
        json.put("undoCharges", undoCharges)
        json.put("suspicious", suspicious)
        json.put("powers", JSONObject().also { p -> powers.forEach { (k, v) -> p.put(k.name, v) } })
        json.put("duelsPlayed", duelsPlayed)
        json.put("duelsWon", duelsWon)
        json.put("bestChain", bestChain)
        json.put("totalFlips", totalFlips)
        json.put("endlessBest", endlessBest)
        json.put("streakDays", streakDays)
        json.put("lastClaimEpochDay", lastClaimEpochDay)
        json.put("passDayIndex", passDayIndex)
        json.put("tutorialStep", tutorialStep)
        json.put("tutorialDone", tutorialDone)
        json.put("achievements", JSONArray(achievements.toList()))
        json.put("cosmetics", JSONArray(cosmetics.toList()))
        json.put("activeCardBack", activeCardBack)
        json.put("activeBoard", activeBoard)
        json.put("soundOn", soundOn)
        json.put("musicOn", musicOn)
        json.put("hapticsOn", hapticsOn)
        json.put("notificationsOn", notificationsOn)
        json.put("legalAcceptedVersion", legalAcceptedVersion)
        json.put("dailyChallengeDay", dailyChallengeDay)
        json.put("dailyChallengeWon", dailyChallengeWon)
        json.put("removeAds", removeAds)
        json.put("duelsSinceInterstitial", duelsSinceInterstitial)
        return json.toString()
    }

    companion object {
        const val SCHEMA_VERSION = 1

        fun xpForLevel(level: Int): Int {
            val l = (level - 1).coerceAtLeast(0)
            return l * l * 120
        }

        fun fromJson(raw: String): PlayerProfile? = try {
            val json = JSONObject(raw)
            val powers = mutableMapOf<PowerType, Int>()
            json.optJSONObject("powers")?.let { p ->
                for (key in p.keys()) {
                    val type = PowerType.entries.firstOrNull { it.name == key }
                    if (type != null) powers[type] = p.optInt(key, 0).coerceIn(0, 99)
                }
            }
            PlayerProfile(
                xp = json.optInt("xp", 0).coerceAtLeast(0),
                shards = json.optInt("shards", 0).coerceAtLeast(0),
                crystals = json.optInt("crystals", 0).coerceAtLeast(0),
                undoCharges = json.optInt("undoCharges", 0).coerceIn(0, 99),
                suspicious = json.optBoolean("suspicious", false),
                powers = powers.filterValues { it > 0 },
                duelsPlayed = json.optInt("duelsPlayed", 0),
                duelsWon = json.optInt("duelsWon", 0),
                bestChain = json.optInt("bestChain", 0),
                totalFlips = json.optInt("totalFlips", 0),
                endlessBest = json.optInt("endlessBest", 0),
                streakDays = json.optInt("streakDays", 0),
                lastClaimEpochDay = json.optLong("lastClaimEpochDay", -1),
                passDayIndex = json.optInt("passDayIndex", 0),
                tutorialStep = json.optInt("tutorialStep", 0),
                tutorialDone = json.optBoolean("tutorialDone", false),
                achievements = json.optJSONArray("achievements").toStringSet(),
                cosmetics = json.optJSONArray("cosmetics").toStringSet()
                    .ifEmpty { setOf("dorso_padrao", "mesa_padrao") },
                activeCardBack = json.optString("activeCardBack", "dorso_padrao"),
                activeBoard = json.optString("activeBoard", "mesa_padrao"),
                soundOn = json.optBoolean("soundOn", true),
                musicOn = json.optBoolean("musicOn", true),
                hapticsOn = json.optBoolean("hapticsOn", true),
                notificationsOn = json.optBoolean("notificationsOn", true),
                legalAcceptedVersion = json.optInt("legalAcceptedVersion", 0),
                dailyChallengeDay = json.optLong("dailyChallengeDay", -1),
                dailyChallengeWon = json.optBoolean("dailyChallengeWon", false),
                removeAds = json.optBoolean("removeAds", false),
                duelsSinceInterstitial = json.optInt("duelsSinceInterstitial", 0)
            )
        } catch (_: Throwable) {
            null
        }

        private fun JSONArray?.toStringSet(): Set<String> {
            if (this == null) return emptySet()
            val out = HashSet<String>(length())
            for (i in 0 until length()) out.add(optString(i))
            return out.filter { it.isNotEmpty() }.toSet()
        }
    }
}
