package com.kardiapulse.game.ui.game.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiapulse.game.core.model.Card
import com.kardiapulse.game.core.model.Side
import com.kardiapulse.game.ui.theme.DangerRed
import com.kardiapulse.game.ui.theme.PulseGold
import com.kardiapulse.game.ui.theme.color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Os efeitos que dão peso ao duelo.
 *
 * Todos são camadas independentes desenhadas por cima do tabuleiro e disparadas por eventos do
 * motor. Nenhuma delas participa da lógica: se qualquer uma falhar, o jogo continua igual.
 */

/** O que a UI precisa saber para animar uma carta voando. */
data class FlyingCard(
    val card: Card,
    val by: Side,
    val sign: Int,
    val key: Int
)

/**
 * A carta saindo da mão e mergulhando no Núcleo.
 *
 * Sem isso a carta simplesmente sumia da mão e o número do Núcleo mudava sozinho — o jogador
 * não via a relação entre as duas coisas. É o efeito que mais muda a leitura da partida.
 */
@Composable
fun FlyingCardLayer(
    flying: FlyingCard?,
    onFinished: () -> Unit
) {
    if (flying == null) return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val progress = remember(flying.key) { Animatable(0f) }
        LaunchedEffect(flying.key) {
            progress.animateTo(1f, tween(durationMillis = 460, easing = FastOutSlowInEasing))
            onFinished()
        }

        val t = progress.value
        val fromPlayer = flying.by == Side.VOCE
        val startY = if (fromPlayer) maxHeight * 0.72f else maxHeight * 0.06f
        val targetY = maxHeight * 0.34f
        // Um arco leve: a carta desvia para o lado da polaridade escolhida antes de cair no centro.
        val bow = sin(t * PI.toFloat()) * (if (flying.sign > 0) 1f else -1f)
        val x = maxWidth * 0.5f - 31.dp + (maxWidth * 0.16f * bow)
        val y = startY + (targetY - startY) * t

        CardView(
            card = flying.card,
            width = 62,
            height = 88,
            playable = true,
            modifier = Modifier
                .offset(x = x, y = y)
                .graphicsLayer {
                    val scale = 1f - 0.45f * t
                    scaleX = scale
                    scaleY = scale
                    rotationZ = bow * 16f
                    alpha = (1f - t * t).coerceIn(0f, 1f)
                }
        )
    }
}

/**
 * Explosão de partículas no centro. Usada na inversão de direção e nas correntes longas —
 * os dois momentos em que o jogador fez algo digno de comemoração.
 */
@Composable
fun BurstLayer(trigger: Int, color: Color, particles: Int = 22) {
    if (trigger == 0) return
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.animateTo(1f, tween(durationMillis = 620, easing = LinearEasing))
    }
    val t = progress.value
    if (t >= 1f) return

    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * 0.34f)
        val maxRadius = size.minDimension * 0.42f
        for (i in 0 until particles) {
            val angle = (i.toFloat() / particles) * 2f * PI.toFloat() + trigger * 0.37f
            // Desaceleração: rápido no começo, quase parado no fim.
            val eased = 1f - (1f - t) * (1f - t)
            val radius = maxRadius * eased
            val fade = (1f - t).coerceIn(0f, 1f)
            drawCircle(
                color = color.copy(alpha = 0.85f * fade * fade),
                radius = (3.5f - 2.5f * t).coerceAtLeast(0.5f),
                center = Offset(
                    center.x + cos(angle) * radius,
                    center.y + sin(angle) * radius
                )
            )
        }
        // Onda de choque.
        drawCircle(
            color = color.copy(alpha = 0.30f * (1f - t)),
            radius = maxRadius * t,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * (1f - t))
        )
    }
}

/** O número do dano subindo e sumindo, logo acima do Núcleo. */
@Composable
fun FloatingDamageLayer(damage: Int?, trigger: Int, blocked: Boolean) {
    if (damage == null || trigger == 0) return
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.animateTo(1f, tween(durationMillis = 1100, easing = FastOutSlowInEasing))
    }
    val t = progress.value
    if (t >= 1f) return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset(y = maxHeight * 0.30f - (maxHeight * 0.10f * t))
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = if (blocked) "BLOQUEADO" else "-$damage",
                color = if (blocked) PulseGold else DangerRed,
                fontSize = if (blocked) 22.sp else 46.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.graphicsLayer {
                    // Um estouro rápido antes de encolher e sumir.
                    val pop = if (t < 0.18f) 0.6f + 2.4f * t else 1.05f - 0.15f * t
                    scaleX = pop
                    scaleY = pop
                    alpha = (1f - t * t * t).coerceIn(0f, 1f)
                }
            )
        }
    }
}

/**
 * Tremor de tela amortecido.
 *
 * Devolve o deslocamento em pixels para ser aplicado com `Modifier.offset { }`. A amplitude cai
 * de forma exponencial, que é o que faz o baque parecer físico em vez de um chacoalhão solto.
 */
@Composable
fun rememberShakeOffset(trigger: Int, intensity: Float = 18f): State<Offset> {
    val offset = remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val animation = Animatable(0f)
        animation.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 480, easing = LinearEasing)
        ) {
            val decay = (1f - value) * (1f - value)
            val phase = value * 46f
            offset.value = Offset(
                x = sin(phase) * intensity * decay,
                y = cos(phase * 0.77f) * intensity * 0.6f * decay
            )
        }
        offset.value = Offset.Zero
    }
    return offset
}

/** Clarão que toma a tela por um instante quando alguém sobrecarrega. */
@Composable
fun FlashLayer(trigger: Int, color: Color = DangerRed) {
    if (trigger == 0) return
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.animateTo(1f, tween(durationMillis = 420, easing = LinearEasing))
    }
    val t = progress.value
    if (t >= 1f) return
    Canvas(Modifier.fillMaxSize()) {
        drawRect(color = color.copy(alpha = 0.28f * (1f - t)))
    }
}

/** Cor de comemoração de acordo com o elemento da carta que disparou o efeito. */
fun burstColorFor(card: Card): Color = card.element.color
