package com.neonsombra.game.ui.theme

import androidx.compose.ui.graphics.Color
import com.neonsombra.game.game.TetrominoType

// Fundo e superficies
val NeonNight = Color(0xFF06010F)
val NeonNightDeep = Color(0xFF0B0320)
val NeonSurface = Color(0xFF140A2E)
val NeonSurfaceSoft = Color(0xCC1A0F38)

// Acentos
val NeonCyan = Color(0xFF00F0FF)
val NeonMagenta = Color(0xFFFF2BD6)
val NeonPurple = Color(0xFF9D4EDD)
val NeonLime = Color(0xFF39FF14)
val NeonYellow = Color(0xFFFFE600)
val NeonOrange = Color(0xFFFF8A00)
val NeonRed = Color(0xFFFF2E4C)
val NeonBlue = Color(0xFF2E6BFF)

// Texto
val NeonTextPrimary = Color(0xFFF2E9FF)
val NeonTextMuted = Color(0xFF9C8FC4)

// O lado sombrio
val ShadowBlock = Color(0xFF07040E)
val ShadowEdge = Color(0xFF3B1050)

/** Cor viva de cada peca. */
val TetrominoType.neonColor: Color
    get() = when (this) {
        TetrominoType.I -> NeonCyan
        TetrominoType.O -> NeonYellow
        TetrominoType.T -> NeonMagenta
        TetrominoType.S -> NeonLime
        TetrominoType.Z -> NeonRed
        TetrominoType.J -> NeonBlue
        TetrominoType.L -> NeonOrange
    }

/** Cor da peca a partir do codigo gravado no tabuleiro. */
fun colorForCode(code: Int): Color =
    TetrominoType.fromCode(code)?.neonColor ?: NeonCyan
