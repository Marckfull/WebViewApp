package com.kardiapulse.game.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kardiapulse.game.ui.theme.DeepIndigo
import com.kardiapulse.game.ui.theme.NightViolet
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.PulseViolet
import com.kardiapulse.game.ui.theme.VoidBlack
import kotlin.math.PI
import kotlin.math.sin

private data class Mote(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val phase: Float,
    val color: Color
)

/**
 * O fundo vivo do jogo: um gradiente profundo com duas auroras que respiram e uma poeira de
 * partículas subindo devagar. Tudo desenhado em um único Canvas, sem imagens.
 */
@Composable
fun KardiaBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    content: @Composable BoxScope.() -> Unit
) {
    val motes = remember {
        val random = java.util.Random(20260806L)
        List(46) {
            Mote(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.6f + random.nextFloat() * 2.2f,
                speed = 0.012f + random.nextFloat() * 0.05f,
                phase = random.nextFloat() * 6.283f,
                color = if (random.nextFloat() > 0.7f) PulseCyan else PulseViolet
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "fundo")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing)),
        label = "deriva"
    )
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(9_000, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "respiro"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(VoidBlack, DeepIndigo, NightViolet, VoidBlack))
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Duas auroras que se movem em fases opostas.
            val auroraOne = Offset(
                x = w * (0.24f + 0.10f * sin(drift * 2 * PI.toFloat())),
                y = h * (0.20f + 0.06f * breath)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PulseViolet.copy(alpha = 0.20f * intensity * (0.7f + 0.3f * breath)),
                        Color.Transparent
                    ),
                    center = auroraOne,
                    radius = w * 0.85f
                ),
                radius = w * 0.85f,
                center = auroraOne
            )

            val auroraTwo = Offset(
                x = w * (0.78f - 0.12f * sin(drift * 2 * PI.toFloat() + 1.4f)),
                y = h * (0.76f - 0.05f * breath)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PulseCyan.copy(alpha = 0.14f * intensity * (1f - 0.3f * breath)),
                        Color.Transparent
                    ),
                    center = auroraTwo,
                    radius = w * 0.75f
                ),
                radius = w * 0.75f,
                center = auroraTwo
            )

            // Poeira luminosa subindo em loop.
            for (mote in motes) {
                val y = ((mote.y - drift * mote.speed * 22f) % 1f + 1f) % 1f
                val twinkle = 0.35f + 0.65f * ((sin(drift * 12f + mote.phase) + 1f) / 2f)
                drawCircle(
                    color = mote.color.copy(alpha = 0.26f * twinkle * intensity),
                    radius = mote.radius,
                    center = Offset(mote.x * w, y * h)
                )
            }
        }
        content()
    }
}
