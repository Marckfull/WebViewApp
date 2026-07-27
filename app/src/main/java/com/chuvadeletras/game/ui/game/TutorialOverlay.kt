package com.chuvadeletras.game.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chuvadeletras.game.ui.components.BigActionButton
import com.chuvadeletras.game.ui.components.blockTouches

/** Guarda onde cada parte da tela está, para o holofote do tutorial mirar certo. */
fun Modifier.tutorialAnchor(
    id: String,
    anchors: MutableMap<String, Rect>
): Modifier = this.onGloballyPositioned { coordinates ->
    anchors[id] = coordinates.boundsInRoot()
}

/**
 * Tutorial de primeira partida: escurece a tela, abre um buraco em volta da
 * parte que está sendo explicada e mostra o texto do passo.
 */
@Composable
fun TutorialOverlay(
    step: TutorialStep,
    stepIndex: Int,
    totalSteps: Int,
    anchors: Map<String, Rect>,
    modifier: Modifier = Modifier,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    val target = step.anchor?.let { anchors[it] }
    val padding = with(density) { 8.dp.toPx() }
    val screenHeightPx = with(density) {
        androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    val holeAlpha by animateFloatAsState(
        targetValue = if (target != null) 1f else 0f,
        animationSpec = tween(280),
        label = "hole"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            // O tutorial segura os toques: cada passo avança pelo botão.
            .blockTouches()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            drawRect(Color.Black.copy(alpha = 0.82f))
            if (target != null && holeAlpha > 0f) {
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(target.left - padding, target.top - padding),
                    size = Size(
                        target.width + padding * 2,
                        target.height + padding * 2
                    ),
                    cornerRadius = CornerRadius(20f, 20f),
                    blendMode = BlendMode.Clear
                )
            }
        }

        // O card fica do lado oposto do holofote, para não cobrir o que explica.
        val cardOnTop = target != null && target.center.y > screenHeightPx / 2f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = if (cardOnTop) Arrangement.Top else Arrangement.Bottom
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Passo ${stepIndex + 1} de $totalSteps",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = onSkip) { Text("Pular") }
                }

                Text(step.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    step.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(totalSteps) { index ->
                        Text(
                            text = if (index == stepIndex) "●" else "○",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                BigActionButton(
                    label = if (stepIndex == totalSteps - 1) "Bora jogar!" else "Entendi",
                    emoji = if (stepIndex == totalSteps - 1) "🚀" else "👉",
                    onClick = onNext
                )
            }
        }
    }
}
