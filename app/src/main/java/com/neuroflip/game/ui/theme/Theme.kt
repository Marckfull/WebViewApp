package com.neuroflip.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Paleta de um tema visual do NeuroFlip. */
data class NeuroPalette(
    val id: String,
    val name: String,
    val price: Int,
    val void0: Color,
    val void1: Color,
    val surface: Color,
    val cardBack: Color,
    val cardBackEdge: Color,
    val cardFace: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val gold: Color,
    val danger: Color,
    val textPrimary: Color,
    val textDim: Color
) {
    val backgroundBrush: Brush
        get() = Brush.linearGradient(listOf(void0, void1, void0))

    val primaryBrush: Brush
        get() = Brush.horizontalGradient(listOf(primary, secondary))
}

object NeuroThemes {

    val CORTEX = NeuroPalette(
        id = "cortex",
        name = "Neon Córtex",
        price = 0,
        void0 = Color(0xFF05060F),
        void1 = Color(0xFF10123A),
        surface = Color(0xFF141833),
        cardBack = Color(0xFF1B2150),
        cardBackEdge = Color(0xFF19E5D2),
        cardFace = Color(0xFF0B1030),
        primary = Color(0xFF19E5D2),
        secondary = Color(0xFFFF3D9A),
        accent = Color(0xFF8A6BFF),
        gold = Color(0xFFFFC857),
        danger = Color(0xFFFF5252),
        textPrimary = Color(0xFFF2F6FF),
        textDim = Color(0xFF8C96C4)
    )

    val SOLAR = NeuroPalette(
        id = "solar",
        name = "Erupção Solar",
        price = 350,
        void0 = Color(0xFF120701),
        void1 = Color(0xFF3B1403),
        surface = Color(0xFF25100A),
        cardBack = Color(0xFF3A1808),
        cardBackEdge = Color(0xFFFFA22B),
        cardFace = Color(0xFF1B0C05),
        primary = Color(0xFFFFA22B),
        secondary = Color(0xFFFF4D2D),
        accent = Color(0xFFFFD966),
        gold = Color(0xFFFFE08A),
        danger = Color(0xFFFF6B6B),
        textPrimary = Color(0xFFFFF3E6),
        textDim = Color(0xFFC79A78)
    )

    val BIOLAB = NeuroPalette(
        id = "biolab",
        name = "Bio Lab",
        price = 500,
        void0 = Color(0xFF02120C),
        void1 = Color(0xFF063326),
        surface = Color(0xFF07231A),
        cardBack = Color(0xFF0B3A2A),
        cardBackEdge = Color(0xFF4DFFA8),
        cardFace = Color(0xFF041A13),
        primary = Color(0xFF4DFFA8),
        secondary = Color(0xFF00C2FF),
        accent = Color(0xFFB6FF3D),
        gold = Color(0xFFE8FF6B),
        danger = Color(0xFFFF7676),
        textPrimary = Color(0xFFEAFFF6),
        textDim = Color(0xFF7FB8A2)
    )

    val SAKURA = NeuroPalette(
        id = "sakura",
        name = "Sakura",
        price = 650,
        void0 = Color(0xFF14061A),
        void1 = Color(0xFF3B0E44),
        surface = Color(0xFF250A2E),
        cardBack = Color(0xFF3D1148),
        cardBackEdge = Color(0xFFFF9ED2),
        cardFace = Color(0xFF1A0722),
        primary = Color(0xFFFF9ED2),
        secondary = Color(0xFFB06BFF),
        accent = Color(0xFF6BE5FF),
        gold = Color(0xFFFFD98A),
        danger = Color(0xFFFF6E9C),
        textPrimary = Color(0xFFFDEEFB),
        textDim = Color(0xFFB98BC4)
    )

    val RETRO = NeuroPalette(
        id = "retro",
        name = "Retro CRT",
        price = 800,
        void0 = Color(0xFF000000),
        void1 = Color(0xFF0B1A0B),
        surface = Color(0xFF07120A),
        cardBack = Color(0xFF0E2412),
        cardBackEdge = Color(0xFF39FF6A),
        cardFace = Color(0xFF031005),
        primary = Color(0xFF39FF6A),
        secondary = Color(0xFFFFB000),
        accent = Color(0xFF9BFF6B),
        gold = Color(0xFFFFD24A),
        danger = Color(0xFFFF4646),
        textPrimary = Color(0xFFD6FFD9),
        textDim = Color(0xFF6FA974)
    )

    val ALL = listOf(CORTEX, SOLAR, BIOLAB, SAKURA, RETRO)

    fun byId(id: String): NeuroPalette = ALL.firstOrNull { it.id == id } ?: CORTEX
}

val LocalNeuro = staticCompositionLocalOf { NeuroThemes.CORTEX }

private val NeuroTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 44.sp,
        letterSpacing = 4.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 1.5.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = 0.8.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 1.2.sp
    )
)

@Composable
fun NeuroFlipTheme(
    paletteId: String = NeuroThemes.CORTEX.id,
    content: @Composable () -> Unit
) {
    // O jogo é sempre escuro por design — o contraste neon depende disso.
    val palette = NeuroThemes.byId(paletteId)

    val scheme = darkColorScheme(
        primary = palette.primary,
        onPrimary = palette.void0,
        secondary = palette.secondary,
        onSecondary = palette.void0,
        tertiary = palette.accent,
        background = palette.void0,
        onBackground = palette.textPrimary,
        surface = palette.surface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.cardBack,
        onSurfaceVariant = palette.textDim,
        error = palette.danger
    )

    CompositionLocalProvider(LocalNeuro provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NeuroTypography,
            content = content
        )
    }
}
