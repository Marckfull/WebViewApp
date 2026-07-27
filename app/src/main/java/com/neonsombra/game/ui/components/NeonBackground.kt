package com.neonsombra.game.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.neonsombra.game.ui.theme.NeonCyan
import com.neonsombra.game.ui.theme.NeonMagenta
import com.neonsombra.game.ui.theme.NeonNight
import com.neonsombra.game.ui.theme.NeonNightDeep
import com.neonsombra.game.ui.theme.NeonPurple
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private data class Star(val x: Float, val y: Float, val radius: Float, val phase: Float)

private data class Building(
    val x: Float,
    val width: Float,
    val height: Float,
    val antenna: Float,
    val windowSeed: Int,
)

/**
 * Cenario do jogo: ceu roxo, estrelas piscando, grade em fuga e a cidade
 * desenhada no rascunho, tudo em neon.
 */
@Composable
fun NeonBackground(
    modifier: Modifier = Modifier,
    showSkyline: Boolean = true,
    intensity: Float = 1f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val transition = rememberInfiniteTransition(label = "cenario")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "tempo",
    )
    val scroll by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "grade",
    )

    val stars = remember {
        val random = Random(1985)
        List(80) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat() * 0.72f,
                radius = 0.6f + random.nextFloat() * 1.7f,
                phase = random.nextFloat(),
            )
        }
    }

    val skyline = remember {
        val random = Random(2077)
        val list = mutableListOf<Building>()
        var cursor = -0.02f
        while (cursor < 1.02f) {
            val width = 0.05f + random.nextFloat() * 0.07f
            list += Building(
                x = cursor,
                width = width,
                height = 0.05f + random.nextFloat() * 0.13f,
                antenna = if (random.nextFloat() < 0.35f) 0.03f + random.nextFloat() * 0.05f else 0f,
                windowSeed = random.nextInt(1000),
            )
            cursor += width + 0.005f + random.nextFloat() * 0.02f
        }
        list.toList()
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        NeonNightDeep,
                        NeonNight,
                        Color(0xFF15043A).copy(alpha = 0.85f),
                    ),
                ),
            )
            drawStars(stars, time, intensity)
            drawHorizonGlow(intensity)
            drawPerspectiveGrid(scroll, intensity)
            if (showSkyline) drawSkyline(skyline, time, intensity)
        }
        content()
    }
}

private fun DrawScope.drawStars(stars: List<Star>, time: Float, intensity: Float) {
    stars.forEach { star ->
        val twinkle = 0.35f + 0.65f * abs(sin((time + star.phase) * 2f * PI.toFloat()))
        drawCircle(
            color = Color.White.copy(alpha = 0.55f * twinkle * intensity),
            radius = star.radius,
            center = Offset(star.x * size.width, star.y * size.height),
        )
    }
}

private fun DrawScope.drawHorizonGlow(intensity: Float) {
    val horizon = size.height * 0.70f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                NeonMagenta.copy(alpha = 0.18f * intensity),
                NeonPurple.copy(alpha = 0.10f * intensity),
                Color.Transparent,
            ),
            startY = horizon - size.height * 0.18f,
            endY = horizon + size.height * 0.10f,
        ),
        topLeft = Offset(0f, horizon - size.height * 0.18f),
        size = Size(size.width, size.height * 0.28f),
    )
}

private fun DrawScope.drawPerspectiveGrid(scroll: Float, intensity: Float) {
    val horizon = size.height * 0.70f
    val depth = size.height - horizon
    val center = size.width / 2f
    val lineColor = NeonPurple.copy(alpha = 0.30f * intensity)

    for (index in -10..10) {
        val topX = center + index * size.width * 0.035f
        val bottomX = center + index * size.width * 0.42f
        drawLine(
            color = lineColor,
            start = Offset(topX, horizon),
            end = Offset(bottomX, size.height),
            strokeWidth = 1.2f,
        )
    }

    for (step in 0..9) {
        val progress = ((step + scroll) / 10f).coerceIn(0f, 1f)
        val y = horizon + depth * progress * progress
        drawLine(
            color = NeonCyan.copy(alpha = 0.22f * intensity * progress),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.2f,
        )
    }
}

private fun DrawScope.drawSkyline(buildings: List<Building>, time: Float, intensity: Float) {
    val baseline = size.height
    buildings.forEach { building ->
        val left = building.x * size.width
        val width = building.width * size.width
        val height = building.height * size.height
        val top = baseline - height

        drawRect(
            color = Color(0xFF080314),
            topLeft = Offset(left, top),
            size = Size(width, height),
        )
        drawLine(
            color = NeonCyan.copy(alpha = 0.55f * intensity),
            start = Offset(left, top),
            end = Offset(left + width, top),
            strokeWidth = 2f,
        )
        drawLine(
            color = NeonMagenta.copy(alpha = 0.30f * intensity),
            start = Offset(left, top),
            end = Offset(left, baseline),
            strokeWidth = 1.4f,
        )

        if (building.antenna > 0f) {
            val antennaTop = top - building.antenna * size.height
            drawLine(
                color = NeonMagenta.copy(alpha = 0.55f * intensity),
                start = Offset(left + width / 2f, top),
                end = Offset(left + width / 2f, antennaTop),
                strokeWidth = 1.8f,
            )
            val blink = 0.4f + 0.6f * abs(sin((time * 3f + building.x) * PI.toFloat()))
            drawCircle(
                color = NeonMagenta.copy(alpha = blink * intensity),
                radius = 2.4f,
                center = Offset(left + width / 2f, antennaTop),
            )
        }

        // Janelas acesas, com um piscar preguicoso.
        val random = Random(building.windowSeed)
        val columns = (width / 9f).toInt().coerceAtLeast(1)
        val rows = (height / 12f).toInt().coerceAtLeast(1)
        for (column in 0 until columns) {
            for (row in 0 until rows) {
                if (random.nextFloat() > 0.42f) continue
                val flicker = if (random.nextFloat() < 0.15f) {
                    0.25f + 0.75f * abs(sin((time * 4f + column + row) * PI.toFloat()))
                } else {
                    1f
                }
                drawRect(
                    color = NeonCyan.copy(alpha = 0.34f * flicker * intensity),
                    topLeft = Offset(left + 3f + column * 9f, top + 5f + row * 12f),
                    size = Size(3.5f, 5f),
                )
            }
        }
    }
}
