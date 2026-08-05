package com.formatfrute.game.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.formatfrute.game.core.Fruit
import com.formatfrute.game.ui.components.ChunkyBar
import com.formatfrute.game.ui.components.OutlinedTitle
import com.formatfrute.game.ui.theme.Fruta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash animada: as frutas caem, quicam e montam o logo. Dura o suficiente
 * para o AdMob e o consentimento inicializarem sem o jogador sentir espera.
 */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    val fruits = remember {
        listOf(Fruit.CEREJA, Fruit.MORANGO, Fruit.LIMAO, Fruit.LARANJA, Fruit.MELANCIA)
    }
    val drops = remember { fruits.map { Animatable(-700f) } }
    val title = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        drops.forEachIndexed { index, anim ->
            launch {
                delay(index * 110L)
                anim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.42f, stiffness = 300f + index * 30f),
                )
            }
        }
        delay(680)
        title.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 320f))
    }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(2100, easing = LinearEasing))
        onDone()
    }

    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val glow by shimmer.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "glow",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFE9C9), Color(0xFFFFB07C), Color(0xFFFF7EA8)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                fruits.forEachIndexed { index, fruit ->
                    Image(
                        painter = painterResource(fruit.art),
                        contentDescription = null,
                        modifier = Modifier
                            .size(if (index == 2) 84.dp else 66.dp)
                            .graphicsLayer {
                                translationY = drops[index].value
                                rotationZ = drops[index].value * 0.05f
                            },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Box(
                Modifier.graphicsLayer {
                    scaleX = 0.6f + title.value * 0.4f
                    scaleY = 0.6f + title.value * 0.4f
                    alpha = title.value
                }
            ) {
                OutlinedTitle(
                    text = "FORMAT FRUTE",
                    color = Fruta.Sun,
                    style = MaterialTheme.typography.displayMedium,
                )
            }

            Text(
                "junte as frutas, encha a cesta",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = glow),
            )

            Spacer(Modifier.height(30.dp))

            ChunkyBar(
                progress = progress.value,
                color = Fruta.Leaf,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                height = 14.dp,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                "descascando as frutas…",
                style = MaterialTheme.typography.labelSmall,
                color = Fruta.Ink.copy(alpha = 0.7f),
            )
        }
    }
}
