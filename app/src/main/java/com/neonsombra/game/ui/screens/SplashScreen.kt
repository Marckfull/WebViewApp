package com.neonsombra.game.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neonsombra.game.game.TetrominoType
import com.neonsombra.game.ui.components.NeonBackground
import com.neonsombra.game.ui.components.NeonLogo
import com.neonsombra.game.ui.game.PiecePreview
import com.neonsombra.game.ui.theme.NeonCyan
import com.neonsombra.game.ui.theme.NeonMagenta
import com.neonsombra.game.ui.theme.NeonTextMuted
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MS = 2600L

/**
 * Abertura do jogo: o logo acende, as sete pecas desfilam e uma barra de neon
 * completa antes de entregar o jogador a proxima tela.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    val logoAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(900),
        label = "logoAlpha",
    )
    val logoScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.75f,
        animationSpec = tween(900),
        label = "logoScale",
    )

    val transition = rememberInfiniteTransition(label = "abertura")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(SPLASH_DURATION_MS.toInt(), easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "barra",
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(SPLASH_DURATION_MS)
        onFinished()
    }

    NeonBackground(modifier = Modifier.fillMaxSize(), intensity = 1f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            NeonLogo(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(logoAlpha)
                    .scale(logoScale),
            )

            Spacer(Modifier.height(40.dp))

            PieceParade(alpha = logoAlpha)

            Spacer(Modifier.height(36.dp))

            LoadingBar(progress = progress, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(14.dp))

            Text(
                text = "CARREGANDO O LADO SOMBRIO...",
                color = NeonTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(logoAlpha),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** As sete pecas desfilando embaixo do logo. */
@Composable
private fun PieceParade(alpha: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .alpha(alpha),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TetrominoType.entries.forEach { type ->
            val transition = rememberInfiniteTransition(label = "peca_${type.name}")
            val bounce by transition.animateFloat(
                initialValue = 0.7f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(700, delayMillis = type.ordinal * 90),
                    RepeatMode.Reverse,
                ),
                label = "bounce_${type.name}",
            )
            PiecePreview(
                type = type,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .scale(bounce),
            )
        }
    }
}

@Composable
private fun LoadingBar(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(12.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(
                color = NeonCyan.copy(alpha = 0.16f),
                size = size,
                cornerRadius = radius,
            )
            drawRoundRect(
                color = NeonCyan.copy(alpha = 0.55f),
                size = size,
                cornerRadius = radius,
                style = Stroke(width = 2f),
            )
            drawRoundRect(
                color = NeonMagenta,
                topLeft = Offset.Zero,
                size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
                cornerRadius = radius,
            )
        }
    }
}
