package com.chuvadeletras.game.ui.progress

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chuvadeletras.game.data.Achievement
import com.chuvadeletras.game.data.Achievements
import com.chuvadeletras.game.data.MissionTemplate
import com.chuvadeletras.game.data.Missions
import com.chuvadeletras.game.data.PlayerProfile
import com.chuvadeletras.game.domain.model.PowerUp
import com.chuvadeletras.game.ui.components.AdOverlay
import com.chuvadeletras.game.ui.components.ChuvaCard
import com.chuvadeletras.game.ui.components.ProgressBar
import com.chuvadeletras.game.ui.components.ScreenHeader
import com.chuvadeletras.game.ui.components.StatPill
import com.chuvadeletras.game.ui.home.HomeMessage
import com.chuvadeletras.game.ui.home.HomeViewModel
import com.chuvadeletras.game.util.findActivity

// ---------------------------------------------------------------------------
// Missões diárias
// ---------------------------------------------------------------------------

@Composable
fun MissionsScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.profile

    ProgressScaffold(
        title = "Missões do dia",
        coins = profile.coins,
        message = state.message,
        onBack = onBack,
        onDismissMessage = viewModel::consumeMessage,
        modifier = modifier
    ) {
        item {
            Text(
                "Missões novas todo dia, à meia-noite. Complete jogando qualquer modo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        items(profile.missions) { progress ->
            val template = Missions.byId(progress.id) ?: return@items
            MissionCard(
                template = template,
                current = progress.progress,
                claimed = progress.claimed,
                onClaim = { viewModel.claimMission(progress.id) }
            )
        }
        if (profile.missions.isEmpty()) {
            item {
                ChuvaCard {
                    Text("Suas missões aparecem assim que você abrir o jogo hoje. 🌧️")
                }
            }
        }
    }
}

