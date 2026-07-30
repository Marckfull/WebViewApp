package com.neuroflip.game.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflip.game.domain.Card
import com.neuroflip.game.domain.CardKind
import com.neuroflip.game.ui.theme.LocalNeuro

/**
 * Carta com virada 3D real (rotationY + cameraDistance), brilho por tipo,
 * anel de pulso ECO e animação de "resolvida".
 */
@Composable
fun MemoryCardView(
    card: Card,
    faceVisible: Boolean,
    previewing: Boolean,
    cellSize: Dp,
    modifier: Modifier = Modifier,
    shake: Boolean = false,
    onClick: () -> Unit
) {
    val palette = LocalNeuro.current
    val density = LocalDensity.current

    val rotation by animateFloatAsState(
        targetValue = if (faceVisible) 180f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "flip"
    )
    val matchedScale by animateFloatAsState(
        targetValue = if (card.matched) 0.86f else 1f,
        animationSpec = tween(320),
        label = "matched"
    )
    val shakeOffset by animateFloatAsState(
        targetValue = if (shake) 1f else 0f,
        animationSpec = tween(120),
        label = "shake"
    )

    val pulse = rememberInfiniteTransition(label = "cardPulse")
    val glow by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1_100), RepeatMode.Reverse),
        label = "glow"
    )

    val kindColor = when (card.kind) {
        CardKind.GOLDEN -> palette.gold
        CardKind.CRYO -> palette.accent
        CardKind.MIRROR -> palette.secondary
        CardKind.PHANTOM -> palette.textDim
        CardKind.UNSTABLE -> palette.danger
        CardKind.NORMAL -> palette.primary
    }

    val borderColor = when {
        card.matched -> palette.primary.copy(alpha = 0.35f)
        previewing -> palette.accent.copy(alpha = glow)
        card.tagged -> palette.secondary.copy(alpha = 0.9f)
        faceVisible -> kindColor
        else -> palette.cardBackEdge.copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier
            .size(cellSize)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density.density
                scaleX = matchedScale
                scaleY = matchedScale
                translationX = if (shakeOffset > 0f) shakeOffset * 10f else 0f
                alpha = if (card.matched) 0.55f else 1f
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (rotation < 90f) backBrush(palette) else faceBrush(palette))
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = !card.matched && !faceVisible) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (rotation < 90f) {
            // Verso: logotipo sináptico discreto
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(cellSize * 0.22f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                palette.cardBackEdge.copy(alpha = 0.18f),
                                Color.Transparent,
                                palette.secondary.copy(alpha = 0.14f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "◇",
                    color = palette.cardBackEdge.copy(alpha = 0.55f),
                    fontSize = (cellSize.value * 0.28f).sp
                )
            }
        } else {
            // Frente: espelhada de volta para não aparecer invertida
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = card.symbol.glyph,
                    fontSize = (cellSize.value * 0.44f).sp
                )
                if (card.kind != CardKind.NORMAL) {
                    Text(
                        text = card.kind.glyph,
                        color = kindColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = (cellSize.value * 0.20f).sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

private fun backBrush(palette: com.neuroflip.game.ui.theme.NeuroPalette) = Brush.linearGradient(
    listOf(palette.cardBack, palette.cardBack.copy(alpha = 0.75f), palette.surface)
)

private fun faceBrush(palette: com.neuroflip.game.ui.theme.NeuroPalette) = Brush.linearGradient(
    listOf(palette.cardFace, palette.surface)
)
