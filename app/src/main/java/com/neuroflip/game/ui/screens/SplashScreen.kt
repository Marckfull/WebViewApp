package com.neuroflip.game.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflip.game.ui.components.AnimatedNeuroBackground
import com.neuroflip.game.ui.components.NeonBar
import com.neuroflip.game.ui.theme.LocalNeuro
import kotlinx.coroutines.delay

/**
 * Splash animada: duas cartas se cruzam e viram, o nome surge letra a letra
 * e uma barra de "sincronização neural" completa a entrada.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val palette = LocalNeuro.current

    val cardEntry = remember { Animatable(0f) }
    val flip = remember { Animatable(0f) }
    val titleReveal = remember { Animatable(0f) }
    val loading = remember { Animatable(0f) }

    val breathing = rememberInfiniteTransition(label = "breathe")
    val breath by breathing.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(2_000), RepeatMode.Reverse),
        label = "breath"
    )

    LaunchedEffect(Unit) {
        cardEntry.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
        flip.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        titleReveal.animateTo(1f, tween(700))
        loading.animateTo(1f, tween(700))
        delay(280)
        onFinished()
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedNeuroBackground(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Cartas cruzando
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer { scaleX = breath; scaleY = breath },
                contentAlignment = Alignment.Center
            ) {
                SplashCard(
                    rotation = -18f * cardEntry.value,
                    offsetX = -26f * cardEntry.value,
                    flipDegrees = 0f,
                    alpha = 0.55f * cardEntry.value,
                    brush = Brush.linearGradient(
                        listOf(palette.secondary.copy(alpha = 0.7f), palette.secondary.copy(alpha = 0.2f))
                    )
                )
                SplashCard(
                    rotation = 14f * cardEntry.value,
                    offsetX = 20f * cardEntry.value,
                    flipDegrees = 180f * flip.value,
                    alpha = cardEntry.value,
                    brush = Brush.linearGradient(listOf(palette.cardBack, palette.surface)),
                    borderColor = palette.primary,
                    glyph = if (flip.value > 0.5f) "🧬" else "◇"
                )
            }

            Spacer(Modifier.height(38.dp))

            // Nome letra a letra
            Row(horizontalArrangement = Arrangement.Center) {
                val name = "NEUROFLIP"
                name.forEachIndexed { index, char ->
                    val threshold = index.toFloat() / name.length
                    val visible = ((titleReveal.value - threshold) * name.length).coerceIn(0f, 1f)
                    Text(
                        text = char.toString(),
                        color = if (index < 5) palette.primary else palette.secondary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier
                            .alpha(visible)
                            .graphicsLayer { translationY = (1f - visible) * 22f }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "MEMÓRIA RECODIFICADA",
                color = palette.textDim,
                fontSize = 12.sp,
                letterSpacing = 5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(titleReveal.value)
            )

            Spacer(Modifier.height(46.dp))

            NeonBar(
                progress = loading.value,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .alpha(0.9f),
                height = 6.dp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "sincronizando sinapses…",
                color = palette.textDim,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(loading.value)
            )
        }
    }
}

@Composable
private fun SplashCard(
    rotation: Float,
    offsetX: Float,
    flipDegrees: Float,
    alpha: Float,
    brush: Brush,
    borderColor: androidx.compose.ui.graphics.Color? = null,
    glyph: String? = null
) {
    val palette = LocalNeuro.current
    Box(
        modifier = Modifier
            .size(width = 88.dp, height = 118.dp)
            .graphicsLayer {
                rotationZ = rotation
                rotationY = flipDegrees
                translationX = offsetX
                cameraDistance = 16f * density
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .border(2.dp, (borderColor ?: palette.secondary).copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (glyph != null) {
            Box(
                Modifier.graphicsLayer { rotationY = if (flipDegrees > 90f) 180f else 0f },
                contentAlignment = Alignment.Center
            ) {
                Text(glyph, fontSize = 34.sp)
            }
        }
    }
}
