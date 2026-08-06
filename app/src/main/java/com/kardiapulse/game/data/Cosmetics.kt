package com.kardiapulse.game.data

import androidx.compose.ui.graphics.Color
import com.kardiapulse.game.ui.theme.EtherColor
import com.kardiapulse.game.ui.theme.FireColor
import com.kardiapulse.game.ui.theme.PositivePole
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.PulseGold
import com.kardiapulse.game.ui.theme.PulseMagenta
import com.kardiapulse.game.ui.theme.PulseViolet

/** Um item cosmético comprável. Muda a aparência, nunca o equilíbrio do jogo. */
data class Cosmetic(
    val id: String,
    val name: String,
    val description: String,
    val color: Color,
    val crystalPrice: Int,
    val kind: Kind
) {
    enum class Kind { DORSO, MESA }
}

object Cosmetics {

    val CARD_BACKS: List<Cosmetic> = listOf(
        Cosmetic(
            "dorso_padrao", "Púrpura", "O dorso original de Kardia.",
            PulseViolet, 0, Cosmetic.Kind.DORSO
        ),
        Cosmetic(
            "dorso_ciano", "Corrente", "Ciano frio, como o Núcleo em repouso.",
            PulseCyan, 6, Cosmetic.Kind.DORSO
        ),
        Cosmetic(
            "dorso_brasa", "Brasa", "Para quem prefere abrir a rodada no Fogo.",
            FireColor, 8, Cosmetic.Kind.DORSO
        ),
        Cosmetic(
            "dorso_eter", "Éter", "Dourado pálido, o dorso mais discreto e o mais caro.",
            EtherColor, 14, Cosmetic.Kind.DORSO
        ),
        Cosmetic(
            "dorso_eclipse", "Eclipse", "Magenta profundo. Some no escuro da mesa.",
            PulseMagenta, 12, Cosmetic.Kind.DORSO
        )
    )

    val BOARDS: List<Cosmetic> = listOf(
        Cosmetic(
            "mesa_padrao", "Laboratório", "A mesa padrão, em índigo profundo.",
            PulseViolet, 0, Cosmetic.Kind.MESA
        ),
        Cosmetic(
            "mesa_aurora", "Aurora", "Auroras mais intensas ao redor do Núcleo.",
            PulseCyan, 10, Cosmetic.Kind.MESA
        ),
        Cosmetic(
            "mesa_forja", "Forja", "Um brilho quente e baixo, como metal esfriando.",
            PulseGold, 12, Cosmetic.Kind.MESA
        ),
        Cosmetic(
            "mesa_jade", "Jade", "Verde sereno para duelos longos.",
            PositivePole, 10, Cosmetic.Kind.MESA
        )
    )

    val ALL: List<Cosmetic> = CARD_BACKS + BOARDS

    fun byId(id: String): Cosmetic? = ALL.firstOrNull { it.id == id }

    fun cardBackColor(id: String): Color =
        CARD_BACKS.firstOrNull { it.id == id }?.color ?: PulseViolet

    fun boardColor(id: String): Color =
        BOARDS.firstOrNull { it.id == id }?.color ?: PulseViolet
}
