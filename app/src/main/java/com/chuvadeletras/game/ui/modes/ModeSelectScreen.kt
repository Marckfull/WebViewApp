package com.chuvadeletras.game.ui.modes

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chuvadeletras.game.data.ModeUnlocks
import com.chuvadeletras.game.domain.model.Difficulty
import com.chuvadeletras.game.domain.model.GameMode
import com.chuvadeletras.game.ui.components.ChuvaCard
import com.chuvadeletras.game.ui.components.ScreenHeader
import com.chuvadeletras.game.ui.home.HomeViewModel

@Composable
fun ModeSelectScreen(
    viewModel: HomeViewModel,
    onStart: (GameMode, Difficulty) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.profile
    var difficulty by remember { mutableStateOf(Difficulty.forLevel(profile.campaignLevel)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        ScreenHeader(title = "Escolha o modo", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                DifficultyPicker(
                    selected = difficulty,
                    onSelect = { difficulty = it }
                )
            }

            items(GameMode.entries.size) { index ->
                val mode = GameMode.entries[index]
                val unlocked = ModeUnlocks.isUnlocked(mode, profile)
                ModeCard(
                    mode = mode,
                    unlocked = unlocked,
                    requiredLevel = ModeUnlocks.requiredLevel(mode),
                    onClick = {
                        if (unlocked) {
                            onStart(
                                mode,
                                // O Diário e o Dilúvio definem a própria dificuldade.
                                when (mode) {
                                    GameMode.DIARIO -> Difficulty.MEDIO
                                    GameMode.DILUVIO -> Difficulty.FACIL
                                    else -> difficulty
                                }
                            )
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun DifficultyPicker(selected: Difficulty, onSelect: (Difficulty) -> Unit) {
    ChuvaCard {
        Text("Tamanho da grade", style = MaterialTheme.typography.titleMedium)
        Text(
            "Vale para a Chuva Calma, a Tempestade e o Sereno.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Difficulty.entries.forEach { option ->
                val active = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .clickable { onSelect(option) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            "${option.targetWords} palavras",
                            fontSize = 10.sp,
                            color = if (active) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    mode: GameMode,
    unlocked: Boolean,
    requiredLevel: Int,
    onClick: () -> Unit
) {
    ChuvaCard(onClick = onClick, modifier = Modifier.alpha(if (unlocked) 1f else 0.55f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(mode.emoji, fontSize = 34.sp)
            Spacer(Modifier.height(0.dp))
            Column(modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)) {
                Text(mode.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    mode.tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!unlocked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔒", fontSize = 20.sp)
                    Text(
                        "Nível $requiredLevel",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            mode.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
