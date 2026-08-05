package com.formatfrute.game.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Paleta tirada da propria arte das frutas: contorno marrom-escuro grosso,
 * miolo saturado e brilho pastel. Tudo no app usa esse mesmo vocabulario.
 */
object Fruta {
    val Ink = Color(0xFF3D2B1F)
    val InkSoft = Color(0xFF6B5344)
    val Cream = Color(0xFFFFF7EA)
    val Sun = Color(0xFFFFC531)
    val Berry = Color(0xFFFF4D6D)
    val Leaf = Color(0xFF43C463)
    val Sky = Color(0xFF3FA9F5)
    val Grape = Color(0xFF7B61FF)
    val Peach = Color(0xFFFFA07A)
    val Coin = Color(0xFFFFD54F)
    val Danger = Color(0xFFE8202A)
}

private val LightScheme = lightColorScheme(
    primary = Fruta.Berry,
    onPrimary = Color.White,
    secondary = Fruta.Sun,
    onSecondary = Fruta.Ink,
    tertiary = Fruta.Leaf,
    background = Fruta.Cream,
    onBackground = Fruta.Ink,
    surface = Color.White,
    onSurface = Fruta.Ink,
    error = Fruta.Danger,
)

private val DarkScheme = darkColorScheme(
    primary = Fruta.Berry,
    onPrimary = Color.White,
    secondary = Fruta.Sun,
    onSecondary = Fruta.Ink,
    tertiary = Fruta.Leaf,
    background = Color(0xFF241B3A),
    onBackground = Color(0xFFF7F1FF),
    surface = Color(0xFF2E2450),
    onSurface = Color(0xFFF7F1FF),
    error = Fruta.Danger,
)

private fun chunky(size: Int, weight: FontWeight = FontWeight.Black, spacing: Double = 0.0) =
    TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        fontSize = size.sp,
        letterSpacing = spacing.sp,
    )

private val FruitTypography = Typography(
    displayLarge = chunky(48, spacing = 1.0),
    displayMedium = chunky(38, spacing = 0.5),
    headlineLarge = chunky(30),
    headlineMedium = chunky(24),
    headlineSmall = chunky(20),
    titleLarge = chunky(20, FontWeight.ExtraBold),
    titleMedium = chunky(17, FontWeight.ExtraBold),
    bodyLarge = chunky(16, FontWeight.SemiBold),
    bodyMedium = chunky(14, FontWeight.SemiBold),
    bodySmall = chunky(12, FontWeight.Medium),
    labelLarge = chunky(15, FontWeight.ExtraBold, 0.4),
    labelMedium = chunky(13, FontWeight.Bold),
    labelSmall = chunky(11, FontWeight.Bold, 0.6),
)

@Composable
fun FormatFruteTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = FruitTypography,
        content = content,
    )
}
