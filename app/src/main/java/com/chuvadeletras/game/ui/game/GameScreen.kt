package com.chuvadeletras.game.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chuvadeletras.game.ads.AdPlacement
import com.chuvadeletras.game.domain.model.CellStatus
import com.chuvadeletras.game.domain.model.Direction
import com.chuvadeletras.game.domain.model.GameMode
import com.chuvadeletras.game.domain.model.GridPos
import com.chuvadeletras.game.domain.model.PuzzleEntry
import com.chuvadeletras.game.ui.components.AdOverlay
import com.chuvadeletras.game.ui.components.StatPill
import com.chuvadeletras.game.util.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val adState by viewModel.adState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()

    val anchors = remember { mutableStateMapOf<String, Rect>() }
    var showClues by remember { mutableStateOf(false) }
    var lockedTarget by remember { mutableStateOf<GridPos?>(null) }
    var showPause by remember { mutableStateOf(false) }

    val game = state.game

    BackHandler(enabled = true) {
        if (state.outcome != null) onExit() else showPause = true
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            if (game == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Montando a grade… 🌧️", style = MaterialTheme.typography.titleLarge)
                }
                return@Column
            }

            GameTopBar(
                mode = game.config.mode,
                round = game.round,
                roundsLeft = game.roundsLeft,
                score = game.score,
                lives = if (game.config.lives != null) game.lives else null,
                secondsLeft = if (game.config.secondsPerRound != null) game.secondsLeft else null,
                stage = game.stageIndex,
                onPause = { showPause = true }
            )

            ClueBanner(
                entry = game.activeEntry,
                solved = game.activeEntryId in game.solvedEntries,
                modifier = Modifier.tutorialAnchor(TutorialSteps.ANCHOR_CLUE, anchors),
                onOpenList = { showClues = true }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .tutorialAnchor(TutorialSteps.ANCHOR_GRID, anchors),
                contentAlignment = Alignment.Center
            ) {
                CrosswordGrid(
                    state = game,
                    modifier = Modifier.fillMaxSize(),
                    onCellTap = { pos ->
                        if (game.cells[pos]?.status == CellStatus.LOCKED &&
                            state.pendingPowerUp == null
                        ) {
                            lockedTarget = pos
                        } else {
                            viewModel.onCellTap(pos)
                        }
                    }
                )
            }

            RoundBar(
                movesLeft = game.movesLeft,
                warning = game.movesLeft > 0,
                modifier = Modifier.tutorialAnchor(TutorialSteps.ANCHOR_MOVES, anchors),
                onEndRound = viewModel::endRound,
                onWatchAdMoves = { viewModel.watchAd(activity, AdPlacement.EXTRA_MOVES) }
            )

            LetterTray(
                tiles = game.tray,
                movesLeft = game.movesLeft,
                pendingPowerUp = state.pendingPowerUp,
                modifier = Modifier.tutorialAnchor(TutorialSteps.ANCHOR_TRAY, anchors),
                onTileTap = viewModel::onTileTap
            )

            Spacer(Modifier.height(8.dp))

            PowerUpBar(
                counts = game.powerUps,
                pending = state.pendingPowerUp,
                showTime = game.config.secondsPerRound != null,
                modifier = Modifier.tutorialAnchor(TutorialSteps.ANCHOR_POWERUPS, anchors),
                onPowerUp = viewModel::onPowerUp
            )

            Spacer(Modifier.height(10.dp))
        }

        // Aviso curto de feedback (letra recusada, item usado, etc.)
        MessageBanner(
            message = state.message,
            modifier = Modifier.align(Alignment.TopCenter),
            onDismiss = viewModel::consumeMessage
        )

        // Aviso de troca de grade no Dilúvio
        state.stageIntro?.let { text ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        state.outcome?.let { outcome ->
            ResultDialog(
                outcome = outcome,
                rewardDoubled = state.rewardDoubled,
                onPlayAgain = viewModel::playAgain,
                onHome = onExit,
                onWatchAdDouble = { viewModel.watchAd(activity, AdPlacement.DOUBLE_MATCH_REWARD) },
                onWatchAdRevive = { viewModel.watchAd(activity, AdPlacement.REVIVE) }
            )
        }

        if (state.isTutorial) {
            TutorialOverlay(
                step = TutorialSteps.all[state.tutorialStep],
                stepIndex = state.tutorialStep,
                totalSteps = TutorialSteps.all.size,
                anchors = anchors,
                onNext = viewModel::nextTutorialStep,
                onSkip = viewModel::finishTutorial
            )
        }

        AdOverlay(state = adState)
    }

    if (showClues && game != null) {
        ModalBottomSheet(
            onDismissRequest = { showClues = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ClueList(
                across = game.puzzle.across,
                down = game.puzzle.down,
                solved = game.solvedEntries,
                activeId = game.activeEntryId,
                onSelect = { id ->
                    viewModel.onClueTap(id)
                    showClues = false
                }
            )
        }
    }

    lockedTarget?.let { pos ->
        AlertDialog(
            onDismissRequest = { lockedTarget = null },
            title = { Text("Quadradinho trancado 🔒") },
            text = {
                Text(
                    "Esta casa se perdeu quando a letra que ela precisava evaporou. " +
                        "Você pode destravá-la com um Reparo ou assistindo a um vídeo."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.watchAd(activity, AdPlacement.REPAIR_CELL)
                    lockedTarget = null
                }) { Text("🎬 Assistir e reparar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.onPowerUp(com.chuvadeletras.game.domain.model.PowerUp.REPARO)
                    lockedTarget = null
                }) { Text("🔧 Usar Reparo") }
            }
        )
    }

    if (showPause) {
        AlertDialog(
            onDismissRequest = { showPause = false },
            title = { Text("Partida pausada") },
            text = { Text("Sair agora encerra a partida e você perde o progresso desta grade.") },
            confirmButton = {
                TextButton(onClick = {
                    showPause = false
                    onExit()
                }) { Text("Sair mesmo assim") }
            },
            dismissButton = {
                TextButton(onClick = { showPause = false }) { Text("Continuar jogando") }
            }
        )
    }

    LaunchedEffect(showPause) { viewModel.setPaused(showPause) }
}

