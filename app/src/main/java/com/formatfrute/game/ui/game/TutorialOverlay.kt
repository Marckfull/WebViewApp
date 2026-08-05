package com.formatfrute.game.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.formatfrute.game.game.RecipeCoachStep
import com.formatfrute.game.game.TutorialSpot
import com.formatfrute.game.game.TutorialStep
import com.formatfrute.game.ui.components.JuicyButton
import com.formatfrute.game.ui.components.PaperCard
import com.formatfrute.game.R
import com.formatfrute.game.ui.theme.Fruta

/**
 * Tutorial em camada, sem travar a tela: a mao animada mostra onde tocar e o
 * cartao explica. O passo seguinte so aparece quando o atual for cumprido —
 * quem decide isso e o [com.formatfrute.game.game.GameViewModel].
 */
@Composable
fun TutorialOverlay(
    step: TutorialStep,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        when (step.spot) {
            TutorialSpot.TABULEIRO -> SwipeHand(step, Modifier.align(Alignment.Center))
            TutorialSpot.PODERES -> BouncingArrow(Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
            TutorialSpot.PREMIADO -> BouncingArrow(Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
            TutorialSpot.PLACAR -> Unit
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(16.dp),
        ) {
            PaperCard(color = Color.White.copy(alpha = 0.97f)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.tut_step_of, step.stepNumber, step.totalSteps),
                            style = MaterialTheme.typography.labelSmall,
                            color = Fruta.InkSoft,
                        )
                        Text(
                            stringResource(R.string.tut_skip),
                            style = MaterialTheme.typography.labelSmall,
                            color = Fruta.InkSoft,
                            modifier = Modifier
                                .clickable(onClick = onSkip)
                                .padding(6.dp),
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    StepDots(step)
                    Spacer(Modifier.height(10.dp))

                    Text(
                        stringResource(step.title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Fruta.Berry,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(step.text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Fruta.Ink,
                        textAlign = TextAlign.Center,
                    )

                    if (step.manual) {
                        Spacer(Modifier.height(14.dp))
                        JuicyButton(
                            text = stringResource(step.cta),
                            emoji = "👍",
                            color = Fruta.Leaf,
                            height = 52.dp,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onAdvance,
                        )
                    } else {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.tut_locked),
                            style = MaterialTheme.typography.labelSmall,
                            color = Fruta.InkSoft,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ensino do Modo Receita. Mesma gramática do tutorial grande — cartão embaixo,
 * bolinhas de progresso, passo travado até ser cumprido — mas só três passos,
 * porque o jogador já sabe jogar; o que falta é entender o pedido.
 */
@Composable
fun RecipeCoachOverlay(
    step: RecipeCoachStep,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (step == RecipeCoachStep.PEDIDO) {
            // A seta aponta para o cartão do pedido, no topo da tela.
            Text(
                text = "☝️",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 150.dp),
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(16.dp),
        ) {
            PaperCard(color = Color.White.copy(alpha = 0.97f)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.coach_step_of, step.stepNumber, step.totalSteps),
                            style = MaterialTheme.typography.labelSmall,
                            color = Fruta.InkSoft,
                        )
                        Text(
                            stringResource(R.string.tut_skip),
                            style = MaterialTheme.typography.labelSmall,
                            color = Fruta.InkSoft,
                            modifier = Modifier
                                .clickable(onClick = onSkip)
                                .padding(6.dp),
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RecipeCoachStep.entries.forEach { entry ->
                            Box(
                                Modifier
                                    .size(if (entry == step) 14.dp else 10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        when {
                                            entry == step -> Fruta.Berry
                                            entry.ordinal < step.ordinal -> Fruta.Leaf
                                            else -> Fruta.InkSoft.copy(alpha = 0.3f)
                                        }
                                    )
                                    .border(2.dp, Fruta.Ink.copy(alpha = 0.4f), RoundedCornerShape(50)),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    Text(
                        stringResource(step.title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Fruta.Berry,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(step.text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Fruta.Ink,
                        textAlign = TextAlign.Center,
                    )

                    if (step.manual) {
                        Spacer(Modifier.height(14.dp))
                        JuicyButton(
                            text = stringResource(step.cta),
                            emoji = "👍",
                            color = Fruta.Leaf,
                            height = 52.dp,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onAdvance,
                        )
                    } else {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.coach_locked),
                            style = MaterialTheme.typography.labelSmall,
                            color = Fruta.InkSoft,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDots(step: TutorialStep) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TutorialStep.entries.forEach { entry ->
            val done = entry.ordinal < step.ordinal
            val current = entry == step
            Box(
                Modifier
                    .size(if (current) 14.dp else 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        when {
                            current -> Fruta.Berry
                            done -> Fruta.Leaf
                            else -> Fruta.InkSoft.copy(alpha = 0.3f)
                        }
                    )
                    .border(2.dp, Fruta.Ink.copy(alpha = 0.4f), RoundedCornerShape(50)),
            )
        }
    }
}

/** Maozinha que repete o gesto pedido, no ritmo de quem esta ensinando. */
@Composable
private fun SwipeHand(step: TutorialStep, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "hand")
    val slide by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "slide",
    )
    val horizontal = step.requiredDirection == null ||
        step.requiredDirection == com.formatfrute.game.core.Direction.RIGHT ||
        step.requiredDirection == com.formatfrute.game.core.Direction.LEFT

    Text(
        text = "👆",
        style = MaterialTheme.typography.displayLarge,
        modifier = modifier.graphicsLayer {
            val travel = 150f * (if (slide < 0.75f) slide / 0.75f else 1f - (slide - 0.75f) / 0.25f)
            if (horizontal) translationX = travel else translationY = travel
            alpha = if (slide > 0.85f) (1f - slide) / 0.15f else 1f
            scaleX = 1.1f
            scaleY = 1.1f
        },
    )
}

@Composable
private fun BouncingArrow(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "arrow")
    val bounce by transition.animateFloat(
        initialValue = 0f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "bounce",
    )
    Text(
        text = "👇",
        style = MaterialTheme.typography.displayMedium,
        modifier = modifier.graphicsLayer { translationY = bounce },
    )
}
