package com.neuroflip.game.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflip.game.data.PlayerState
import com.neuroflip.game.domain.CardKind
import com.neuroflip.game.domain.LevelCatalog
import com.neuroflip.game.ui.DailyReward
import com.neuroflip.game.ui.components.AnimatedNeuroBackground
import com.neuroflip.game.ui.components.GlassPanel
import com.neuroflip.game.ui.components.NeonBar
import com.neuroflip.game.ui.components.NeonButton
import com.neuroflip.game.ui.components.NeonVariant
import com.neuroflip.game.ui.components.RewardedAdButton
import com.neuroflip.game.ui.components.StatChip
import com.neuroflip.game.ui.theme.LocalNeuro

@Composable
fun HomeScreen(
    player: PlayerState,
    daily: DailyReward,
    onCampaign: () -> Unit,
    onBlitz: () -> Unit,
    onZen: () -> Unit,
    onShop: () -> Unit,
    onSettings: () -> Unit,
    onLegal: (String) -> Unit,
    onClaimDaily: () -> Unit,
    onClaimDailyWithAd: () -> Unit
) {
    val palette = LocalNeuro.current
    var showHowTo by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AnimatedNeuroBackground(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatChip("🧠", "${player.neurons}")
                StatChip("★", "${player.totalStars}", tint = palette.gold)
                if (player.dailyStreak > 0) {
                    StatChip("🔥", "${player.dailyStreak}d", tint = palette.secondary)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "⚙",
                    fontSize = 22.sp,
                    color = palette.textDim,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSettings() }
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(34.dp))

            Text(
                "NEUROFLIP",
                color = palette.primary,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Text(
                "MEMÓRIA RECODIFICADA",
                color = palette.textDim,
                fontSize = 11.sp,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(26.dp))

            if (daily.available) {
                GlassPanel(Modifier.fillMaxWidth(), borderColor = palette.gold) {
                    Text(
                        "BÔNUS DIÁRIO",
                        color = palette.gold,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Sequência de ${daily.streak} dia(s) — receba ${daily.amount} neurônios.",
                        color = palette.textPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonButton(
                            text = "RESGATAR",
                            glyph = "🎁",
                            modifier = Modifier.weight(1f),
                            onClick = onClaimDaily
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    RewardedAdButton(
                        label = "Dobrar bônus",
                        reward = "+${daily.amount * 2} neurônios",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onClaimDailyWithAd
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            // Campanha
            GlassPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "CAMPANHA SINAPSE",
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${player.unlockedLevel}/${LevelCatalog.TOTAL_LEVELS}",
                        color = palette.primary,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                NeonBar(
                    progress = player.unlockedLevel.toFloat() / LevelCatalog.TOTAL_LEVELS,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${player.totalStars} de ${LevelCatalog.TOTAL_LEVELS * 3} estrelas",
                    color = palette.textDim,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))
                NeonButton(
                    text = "JOGAR",
                    glyph = "▶",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCampaign
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NeonButton(
                    text = "BLITZ 90",
                    glyph = "⚡",
                    subtitle = if (player.blitzBest > 0) "recorde ${player.blitzBest}" else "90 segundos",
                    variant = NeonVariant.GOLD,
                    modifier = Modifier.weight(1f),
                    onClick = onBlitz
                )
                NeonButton(
                    text = "ZEN",
                    glyph = "🌙",
                    subtitle = "sem tempo",
                    variant = NeonVariant.GHOST,
                    modifier = Modifier.weight(1f),
                    onClick = onZen
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NeonButton(
                    text = "LOJA",
                    glyph = "🛒",
                    modifier = Modifier.weight(1f),
                    onClick = onShop
                )
                NeonButton(
                    text = "COMO JOGAR",
                    glyph = "❔",
                    variant = NeonVariant.GHOST,
                    modifier = Modifier.weight(1f),
                    onClick = { showHowTo = true }
                )
            }

            Spacer(Modifier.height(30.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LegalLink("Termos de Uso") { onLegal("terms") }
                Text("  ·  ", color = palette.textDim, fontSize = 11.sp)
                LegalLink("Política de Privacidade") { onLegal("privacy") }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "NeuroFlip v1.0 — jogue com moderação",
                color = palette.textDim.copy(alpha = 0.6f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showHowTo) {
        HowToPlayDialog(onDismiss = { showHowTo = false })
    }
}

@Composable
private fun LegalLink(text: String, onClick: () -> Unit) {
    val palette = LocalNeuro.current
    Text(
        text = text,
        color = palette.primary.copy(alpha = 0.8f),
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
fun HowToPlayDialog(onDismiss: () -> Unit) {
    val palette = LocalNeuro.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.primary,
        textContentColor = palette.textPrimary,
        title = { Text("O que muda no NeuroFlip", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Rule("◎ ECO", "Ao acertar um par, um pulso revela por um instante as cartas vizinhas. Quem presta atenção no pulso ganha bônus.")
                Rule("⇄ MUTAÇÃO", "De tempos em tempos duas cartas trocam de lugar na sua frente. Dá para seguir o rastro — memória em movimento.")
                Rule("⚡ SINAPSE", "Acertos em sequência carregam a barra. Cheia, ative a SOBRECARGA: revela tudo por 1,5s e dobra os pontos por 10s.")
                Spacer(Modifier.height(10.dp))
                Text(
                    "CARTAS ESPECIAIS",
                    color = palette.secondary,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                CardKind.entries.filter { it != CardKind.NORMAL }.forEach {
                    Rule("${it.glyph} ${it.label.uppercase()}", it.description)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ENTENDI", color = palette.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun Rule(title: String, body: String) {
    val palette = LocalNeuro.current
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(title, color = palette.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(body, color = palette.textDim, fontSize = 12.sp)
    }
}
