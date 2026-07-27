package com.chuvadeletras.game.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chuvadeletras.game.ui.components.RainBackground
import com.chuvadeletras.game.ui.theme.ChuvaCyan
import com.chuvadeletras.game.ui.theme.ChuvaNight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash animado: as letras do nome caem uma a uma, como a chuva do jogo.
 * Dura ~1,9s e então entrega a tela inicial.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = "CHUVA"
    val subtitle = "DE LETRAS"

    val drops = remember { List(title.length + subtitle.length) { Animatable(0f) } }
    val tagline = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Cada letra cai um pouquinho depois da anterior, todas em paralelo.
        drops.forEachIndexed { index, animatable ->
            launch {
                delay(index * 60L)
                animatable.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 220f))
            }
        }
        delay(1000)
        tagline.animateTo(1f, tween(450))
        delay(550)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ChuvaNight, ChuvaNight.copy(alpha = 0.85f), ChuvaCyan.copy(alpha = 0.35f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        RainBackground(
            modifier = Modifier.fillMaxSize(),
            dropCount = 80,
            baseAlpha = 0.3f,
            speedMillis = 1800
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            FallingWord(word = title, animatables = drops.take(title.length), fontSize = 54)
            FallingWord(
                word = subtitle,
                animatables = drops.drop(title.length),
                fontSize = 30
            )
            Text(
                text = "As letras caem. Se você não usar, elas somem.",
                style = MaterialTheme.typography.bodyLarge,
                color = ChuvaCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 18.dp, start = 32.dp, end = 32.dp)
                    .graphicsLayer { alpha = tagline.value }
            )
        }
    }
}

@Composable
private fun FallingWord(
    word: String,
    animatables: List<Animatable<Float, *>>,
    fontSize: Int
) {
    Row {
        word.forEachIndexed { index, char ->
            val progress = animatables.getOrNull(index)?.value ?: 1f
            Text(
                text = char.toString(),
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    translationY = (1f - progress) * -260f
                    alpha = progress.coerceIn(0f, 1f)
                    rotationZ = (1f - progress) * 25f
                }
            )
        }
    }
}
