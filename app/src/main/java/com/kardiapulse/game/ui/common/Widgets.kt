package com.kardiapulse.game.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.PulseViolet
import com.kardiapulse.game.ui.theme.SurfaceCard
import com.kardiapulse.game.ui.theme.SurfaceStroke
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary
import com.kardiapulse.game.ui.theme.TextSecondary
import com.kardiapulse.game.ui.theme.VoidBlack

/** Painel translúcido com borda fina. A caixa padrão de tudo no jogo. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = SurfaceStroke,
    background: Color = SurfaceCard.copy(alpha = 0.82f),
    cornerRadius: Int = 22,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(background)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(cornerRadius.dp))
            .padding(16.dp),
        content = content
    )
}

/**
 * O botão principal. Encolhe ao ser pressionado com uma mola — é o gesto que dá a sensação
 * de "peso" que o jogo inteiro persegue.
 */
@Composable
fun PulseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    subtitle: String? = null,
    primary: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "escala"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> TextMuted
            primary -> VoidBlack
            else -> TextPrimary
        },
        label = "cor"
    )

    val brush = when {
        !enabled -> Brush.horizontalGradient(listOf(SurfaceCard, SurfaceCard))
        primary -> Brush.horizontalGradient(listOf(PulseCyan, PulseViolet))
        else -> Brush.horizontalGradient(
            listOf(SurfaceCard.copy(alpha = 0.9f), SurfaceCard.copy(alpha = 0.9f))
        )
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = modifier.scale(scale)
    ) {
        Row(
            modifier = Modifier
                .background(brush)
                .then(
                    if (!primary) Modifier.border(
                        BorderStroke(1.dp, SurfaceStroke),
                        RoundedCornerShape(18.dp)
                    ) else Modifier
                )
                .padding(horizontal = 22.dp, vertical = if (subtitle == null) 15.dp else 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 2.dp)
                )
                Box(Modifier.width(10.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Etiqueta compacta para recursos: Fragmentos, Cristais, nível. */
@Composable
fun ResourceChip(
    glyph: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(SurfaceCard.copy(alpha = 0.85f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.35f)), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(glyph, color = color, style = MaterialTheme.typography.titleMedium)
        Text(
            value,
            color = TextPrimary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** Título de seção com um traço luminoso do lado. */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(PulseCyan, PulseViolet)))
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        )
        trailing()
    }
}

/** Barra de progresso fina com brilho. */
@Composable
fun GlowBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Int = 8,
    colors: List<Color> = listOf(PulseCyan, PulseViolet),
    track: Color = SurfaceCard
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "barra"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(colors))
        )
    }
}

/** Texto de apoio, usado em descrições e legendas. */
@Composable
fun Caption(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextSecondary,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = align,
        modifier = modifier
    )
}

/** Cabeçalho de tela com botão de voltar. */
@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
                color = SurfaceCard.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, SurfaceStroke),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("‹", color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Box(Modifier.width(12.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}
