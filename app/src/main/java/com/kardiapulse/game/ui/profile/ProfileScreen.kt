package com.kardiapulse.game.ui.profile

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiapulse.game.data.Achievements
import com.kardiapulse.game.data.PlayerProfile
import com.kardiapulse.game.data.Rank
import com.kardiapulse.game.ui.common.Caption
import com.kardiapulse.game.ui.common.GlassPanel
import com.kardiapulse.game.ui.common.GlowBar
import com.kardiapulse.game.ui.common.KardiaBackground
import com.kardiapulse.game.ui.common.ScreenHeader
import com.kardiapulse.game.ui.common.SectionTitle
import com.kardiapulse.game.ui.theme.DangerRed
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.PulseGold
import com.kardiapulse.game.ui.theme.PulseViolet
import com.kardiapulse.game.ui.theme.SurfaceCard
import com.kardiapulse.game.ui.theme.SurfaceStroke
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary

@Composable
fun ProfileScreen(profile: PlayerProfile, trustworthyDevice: Boolean, onBack: () -> Unit) {
    KardiaBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            ScreenHeader(title = "Perfil", onBack = onBack)

            GlassPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceCard)
                            .border(
                                BorderStroke(1.dp, PulseViolet.copy(alpha = 0.6f)),
                                RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(profile.rank.glyph, color = PulseViolet, fontSize = 24.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Nível ${profile.level} · ${profile.rank.ptName}",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        val next = Rank.next(profile.level)
                        Caption(
                            if (next != null)
                                "Próxima patente: ${next.ptName} no nível ${next.minLevel}"
                            else "Patente máxima alcançada."
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                GlowBar(progress = profile.levelProgress)
                Spacer(Modifier.height(5.dp))
                Text(
                    "${profile.xp} XP acumulados",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }

            if (profile.tampered) {
                Spacer(Modifier.height(12.dp))
                GlassPanel(Modifier.fillMaxWidth(), borderColor = DangerRed.copy(alpha = 0.6f)) {
                    Text(
                        "Progresso restaurado",
                        color = DangerRed,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Caption(
                        "A assinatura do arquivo de progresso não conferiu, então ele foi " +
                            "reiniciado. Isso acontece quando o save é editado por fora do jogo — " +
                            "ou, mais raramente, depois de uma restauração de backup."
                    )
                }
            }

            if (profile.suspicious) {
                Spacer(Modifier.height(12.dp))
                GlassPanel(Modifier.fillMaxWidth(), borderColor = DangerRed.copy(alpha = 0.5f)) {
                    Text(
                        "Gravação barrada",
                        color = DangerRed,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Caption(
                        "Uma alteração impossível de acontecer jogando foi detectada e recusada — " +
                            "o valor anterior foi mantido. Se isso apareceu sem você ter mexido em " +
                            "nada, apagar o progresso em Ajustes limpa a marca."
                    )
                }
            }

            if (!trustworthyDevice) {
                Spacer(Modifier.height(12.dp))
                GlassPanel(Modifier.fillMaxWidth(), borderColor = PulseGold.copy(alpha = 0.5f)) {
                    Caption(
                        "Este aparelho está com root ou em modo de depuração. O jogo funciona " +
                            "normalmente; os recordes apenas não são marcados como verificados."
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Números")
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile("duelos", profile.duelsPlayed.toString(), PulseCyan, Modifier.weight(1f))
                StatTile("vitórias", profile.duelsWon.toString(), PulseGold, Modifier.weight(1f))
                StatTile("aproveitamento", "${profile.winRate}%", PulseViolet, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile("maior corrente", profile.bestChain.toString(), PulseGold, Modifier.weight(1f))
                StatTile("inversões", profile.totalFlips.toString(), PulseCyan, Modifier.weight(1f))
                StatTile("sobrevivência", profile.endlessBest.toString(), PulseViolet, Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            val unlocked = Achievements.ALL.count { it.id in profile.achievements }
            SectionTitle("Conquistas · $unlocked/${Achievements.ALL.size}")
            Spacer(Modifier.height(10.dp))
            Achievements.ALL.forEach { achievement ->
                val done = achievement.id in profile.achievements
                val (current, target) = achievement.progress(profile)
                GlassPanel(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    borderColor = if (done) PulseGold.copy(alpha = 0.6f) else SurfaceStroke
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            achievement.glyph,
                            color = if (done) PulseGold else TextMuted,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                achievement.name,
                                color = if (done) TextPrimary else TextMuted,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Caption(achievement.description)
                        }
                        Text(
                            text = if (done) "+${achievement.shardReward} ◈" else "$current/$target",
                            color = if (done) PulseGold else TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    if (!done) {
                        Spacer(Modifier.height(8.dp))
                        GlowBar(
                            progress = achievement.progressFraction(profile),
                            height = 4,
                            colors = listOf(PulseViolet, PulseCyan)
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard.copy(alpha = 0.85f))
            .border(BorderStroke(1.dp, SurfaceStroke), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(2.dp))
        Text(label.uppercase(), color = TextMuted, fontSize = 8.sp)
    }
}
