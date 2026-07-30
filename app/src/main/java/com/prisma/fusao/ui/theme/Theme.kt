package com.prisma.fusao.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.prisma.fusao.core.GemColor

// Paleta base: um fundo noturno arroxeado para as gemas brilharem por contraste.
val NoiteProfunda = Color(0xFF0B0620)
val NoiteMedia = Color(0xFF160C33)
val RoxoCartao = Color(0xFF231449)
val RoxoBorda = Color(0xFF3B2270)
val LilasClaro = Color(0xFFC7B4FF)
val BrancoGelo = Color(0xFFF2ECFF)
val DouradoEstrela = Color(0xFFFFC53D)
val VerdeConfirma = Color(0xFF3DDC97)
val VermelhoAlerta = Color(0xFFFF5C7A)

/** Cor principal de cada gema. */
fun GemColor.primary(): Color = when (this) {
    GemColor.RUBI -> Color(0xFFFF4D6D)
    GemColor.AMBAR -> Color(0xFFFF9F45)
    GemColor.TOPAZIO -> Color(0xFFFFDD57)
    GemColor.ESMERALDA -> Color(0xFF4ADE80)
    GemColor.SAFIRA -> Color(0xFF48BFE3)
    GemColor.AMETISTA -> Color(0xFFB185FF)
}

/** Tom claro para o brilho e o degradê interno da gema. */
fun GemColor.highlight(): Color = when (this) {
    GemColor.RUBI -> Color(0xFFFFB3C1)
    GemColor.AMBAR -> Color(0xFFFFD5A5)
    GemColor.TOPAZIO -> Color(0xFFFFF3B0)
    GemColor.ESMERALDA -> Color(0xFFB7F0CB)
    GemColor.SAFIRA -> Color(0xFFB9E6F5)
    GemColor.AMETISTA -> Color(0xFFE0D0FF)
}

/** Tom escuro da borda, para dar volume. */
fun GemColor.shade(): Color = when (this) {
    GemColor.RUBI -> Color(0xFF9B1B36)
    GemColor.AMBAR -> Color(0xFF9E5410)
    GemColor.TOPAZIO -> Color(0xFF9C7C00)
    GemColor.ESMERALDA -> Color(0xFF15803D)
    GemColor.SAFIRA -> Color(0xFF176D8C)
    GemColor.AMETISTA -> Color(0xFF5B3FA8)
}

/** Nome legível — usado nos objetivos e na acessibilidade. */
fun GemColor.label(): String = when (this) {
    GemColor.RUBI -> "Rubi"
    GemColor.AMBAR -> "Âmbar"
    GemColor.TOPAZIO -> "Topázio"
    GemColor.ESMERALDA -> "Esmeralda"
    GemColor.SAFIRA -> "Safira"
    GemColor.AMETISTA -> "Ametista"
}

private val PrismaColors = darkColorScheme(
    primary = LilasClaro,
    onPrimary = NoiteProfunda,
    secondary = DouradoEstrela,
    onSecondary = NoiteProfunda,
    background = NoiteProfunda,
    onBackground = BrancoGelo,
    surface = RoxoCartao,
    onSurface = BrancoGelo,
    surfaceVariant = NoiteMedia,
    onSurfaceVariant = LilasClaro,
    error = VermelhoAlerta,
    outline = RoxoBorda,
)

private val PrismaType = Typography(
    displayLarge = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
)

/** O jogo é sempre escuro: o tabuleiro depende do contraste com o fundo noturno. */
@Composable
fun PrismaTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = PrismaColors,
        typography = PrismaType,
        content = content,
    )
}
