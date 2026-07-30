package com.prisma.fusao.ui.game

import com.prisma.fusao.core.Objective
import com.prisma.fusao.core.ObjectiveState
import com.prisma.fusao.core.ObjectiveType
import com.prisma.fusao.ui.theme.label

/** Texto completo do objetivo, usado antes de começar a fase. */
fun objectiveText(objective: Objective): String = when (objective.type) {
    ObjectiveType.SCORE -> "Faça ${"%,d".format(objective.target)} pontos"
    ObjectiveType.COLLECT ->
        "Colete ${objective.target} gemas ${objective.color?.label() ?: ""}".trim()
    ObjectiveType.ICE -> "Quebre ${objective.target} camadas de cristal"
    ObjectiveType.FUSION -> "Faça ${objective.target} ${plural(objective.target, "fusão", "fusões")}"
    ObjectiveType.PRISMOID ->
        "Leve ${objective.target} ${plural(objective.target, "prismoide", "prismoides")} até a base"
    ObjectiveType.STONE -> "Destrua ${objective.target} blocos de pedra"
}

/** Versão curta para a HUD, no formato "atual/meta". */
fun objectiveShort(state: ObjectiveState): String {
    val name = when (state.objective.type) {
        ObjectiveType.SCORE -> "Pontos"
        ObjectiveType.COLLECT -> state.objective.color?.label() ?: "Cor"
        ObjectiveType.ICE -> "Cristal"
        ObjectiveType.FUSION -> "Fusões"
        ObjectiveType.PRISMOID -> "Prismoides"
        ObjectiveType.STONE -> "Pedras"
    }
    return "$name ${state.current}/${state.objective.target}"
}

/** O que ainda falta, para a tela de derrota. */
fun objectiveMissing(state: ObjectiveState): String {
    val remaining = (state.objective.target - state.current).coerceAtLeast(0)
    return when (state.objective.type) {
        ObjectiveType.SCORE -> "${"%,d".format(remaining)} pontos"
        ObjectiveType.COLLECT ->
            "$remaining gemas ${state.objective.color?.label() ?: ""}".trim()
        ObjectiveType.ICE -> "$remaining camadas de cristal"
        ObjectiveType.FUSION -> "$remaining ${plural(remaining, "fusão", "fusões")}"
        ObjectiveType.PRISMOID -> "$remaining ${plural(remaining, "prismoide", "prismoides")}"
        ObjectiveType.STONE -> "$remaining blocos de pedra"
    }
}

private fun plural(count: Int, singular: String, plural: String): String =
    if (count == 1) singular else plural
