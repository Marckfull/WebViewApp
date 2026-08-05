package com.formatfrute.game.data

import androidx.annotation.StringRes
import com.formatfrute.game.R
import com.formatfrute.game.core.Fruit
import com.formatfrute.game.core.RecipeBook

/** De onde cada conquista tira o progresso — tudo já é contado pelo jogo. */
enum class AchievementKind {
    FUSOES, PARTIDAS, COLHEITAS, FRUTA, FASES, ESTRELAS, PODERES, SEQUENCIA, NIVEL, PASSE, PELES
}

data class Achievement(
    val id: String,
    @StringRes val title: Int,
    @StringRes val desc: Int,
    val emoji: String,
    val kind: AchievementKind,
    val target: Int,
    val reward: Int,
) {
    fun progressOf(profile: Profile): Int = when (kind) {
        AchievementKind.FUSOES -> profile.totalMerges
        AchievementKind.PARTIDAS -> profile.totalGames
        AchievementKind.COLHEITAS -> profile.totalHarvests
        AchievementKind.FRUTA -> profile.highestFruit
        AchievementKind.FASES -> profile.recipeCleared
        AchievementKind.ESTRELAS -> profile.recipeStarTotal
        AchievementKind.PODERES -> profile.powersUsed
        AchievementKind.SEQUENCIA -> profile.streak
        AchievementKind.NIVEL -> profile.level
        AchievementKind.PASSE -> profile.passTier
        // Só conta pele comprada: as do Passe entram de graça e não valem prova.
        AchievementKind.PELES -> BoardTheme.shop.count { it.id in profile.unlockedThemes }
    }.coerceAtMost(target)

    fun isDone(profile: Profile): Boolean = progressOf(profile) >= target
}

/**
 * Conquistas do Format Frute. São locais de propósito: nenhum login, nenhuma
 * conta, nenhum servidor — coerente com o resto do jogo.
 */
object Achievements {

    val all: List<Achievement> = listOf(
        Achievement(
            "primeira_fusao", R.string.ach_primeira_fusao_title, R.string.ach_primeira_fusao_desc,
            "🍒", AchievementKind.FUSOES, 1, 40,
        ),
        Achievement(
            "cem_fusoes", R.string.ach_cem_fusoes_title, R.string.ach_cem_fusoes_desc,
            "👐", AchievementKind.FUSOES, 100, 120,
        ),
        Achievement(
            "mil_fusoes", R.string.ach_mil_fusoes_title, R.string.ach_mil_fusoes_desc,
            "💪", AchievementKind.FUSOES, 1_000, 400,
        ),
        Achievement(
            "dez_mil_fusoes", R.string.ach_dez_mil_fusoes_title, R.string.ach_dez_mil_fusoes_desc,
            "🌟", AchievementKind.FUSOES, 10_000, 1_500,
        ),

        Achievement(
            "maca", R.string.ach_maca_title, R.string.ach_maca_desc,
            "🍎", AchievementKind.FRUTA, Fruit.MACA.ordinal, 80,
        ),
        Achievement(
            "kiwi", R.string.ach_kiwi_title, R.string.ach_kiwi_desc,
            "🥝", AchievementKind.FRUTA, Fruit.KIWI.ordinal, 200,
        ),
        Achievement(
            "pitaya", R.string.ach_pitaya_title, R.string.ach_pitaya_desc,
            "🐉", AchievementKind.FRUTA, Fruit.PITAYA.ordinal, 500,
        ),
        Achievement(
            "melancia", R.string.ach_melancia_title, R.string.ach_melancia_desc,
            "🍉", AchievementKind.FRUTA, Fruit.MELANCIA.ordinal, 1_200,
        ),

        Achievement(
            "colheita", R.string.ach_colheita_title, R.string.ach_colheita_desc,
            "🧺", AchievementKind.COLHEITAS, 1, 300,
        ),
        Achievement(
            "colheita_dez", R.string.ach_colheita_dez_title, R.string.ach_colheita_dez_desc,
            "🚜", AchievementKind.COLHEITAS, 10, 900,
        ),

        Achievement(
            "dez_partidas", R.string.ach_dez_partidas_title, R.string.ach_dez_partidas_desc,
            "🎮", AchievementKind.PARTIDAS, 10, 90,
        ),
        Achievement(
            "cem_partidas", R.string.ach_cem_partidas_title, R.string.ach_cem_partidas_desc,
            "🏪", AchievementKind.PARTIDAS, 100, 600,
        ),

        Achievement(
            "cinco_fases", R.string.ach_cinco_fases_title, R.string.ach_cinco_fases_desc,
            "🍳", AchievementKind.FASES, 5, 100,
        ),
        Achievement(
            "vinte_cinco_fases", R.string.ach_vinte_cinco_fases_title, R.string.ach_vinte_cinco_fases_desc,
            "👨‍🍳", AchievementKind.FASES, 25, 450,
        ),
        Achievement(
            "todas_fases", R.string.ach_todas_fases_title, R.string.ach_todas_fases_desc,
            "📕", AchievementKind.FASES, RecipeBook.TOTAL, 2_000,
        ),
        Achievement(
            "cem_estrelas", R.string.ach_cem_estrelas_title, R.string.ach_cem_estrelas_desc,
            "⭐", AchievementKind.ESTRELAS, 100, 700,
        ),
        Achievement(
            "estrela_cheia", R.string.ach_estrela_cheia_title, R.string.ach_estrela_cheia_desc,
            "🌠", AchievementKind.ESTRELAS, RecipeBook.TOTAL * 3, 3_000,
        ),

        Achievement(
            "poderes", R.string.ach_poderes_title, R.string.ach_poderes_desc,
            "✨", AchievementKind.PODERES, 25, 250,
        ),
        Achievement(
            "semana", R.string.ach_semana_title, R.string.ach_semana_desc,
            "🔥", AchievementKind.SEQUENCIA, 7, 500,
        ),
        Achievement(
            "nivel_dez", R.string.ach_nivel_dez_title, R.string.ach_nivel_dez_desc,
            "📈", AchievementKind.NIVEL, 10, 300,
        ),
        Achievement(
            "nivel_vinte_cinco", R.string.ach_nivel_vinte_cinco_title, R.string.ach_nivel_vinte_cinco_desc,
            "🎖️", AchievementKind.NIVEL, 25, 1_000,
        ),
        Achievement(
            "passe_completo", R.string.ach_passe_completo_title, R.string.ach_passe_completo_desc,
            "🎟️", AchievementKind.PASSE, SeasonPass.TIERS, 1_500,
        ),
        Achievement(
            "colecionador", R.string.ach_colecionador_title, R.string.ach_colecionador_desc,
            "🎨", AchievementKind.PELES, BoardTheme.shop.size, 800,
        ),
    )

    fun byId(id: String): Achievement? = all.firstOrNull { it.id == id }

    fun doneCount(profile: Profile): Int = all.count { it.isDone(profile) }

    /** Conquistas prontas mas ainda não retiradas — o badge da tela inicial. */
    fun pending(profile: Profile): List<Achievement> =
        all.filter { it.isDone(profile) && it.id !in profile.achievementsClaimed }
}
