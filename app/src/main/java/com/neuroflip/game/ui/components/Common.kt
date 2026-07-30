package com.neuroflip.game.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflip.game.ui.theme.LocalNeuro
import kotlin.math.cos
import kotlin.math.sin

/** Fundo animado: névoa neon em movimento lento + malha sináptica discreta. */
@Composable
fun AnimatedNeuroBackground(modifier: Modifier = Modifier, intensity: Float = 1f) {
    val palette = LocalNeuro.current
    val transition = rememberInfiniteTransition(label = "bg")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(28_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(modifier.background(palette.backgroundBrush)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val blobs = listOf(
                Triple(palette.primary, Offset(w * (0.25f + 0.16f * cos(phase)), h * (0.22f + 0.09f * sin(phase))), w * 0.75f),
                Triple(palette.secondary, Offset(w * (0.78f + 0.14f * sin(phase * 0.8f)), h * (0.68f + 0.10f * cos(phase * 1.1f))), w * 0.68f),
                Triple(palette.accent, Offset(w * (0.5f + 0.2f * cos(phase * 0.6f)), h * (0.95f + 0.05f * sin(phase))), w * 0.6f)
            )

            blobs.forEach { (color, center, radius) ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.20f * intensity), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }

            // Malha de fundo
            val step = 56f
            val gridColor = palette.primary.copy(alpha = 0.05f * intensity)
            var x = 0f
            while (x < w) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < h) {
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                y += step
            }
        }
    }
}

enum class NeonVariant { PRIMARY, GHOST, DANGER, GOLD }

@Composable
fun NeonButton(
    text: String,
    modifier: Modifier = Modifier,
    glyph: String? = null,
    variant: NeonVariant = NeonVariant.PRIMARY,
    enabled: Boolean = true,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val palette = LocalNeuro.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(90, easing = FastOutSlowInEasing),
        label = "press"
    )

    val accent = when (variant) {
        NeonVariant.PRIMARY -> palette.primary
        NeonVariant.GHOST -> palette.textDim
        NeonVariant.DANGER -> palette.danger
        NeonVariant.GOLD -> palette.gold
    }

    val fill = when (variant) {
        NeonVariant.PRIMARY -> Brush.horizontalGradient(
            listOf(palette.primary.copy(alpha = 0.22f), palette.secondary.copy(alpha = 0.22f))
        )
        NeonVariant.GOLD -> Brush.horizontalGradient(
            listOf(palette.gold.copy(alpha = 0.26f), palette.secondary.copy(alpha = 0.18f))
        )
        else -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .border(1.5.dp, accent.copy(alpha = 0.85f), RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .padding(horizontal = 22.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (glyph != null) {
                Text(glyph, fontSize = 19.sp, modifier = Modifier.padding(end = 10.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = text,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = palette.textDim,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalNeuro.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surface.copy(alpha = 0.72f))
            .border(
                1.dp,
                (borderColor ?: palette.primary).copy(alpha = 0.30f),
                RoundedCornerShape(22.dp)
            )
            .padding(18.dp),
        content = content
    )
}

@Composable
fun StatChip(
    glyph: String,
    value: String,
    modifier: Modifier = Modifier,
    tint: Color? = null
) {
    val palette = LocalNeuro.current
    val color = tint ?: palette.primary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.void0.copy(alpha = 0.55f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(glyph, fontSize = 14.sp)
        Text(
            text = value,
            color = palette.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

/** Barra de energia com brilho — usada pela SINAPSE e pelo cronômetro. */
@Composable
fun NeonBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    brush: Brush? = null,
    trackAlpha: Float = 0.25f
) {
    val palette = LocalNeuro.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "bar"
    )
    Box(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(palette.void0.copy(alpha = trackAlpha))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(brush ?: palette.primaryBrush)
        )
    }
}

@Composable
fun StarRow(stars: Int, modifier: Modifier = Modifier, size: Int = 22, max: Int = 3) {
    val palette = LocalNeuro.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(max) { index ->
            val earned = index < stars
            Text(
                text = if (earned) "★" else "☆",
                fontSize = size.sp,
                color = if (earned) palette.gold else palette.textDim.copy(alpha = 0.5f)
            )
        }
    }
}

/** Selo "assista um vídeo" — deixa claro que a recompensa vem de anúncio. */
@Composable
fun RewardedAdButton(
    label: String,
    reward: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val palette = LocalNeuro.current
    val transition = rememberInfiniteTransition(label = "reward")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1_400), RepeatMode.Reverse),
        label = "glow"
    )
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(palette.gold.copy(alpha = 0.20f), palette.secondary.copy(alpha = 0.14f))
                )
            )
            .border(1.5.dp, palette.gold.copy(alpha = glow), RoundedCornerShape(18.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(palette.gold.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Text("▶", fontSize = 14.sp, color = palette.gold)
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = palette.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(reward, color = palette.gold, fontSize = 12.sp)
        }
        Text("VÍDEO", color = palette.textDim, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

/** Explosão de partículas ao acertar um par. */
@Composable
fun ParticleBurst(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    particles: Int = 10
) {
    Canvas(modifier) {
        if (progress <= 0f || progress >= 1f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension * 0.75f
        repeat(particles) { i ->
            val angle = (2 * Math.PI * i / particles).toFloat()
            val distance = maxRadius * progress
            val p = Offset(
                center.x + cos(angle) * distance,
                center.y + sin(angle) * distance
            )
            drawCircle(
                color = color.copy(alpha = (1f - progress).coerceIn(0f, 1f)),
                radius = size.minDimension * 0.05f * (1f - progress),
                center = p
            )
        }
        drawCircle(
            color = color.copy(alpha = (0.5f * (1f - progress)).coerceIn(0f, 1f)),
            radius = maxRadius * progress,
            center = center,
            style = Stroke(width = 3f * (1f - progress))
        )
    }
}

