package com.kardiapulse.game.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiapulse.game.data.DailyPass
import com.kardiapulse.game.data.PlayerProfile
import com.kardiapulse.game.ui.common.Caption
import com.kardiapulse.game.ui.common.GlassPanel
import com.kardiapulse.game.ui.common.GlowBar
import com.kardiapulse.game.ui.common.KardiaBackground
import com.kardiapulse.game.ui.common.PulseButton
import com.kardiapulse.game.ui.common.ResourceChip
import com.kardiapulse.game.ui.legal.LegalFooter
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.PulseGold
import com.kardiapulse.game.ui.theme.PulseViolet
import com.kardiapulse.game.ui.theme.SurfaceCard
import com.kardiapulse.game.ui.theme.SurfaceStroke
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary
import com.kardiapulse.game.ui.theme.TextSecondary

data class HomeActions(
    val onPlay: () -> Unit,
    val onTutorial: () -> Unit,
    val onDaily: () -> Unit,
    val onDailyChallenge: () -> Unit,
    val onShop: () -> Unit,
    val onProfile: () -> Unit,
    val onExtras: () -> Unit,
    val onSettings: () -> Unit,
    val onPrivacy: () -> Unit,
    val onTerms: () -> Unit
)

@Composable
fun HomeScreen(profile: PlayerProfile, actions: HomeActions) {
    val canClaim = DailyPass.canClaim(profile)
    val dailyChallengeDone = profile.dailyChallengeDay == DailyPass.todayEpochDay()

    KardiaBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            TopResources(profile)

            Spacer(Modifier.height(22.dp))
            Logo()

            Spacer(Modifier.height(20.dp))
            LevelCard(profile, actions.onProfile)

            Spacer(Modifier.height(14.dp))
            if (canClaim) {
                DailyCallout(profile, actions.onDaily)
                Spacer(Modifier.height(12.dp))
            }

            if (!profile.tutorialDone) {
                TutorialCallout(profile, actions.onTutorial)
                Spacer(Modifier.height(12.dp))
            }

            PulseButton(
                text = "JOGAR",
                subtitle = "cinco modos, um Núcleo",
                onClick = actions.onPlay,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            PulseButton(
                text = if (dailyChallengeDone) "DESAFIO DIÁRIO CONCLUÍDO" else "DESAFIO DIÁRIO",
                subtitle = if (dailyChallengeDone) "volte amanhã para o próximo"
                else "as mesmas cartas para o mundo inteiro",
                onClick = actions.onDailyChallenge,
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuTile("◈", "Loja", Modifier.weight(1f), actions.onShop)
                MenuTile("◷", "Passe", Modifier.weight(1f), actions.onDaily)
                MenuTile("◉", "Perfil", Modifier.weight(1f), actions.onProfile)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuTile("✦", "Extras", Modifier.weight(1f), actions.onExtras)
                MenuTile("≡", "Tutorial", Modifier.weight(1f), actions.onTutorial)
                MenuTile("⚙", "Ajustes", Modifier.weight(1f), actions.onSettings)
            }

            Spacer(Modifier.height(20.dp))
            LegalFooter(onPrivacy = actions.onPrivacy, onTerms = actions.onTerms)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TopResources(profile: PlayerProfile) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ResourceChip("◈", profile.shards.toString(), PulseGold)
        ResourceChip("✧", profile.crystals.toString(), PulseCyan)
        Spacer(Modifier.weight(1f))
        if (profile.streakDays > 0) {
            ResourceChip("◷", "${profile.streakDays}d", PulseViolet)
        }
    }
}

@Composable
private fun Logo() {
    val transition = rememberInfiniteTransition(label = "logo")
    val glow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "brilho"
    )
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "KARDIA",
            color = TextPrimary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 10.sp
        )
        Text(
            text = "P U L S E",
            color = PulseCyan.copy(alpha = 0.65f + 0.35f * glow),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 7.sp
        )
    }
}

@Composable
private fun LevelCard(profile: PlayerProfile, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        GlassPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .border(
                            BorderStroke(1.dp, PulseViolet.copy(alpha = 0.5f)),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${profile.rank.glyph} ${profile.level}",
                        color = PulseViolet,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = profile.rank.ptName,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${profile.duelsWon} vitórias · ${profile.winRate}% de aproveitamento",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            GlowBar(progress = profile.levelProgress, height = 6)
            Spacer(Modifier.height(5.dp))
            Text(
                text = "${profile.xp - profile.xpForCurrentLevel} / " +
                    "${profile.xpForNextLevel - profile.xpForCurrentLevel} XP para o nível ${profile.level + 1}",
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun DailyCallout(profile: PlayerProfile, onClick: () -> Unit) {
    val day = DailyPass.CYCLE[DailyPass.pendingDayIndex(profile)]
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = PulseGold.copy(alpha = 0.55f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("◷", color = PulseGold, fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Passe Diário disponível",
                        color = PulseGold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Caption("Dia ${day.index + 1} do ciclo · ${day.shards} Fragmentos e ${day.label}")
                }
                Text("›", color = PulseGold, fontSize = 26.sp)
            }
        }
    }
}

@Composable
private fun TutorialCallout(profile: PlayerProfile, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = PulseCyan.copy(alpha = 0.5f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("≡", color = PulseCyan, fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (profile.tutorialStep == 0) "Aprenda a mecânica" else "Tutorial em andamento",
                        color = PulseCyan,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Caption("Oito etapas, uma de cada vez. Rende 250 Fragmentos no final.")
                }
                Text("›", color = PulseCyan, fontSize = 26.sp)
            }
        }
    }
}

@Composable
private fun MenuTile(glyph: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, SurfaceStroke),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(glyph, color = PulseCyan, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
