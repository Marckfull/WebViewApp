package com.kardiapulse.game.data

/**
 * Conquistas. Cada uma é uma condição pura sobre o perfil, então basta reavaliar a lista
 * depois de qualquer evento para descobrir o que acabou de ser desbloqueado.
 */
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val glyph: String,
    val shardReward: Int,
    val progress: (PlayerProfile) -> Pair<Int, Int>
) {
    fun isUnlocked(profile: PlayerProfile): Boolean {
        val (current, target) = progress(profile)
        return current >= target
    }

    fun progressFraction(profile: PlayerProfile): Float {
        val (current, target) = progress(profile)
        return if (target <= 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
    }
}

object Achievements {

    val ALL: List<Achievement> = listOf(
        Achievement(
            id = "primeiro_pulso",
            name = "Primeiro Pulso",
            description = "Termine o seu primeiro duelo.",
            glyph = "·",
            shardReward = 50
        ) { it.duelsPlayed to 1 },
        Achievement(
            id = "dez_vitorias",
            name = "Mão Firme",
            description = "Vença 10 duelos.",
            glyph = "▲",
            shardReward = 150
        ) { it.duelsWon to 10 },
        Achievement(
            id = "cinquenta_vitorias",
            name = "Leitor de Núcleo",
            description = "Vença 50 duelos.",
            glyph = "◈",
            shardReward = 400
        ) { it.duelsWon to 50 },
        Achievement(
            id = "corrente_5",
            name = "Ressonância",
            description = "Feche uma corrente de 5 cartas.",
            glyph = "≡",
            shardReward = 120
        ) { it.bestChain to 5 },
        Achievement(
            id = "corrente_10",
            name = "Harmonia Perfeita",
            description = "Feche uma corrente de 10 cartas.",
            glyph = "✦",
            shardReward = 350
        ) { it.bestChain to 10 },
        Achievement(
            id = "cem_inversoes",
            name = "Contramão",
            description = "Inverta a direção do Núcleo 100 vezes.",
            glyph = "⇄",
            shardReward = 200
        ) { it.totalFlips to 100 },
        Achievement(
            id = "sobrevivente_5",
            name = "Sobrevivente",
            description = "Derrote 5 rivais seguidos na Sobrevivência.",
            glyph = "∞",
            shardReward = 250
        ) { it.endlessBest to 5 },
        Achievement(
            id = "sobrevivente_12",
            name = "Inabalável",
            description = "Derrote 12 rivais seguidos na Sobrevivência.",
            glyph = "◉",
            shardReward = 600
        ) { it.endlessBest to 12 },
        Achievement(
            id = "streak_7",
            name = "Rotina de Duelo",
            description = "Mantenha o Passe Diário por 7 dias seguidos.",
            glyph = "◷",
            shardReward = 300
        ) { it.streakDays to 7 },
        Achievement(
            id = "streak_30",
            name = "Um Mês no Núcleo",
            description = "Mantenha o Passe Diário por 30 dias seguidos.",
            glyph = "☀",
            shardReward = 1000
        ) { it.streakDays to 30 },
        Achievement(
            id = "nivel_10",
            name = "Corrente Viva",
            description = "Alcance o nível 10.",
            glyph = "⬡",
            shardReward = 250
        ) { it.level to 10 },
        Achievement(
            id = "nivel_25",
            name = "Prisma",
            description = "Alcance o nível 25.",
            glyph = "◇",
            shardReward = 700
        ) { it.level to 25 }
    )

    fun byId(id: String): Achievement? = ALL.firstOrNull { it.id == id }

    /**
     * Descobre o que acabou de ser desbloqueado, credita os Fragmentos e devolve o perfil novo
     * junto da lista de conquistas para a UI comemorar.
     */
    fun claimNewly(profile: PlayerProfile): Pair<PlayerProfile, List<Achievement>> {
        val newly = ALL.filter { it.isUnlocked(profile) && it.id !in profile.achievements }
        if (newly.isEmpty()) return profile to emptyList()
        val updated = profile.copy(
            achievements = profile.achievements + newly.map { it.id },
            shards = profile.shards + newly.sumOf { it.shardReward }
        )
        return updated to newly
    }
}
