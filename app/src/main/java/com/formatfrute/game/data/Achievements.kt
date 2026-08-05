package com.formatfrute.game.data

import com.formatfrute.game.core.Fruit
import com.formatfrute.game.core.RecipeBook

/** De onde cada conquista tira o progresso — tudo já é contado pelo jogo. */
enum class AchievementKind {
    FUSOES, PARTIDAS, COLHEITAS, FRUTA, FASES, ESTRELAS, PODERES, SEQUENCIA, NIVEL, PASSE, PELES
}

data class Achievement(
    val id: String,
    val title: String,
    val desc: String,
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
            "primeira_fusao", "Primeira Fusão", "Junte suas duas primeiras frutas.",
            "🍒", AchievementKind.FUSOES, 1, 40,
        ),
        Achievement(
            "cem_fusoes", "Mão na Massa", "Faça 100 fusões.",
            "👐", AchievementKind.FUSOES, 100, 120,
        ),
        Achievement(
            "mil_fusoes", "Feirante Calejado", "Faça 1.000 fusões.",
            "💪", AchievementKind.FUSOES, 1_000, 400,
        ),
        Achievement(
            "dez_mil_fusoes", "Lenda da Fusão", "Faça 10.000 fusões.",
            "🌟", AchievementKind.FUSOES, 10_000, 1_500,
        ),

        Achievement(
            "maca", "Bom de Maçã", "Chegue na Maçã.",
            "🍎", AchievementKind.FRUTA, Fruit.MACA.ordinal, 80,
        ),
        Achievement(
            "kiwi", "Peludo mas Doce", "Chegue no Kiwi.",
            "🥝", AchievementKind.FRUTA, Fruit.KIWI.ordinal, 200,
        ),
        Achievement(
            "pitaya", "Caçador de Dragão", "Chegue na Pitaya.",
            "🐉", AchievementKind.FRUTA, Fruit.PITAYA.ordinal, 500,
        ),
        Achievement(
            "melancia", "Barão da Melancia", "Chegue na Melancia.",
            "🍉", AchievementKind.FRUTA, Fruit.MELANCIA.ordinal, 1_200,
        ),

        Achievement(
            "colheita", "Primeira Colheita", "Junte duas Melancias.",
            "🧺", AchievementKind.COLHEITAS, 1, 300,
        ),
        Achievement(
            "colheita_dez", "Colheita Farta", "Faça 10 colheitas.",
            "🚜", AchievementKind.COLHEITAS, 10, 900,
        ),

        Achievement(
            "dez_partidas", "Freguês da Casa", "Jogue 10 partidas.",
            "🎮", AchievementKind.PARTIDAS, 10, 90,
        ),
        Achievement(
            "cem_partidas", "Dono da Barraca", "Jogue 100 partidas.",
            "🏪", AchievementKind.PARTIDAS, 100, 600,
        ),

        Achievement(
            "cinco_fases", "Cozinheiro de Fim de Semana", "Feche 5 fases da Receita.",
            "🍳", AchievementKind.FASES, 5, 100,
        ),
        Achievement(
            "vinte_cinco_fases", "Chef de Confiança", "Feche 25 fases da Receita.",
            "👨‍🍳", AchievementKind.FASES, 25, 450,
        ),
        Achievement(
            "todas_fases", "Caderno Completo", "Feche todas as ${RecipeBook.TOTAL} fases.",
            "📕", AchievementKind.FASES, RecipeBook.TOTAL, 2_000,
        ),
        Achievement(
            "cem_estrelas", "Chuva de Estrelas", "Junte 100 estrelas na Receita.",
            "⭐", AchievementKind.ESTRELAS, 100, 700,
        ),
        Achievement(
            "estrela_cheia", "Nota Máxima", "Junte ${RecipeBook.TOTAL * 3} estrelas.",
            "🌠", AchievementKind.ESTRELAS, RecipeBook.TOTAL * 3, 3_000,
        ),

        Achievement(
            "poderes", "Mão de Poder", "Use 25 poderes.",
            "✨", AchievementKind.PODERES, 25, 250,
        ),
        Achievement(
            "semana", "Semana Cheia", "Sete dias seguidos pegando o presente.",
            "🔥", AchievementKind.SEQUENCIA, 7, 500,
        ),
        Achievement(
            "nivel_dez", "Subindo na Vida", "Chegue ao nível 10.",
            "📈", AchievementKind.NIVEL, 10, 300,
        ),
        Achievement(
            "nivel_vinte_cinco", "Autoridade da Feira", "Chegue ao nível 25.",
            "🎖️", AchievementKind.NIVEL, 25, 1_000,
        ),
        Achievement(
            "passe_completo", "Passe Fechado", "Chegue ao degrau ${SeasonPass.TIERS} do Passe.",
            "🎟️", AchievementKind.PASSE, SeasonPass.TIERS, 1_500,
        ),
        Achievement(
            "colecionador", "Colecionador", "Compre todas as peles da Barraquinha.",
            "🎨", AchievementKind.PELES, BoardTheme.shop.size, 800,
        ),
    )

    fun byId(id: String): Achievement? = all.firstOrNull { it.id == id }

    fun doneCount(profile: Profile): Int = all.count { it.isDone(profile) }

    /** Conquistas prontas mas ainda não retiradas — o badge da tela inicial. */
    fun pending(profile: Profile): List<Achievement> =
        all.filter { it.isDone(profile) && it.id !in profile.achievementsClaimed }
}
