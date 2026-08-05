package com.formatfrute.game.ui.achievements

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formatfrute.game.audio.Haptics
import com.formatfrute.game.audio.Sfx
import com.formatfrute.game.audio.SoundManager
import com.formatfrute.game.data.Achievement
import com.formatfrute.game.data.Achievements
import com.formatfrute.game.data.GameRepository
import com.formatfrute.game.ui.components.ChunkyBar
import com.formatfrute.game.ui.components.Confetti
import com.formatfrute.game.ui.components.FruitBackground
import com.formatfrute.game.ui.components.JuicyButton
import com.formatfrute.game.ui.components.PaperCard
import com.formatfrute.game.ui.components.Pulse
import com.formatfrute.game.ui.components.StatPill
import com.formatfrute.game.ui.theme.Fruta

/**
 * Conquistas. Locais de propósito: sem login, sem conta, sem servidor — e por
 * isso funcionam offline, do primeiro minuto de jogo em diante.
 */
@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { GameRepository.get(context) }
    val sound = remember { SoundManager.get(context) }
    val haptics = remember { Haptics.get(context) }
    val profile by repo.profile.collectAsStateWithLifecycle()

    var celebrate by remember { mutableStateOf(false) }
    LaunchedEffect(celebrate) {
        if (celebrate) {
            kotlinx.coroutines.delay(2400)
            celebrate = false
        }
    }

    val done = Achievements.doneCount(profile)

    // Prontas primeiro, depois as em andamento, e as já retiradas no fim.
    val ordered = remember(profile) {
        Achievements.all.sortedBy { achievement ->
            when {
                achievement.isDone(profile) &&
                    achievement.id !in profile.achievementsClaimed -> 0
                !achievement.isDone(profile) -> 1
                else -> 2
            }
        }
    }

    FruitBackground(profile.boardTheme, density = 4) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(3.dp, Fruta.Ink.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⬅", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Conquistas",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (profile.boardTheme.dark) Color.White else Fruta.Ink,
                    )
                    Text(
                        "$done de ${Achievements.all.size} desbloqueadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (profile.boardTheme.dark) Color.White.copy(alpha = 0.8f)
                        else Fruta.InkSoft,
                    )
                }
                StatPill("🏅", "$done")
            }

            ChunkyBar(
                progress = done.toFloat() / Achievements.all.size,
                color = Fruta.Sun,
                height = 14.dp,
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(ordered) { achievement ->
                    AchievementRow(
                        achievement = achievement,
                        progress = achievement.progressOf(profile),
                        claimed = achievement.id in profile.achievementsClaimed,
                        onClaim = {
                            if (repo.claimAchievement(achievement) > 0) {
                                sound.play(Sfx.COIN)
                                haptics.win()
                                celebrate = true
                            }
                        },
                    )
                }
                item { Spacer(Modifier.height(28.dp)) }
            }
        }

        Confetti(active = celebrate)
    }
}

@Composable
private fun AchievementRow(
    achievement: Achievement,
    progress: Int,
    claimed: Boolean,
    onClaim: () -> Unit,
) {
    val done = progress >= achievement.target
    PaperCard(
        Modifier.fillMaxWidth(),
        color = when {
            claimed -> Fruta.Leaf.copy(alpha = 0.16f)
            done -> Color.White
            else -> Color.White.copy(alpha = 0.8f)
        },
        corner = 20.dp,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (done) Fruta.Sun.copy(alpha = 0.28f) else Fruta.InkSoft.copy(alpha = 0.14f))
                    .border(
                        2.5.dp,
                        if (done) Fruta.Sun else Fruta.InkSoft.copy(alpha = 0.4f),
                        RoundedCornerShape(18.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (done) achievement.emoji else "🔒",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (done) Fruta.Ink else Fruta.InkSoft,
                )
                Text(
                    achievement.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Fruta.InkSoft,
                )
                Spacer(Modifier.height(5.dp))
                ChunkyBar(
                    progress = progress.toFloat() / achievement.target,
                    color = if (done) Fruta.Leaf else Fruta.Sun,
                    height = 11.dp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$progress / ${achievement.target}  •  +${achievement.reward} 🌱",
                    style = MaterialTheme.typography.labelSmall,
                    color = Fruta.InkSoft,
                )
            }

            Spacer(Modifier.width(8.dp))

            when {
                claimed -> Text("✅", style = MaterialTheme.typography.headlineSmall)
                done -> Box(contentAlignment = Alignment.Center) {
                    Pulse(Fruta.Leaf.copy(alpha = 0.5f), Modifier.size(86.dp, 42.dp), corner = 18.dp)
                    JuicyButton(
                        text = "Pegar",
                        color = Fruta.Leaf,
                        height = 36.dp,
                        modifier = Modifier.width(86.dp),
                        onClick = onClaim,
                    )
                }
                else -> Text("⏳", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
