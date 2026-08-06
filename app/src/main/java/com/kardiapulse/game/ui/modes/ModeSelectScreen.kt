package com.kardiapulse.game.ui.modes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiapulse.game.core.model.Difficulty
import com.kardiapulse.game.core.model.GameMode
import com.kardiapulse.game.ui.common.Caption
import com.kardiapulse.game.ui.common.GlassPanel
import com.kardiapulse.game.ui.common.KardiaBackground
import com.kardiapulse.game.ui.common.PulseButton
import com.kardiapulse.game.ui.common.ScreenHeader
import com.kardiapulse.game.ui.common.SectionTitle
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.PulseGold
import com.kardiapulse.game.ui.theme.SurfaceCard
import com.kardiapulse.game.ui.theme.SurfaceStroke
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary

@Composable
fun ModeSelectScreen(
    endlessBest: Int,
    onStart: (GameMode, Difficulty) -> Unit,
    onBack: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(GameMode.DUELO) }
    var difficulty by remember { mutableStateOf(Difficulty.NORMAL) }

    KardiaBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            ScreenHeader(title = "Modos", onBack = onBack)

            GameMode.ALL.filter { it != GameMode.DIARIO }.forEach { mode ->
                ModeCard(
                    mode = mode,
                    selected = selectedMode == mode,
                    extra = if (mode == GameMode.SOBREVIVENCIA && endlessBest > 0)
                        "seu recorde: $endlessBest rivais" else null,
                    onClick = { selectedMode = mode }
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(6.dp))
            SectionTitle("Dificuldade")
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Difficulty.ALL.forEach { level ->
                    DifficultyChip(
                        difficulty = level,
                        selected = difficulty == level,
                        modifier = Modifier.weight(1f),
                        onClick = { difficulty = level }
                    )
                }
            }
            if (selectedMode == GameMode.SOBREVIVENCIA) {
                Spacer(Modifier.height(8.dp))
                Caption(
                    "Na Sobrevivência a dificuldade sobe sozinha a cada rival derrotado. " +
                        "Isto aqui é só o ponto de partida."
                )
            }

            Spacer(Modifier.height(20.dp))
            PulseButton(
                text = "ENTRAR NO DUELO",
                subtitle = "${selectedMode.ptName} · ${difficulty.ptName}",
                onClick = { onStart(selectedMode, difficulty) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModeCard(
    mode: GameMode,
    selected: Boolean,
    extra: String?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (selected) PulseCyan else SurfaceStroke
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = mode.glyph,
                    color = if (selected) PulseCyan else TextMuted,
                    fontSize = 24.sp
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = mode.ptName,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = mode.tagline,
                        color = if (selected) PulseCyan else TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Caption(mode.description)
                    if (extra != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(extra, color = PulseGold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyChip(
    difficulty: Difficulty,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) PulseCyan.copy(alpha = 0.16f) else SurfaceCard.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, if (selected) PulseCyan else SurfaceStroke),
        modifier = modifier
    ) {
        Box(
            Modifier.padding(vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = difficulty.ptName,
                color = if (selected) PulseCyan else TextMuted,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
