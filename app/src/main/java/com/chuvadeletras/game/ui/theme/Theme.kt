package com.chuvadeletras.game.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Paleta: céu de tempestade à noite, gotas em ciano, prêmios em amarelo-sol.
val ChuvaCyan = Color(0xFF45D3F5)
val ChuvaDeepCyan = Color(0xFF1E9CC4)
val ChuvaNight = Color(0xFF0B1026)
val ChuvaNightSurface = Color(0xFF161E42)
val ChuvaNightSurfaceHigh = Color(0xFF212C5C)
val ChuvaSun = Color(0xFFFFD166)
val ChuvaLilac = Color(0xFFA78BFA)
val ChuvaCoral = Color(0xFFFF6B6B)
val ChuvaMint = Color(0xFF4ADE80)
val ChuvaFog = Color(0xFFE7F1FB)

private val DarkColors = darkColorScheme(
    primary = ChuvaCyan,
    onPrimary = ChuvaNight,
    primaryContainer = ChuvaDeepCyan,
    onPrimaryContainer = Color.White,
    secondary = ChuvaSun,
    onSecondary = ChuvaNight,
    secondaryContainer = Color(0xFF6B5510),
    onSecondaryContainer = ChuvaSun,
    tertiary = ChuvaLilac,
    onTertiary = ChuvaNight,
    background = ChuvaNight,
    onBackground = ChuvaFog,
    surface = ChuvaNightSurface,
    onSurface = ChuvaFog,
    surfaceVariant = ChuvaNightSurfaceHigh,
    onSurfaceVariant = Color(0xFFB9C6E6),
    error = ChuvaCoral,
    onError = Color.White,
    outline = Color(0xFF3D4A7A)
)

private val LightColors = lightColorScheme(
    primary = ChuvaDeepCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBEEBF9),
    onPrimaryContainer = Color(0xFF063547),
    secondary = Color(0xFFB98600),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7A8),
    onSecondaryContainer = Color(0xFF3D2E00),
    tertiary = Color(0xFF6D46D9),
    onTertiary = Color.White,
    background = Color(0xFFF2F8FF),
    onBackground = Color(0xFF10182E),
    surface = Color.White,
    onSurface = Color(0xFF10182E),
    surfaceVariant = Color(0xFFE1EAF7),
    onSurfaceVariant = Color(0xFF44506E),
    error = Color(0xFFC62828),
    onError = Color.White,
    outline = Color(0xFF9EB0CC)
)

private val ChuvaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 44.sp,
        letterSpacing = (-1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun ChuvaDeLetrasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = ChuvaTypography,
        content = content
    )
}
