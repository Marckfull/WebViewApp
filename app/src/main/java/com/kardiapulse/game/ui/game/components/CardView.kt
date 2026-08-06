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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiapulse.game.core.model.Card
import com.kardiapulse.game.ui.theme.PulseViolet
import com.kardiapulse.game.ui.theme.SurfaceCard
import com.kardiapulse.game.ui.theme.SurfaceStroke
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary
import com.kardiapulse.game.ui.theme.VoidBlack
import com.kardiapulse.game.ui.theme.color
import kotlin.math.sin

/**
 * Uma carta na mão.
 *
 * Três estados visuais carregam a informação que importa durante o duelo:
 * - **jogável ou não**: cartas sem jogada legal ficam apagadas e sem borda;
 * - **ressonante**: uma auréola pulsante marca as cartas capazes de inverter a direção,
 *   que é a decisão mais importante do jogo;
 * - **selecionada**: a carta sobe e ganha brilho enquanto a polaridade é escolhida.
 */
@Composable
fun CardView(
    card: Card,
    modifier: Modifier = Modifier,
    playable: Boolean = true,
    resonant: Boolean = false,
    selected: Boolean = false,
    hideValue: Boolean = false,
    width: Int = 62,
    height: Int = 88,
    onClick: (() -> Unit)? = null
) {
    val elementColor = card.element.color

    val lift by animateFloatAsState(
        targetValue = if (selected) -18f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "elevacao"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.10f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "escala"
    )

    val transition = rememberInfiniteTransition(label = "carta")
    val halo by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "halo"
    )

    val borderColor = when {
        !playable -> SurfaceStroke.copy(alpha = 0.4f)
        selected -> elementColor
        resonant -> elementColor.copy(alpha = 0.55f + 0.45f * halo)
        else -> SurfaceStroke
    }

    Box(
        modifier = modifier
            .size(width = width.dp, height = height.dp)
            .graphicsLayer {
                translationY = lift
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (playable) 1f else 0.38f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        elementColor.copy(alpha = if (playable) 0.28f else 0.10f),
                        SurfaceCard,
                        VoidBlack
                    )
                )
            )
            .border(
                BorderStroke(if (selected || resonant) 2.dp else 1.dp, borderColor),
                RoundedCornerShape(12.dp)
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // Brilho interno que acompanha a auréola de ressonância.
        if (resonant && playable) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                elementColor.copy(alpha = 0.18f * (0.4f + 0.6f * halo)),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = card.element.glyph,
                color = elementColor,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Text(
                text = if (hideValue) "?" else card.value.toString(),
                color = if (playable) TextPrimary else TextMuted,
                fontSize = (width * 0.52f).sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = card.element.ptName.uppercase(),
                color = elementColor.copy(alpha = 0.85f),
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** O verso da carta, usado para a mão do rival. */
@Composable
fun CardBack(
    modifier: Modifier = Modifier,
    width: Int = 44,
    height: Int = 62,
    index: Int = 0,
    tint: Color = PulseViolet
) {
    val transition = rememberInfiniteTransition(label = "verso")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "brilho"
    )
    val wave = sin((shimmer * 6.283f) + index * 0.7f) * 0.5f + 0.5f

    Box(
        modifier = modifier
            .size(width = width.dp, height = height.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        SurfaceCard,
                        tint.copy(alpha = 0.20f + 0.14f * wave),
                        VoidBlack
                    )
                )
            )
            .border(BorderStroke(1.dp, SurfaceStroke), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✦",
            color = tint.copy(alpha = 0.5f + 0.3f * wave),
            fontSize = (width * 0.36f).sp
        )
    }
}

/** Espaçador fino usado entre cartas. */
@Composable
fun CardGap(size: Int = 6) {
    Box(Modifier.width(size.dp).height(1.dp))
}
