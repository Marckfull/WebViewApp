package com.kardiapulse.game.ui.theme

import androidx.compose.ui.graphics.Color
import com.kardiapulse.game.core.model.Element

/**
 * A paleta de Kardia. A ideia visual é um laboratório escuro onde a única luz vem do Núcleo e
 * das cartas — por isso o fundo é quase preto com um roxo profundo, e todo o resto brilha.
 */

// Fundos
val VoidBlack = Color(0xFF07060D)
val DeepIndigo = Color(0xFF120E24)
val NightViolet = Color(0xFF1B1338)
val SurfaceCard = Color(0xFF181330)
val SurfaceRaised = Color(0xFF221A44)
val SurfaceStroke = Color(0xFF352A63)

// Acentos
val PulseCyan = Color(0xFF35E8E0)
val PulseViolet = Color(0xFF9B6BFF)
val PulseMagenta = Color(0xFFFF5FA2)
val PulseGold = Color(0xFFFFD166)

// Semânticos
val PositivePole = Color(0xFF48E5A0)
val NegativePole = Color(0xFFFF7A6B)
val DangerRed = Color(0xFFFF4D5E)
val TextPrimary = Color(0xFFF2EEFF)
val TextSecondary = Color(0xFFA79EC9)
val TextMuted = Color(0xFF6E668F)

// Elementos
val FireColor = Color(0xFFFF7A45)
val WaterColor = Color(0xFF3FC9F0)
val EarthColor = Color(0xFF6BD97F)
val AirColor = Color(0xFFC9A6FF)
val EtherColor = Color(0xFFFFE9A8)

val Element.color: Color
    get() = when (this) {
        Element.FOGO -> FireColor
        Element.AGUA -> WaterColor
        Element.TERRA -> EarthColor
        Element.AR -> AirColor
        Element.ETER -> EtherColor
    }

/** Versão suave do elemento, para fundos e brilhos. */
val Element.glowColor: Color
    get() = color.copy(alpha = 0.35f)
