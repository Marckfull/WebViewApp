package com.kardiapulse.game.ui.daily

import android.app.Activity
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiapulse.game.LocalServices
import com.kardiapulse.game.audio.Sfx
import com.kardiapulse.game.data.DailyPass
import com.kardiapulse.game.data.PlayerProfile
import com.kardiapulse.game.haptics.HapticPattern
import com.kardiapulse.game.ui.common.Caption
import com.kardiapulse.game.ui.common.GlassPanel
import com.kardiapulse.game.ui.common.KardiaBackground
import com.kardiapulse.game.ui.common.PulseButton
import com.kardiapulse.game.ui.common.ScreenHeader
import com.kardiapulse.game.ui.common.SectionTitle
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.PulseGold
import com.kardiapulse.game.ui.theme.PulseViolet
import com.kardiapulse.game.ui.theme.SurfaceCard
import com.kardiapulse.game.ui.theme.SurfaceStroke
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary
import com.kardiapulse.game.ui.theme.VoidBlack
import kotlinx.coroutines.launch

/**
 * O Passe Diário. É o motor de retenção do jogo: um ciclo de sete dias que reinicia sozinho,
 * uma sequência que só cresce enquanto o jogador aparece, e um anúncio premiado opcional para
 * recuperar a sequência quando ela quebra.
 */
@Composable
fun DailyPassScreen(profile: PlayerProfile, onBack: () -> Unit) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? Activity
    var message by remember { mutableStateOf<String?>(null) }

    val today = DailyPass.todayEpochDay()
    val canClaim = DailyPass.canClaim(profile, today)
    val pendingIndex = DailyPass.pendingDayIndex(profile, today)
    val broken = DailyPass.streakBroken(profile, today)

    KardiaBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            ScreenHeader(title = "Passe Diário", onBack = onBack)

            GlassPanel(Modifier.fillMaxWidth(), borderColor = PulseGold.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("◷", color = PulseGold, fontSize = 34.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "${profile.streakDays} " +
                                if (profile.streakDays == 1) "dia seguido" else "dias seguidos",
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Caption(
                            if (canClaim) "A recompensa de hoje está esperando."
                            else "Você já resgatou hoje. Volte amanhã para continuar."
                        )
                    }
                }
            }

            if (message != null) {
                Spacer(Modifier.height(12.dp))
                GlassPanel(Modifier.fillMaxWidth(), borderColor = PulseCyan.copy(alpha = 0.5f)) {
                    Text(message!!, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Ciclo de 7 dias")
            Spacer(Modifier.height(10.dp))

            DailyPass.CYCLE.chunked(4).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { day ->
                        DayTile(
                            index = day.index,
                            shards = day.shards,
                            crystals = day.crystals,
                            label = day.label,
                            claimed = !canClaim && day.index == profile.passDayIndex,
                            pending = canClaim && day.index == pendingIndex,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(14.dp))
            PulseButton(
                text = if (canClaim) "RESGATAR RECOMPENSA DE HOJE" else "VOLTE AMANHÃ",
                subtitle = if (canClaim)
                    "dia ${pendingIndex + 1} do ciclo" else "a sequência continua se você aparecer",
                enabled = canClaim,
                onClick = {
                    scope.launch {
                        var claimedLabel = ""
                        services.repository.update { current ->
                            val (updated, day) = DailyPass.claim(current, today)
                            claimedLabel = day?.let {
                                "+${it.shards} Fragmentos" +
                                    (if (it.crystals > 0) ", +${it.crystals} Cristais" else "") +
                                    (if (it.power != null) ", ${it.power.ptName}" else "")
                            } ?: ""
                            updated
                        }
                        services.audio.play(Sfx.COIN)
                        services.haptics.perform(HapticPattern.REWARD)
                        message = claimedLabel.ifEmpty { "Recompensa resgatada." }
                    }
                }
            )

            if (broken) {
                Spacer(Modifier.height(16.dp))
                SectionTitle("Sequência quebrada")
                Spacer(Modifier.height(8.dp))
                GlassPanel(Modifier.fillMaxWidth(), borderColor = PulseViolet.copy(alpha = 0.5f)) {
                    Caption(
                        "Você ficou mais de um dia sem aparecer e a sequência de " +
                            "${profile.streakDays} dias vai reiniciar. Um anúncio premiado " +
                            "recupera ela — mas isso é totalmente opcional."
                    )
                    Spacer(Modifier.height(12.dp))
                    PulseButton(
                        text = if (services.ads.rewardedReady) "ASSISTIR E RECUPERAR"
                        else "ANÚNCIO INDISPONÍVEL",
                        enabled = services.ads.rewardedReady && activity != null,
                        primary = false,
                        onClick = {
                            activity?.let { act ->
                                services.ads.showRewarded(act) { earned ->
                                    if (earned) {
                                        scope.launch {
                                            services.repository.update {
                                                DailyPass.restoreStreak(it, today)
                                            }
                                            services.audio.play(Sfx.COIN)
                                            message = "Sequência recuperada. Resgate hoje para continuar."
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DayTile(
    index: Int,
    shards: Int,
    crystals: Int,
    label: String,
    claimed: Boolean,
    pending: Boolean,
    modifier: Modifier
) {
    val border = when {
        pending -> PulseGold
        claimed -> PulseCyan
        else -> SurfaceStroke
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (pending) PulseGold.copy(alpha = 0.12f) else SurfaceCard.copy(alpha = 0.8f))
            .border(BorderStroke(1.dp, border), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "D${index + 1}",
            color = if (pending) PulseGold else TextMuted,
            fontSize = 10.sp
        )
        Spacer(Modifier.height(4.dp))
        Text("◈$shards", color = TextPrimary, fontSize = 11.sp)
        if (crystals > 0) {
            Text("✧$crystals", color = PulseCyan, fontSize = 10.sp)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = TextMuted,
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (claimed) {
            Spacer(Modifier.height(3.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(PulseCyan)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text("✓", color = VoidBlack, fontSize = 9.sp)
            }
        }
    }
}
