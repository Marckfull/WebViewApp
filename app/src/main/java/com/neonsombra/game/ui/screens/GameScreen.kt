package com.neonsombra.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neonsombra.game.NeonSombraApplication
import com.neonsombra.game.game.GameSnapshot
import com.neonsombra.game.game.GameStatus
import com.neonsombra.game.game.GameViewModel
import com.neonsombra.game.game.NEXT_COUNT
import com.neonsombra.game.game.TetrominoType
import com.neonsombra.game.ui.components.NeonBackground
import com.neonsombra.game.ui.components.NeonButton
import com.neonsombra.game.ui.components.NeonLabel
import com.neonsombra.game.ui.components.NeonLogo
import com.neonsombra.game.ui.components.NeonPanel
import com.neonsombra.game.ui.components.NeonText
import com.neonsombra.game.ui.game.PiecePreview
import com.neonsombra.game.ui.game.TetrisBoard
import com.neonsombra.game.ui.theme.NeonCyan
import com.neonsombra.game.ui.theme.NeonLime
import com.neonsombra.game.ui.theme.NeonMagenta
import com.neonsombra.game.ui.theme.NeonPurple
import com.neonsombra.game.ui.theme.NeonTextMuted
import com.neonsombra.game.ui.theme.NeonYellow
import kotlin.math.ceil

/**
 * A tela de jogo, montada como no rascunho: o tabuleiro a esquerda, a coluna de
 * informacoes a direita e o nome do jogo acesso no alto de tudo.
 */
@Composable
fun GameScreen(onExitToMenu: () -> Unit) {
    val application = LocalContext.current.applicationContext as NeonSombraApplication
    val viewModel: GameViewModel = viewModel(
        factory = GameViewModel.factory(application.prefs, application.sound),
    )
    val snapshot = viewModel.snapshot

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.pauseGame() }

    BackHandler {
        if (snapshot.status == GameStatus.PLAYING) viewModel.pauseGame() else onExitToMenu()
    }

    NeonBackground(
        modifier = Modifier.fillMaxSize(),
        showSkyline = true,
        intensity = 0.55f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeonLogo(modifier = Modifier.weight(1f), compact = true)
                NeonButton(
                    text = if (snapshot.status == GameStatus.PAUSED) "SEGUIR" else "PAUSA",
                    onClick = viewModel::togglePause,
                    accent = NeonPurple,
                )
            }

            StatusStrip(snapshot = snapshot)

            Spacer(Modifier.height(6.dp))

            Row(modifier = Modifier.weight(1f)) {
                TetrisBoard(
                    snapshot = snapshot,
                    showGhost = viewModel.ghostEnabled,
                    onPressStart = viewModel::onPressStart,
                    onPressEnd = viewModel::onPressEnd,
                    onHorizontalStep = viewModel::onHorizontalStep,
                    onHardDrop = viewModel::onHardDrop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                Spacer(Modifier.width(10.dp))

                SidePanel(
                    playerName = viewModel.playerName,
                    score = snapshot.score,
                    highScore = viewModel.highScore,
                    level = snapshot.level,
                    lines = snapshot.lines,
                    next = snapshot.next,
                    modifier = Modifier
                        .widthIn(min = 96.dp, max = 132.dp)
                        .fillMaxHeight(),
                )
            }
        }

        AnimatedVisibility(
            visible = snapshot.status == GameStatus.PAUSED,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PausedOverlay(
                onResume = viewModel::resumeGame,
                onRestart = viewModel::newGame,
                onExit = onExitToMenu,
            )
        }

        AnimatedVisibility(
            visible = snapshot.status == GameStatus.GAME_OVER,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut(),
        ) {
            GameOverOverlay(
                score = snapshot.score,
                lines = snapshot.lines,
                level = snapshot.level,
                isRecord = viewModel.isNewRecord,
                highScore = viewModel.highScore,
                onRestart = viewModel::newGame,
                onExit = onExitToMenu,
            )
        }
    }
}

/**
 * Uma linha so, entre o logo e o tabuleiro, dizendo o que esta em jogo agora:
 * a Purga rodando, o combo em andamento ou o aviso de que a sombra ataca.
 */
