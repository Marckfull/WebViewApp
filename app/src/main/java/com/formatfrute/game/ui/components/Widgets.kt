package com.formatfrute.game.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.formatfrute.game.audio.Haptics
import com.formatfrute.game.audio.Sfx
import com.formatfrute.game.audio.SoundManager
import com.formatfrute.game.core.Fruit
import com.formatfrute.game.data.BoardTheme
import com.formatfrute.game.ui.theme.Fruta
import kotlin.math.sin
import kotlin.random.Random

/**
 * Botao com "labio" embaixo, contorno grosso e afundada no toque — a mesma
 * linguagem visual do contorno das frutas.
 */
@Composable
fun JuicyButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Fruta.Berry,
    textColor: Color = Color.White,
    emoji: String? = null,
    enabled: Boolean = true,
    height: Dp = 60.dp,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val drop by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "press",
    )
    val lip = 6.dp
    val shade = Color(
        red = color.red * 0.72f,
        green = color.green * 0.72f,
        blue = color.blue * 0.72f,
        alpha = 1f,
    )

    Box(
        modifier = modifier
            .height(height + lip)
            .alpha(if (enabled) 1f else 0.45f),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(20.dp))
                .background(shade),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = drop * lip.toPx() }
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(color.lighten(0.16f), color),
                    )
                )
                .border(3.dp, Fruta.Ink.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                .clickableSquish(enabled, interaction) {
                    SoundManager.get(context).play(Sfx.BUTTON, volume = 0.7f)
                    Haptics.get(context).tap()
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (emoji != null) Text(emoji, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = text,
                    color = textColor,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun Modifier.clickableSquish(
    enabled: Boolean,
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier = this.clickable(
    interactionSource = interaction,
    indication = null,
    enabled = enabled,
    onClick = onClick,
)

fun Color.lighten(amount: Float): Color = Color(
    red = (red + (1f - red) * amount).coerceIn(0f, 1f),
    green = (green + (1f - green) * amount).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * amount).coerceIn(0f, 1f),
    alpha = alpha,
)

fun Color.darken(amount: Float): Color = Color(
    red = (red * (1f - amount)).coerceIn(0f, 1f),
    green = (green * (1f - amount)).coerceIn(0f, 1f),
    blue = (blue * (1f - amount)).coerceIn(0f, 1f),
    alpha = alpha,
)

/** Pilula de recurso: sementes, troféu, nível. */
@Composable
fun StatPill(
    emoji: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    contentColor: Color = Fruta.Ink,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.92f))
            .border(2.5.dp, Fruta.Ink.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.labelMedium, color = contentColor)
    }
}

@Composable
fun FruitArt(fruit: Fruit, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(fruit.art),
        contentDescription = fruit.label,
        modifier = modifier,
    )
}

/** Cartao de papel com contorno grosso, base de quase toda a UI. */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    corner: Dp = 26.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(color)
            .border(3.dp, Fruta.Ink.copy(alpha = 0.8f), RoundedCornerShape(corner)),
    ) { content() }
}

/**
 * Fundo vivo: degrade do tema escolhido e frutas fantasmas boiando devagar.
 * Roda o tempo todo, mas so com transformacoes de camada — barato.
 */
@Composable
fun FruitBackground(
    theme: BoardTheme,
    modifier: Modifier = Modifier,
    density: Int = 5,
    content: @Composable () -> Unit,
) {
    // Cada fruta boiando é um bitmap redesenhado todo frame. Em aparelho de
    // entrada isso come orçamento de graça, então o cenário encolhe sozinho
    // onde a memória é curta — sem sumir com o clima em quem tem folga.
    val context = LocalContext.current
    val budget = remember(density) {
        val lowRam = runCatching {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                ?.isLowRamDevice == true
        }.getOrDefault(false)
        if (lowRam) (density / 2).coerceAtLeast(2) else density
    }

    val floaters = remember(budget) {
        val rng = Random(budget * 7919L)
        List(budget) {
            FloatingFruit(
                fruit = Fruit.entries[rng.nextInt(Fruit.entries.size)],
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                size = 44f + rng.nextFloat() * 46f,
                speed = 0.35f + rng.nextFloat() * 0.7f,
                phase = rng.nextFloat() * 6.28f,
                spin = if (rng.nextBoolean()) 1f else -1f,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "bg")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(24_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bgt",
    )

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.top, theme.bottom))),
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        floaters.forEach { f ->
            val progress = (f.y + t * f.speed) % 1.25f - 0.15f
            val wobble = sin((t * 6.28f + f.phase).toDouble()).toFloat()
            Image(
                painter = painterResource(f.fruit.art),
                contentDescription = null,
                modifier = Modifier
                    .size(f.size.dp)
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        translationX = f.x * widthPx + wobble * 26f
                        translationY = progress * heightPx
                        rotationZ = wobble * 18f * f.spin
                        alpha = 0.16f
                    },
            )
        }
        content()
    }
}

