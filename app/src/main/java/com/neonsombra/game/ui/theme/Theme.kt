package com.neonsombra.game.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val NeonColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = NeonNight,
    secondary = NeonMagenta,
    onSecondary = NeonNight,
    tertiary = NeonPurple,
    background = NeonNight,
    onBackground = NeonTextPrimary,
    surface = NeonSurface,
    onSurface = NeonTextPrimary,
    surfaceVariant = NeonSurfaceSoft,
    onSurfaceVariant = NeonTextMuted,
    error = NeonRed,
)

/**
 * Numeros e titulos usam monoespacada: o jogo inteiro tem cara de fliperama
 * sem precisar embarcar nenhuma fonte.
 */
private val NeonTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        letterSpacing = 4.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 2.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 1.5.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
)

@Composable
fun NeonSombraTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // O jogo e sempre noturno: nao existe versao clara do lado sombrio.
    MaterialTheme(
        colorScheme = NeonColorScheme,
        typography = NeonTypography,
        content = content,
    )
}