@Composable
private fun StatusStrip(snapshot: GameSnapshot) {
    val transition = rememberInfiniteTransition(label = "estado")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pisca",
    )

    val status: Pair<String, Color>? = when {
        snapshot.purgeActive ->
            "PURGA ATIVA  ${ceil(snapshot.purgeSecondsLeft).toInt()}s" to NeonLime

        snapshot.combo >= 2 ->
            "COMBO x${snapshot.combo}" to NeonYellow

        snapshot.shadowAttacksActive ->
            "CUIDADO: A SOMBRA ATACA" to NeonMagenta

        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (status != null) {
            NeonText(
                text = status.first,
                color = status.second,
                glowRadius = 18f * pulse,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SidePanel(
    playerName: String,
    score: Int,
    highScore: Int,
    level: Int,
    lines: Int,
    next: List<TetrominoType>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatPanel(label = "JOGADOR", value = playerName, accent = NeonCyan, valueSize = 14)
        StatPanel(label = "PONTUACAO", value = score.toString(), accent = NeonYellow, valueSize = 20)
        StatPanel(label = "RECORDE", value = highScore.toString(), accent = NeonMagenta, valueSize = 16)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPanel(
                label = "NIVEL",
                value = level.toString(),
                accent = NeonLime,
                valueSize = 14,
                modifier = Modifier.weight(1f),
            )
            StatPanel(
                label = "LINHAS",
                value = lines.toString(),
                accent = NeonLime,
                valueSize = 14,
                modifier = Modifier.weight(1f),
            )
        }

        NeonPanel(
            accent = NeonPurple,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = 8.dp,
        ) {
            NeonLabel(text = "PROXIMAS", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            repeat(NEXT_COUNT) { index ->
                PiecePreview(
                    type = next.getOrNull(index),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun StatPanel(
    label: String,
    value: String,
    accent: Color,
    valueSize: Int,
    modifier: Modifier = Modifier,
) {
    NeonPanel(
        accent = accent,
        modifier = modifier.fillMaxWidth(),
        contentPadding = 8.dp,
    ) {
        NeonLabel(text = label, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(2.dp))
        NeonText(
            text = value,
            color = accent,
            glowRadius = 12f,
            maxLines = 1,
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = valueSize.sp,
                letterSpacing = 1.sp,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PausedOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC05010D))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        NeonPanel(
            accent = NeonPurple,
            modifier = Modifier.padding(32.dp),
            contentPadding = 24.dp,
        ) {
            NeonText(
                text = "PAUSA",
                color = NeonPurple,
                glowRadius = 24f,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            NeonButton(
                text = "CONTINUAR",
                onClick = onResume,
                accent = NeonCyan,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            NeonButton(
                text = "RECOMECAR",
                onClick = onRestart,
                accent = NeonYellow,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            NeonButton(
                text = "SAIR",
                onClick = onExit,
                accent = NeonMagenta,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    lines: Int,
    level: Int,
    isRecord: Boolean,
    highScore: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE605010D)),
        contentAlignment = Alignment.Center,
    ) {
        NeonPanel(
            accent = NeonMagenta,
            modifier = Modifier.padding(28.dp),
            contentPadding = 24.dp,
        ) {
            NeonText(
                text = "FIM DE JOGO",
                color = NeonMagenta,
                glowRadius = 26f,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 26.sp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))

            if (isRecord) {
                NeonText(
                    text = "NOVO RECORDE!",
                    color = NeonYellow,
                    glowRadius = 20f,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }

            ResultLine(label = "PONTUACAO", value = score.toString(), accent = NeonYellow)
            ResultLine(label = "RECORDE", value = highScore.toString(), accent = NeonMagenta)
            ResultLine(label = "LINHAS", value = lines.toString(), accent = NeonLime)
            ResultLine(label = "NIVEL", value = level.toString(), accent = NeonCyan)

            Spacer(Modifier.height(20.dp))
            NeonButton(
                text = "JOGAR DE NOVO",
                onClick = onRestart,
                accent = NeonCyan,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            NeonButton(
                text = "MENU",
                onClick = onExit,
                accent = NeonPurple,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ResultLine(label: String, value: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = NeonTextMuted,
            style = MaterialTheme.typography.labelSmall,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        Spacer(Modifier.width(16.dp))
        NeonText(
            text = value,
            color = accent,
            glowRadius = 12f,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