@Composable
private fun MissionCard(
    template: MissionTemplate,
    current: Int,
    claimed: Boolean,
    onClaim: () -> Unit
) {
    val complete = current >= template.target
    ChuvaCard(highlighted = complete && !claimed) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(template.emoji, fontSize = 26.sp)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(template.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "💧 ${template.coins} · ⚡ ${template.xp} XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                claimed -> Text("✅", fontSize = 24.sp)
                complete -> ClaimButton(onClaim)
                else -> Text(
                    "$current/${template.target}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        ProgressBar(
            progress = current.toFloat() / template.target,
            color = if (complete) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Conquistas
// ---------------------------------------------------------------------------

@Composable
fun AchievementsScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.profile

    ProgressScaffold(
        title = "Conquistas",
        coins = profile.coins,
        message = state.message,
        onBack = onBack,
        onDismissMessage = viewModel::consumeMessage,
        modifier = modifier
    ) {
        item { PlayerStatsCard(profile) }
        items(Achievements.all) { achievement ->
            AchievementCard(
                achievement = achievement,
                profile = profile,
                onClaim = { viewModel.claimAchievement(achievement.id) }
            )
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    profile: PlayerProfile,
    onClaim: () -> Unit
) {
    val progress = Achievements.progressOf(achievement, profile)
    val complete = progress >= achievement.target
    val claimed = achievement.id in profile.claimedAchievements

    ChuvaCard(highlighted = complete && !claimed) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                achievement.emoji,
                fontSize = 28.sp,
                modifier = Modifier.alpha(if (complete) 1f else 0.45f)
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(achievement.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                claimed -> Text("✅", fontSize = 22.sp)
                complete -> ClaimButton(onClaim)
                else -> Text(
                    "$progress/${achievement.target}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!complete) {
            Spacer(Modifier.height(10.dp))
            ProgressBar(progress = progress.toFloat() / achievement.target)
        }
    }
}

@Composable
private fun PlayerStatsCard(profile: PlayerProfile) {
    ChuvaCard {
        Text("Seus números", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        StatLine("Partidas jogadas", "${profile.stats.gamesPlayed}")
        StatLine("Vitórias", "${profile.stats.gamesWon}")
        StatLine("Palavras completas", "${profile.stats.wordsSolved}")
        StatLine("Partidas perfeitas", "${profile.stats.perfectGames}")
        StatLine("Melhor combo", "${profile.stats.bestCombo}x")
        StatLine("Melhor pontuação", "${profile.stats.bestScore}")
        StatLine("Quadradinhos perdidos", "${profile.stats.cellsLost}")
        StatLine("Desafios diários", "${profile.stats.dailiesCompleted}")
        StatLine("Melhor ofensiva", "${profile.bestStreak} dias")
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

// ---------------------------------------------------------------------------
// Loja
// ---------------------------------------------------------------------------

@Composable
fun ShopScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val adState by viewModel.adState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()
    val profile = state.profile

    Box(modifier = modifier.fillMaxSize()) {
        ProgressScaffold(
            title = "Loja",
            coins = profile.coins,
            message = state.message,
            onBack = onBack,
            onDismissMessage = viewModel::consumeMessage
        ) {
            item {
                ChuvaCard(highlighted = state.freeCoinsReady) {
                    Text("Gotas grátis 🎬", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (state.freeCoinsReady) {
                            "Assista a um vídeo curto e leve 100 gotas. Volta a cada 4 horas."
                        } else {
                            "Você já pegou as gotas grátis. Volte daqui a algumas horas."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (state.freeCoinsReady) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable(enabled = state.freeCoinsReady) {
                                viewModel.watchForCoins(activity)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (state.freeCoinsReady) "Assistir e ganhar 💧100" else "Indisponível agora",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (state.freeCoinsReady) {
                                MaterialTheme.colorScheme.onSecondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    "Itens",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(PowerUp.entries.toList()) { powerUp ->
                ShopItemCard(
                    powerUp = powerUp,
                    owned = profile.powerUpCount(powerUp),
                    affordable = profile.coins >= powerUp.price,
                    onBuy = { viewModel.buy(powerUp) }
                )
            }
        }
        AdOverlay(state = adState)
    }
}

@Composable
private fun ShopItemCard(
    powerUp: PowerUp,
    owned: Int,
    affordable: Boolean,
    onBuy: () -> Unit
) {
    ChuvaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(powerUp.emoji, fontSize = 30.sp)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(powerUp.label, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "(você tem $owned)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    powerUp.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (affordable) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .clickable(enabled = affordable) { onBuy() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    "💧${powerUp.price}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (affordable) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Ajustes
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    onReplayTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.profile

    ProgressScaffold(
        title = "Ajustes",
        coins = profile.coins,
        message = state.message,
        onBack = onBack,
        onDismissMessage = viewModel::consumeMessage,
        modifier = modifier
    ) {
        item {
            ChuvaCard {
                ToggleRow(
                    label = "Som",
                    checked = profile.soundEnabled,
                    onChange = viewModel::setSound
                )
                ToggleRow(
                    label = "Vibração",
                    checked = profile.vibrationEnabled,
                    onChange = viewModel::setVibration
                )
            }
        }
        item {
            ChuvaCard(onClick = {
                viewModel.replayTutorial()
                onReplayTutorial()
            }) {
                Text("Rever o tutorial 🎓", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Mostra o passo a passo de novo na sua próxima partida.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            ChuvaCard(onClick = onTerms) {
                Text("Termos de Uso 📄", style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            ChuvaCard(onClick = onPrivacy) {
                Text("Política de Privacidade 🔒", style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            ChuvaCard {
                Text("Sobre", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Chuva de Letras · versão 1.0.0\n" +
                        "Seu progresso fica salvo apenas neste aparelho.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ---------------------------------------------------------------------------
// Peças compartilhadas
// ---------------------------------------------------------------------------

@Composable
private fun ClaimButton(onClaim: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .clickable { onClaim() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            "Resgatar",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}

@Composable
private fun ProgressScaffold(
    title: String,
    coins: Int,
    message: String?,
    onBack: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            ScreenHeader(
                title = title,
                onBack = onBack,
                trailing = {
                    StatPill(
                        emoji = "💧",
                        value = "$coins",
                        tint = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
        HomeMessage(
            message = message,
            modifier = Modifier.align(Alignment.BottomCenter),
            onDismiss = onDismissMessage
        )
    }
}