@Composable
private fun GameTopBar(
    mode: GameMode,
    round: Int,
    roundsLeft: Int?,
    score: Int,
    lives: Int?,
    secondsLeft: Int?,
    stage: Int,
    onPause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { onPause() }
                .padding(8.dp)
        ) {
            Text("⏸", style = MaterialTheme.typography.titleLarge)
        }

        StatPill(emoji = mode.emoji, value = if (mode == GameMode.DILUVIO) "Grade $stage" else mode.title)

        StatPill(
            emoji = "🔄",
            value = roundsLeft?.let { "$it" } ?: "$round"
        )

        if (secondsLeft != null) {
            val urgent = secondsLeft <= 8
            val pulse by animateFloatAsState(
                targetValue = if (urgent) 1.12f else 1f,
                animationSpec = spring(dampingRatio = 0.3f),
                label = "timerPulse"
            )
            StatPill(
                emoji = "⏱️",
                value = "${secondsLeft}s",
                tint = if (urgent) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.scale(pulse)
            )
        }

        if (lives != null) {
            StatPill(emoji = "❤️", value = "$lives")
        }

        Spacer(Modifier.weight(1f))

        StatPill(
            emoji = "🏅",
            value = "$score",
            tint = MaterialTheme.colorScheme.primaryContainer
        )
    }
}

@Composable
private fun ClueBanner(
    entry: PuzzleEntry?,
    solved: Boolean,
    modifier: Modifier = Modifier,
    onOpenList: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onOpenList() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry?.let {
                    val dir = if (it.direction == Direction.ACROSS) "horizontal" else "vertical"
                    "${it.number} $dir · ${it.length} letras"
                } ?: "Escolha uma palavra",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = entry?.clue ?: "Toque em um quadradinho ou veja a lista de dicas",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(if (solved) "✅" else "📋", fontSize = 22.sp)
    }
}

@Composable
private fun RoundBar(
    movesLeft: Int,
    warning: Boolean,
    modifier: Modifier = Modifier,
    onEndRound: () -> Unit,
    onWatchAdMoves: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (warning) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .clickable { onEndRound() }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = if (warning) "Passar rodada ⚠️" else "Nova rodada ▶",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (warning) {
                    "Sobrou jogada: um quadradinho pode trancar"
                } else {
                    "Todas as jogadas usadas — sem risco"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable { onWatchAdMoves() }
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text("🎬+3", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ClueList(
    across: List<PuzzleEntry>,
    down: List<PuzzleEntry>,
    solved: Set<Int>,
    activeId: Int?,
    onSelect: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(480.dp)
    ) {
        item {
            Text(
                "Horizontais ➡️",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items(across) { entry ->
            ClueRow(entry, entry.id in solved, entry.id == activeId, onSelect)
        }
        item {
            Text(
                "Verticais ⬇️",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items(down) { entry ->
            ClueRow(entry, entry.id in solved, entry.id == activeId, onSelect)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ClueRow(
    entry: PuzzleEntry,
    solved: Boolean,
    active: Boolean,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (active) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                }
            )
            .clickable { onSelect(entry.id) }
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "${entry.number}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = entry.clue,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (solved) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        Text(if (solved) "✅" else "${entry.length}")
    }
}

@Composable
private fun MessageBanner(
    message: String?,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2200)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier.padding(top = 60.dp)
    ) {
        Text(
            text = message.orEmpty(),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
