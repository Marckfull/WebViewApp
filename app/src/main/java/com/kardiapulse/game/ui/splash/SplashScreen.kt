package com.kardiapulse.game.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.kardiapulse.game.ui.common.KardiaBackground
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.PulseViolet
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A abertura: um Núcleo se acendendo antes do nome aparecer. Dura pouco mais de dois segundos
 * e serve também como tela de carregamento enquanto o áudio é sintetizado em background.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    var titleAlpha by remember { mutableFloatStateOf(0f) }
    var ringScale by remember { mutableFloatStateOf(0.2f) }

    LaunchedEffect(Unit) {
        // Anima na mão para controlar a sequência exata: anel primeiro, nome depois.
        val steps = 34
        repeat(steps) { i ->
            ringScale = 0.2f + 0.8f * easeOut(i / steps.toFloat())
            delay(16)
        }
        repeat(steps) { i ->
            titleAlpha = easeOut(i / steps.toFloat())
            delay(16)
        }
        delay(950)
        onDone()
    }

    val transition = rememberInfiniteTransition(label = "abertura")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "giro"
    )
    val glow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "brilho"
    )

    KardiaBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(190.dp)) {
                    val radius = size.minDimension * 0.36f * ringScale
                    val center = Offset(size.width / 2, size.height / 2)

                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(PulseCyan.copy(alpha = 0.25f * glow), Color.Transparent),
                            center = center,
                            radius = radius * 2.1f
                        ),
                        radius = radius * 2.1f,
                        center = center
                    )
                    drawCircle(
                        color = PulseViolet.copy(alpha = 0.55f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )
                    // Três arcos girando em velocidades diferentes.
                    for (k in 0..2) {
                        val start = spin * 360f * (1f + k * 0.35f) + k * 120f
                        drawArc(
                            color = if (k % 2 == 0) PulseCyan else PulseViolet,
                            startAngle = start,
                            sweepAngle = 52f - k * 9f,
                            useCenter = false,
                            topLeft = Offset(
                                center.x - radius * (1f + k * 0.16f),
                                center.y - radius * (1f + k * 0.16f)
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                radius * 2 * (1f + k * 0.16f),
                                radius * 2 * (1f + k * 0.16f)
                            ),
                            style = Stroke(width = 4f - k * 0.8f, cap = StrokeCap.Round)
                        )
                    }
                    // Faíscas orbitando.
                    for (k in 0..5) {
                        val angle = (spin * 2 * PI.toFloat()) + k * (PI.toFloat() / 3f)
                        val r = radius * 1.45f
                        drawCircle(
                            color = PulseCyan.copy(alpha = 0.7f),
                            radius = 2.4f,
                            center = Offset(
                                center.x + cos(angle) * r,
                                center.y + sin(angle) * r
                            )
                        )
                    }
                }
                Text(
                    text = "✦",
                    color = PulseCyan.copy(alpha = 0.6f + 0.4f * glow),
                    fontSize = 30.sp
                )
            }

            Spacer(Modifier.height(30.dp))
            Text(
                text = "KARDIA",
                color = TextPrimary,
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 12.sp,
                modifier = Modifier.alpha(titleAlpha)
            )
            Text(
                text = "P U L S E",
                color = PulseCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                modifier = Modifier.alpha(titleAlpha)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "quem fica sem saída, perde",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(titleAlpha * 0.9f)
            )
        }
    }
}

private fun easeOut(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return 1f - (1f - x) * (1f - x) * (1f - x)
}
