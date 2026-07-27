package com.chuvadeletras.game.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin
import kotlin.random.Random

private data class RainDrop(
    val x: Float,
    val speed: Float,
    val length: Float,
    val phase: Float,
    val alpha: Float,
    val width: Float
)

/**
 * Chuva de fundo. Uma única animação infinita move todas as gotas — nada de
 * um estado por partícula, para não pesar na tela de jogo.
 */
@Composable
fun RainBackground(
    modifier: Modifier = Modifier,
    dropCount: Int = 55,
    color: Color = Color.White,
    baseAlpha: Float = 0.16f,
    speedMillis: Int = 2600
) {
    val drops = remember(dropCount) {
        val random = Random(dropCount * 31)
        List(dropCount) {
            RainDrop(
                x = random.nextFloat(),
                speed = 0.55f + random.nextFloat() * 0.9f,
                length = 18f + random.nextFloat() * 46f,
                phase = random.nextFloat(),
                alpha = 0.25f + random.nextFloat() * 0.75f,
                width = 1f + random.nextFloat() * 1.8f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "rain")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(speedMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainProgress"
    )

    Canvas(modifier = modifier) {
        val travel = size.height + 160f
        drops.forEach { drop ->
            val cycle = (progress * drop.speed + drop.phase) % 1f
            val y = cycle * travel - 80f
            val x = drop.x * size.width + sin((cycle + drop.phase) * 6.28f) * 6f
            drawLine(
                color = color.copy(alpha = baseAlpha * drop.alpha),
                start = Offset(x, y),
                end = Offset(x, y + drop.length),
                strokeWidth = drop.width,
                cap = StrokeCap.Round
            )
        }
    }
}

private data class ConfettiPiece(
    val x: Float,
    val delay: Float,
    val drift: Float,
    val spin: Float,
    val size: Float,
    val color: Color,
    val speed: Float
)

/**
 * Chuva de confete da comemoração. [running] liga e desliga; quando desligado
 * a animação para de desenhar e não custa nada.
 */
@Composable
fun ConfettiOverlay(
    running: Boolean,
    modifier: Modifier = Modifier,
    pieceCount: Int = 90,
    colors: List<Color> = listOf(
        Color(0xFF45D3F5),
        Color(0xFFFFD166),
        Color(0xFFA78BFA),
        Color(0xFF4ADE80),
        Color(0xFFFF6B6B)
    )
) {
    if (!running) return

    val pieces = remember(pieceCount) {
        val random = Random(pieceCount * 7)
        List(pieceCount) {
            ConfettiPiece(
                x = random.nextFloat(),
                delay = random.nextFloat(),
                drift = (random.nextFloat() - 0.5f) * 0.28f,
                spin = 2f + random.nextFloat() * 8f,
                size = 7f + random.nextFloat() * 12f,
                color = colors[random.nextInt(colors.size)],
                speed = 0.7f + random.nextFloat() * 0.8f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiProgress"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            pieces.forEach { piece ->
                val cycle = (progress * piece.speed + piece.delay) % 1f
                val y = cycle * (size.height + 120f) - 60f
                val x = (piece.x + piece.drift * cycle) * size.width
                val angle = cycle * 360f * piece.spin
                // Encolhe na vertical para dar impressão de girar no ar.
                val squash = 0.35f + 0.65f * kotlin.math.abs(sin(cycle * 12f))
                rotate(degrees = angle, pivot = Offset(x, y)) {
                    drawRect(
                        color = piece.color.copy(alpha = (1f - cycle).coerceIn(0f, 1f)),
                        topLeft = Offset(x - piece.size / 2f, y - piece.size / 2f),
                        size = Size(piece.size, piece.size * squash)
                    )
                }
            }
        }
    }
}

/** Halo pulsante usado para destacar botões importantes. */
@Composable
fun PulsingGlow(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFD166),
    maxRadius: Float = 1.25f
) {
    val transition = rememberInfiniteTransition(label = "glow")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = maxRadius,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f * scale
        drawCircle(
            color = color.copy(alpha = 0.18f * (maxRadius - scale + 0.4f)),
            radius = radius,
            center = center
        )
    }
}
