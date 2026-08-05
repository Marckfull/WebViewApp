package com.formatfrute.game.data

import com.formatfrute.game.core.GameMode
import com.formatfrute.game.core.Power
import com.formatfrute.game.core.RecipeBook

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
    val dailyRecipeDay: String = "",
    val legalAccepted: Boolean = false,
    val achievementsClaimed: Set<String> = emptySet(),
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
