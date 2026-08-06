package com.kardiapulse.game.ui.game.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiapulse.game.core.model.Element
import com.kardiapulse.game.ui.theme.DangerRed
import com.kardiapulse.game.ui.theme.NegativePole
import com.kardiapulse.game.ui.theme.PositivePole
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.SurfaceCard
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary
import com.kardiapulse.game.ui.theme.TextSecondary
import com.kardiapulse.game.ui.theme.color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val START_ANGLE = 135f
private const val SWEEP = 270f

/**
 * O medidor do Núcleo: o elemento visual central do jogo.
 *
 * O arco vai de -limite (esquerda) a +limite (direita), com o zero no topo. Três informações
 * ficam legíveis de relance durante o duelo:
 * - o **preenchimento** mostra para que lado e quão longe o Núcleo foi;
 * - as **faixas escuras nas pontas** são o Colapso: o campo que já foi perdido nesta rodada;
 * - a **cor** vira vermelha conforme a folga acaba.
 */
@Composable
fun NucleusGauge(
    nucleus: Int,
    baseLimit: Int,
    limitNow: Int,
    direction: Int,
    lastElement: Element?,
    chain: Int,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = nucleus.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nucleo"
    )
    val animatedLimit by animateFloatAsState(
        targetValue = limitNow.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "limite"
    )

    val transition = rememberInfiniteTransition(label = "medidor")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "pulso"
    )

    val headroom = limitNow - abs(nucleus)
    val danger = (1f - (headroom.toFloat() / limitNow.coerceAtLeast(1))).coerceIn(0f, 1f)
    val poleColor = when {
        nucleus > 0 -> PositivePole
        nucleus < 0 -> NegativePole
        else -> PulseCyan
    }
    val liveColor = lerpColor(poleColor, DangerRed, danger * 0.85f)

    Box(modifier = modifier.aspectRatio(1.25f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.075f
            val radius = size.minDimension * 0.40f
            val center = Offset(size.width / 2f, size.height * 0.56f)
            val arcSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(center.x - radius, center.y - radius)

            // Trilho completo.
            drawArc(
                color = SurfaceCard,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Zonas mortas do Colapso, nas duas pontas.
            val lostFraction = ((baseLimit - animatedLimit) / (2f * baseLimit)).coerceIn(0f, 0.49f)
            if (lostFraction > 0.001f) {
                val lostSweep = SWEEP * lostFraction
                drawArc(
                    color = DangerRed.copy(alpha = 0.30f),
                    startAngle = START_ANGLE,
                    sweepAngle = lostSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
                )
                drawArc(
                    color = DangerRed.copy(alpha = 0.30f),
                    startAngle = START_ANGLE + SWEEP - lostSweep,
                    sweepAngle = lostSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
                )
            }

            // Preenchimento do zero até o valor atual.
            val zeroAngle = angleFor(0f, baseLimit)
            val valueAngle = angleFor(animatedValue, baseLimit)
            val from = minOf(zeroAngle, valueAngle)
            val span = abs(valueAngle - zeroAngle)
            if (span > 0.3f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(liveColor.copy(alpha = 0.85f), liveColor, liveColor.copy(alpha = 0.85f))
                    ),
                    startAngle = from,
                    sweepAngle = span,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(
                        width = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }

            // Marca do zero.
            drawTick(center, radius, zeroAngle, stroke, TextSecondary, 1.55f)

            // Marcas do limite vivo.
            drawTick(center, radius, angleFor(animatedLimit, baseLimit), stroke, DangerRed, 1.35f)
            drawTick(center, radius, angleFor(-animatedLimit, baseLimit), stroke, DangerRed, 1.35f)

            // Ponteiro luminoso na posição atual.
            val rad = valueAngle * PI.toFloat() / 180f
            val knob = Offset(
                center.x + cos(rad) * radius,
                center.y + sin(rad) * radius
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(liveColor.copy(alpha = 0.55f), Color.Transparent),
                    center = knob,
                    radius = stroke * 2.6f
                ),
                radius = stroke * 2.6f,
                center = knob
            )
            drawCircle(color = liveColor, radius = stroke * 0.62f, center = knob)
            drawCircle(color = TextPrimary, radius = stroke * 0.26f, center = knob)

            // Halo central, mais forte conforme o perigo aumenta.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        liveColor.copy(alpha = (0.08f + 0.22f * danger) * (0.6f + 0.4f * pulse)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.95f
                ),
                radius = radius * 0.95f,
                center = center
            )
        }

        // Leitura numérica no miolo.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = if (nucleus > 0) "+$nucleus" else nucleus.toString(),
                color = liveColor,
                fontSize = 54.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "limite ±$limitNow",
                color = if (limitNow < baseLimit) DangerRed.copy(alpha = 0.9f) else TextMuted,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/** Faixa de estado abaixo do medidor: direção travada, último elemento e corrente. */
@Composable
fun NucleusStatusRow(
    direction: Int,
    lastElement: Element?,
    chain: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusPill(
            label = "direção",
            value = when (direction) {
                1 -> "subindo"
                -1 -> "descendo"
                else -> "livre"
            },
            color = when (direction) {
                1 -> PositivePole
                -1 -> NegativePole
                else -> PulseCyan
            },
            glyph = when (direction) {
                1 -> "▲"
                -1 -> "▼"
                else -> "◇"
            }
        )
        StatusPill(
            label = "ressoa com",
            value = lastElement?.ptName ?: "qualquer",
            color = lastElement?.color ?: PulseCyan,
            glyph = lastElement?.glyph ?: "✦"
        )
        if (chain > 1) {
            StatusPill(
                label = "corrente",
                value = chain.toString(),
                color = com.kardiapulse.game.ui.theme.PulseGold,
                glyph = "≡"
            )
        }
    }
}

@Composable
private fun StatusPill(label: String, value: String, color: Color, glyph: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$glyph $value",
            color = color,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = label.uppercase(),
            color = TextMuted,
            fontSize = 8.5.sp,
            letterSpacing = 1.sp
        )
    }
}

// ---------------------------------------------------------------------- utilidades

private fun angleFor(value: Float, limit: Int): Float {
    val fraction = ((value + limit) / (2f * limit)).coerceIn(0f, 1f)
    return START_ANGLE + SWEEP * fraction
}

private fun DrawScope.drawTick(
    center: Offset,
    radius: Float,
    angleDegrees: Float,
    stroke: Float,
    color: Color,
    lengthFactor: Float
) {
    val rad = angleDegrees * PI.toFloat() / 180f
    val inner = radius - stroke * 0.55f
    val outer = radius + stroke * 0.55f * lengthFactor
    drawLine(
        color = color.copy(alpha = 0.8f),
        start = Offset(center.x + cos(rad) * inner, center.y + sin(rad) * inner),
        end = Offset(center.x + cos(rad) * outer, center.y + sin(rad) * outer),
        strokeWidth = stroke * 0.16f
    )
}

private fun lerpColor(from: Color, to: Color, t: Float): Color {
    val f = t.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * f,
        green = from.green + (to.green - from.green) * f,
        blue = from.blue + (to.blue - from.blue) * f,
        alpha = from.alpha + (to.alpha - from.alpha) * f
    )
}
