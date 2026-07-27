package com.chuvadeletras.game.ui.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chuvadeletras.game.data.Achievements
import com.chuvadeletras.game.data.DailyRewards
import com.chuvadeletras.game.data.Missions
import com.chuvadeletras.game.data.PlayerRank
import com.chuvadeletras.game.data.StreakBonus
import com.chuvadeletras.game.domain.model.GameMode
import com.chuvadeletras.game.ui.components.AdOverlay
import com.chuvadeletras.game.ui.components.BigActionButton
import com.chuvadeletras.game.ui.components.ChuvaCard
import com.chuvadeletras.game.ui.components.ProgressBar
import com.chuvadeletras.game.ui.components.PulsingGlow
import com.chuvadeletras.game.ui.components.RainBackground
import com.chuvadeletras.game.ui.components.StatPill
import com.chuvadeletras.game.util.findActivity

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlay: () -> Unit,
    onDailyChallenge: () -> Unit,
    onMissions: () -> Unit,
    onAchievements: () -> Unit,
    onShop: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val adState by viewModel.adState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()
    val profile = state.profile

    Box(modifier = modifier.fillMaxSize()) {
        RainBackground(
            modifier = Modifier.fillMaxSize(),
            dropCount = 40,
            baseAlpha = 0.1f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Topo: gotas, ofensiva e ajustes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPill(
                    emoji = "💧",
                    value = "${profile.coins}",
                    tint = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onShop
                )
                StatPill(emoji = "🔥", value = "${profile.streak} dias")
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSettings() }
                        .padding(10.dp)
                ) {
                    Text("⚙️", fontSize = 20.sp)
                }
            }

            LevelCard(
                level = profile.level,
                rank = PlayerRank.titleFor(profile.level),
                progress = profile.levelProgress,
                xpInto = profile.xpIntoLevel,
                xpNeeded = profile.xpForNextLevel
            )

            Text(
                text = "CHUVA DE LETRAS",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Baú diário: o gancho principal para voltar todo dia
            if (state.canClaimDaily) {
                DailyChestCard(
                    dayIndex = profile.rewardDayIndex,
                    streak = profile.streak,
                    onClaim = { viewModel.claimDaily(doubled = false, activity = activity) },
                    onClaimDoubled = { viewModel.claimDaily(doubled = true, activity = activity) }
                )
            }

            BigActionButton(
                label = "Jogar",
                emoji = "▶️",
                subtitle = "Escolha o modo e mande ver",
                onClick = onPlay
            )

            BigActionButton(
                label = "Desafio Diário",
                emoji = GameMode.DIARIO.emoji,
                subtitle = if (state.dailyChallengeDone) {
                    "Concluído hoje ✅ — volte amanhã"
                } else {
                    "A mesma grade para o mundo todo"
                },
                container = if (state.dailyChallengeDone) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                content = if (state.dailyChallengeDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onTertiary
                },
                onClick = onDailyChallenge
            )

            MissionsPreview(
                profile = profile,
                onOpen = onMissions
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuTile(
                    emoji = "🏆",
                    label = "Conquistas",
                    badge = Achievements.claimable(profile).size,
                    modifier = Modifier.weight(1f),
                    onClick = onAchievements
                )
                MenuTile(
                    emoji = "🛒",
                    label = "Loja",
                    badge = 0,
                    modifier = Modifier.weight(1f),
                    onClick = onShop
                )
            }

            // Gotas grátis por anúncio, com espera de 4h
            ChuvaCard(highlighted = state.freeCoinsReady) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎬", fontSize = 26.sp)
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gotas grátis", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (state.freeCoinsReady) {
                                "Assista a um vídeo e leve 100 gotas"
                            } else {
                                "Já coletado — volte daqui a algumas horas"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
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
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "+100",
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

            StreakCard(streak = profile.streak, best = profile.bestStreak)

            Spacer(Modifier.height(24.dp))
        }

        state.claimedReward?.let { reward ->
            RewardClaimedDialog(
                reward = reward,
                streakBonus = StreakBonus.bonusFor(profile.streak),
                onDismiss = viewModel::dismissClaimedReward
            )
        }

        HomeMessage(
            message = state.message,
            modifier = Modifier.align(Alignment.BottomCenter),
            onDismiss = viewModel::consumeMessage
        )

        AdOverlay(state = adState)
    }
}

@Composable
private fun LevelCard(
    level: Int,
    rank: String,
    progress: Float,
    xpInto: Int,
    xpNeeded: Int
) {
    ChuvaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$level",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rank, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$xpInto / $xpNeeded XP para o próximo nível",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        ProgressBar(progress = progress)
    }
}

@Composable
private fun DailyChestCard(
    dayIndex: Int,
    streak: Int,
    onClaim: () -> Unit,
    onClaimDoubled: () -> Unit
) {
    val reward = DailyRewards.rewardFor(dayIndex)
    ChuvaCard(highlighted = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                PulsingGlow(modifier = Modifier.size(64.dp))
                Text(if (reward.isBig) "🎁" else "📦", fontSize = 34.sp)
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Baú do dia ${reward.day} de 7",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = buildString {
                        append("💧 ${reward.coins} gotas")
                        reward.powerUp?.let { append(" + ${it.emoji} ${it.label} x${reward.powerUpAmount}") }
                        if (StreakBonus.bonusFor(streak) > 0) {
                            append("\n🔥 Bônus de ofensiva: +${StreakBonus.bonusFor(streak)} gotas!")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onClaim() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Resgatar",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondary)
                    .clickable { onClaimDoubled() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🎬 Resgatar em dobro",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }
}

@Composable
private fun MissionsPreview(
    profile: com.chuvadeletras.game.data.PlayerProfile,
    onOpen: () -> Unit
) {
    val claimable = profile.missions.count { progress ->
        val template = Missions.byId(progress.id)
        template != null && !progress.claimed && progress.progress >= template.target
    }
    ChuvaCard(highlighted = claimable > 0, onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎯", fontSize = 26.sp)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Missões do dia", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (claimable > 0) {
                        "$claimable recompensa${if (claimable > 1) "s" else ""} esperando por você!"
                    } else {
                        "3 missões novas todo dia"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (claimable > 0) {
                Badge(count = claimable)
            } else {
                Text("›", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Composable
private fun MenuTile(
    emoji: String,
    label: String,
    badge: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onClick() }
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
        if (badge > 0) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Badge(count = badge)
            }
        }
    }
}

@Composable
private fun Badge(count: Int) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.error),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onError
        )
    }
}

@Composable
private fun StreakCard(streak: Int, best: Int) {
    val next = StreakBonus.nextMilestone(streak)
    ChuvaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔥", fontSize = 26.sp)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Ofensiva de $streak dia${if (streak == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = next?.let {
                        "Faltam ${it - streak} dia(s) para o bônus de ${StreakBonus.bonusFor(it)} gotas"
                    } ?: "Seu recorde: $best dias. Continue assim!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(7) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (index < (streak % 7).let { if (it == 0 && streak > 0) 7 else it }) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }
    }
}
