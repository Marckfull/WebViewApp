package com.prisma.fusao.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prisma.fusao.ui.components.StarfieldBackground
import com.prisma.fusao.ui.theme.BrancoGelo
import com.prisma.fusao.ui.theme.LilasClaro
import kotlinx.coroutines.delay

/**
 * Splash com a metáfora do jogo: um feixe branco entra no prisma e sai decomposto
 * nas seis cores das gemas. É a explicação visual da mecânica antes da primeira fase.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var started by remember { mutableStateOf(false) }

    val beam by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(900, easing = LinearEasing),
        label = "feixe",
    )
    val spectrum by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(1100, delayMillis = 650, easing = LinearEasing),
        label = "espectro",
    )
    val title by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(700, delayMillis = 1150),
        label = "titulo",
    )

    LaunchedEffect(Unit) {
        started = true
        delay(2600)
        onFinished()
    }

    StarfieldBackground {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val side = size.minDimension * 0.34f

                    // Feixe branco entrando pela esquerda.
                    if (beam > 0f) {
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.95f)),
                            ),
                            start = Offset(cx - side * 3f, cy),
                            end = Offset(cx - side * 3f + (side * 2.4f) * beam, cy),
                            strokeWidth = 6f,
                        )
                    }

                    // Espectro saindo pela direita, uma faixa por cor de gema.
                    if (spectrum > 0f) {
                        val colors = listOf(
                            Color(0xFFFF4D6D), Color(0xFFFF9F45), Color(0xFFFFDD57),
                            Color(0xFF4ADE80), Color(0xFF48BFE3), Color(0xFFB185FF),
                        )
                        colors.forEachIndexed { index, color ->
                            val spread = (index - 2.5f) * side * 0.20f
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    listOf(color.copy(alpha = 0.95f), Color.Transparent),
                                ),
                                start = Offset(cx + side * 0.35f, cy),
                                end = Offset(
                                    cx + side * 0.35f + side * 3f * spectrum,
                                    cy + spread * spectrum * 3f,
                                ),
                                strokeWidth = 7f,
                            )
                        }
                    }

                    // O prisma.
                    val triangle = Path().apply {
                        moveTo(cx, cy - side)
                        lineTo(cx + side * 0.9f, cy + side * 0.72f)
                        lineTo(cx - side * 0.9f, cy + side * 0.72f)
                        close()
                    }
                    drawPath(
                        triangle,
                        Brush.linearGradient(
                            listOf(Color(0x55FFFFFF), Color(0x18FFFFFF)),
                            start = Offset(cx - side, cy - side),
                            end = Offset(cx + side, cy + side),
                        ),
                    )
                    drawPath(triangle, Color.White.copy(alpha = 0.85f), style = Stroke(width = 5f))
                }
            }

            Text(
                "PRISMA",
                style = MaterialTheme.typography.displayLarge,
                color = BrancoGelo,
                modifier = Modifier.alpha(title),
            )
            Text(
                "Fusão Cromática",
                fontSize = 17.sp,
                color = LilasClaro,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(title),
            )
        }
    }
}
