package com.formatfrute.game.data

import com.formatfrute.game.core.Fruit
import com.formatfrute.game.core.GameMode
import kotlin.random.Random

enum class MissionKind { FUSOES, CHEGAR_NA_FRUTA, PONTOS, PARTIDAS, PODERES, COLHEITA, MODO }

data class Mission(
    val kind: MissionKind,
    val target: Int,
    val extra: String = "",
    val reward: Int,
) {
    val title: String
        get() = when (kind) {
            MissionKind.FUSOES -> "Junte $target frutas"
            MissionKind.CHEGAR_NA_FRUTA -> "Chegue na ${Fruit.of(target).label}"
            MissionKind.PONTOS -> "Faça $target pontos numa partida"
            MissionKind.PARTIDAS -> "Jogue $target partidas"
            MissionKind.PODERES -> "Use $target poderes"
            MissionKind.COLHEITA -> "Faça $target colheita${if (target > 1) "s" else ""} de Melancia"
            MissionKind.MODO -> "Jogue o modo ${GameMode.byId(extra).title}"
        }

    val emoji: String
        get() = when (kind) {
            MissionKind.FUSOES -> "🍒"
            MissionKind.CHEGAR_NA_FRUTA -> "🏆"
            MissionKind.PONTOS -> "⭐"
            MissionKind.PARTIDAS -> "🎮"
            MissionKind.PODERES -> "✨"
            MissionKind.COLHEITA -> "🍉"
            MissionKind.MODO -> "🧭"
        }

    /** Chave estavel para guardar o progresso do dia. */
    val key: String get() = "${kind.name}_${target}_$extra"
}

/**
 * Tres missoes por dia, sorteadas pela data — todo mundo pega as mesmas,
 * e elas se renovam sozinhas na virada.
 */
object MissionFactory {

    fun daily(day: String): List<Mission> {
        val rng = Random(day.hashCode().toLong() * 31 + 7)
        val pool = MissionKind.entries.toMutableList()
        pool.remove(MissionKind.COLHEITA)
        pool.shuffle(rng)
        val kinds = pool.take(2) + if (rng.nextFloat() < 0.25f) MissionKind.COLHEITA else pool[2]
        return kinds.map { build(it, rng) }
    }

    private fun build(kind: MissionKind, rng: Random): Mission = when (kind) {
        MissionKind.FUSOES -> Mission(kind, listOf(30, 50, 80, 120).random(rng), reward = 60)
        MissionKind.CHEGAR_NA_FRUTA -> {
            val level = rng.nextInt(5, 9)
            Mission(kind, level, reward = 40 + level * 12)
        }
        MissionKind.PONTOS -> Mission(kind, listOf(1500, 3000, 6000, 10000).random(rng), reward = 90)
        MissionKind.PARTIDAS -> Mission(kind, rng.nextInt(2, 5), reward = 50)
        MissionKind.PODERES -> Mission(kind, rng.nextInt(2, 4), reward = 70)
        MissionKind.COLHEITA -> Mission(kind, 1, reward = 300)
        MissionKind.MODO -> {
            val mode = GameMode.entries.random(rng)
            Mission(kind, 1, extra = mode.id, reward = 80)
        }
    }
}

/** Sequencia de recompensa diaria — 7 dias, o setimo compensa a semana. */
object DailyRewards {
    val table = listOf(50, 80, 120, 160, 220, 300, 500)

    fun rewardFor(streakDay: Int): Int = table[(streakDay - 1).coerceIn(0, table.lastIndex)]
}