private data class FloatingFruit(
    val fruit: Fruit,
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val phase: Float,
    val spin: Float,
)

/** Chuva de confete para recorde, vitoria e nivel novo. */
@Composable
fun Confetti(active: Boolean, modifier: Modifier = Modifier, pieces: Int = 90) {
    if (!active) return
    val bits = remember(pieces) {
        val rng = Random(4242)
        List(pieces) {
            ConfettiBit(
                x = rng.nextFloat(),
                delay = rng.nextFloat(),
                size = 6f + rng.nextFloat() * 12f,
                color = listOf(
                    Fruta.Berry, Fruta.Sun, Fruta.Leaf, Fruta.Sky, Fruta.Grape, Fruta.Peach,
                )[rng.nextInt(6)],
                spin = rng.nextFloat() * 8f - 4f,
                drift = rng.nextFloat() * 2f - 1f,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "confetti")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "ct",
    )

    Box(modifier.fillMaxSize()) {
        bits.forEach { bit ->
            val p = (t + bit.delay) % 1f
            Box(
                Modifier
                    .size(bit.size.dp, (bit.size * 0.55f).dp)
                    .graphicsLayer {
                        translationX = bit.x * 1200f + bit.drift * p * 220f
                        translationY = p * 2400f - 200f
                        rotationZ = p * 360f * bit.spin
                        alpha = (1f - p).coerceIn(0f, 1f)
                    }
                    .background(bit.color, RoundedCornerShape(2.dp)),
            )
        }
    }
}

private data class ConfettiBit(
    val x: Float,
    val delay: Float,
    val size: Float,
    val color: Color,
    val spin: Float,
    val drift: Float,
)

/** Barra de progresso gorda com contorno, usada em XP, vida do boss e missões. */
@Composable
fun ChunkyBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Fruta.Leaf,
    track: Color = Color(0x33000000),
    height: Dp = 16.dp,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 220f),
        label = "bar",
    )
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(track)
            .border(2.dp, Fruta.Ink.copy(alpha = 0.55f), RoundedCornerShape(50)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color.lighten(0.25f), color))),
        )
    }
}

/** Brilho pulsante atras de algo importante (botao de vídeo, presente do dia). */
@Composable
fun Pulse(color: Color, modifier: Modifier = Modifier, corner: Dp = 24.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ps",
    )
    val glow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pg",
    )
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = glow
            }
            .clip(RoundedCornerShape(corner))
            .background(color),
    )
}

/** Texto com contorno — legível por cima de qualquer fruta. */
@Composable
fun OutlinedTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    outline: Color = Fruta.Ink,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displayMedium,
) {
    Box(modifier) {
        listOf(
            Offset(-2.5f, 0f), Offset(2.5f, 0f), Offset(0f, -2.5f), Offset(0f, 2.5f),
            Offset(-2f, -2f), Offset(2f, -2f), Offset(-2f, 2f), Offset(2f, 2f),
        ).forEach { off ->
            Text(
                text = text,
                style = style,
                color = outline,
                modifier = Modifier.graphicsLayer {
                    translationX = off.x
                    translationY = off.y
                },
            )
        }
        Text(text = text, style = style, color = color)
    }
}

/** Sombrinha suave sob cartoes, sem depender de elevation do Material. */
fun Modifier.softShadow(color: Color = Fruta.Ink, alpha: Float = 0.18f, offset: Dp = 6.dp) =
    this.drawBehind {
        drawRoundRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(0f, offset.toPx()),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(26.dp.toPx()),
        )
    }

/**
 * Número que rola até o valor novo em vez de pular. Custa quase nada e é a
 * diferença entre "o placar mudou" e "eu ganhei aquilo ali".
 */
@Composable
fun RollingNumber(
    value: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineSmall,
    color: Color = Color.White,
    format: (Int) -> String = { it.toString() },
) {
    val shown by animateIntAsState(
        targetValue = value,
        animationSpec = tween(340, easing = FastOutSlowInEasing),
        label = "roll",
    )
    Text(text = format(shown), style = style, color = color, modifier = modifier)
}

@Composable
fun LabelSmallMuted(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = LocalContentColor.current.copy(alpha = 0.7f),
    )
}
