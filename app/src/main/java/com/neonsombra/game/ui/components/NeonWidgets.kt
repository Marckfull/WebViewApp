package com.neonsombra.game.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neonsombra.game.ui.LocalSound
import com.neonsombra.game.ui.theme.NeonCyan
import com.neonsombra.game.ui.theme.NeonMagenta
import com.neonsombra.game.ui.theme.NeonSurfaceSoft
import com.neonsombra.game.ui.theme.NeonTextMuted

/**
 * Texto com halo de neon: uma camada difusa por baixo e o nucleo aceso por cima.
 */
@Composable
fun NeonText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NeonCyan,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    glowRadius: Float = 16f,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val inner = if (textAlign != null) Modifier.fillMaxWidth() else Modifier
    Box(modifier = modifier) {
        Text(
            text = text,
            modifier = inner,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style.copy(
                color = color.copy(alpha = 0.55f),
                shadow = Shadow(color = color, offset = Offset.Zero, blurRadius = glowRadius * 2f),
            ),
        )
        Text(
            text = text,
            modifier = inner,
            textAlign = textAlign,
            maxLines = maxLines,
            style = style.copy(
                color = Color.White.copy(alpha = 0.92f),
                shadow = Shadow(color = color, offset = Offset.Zero, blurRadius = glowRadius),
            ),
        )
    }
}

/**
 * O logotipo do jogo, com pulsacao e o reflexo sombrio invertido logo abaixo --
 * a mesma ideia que o jogador enfrenta no tabuleiro.
 */
@Composable
fun NeonLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "logo")
    val pulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "pulse",
    )

    // Na tela de jogo o logo vira uma unica linha, para sobrar espaco ao tabuleiro.
    if (compact) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeonText(
                text = "NEON ",
                color = NeonCyan,
                glowRadius = 16f * pulse,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
            )
            NeonText(
                text = "SOMBRA",
                color = NeonMagenta,
                glowRadius = 16f * pulse,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
            )
        }
        return
    }

    val titleStyle = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NeonText(
            text = "NEON",
            color = NeonCyan,
            glowRadius = 26f * pulse,
            textAlign = TextAlign.Center,
            style = titleStyle,
        )
        NeonText(
            text = "SOMBRA",
            color = NeonMagenta,
            glowRadius = 26f * pulse,
            textAlign = TextAlign.Center,
            style = titleStyle,
        )
        // Reflexo do lado sombrio, a mesma ideia que o jogador enfrenta no tabuleiro.
        NeonText(
            text = "SOMBRA",
            color = NeonMagenta,
            glowRadius = 8f,
            textAlign = TextAlign.Center,
            style = titleStyle,
            modifier = Modifier.graphicsLayer {
                scaleY = -1f
                alpha = 0.16f
            },
        )
        Text(
            text = "O  L A D O  E S C U R O  D O S  B L O C O S",
            color = NeonTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        )
    }
}

/** Caixa de vidro escuro com borda acesa, usada em todos os painteis do jogo. */
@Composable
fun NeonPanel(
    modifier: Modifier = Modifier,
    accent: Color = NeonCyan,
    contentPadding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = accent.copy(alpha = 0.14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                    style = Stroke(width = 7.dp.toPx()),
                )
            }
            .background(
                brush = Brush.verticalGradient(
                    listOf(NeonSurfaceSoft, Color(0xAA0A0420)),
                ),
                shape = shape,
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.25f)),
                ),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

/** Botao de fliperama: escala ao toque, brilha e toca um clique. */
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = NeonCyan,
    enabled: Boolean = true,
    leading: String? = null,
) {
    val sound = LocalSound.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(90),
        label = "press",
    )
    val alpha = if (enabled) 1f else 0.35f
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .scale(scale)
            .drawBehind {
                drawRoundRect(
                    color = accent.copy(alpha = 0.18f * alpha),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 8.dp.toPx()),
                )
            }
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.28f * alpha),
                        accent.copy(alpha = 0.10f * alpha),
                    ),
                ),
                shape = shape,
            )
            .border(1.5.dp, accent.copy(alpha = 0.85f * alpha), shape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
            ) {
                sound.click()
                onClick()
            }
            .padding(horizontal = 22.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Text(
                text = leading,
                style = MaterialTheme.typography.labelLarge,
                color = accent.copy(alpha = alpha),
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        NeonText(
            text = text,
            color = accent.copy(alpha = alpha),
            glowRadius = if (enabled) 14f else 0f,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Rotulo pequeno em caixa alta, no estilo dos placares de fliperama. */
@Composable
fun NeonLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NeonTextMuted,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.labelSmall,
    )
}
