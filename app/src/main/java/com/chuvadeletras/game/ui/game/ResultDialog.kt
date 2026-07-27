package com.chuvadeletras.game.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chuvadeletras.game.data.MatchOutcome
import com.chuvadeletras.game.domain.model.GameMode
import com.chuvadeletras.game.ui.components.BigActionButton
import com.chuvadeletras.game.ui.components.blockTouches
import com.chuvadeletras.game.ui.components.ConfettiOverlay
import com.chuvadeletras.game.ui.components.StarRow

/** Frase da comemoração — muda conforme o desempenho, para não cansar. */
private fun celebrationTitle(stars: Int, perfect: Boolean): String = when {
    perfect -> "PERFEITO! 💎"
    stars >= 3 -> "SENSACIONAL! 🎉"
    stars == 2 -> "MUITO BOM! 🌟"
    else -> "GRADE COMPLETA! 👏"
}

private fun celebrationSubtitle(stars: Int, locked: Int): String = when {
    locked == 0 -> "Nenhum quadradinho perdido. Isso é domínio total da chuva."
    locked <= 2 -> "Quase impecável — só $locked quadradinho${if (locked > 1) "s" else ""} escapou."
    else -> "Você terminou, mas $locked quadradinhos ficaram trancados. Dá para melhorar!"
}

@Composable
fun ResultDialog(
    outcome: MatchOutcome,
    rewardDoubled: Boolean,
    modifier: Modifier = Modifier,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
    onWatchAdDouble: () -> Unit,
    onWatchAdRevive: () -> Unit
) {
    val summary = outcome.summary
    val won = summary.won

    // O card entra dando um "pulo".
    val pop = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 260f))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .blockTouches(),
        contentAlignment = Alignment.Center
    ) {
        ConfettiOverlay(running = won)

        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (won) celebrationTitle(summary.stars, summary.perfect) else "A chuva passou 🌫️",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = if (won) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )

            if (won) {
                StarRow(stars = summary.stars)
                Text(
                    text = celebrationSubtitle(summary.stars, summary.lockedCells),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Você completou ${summary.wordsSolved} de ${summary.totalWords} palavras. " +
                        "Quer continuar de onde parou?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            ResultStats(
                score = summary.score,
                words = "${summary.wordsSolved}/${summary.totalWords}",
                combo = summary.bestCombo,
                locked = summary.lockedCells,
                rounds = summary.roundsUsed
            )

            RewardRow(
                coins = summary.coinsEarned * if (rewardDoubled) 2 else 1,
                xp = summary.xpEarned,
                doubled = rewardDoubled
            )

            if (outcome.leveledUp) {
                HighlightBanner(
                    emoji = "🎖️",
                    title = "Subiu para o nível ${outcome.newLevel}!",
                    subtitle = "+${outcome.levelUpCoins} gotas de bônus"
                )
            }
            outcome.unlockedMode?.let { mode ->
                HighlightBanner(
                    emoji = mode.emoji,
                    title = "Modo ${mode.title} desbloqueado!",
                    subtitle = mode.tagline
                )
            }
            outcome.completedMissions.forEach { mission ->
                HighlightBanner(
                    emoji = mission.emoji,
                    title = "Missão concluída: ${mission.title}",
                    subtitle = "Resgate na tela inicial"
                )
            }
            outcome.unlockedAchievements.forEach { achievement ->
                HighlightBanner(
                    emoji = achievement.emoji,
                    title = "Conquista: ${achievement.title}",
                    subtitle = "+${achievement.coins} gotas para resgatar"
                )
            }

            Spacer(Modifier.height(4.dp))

            if (!won) {
                BigActionButton(
                    label = "Continuar jogando",
                    emoji = "❤️",
                    subtitle = "Assista a um vídeo e volte com tudo reparado",
                    container = MaterialTheme.colorScheme.secondary,
                    content = MaterialTheme.colorScheme.onSecondary,
                    onClick = onWatchAdRevive
                )
            } else if (!rewardDoubled) {
                BigActionButton(
                    label = "Dobrar prêmio",
                    emoji = "✨",
                    subtitle = "Assista a um vídeo e leve ${summary.coinsEarned} gotas extras",
                    container = MaterialTheme.colorScheme.secondary,
                    content = MaterialTheme.colorScheme.onSecondary,
                    onClick = onWatchAdDouble
                )
            }

            BigActionButton(
                label = if (summary.mode == GameMode.DIARIO) "Jogar outro modo" else "Jogar de novo",
                emoji = "🔁",
                onClick = if (summary.mode == GameMode.DIARIO) onHome else onPlayAgain
            )

            TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                Text("Voltar ao início")
            }
        }
    }
}

@Composable
private fun ResultStats(score: Int, words: String, combo: Int, locked: Int, rounds: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatLine("Pontuação", score.toString(), big = true)
        StatLine("Palavras", words)
        StatLine("Melhor combo", "${combo}x")
        StatLine("Quadradinhos perdidos", locked.toString())
        StatLine("Rodadas usadas", rounds.toString())
    }
}

@Composable
private fun StatLine(label: String, value: String, big: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (big) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = if (big) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RewardRow(coins: Int, xp: Int, doubled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RewardChip("💧", "$coins", if (doubled) "gotas (dobrado!)" else "gotas", Modifier.weight(1f))
        RewardChip("⚡", "$xp", "XP", Modifier.weight(1f))
    }
}

@Composable
private fun RewardChip(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HighlightBanner(emoji: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(emoji, fontSize = 24.sp)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}
