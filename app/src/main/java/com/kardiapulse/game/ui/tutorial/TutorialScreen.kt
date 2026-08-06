package com.kardiapulse.game.ui.tutorial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kardiapulse.game.LocalServices
import com.kardiapulse.game.core.engine.GameEngine
import com.kardiapulse.game.core.model.GameState
import com.kardiapulse.game.core.model.PowerType
import com.kardiapulse.game.data.TutorialSteps
import com.kardiapulse.game.ui.common.Caption
import com.kardiapulse.game.ui.common.GlassPanel
import com.kardiapulse.game.ui.common.KardiaBackground
import com.kardiapulse.game.ui.common.PulseButton
import com.kardiapulse.game.ui.game.components.CardView
import com.kardiapulse.game.ui.game.components.NucleusGauge
import com.kardiapulse.game.ui.game.components.NucleusStatusRow
import com.kardiapulse.game.ui.theme.DangerRed
import com.kardiapulse.game.ui.theme.NegativePole
import com.kardiapulse.game.ui.theme.PositivePole
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.PulseGold
import com.kardiapulse.game.ui.theme.SurfaceCard
import com.kardiapulse.game.ui.theme.SurfaceStroke
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary
import com.kardiapulse.game.ui.theme.TextSecondary
import com.kardiapulse.game.ui.theme.VoidBlack
import kotlinx.coroutines.delay

@Composable
fun TutorialScreen(onDone: () -> Unit) {
    val services = LocalServices.current
    val viewModel: TutorialViewModel = viewModel(factory = TutorialViewModel.Factory(services))
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(ui.finished) {
        if (ui.finished) onDone()
    }
    LaunchedEffect(ui.toast) {
        if (ui.toast != null) {
            delay(2800)
            viewModel.dismissToast()
        }
    }

    val game = ui.game

    KardiaBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            StepProgress(ui.stepIndex)

            Spacer(Modifier.height(10.dp))
            InstructionPanel(
                title = ui.step.title,
                body = ui.step.body,
                hint = ui.step.hint,
                goalMet = ui.goalMet,
                stepIndex = ui.stepIndex
            )

            if (game != null) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NucleusGauge(
                            nucleus = game.nucleus,
                            baseLimit = game.config.limit,
                            limitNow = game.limitNow,
                            direction = game.direction,
                            lastElement = game.lastElement,
                            chain = game.chain,
                            modifier = Modifier.fillMaxWidth(0.74f)
                        )
                        NucleusStatusRow(game.direction, game.lastElement, game.chain)
                    }
                }

                AnimatedVisibility(visible = ui.toast != null, enter = fadeIn(), exit = fadeOut()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard.copy(alpha = 0.95f))
                            .border(
                                BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(
                            ui.toast.orEmpty(),
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                PowerStrip(game, ui.yourTurn, viewModel::usePower)

                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(
                    visible = ui.selectedCardId != null,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut()
                ) {
                    PolarityRow(game, ui.selectedCardId, viewModel::play)
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom
                ) {
                    game.you.hand.forEach { card ->
                        CardView(
                            card = card,
                            playable = ui.yourTurn && (
                                GameEngine.isLegal(game, card, 1) ||
                                    GameEngine.isLegal(game, card, -1)
                                ),
                            resonant = card.resonatesWith(game.lastElement) && game.direction != 0,
                            selected = card.id == ui.selectedCardId,
                            width = 56,
                            height = 80,
                            onClick = { viewModel.selectCard(card.id) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(0.38f)) {
                    PulseButton(text = "PULAR", onClick = viewModel::skip, primary = false)
                }
                Box(Modifier.weight(0.62f)) {
                    PulseButton(
                        text = if (ui.isLastStep) "CONCLUIR" else "PRÓXIMA ETAPA",
                        subtitle = if (ui.goalMet) null else "complete a etapa para liberar",
                        onClick = viewModel::advance,
                        enabled = ui.goalMet
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** A trilha de etapas no topo: passadas, atual e ainda trancadas. */
@Composable
private fun StepProgress(current: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TutorialSteps.ALL.indices.forEach { index ->
            val color = when {
                index < current -> PulseCyan
                index == current -> PulseGold
                else -> SurfaceStroke
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
private fun InstructionPanel(
    title: String,
    body: String,
    hint: String,
    goalMet: Boolean,
    stepIndex: Int
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (goalMet) PulseCyan else PulseGold.copy(alpha = 0.6f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (goalMet) PulseCyan else SurfaceCard)
                    .border(
                        BorderStroke(1.dp, if (goalMet) PulseCyan else SurfaceStroke),
                        RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (goalMet) "✓" else "${stepIndex + 1}",
                    color = if (goalMet) VoidBlack else TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${stepIndex + 1}/${TutorialSteps.ALL.size}",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Caption(body)
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (goalMet) "Etapa concluída." else "Objetivo: $hint",
            color = if (goalMet) PulseCyan else PulseGold,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun PowerStrip(game: GameState, enabled: Boolean, onUse: (PowerType) -> Unit) {
    val owned = PowerType.ALL.filter { game.you.powerCount(it) > 0 }
    if (owned.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        owned.forEach { power ->
            Surface(
                onClick = { if (enabled) onUse(power) },
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                color = SurfaceCard.copy(alpha = if (enabled) 0.95f else 0.5f),
                border = BorderStroke(1.dp, if (enabled) PulseCyan.copy(alpha = 0.5f) else SurfaceStroke)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(power.glyph, color = PulseCyan, fontSize = 14.sp)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        power.ptName,
                        color = if (enabled) TextPrimary else TextMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("×${game.you.powerCount(power)}", color = PulseGold, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun PolarityRow(game: GameState, selectedCardId: Int?, onChoose: (Int) -> Unit) {
    val card = game.you.hand.firstOrNull { it.id == selectedCardId } ?: return
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(-1, 1).forEach { sign ->
            val legal = GameEngine.isLegal(game, card, sign)
            val color = if (sign > 0) PositivePole else NegativePole
            Surface(
                onClick = { if (legal) onChoose(sign) },
                enabled = legal,
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                if (legal) listOf(color.copy(alpha = 0.30f), SurfaceCard)
                                else listOf(SurfaceCard.copy(alpha = 0.4f), VoidBlack.copy(alpha = 0.4f))
                            )
                        )
                        .border(
                            BorderStroke(1.dp, if (legal) color else SurfaceStroke),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (sign > 0) "▲  +${card.value}" else "▼  −${card.value}",
                        color = if (legal) color else TextMuted,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (legal) "Núcleo → ${game.nucleus + sign * card.value}" else "bloqueado",
                        color = if (legal) TextSecondary else TextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
