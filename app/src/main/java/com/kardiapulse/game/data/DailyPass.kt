package com.kardiapulse.game.data

import com.kardiapulse.game.core.model.PowerType
import java.util.TimeZone

/**
 * O Passe Diário: um ciclo de 7 dias que reinicia sozinho e uma sequência (streak) que só
 * cresce enquanto o jogador aparece todo dia. É o motivo de abrir o app amanhã.
 */
object DailyPass {

    data class Day(
        val index: Int,
        val shards: Int,
        val crystals: Int,
        val power: PowerType?,
        val label: String
    )

    val CYCLE: List<Day> = listOf(
        Day(0, shards = 80, crystals = 0, power = null, label = "Fragmentos"),
        Day(1, shards = 100, crystals = 0, power = PowerType.VISAO, label = "Visão"),
        Day(2, shards = 130, crystals = 1, power = null, label = "Cristal"),
        Day(3, shards = 160, crystals = 0, power = PowerType.DESCARGA, label = "Descarga"),
        Day(4, shards = 200, crystals = 1, power = null, label = "Cristal duplo"),
        Day(5, shards = 260, crystals = 1, power = PowerType.INVERSOR, label = "Inversor"),
        Day(6, shards = 400, crystals = 3, power = PowerType.ECO, label = "Recompensa maior")
    )

    /** O dia local do jogador, não o UTC — ninguém quer perder a sequência por fuso horário. */
    fun todayEpochDay(now: Long = System.currentTimeMillis()): Long {
        val offset = TimeZone.getDefault().getOffset(now)
        return Math.floorDiv(now + offset, 86_400_000L)
    }

    fun canClaim(profile: PlayerProfile, today: Long = todayEpochDay()): Boolean =
        profile.lastClaimEpochDay != today

    /** O dia da sequência que será resgatado agora (0..6). */
    fun pendingDayIndex(profile: PlayerProfile, today: Long = todayEpochDay()): Int {
        if (profile.lastClaimEpochDay < 0) return 0
        val continued = profile.lastClaimEpochDay == today - 1
        return if (continued) (profile.passDayIndex + 1) % CYCLE.size else 0
    }

    /** A sequência quebra quando o jogador some por mais de um dia. */
    fun streakBroken(profile: PlayerProfile, today: Long = todayEpochDay()): Boolean =
        profile.streakDays > 0 &&
            profile.lastClaimEpochDay >= 0 &&
            profile.lastClaimEpochDay < today - 1

    fun claim(profile: PlayerProfile, today: Long = todayEpochDay()): Pair<PlayerProfile, Day?> {
        if (!canClaim(profile, today)) return profile to null

        val continued = profile.lastClaimEpochDay == today - 1
        val index = pendingDayIndex(profile, today)
        val day = CYCLE[index]

        var updated = profile.copy(
            shards = profile.shards + day.shards,
            crystals = profile.crystals + day.crystals,
            streakDays = if (continued) profile.streakDays + 1 else 1,
            passDayIndex = index,
            lastClaimEpochDay = today
        )
        if (day.power != null) updated = updated.withPower(day.power, 1)
        return updated to day
    }

    /**
     * Restaura a sequência perdida — o gancho do anúncio premiado que salva 30 dias de rotina.
     * Só faz sentido quando a sequência realmente quebrou.
     */
    fun restoreStreak(profile: PlayerProfile, today: Long = todayEpochDay()): PlayerProfile =
        if (!streakBroken(profile, today)) profile
        else profile.copy(lastClaimEpochDay = today - 1)
}
