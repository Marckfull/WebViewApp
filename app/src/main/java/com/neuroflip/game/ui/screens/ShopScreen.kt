package com.neuroflip.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflip.game.data.PlayerState
import com.neuroflip.game.data.PowerUp
import com.neuroflip.game.ui.components.AnimatedNeuroBackground
import com.neuroflip.game.ui.components.GlassPanel
import com.neuroflip.game.ui.components.NeonButton
import com.neuroflip.game.ui.components.NeonVariant
import com.neuroflip.game.ui.components.RewardedAdButton
import com.neuroflip.game.ui.components.StatChip
import com.neuroflip.game.ui.theme.LocalNeuro
import com.neuroflip.game.ui.theme.NeuroThemes

@Composable
fun ShopScreen(
    player: PlayerState,
    onBack: () -> Unit,
    onBuyPowerUp: (PowerUp) -> Unit,
    onWatchForNeurons: () -> Unit,
    onWatchForPowerUp: (PowerUp) -> Unit,
    onSelectTheme: (String) -> Unit,
    onBuyTheme: (String) -> Unit
) {
    val palette = LocalNeuro.current

    Box(Modifier.fillMaxSize()) {
        AnimatedNeuroBackground(Modifier.fillMaxSize(), intensity = 0.6f)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹",
                    color = palette.primary,
                    fontSize = 30.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp)
                )
                Text(
                    "LOJA NEURAL",
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.weight(1f))
                StatChip("🧠", "${player.neurons}")
            }

            Spacer(Modifier.height(18.dp))

            // -------------------------------------------------- ganhar assistindo
            GlassPanel(Modifier.fillMaxWidth(), borderColor = palette.gold) {
                SectionTitle("GANHE ASSISTINDO", palette.gold)
                Text(
                    "Vídeos opcionais. Assista até o fim e receba na hora.",
                    color = palette.textDim,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(12.dp))
                RewardedAdButton(
                    label = "Pacote de neurônios",
                    reward = "+60 🧠",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onWatchForNeurons
                )
                Spacer(Modifier.height(8.dp))
                RewardedAdButton(
                    label = "Scan grátis",
                    reward = "+1 🔍",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onWatchForPowerUp(PowerUp.SCAN) }
                )
                Spacer(Modifier.height(8.dp))
                RewardedAdButton(
                    label = "Curinga grátis",
                    reward = "+1 ⧉",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onWatchForPowerUp(PowerUp.WILD) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // -------------------------------------------------- itens
            GlassPanel(Modifier.fillMaxWidth()) {
                SectionTitle("ITENS", palette.primary)
                Spacer(Modifier.height(10.dp))
                PowerUp.entries.forEach { kind ->
                    val owned = when (kind) {
                        PowerUp.SCAN -> player.scanCount
                        PowerUp.WILD -> player.wildCount
                        PowerUp.TIME -> player.timeCount
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(kind.glyph, fontSize = 18.sp)
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                "${kind.label}  ×$owned",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(kind.description, color = palette.textDim, fontSize = 11.sp)
                        }
                        NeonButton(
                            text = "${kind.price} 🧠",
                            variant = if (player.neurons >= kind.price) {
                                NeonVariant.PRIMARY
                            } else {
                                NeonVariant.GHOST
                            },
                            onClick = { onBuyPowerUp(kind) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // -------------------------------------------------- temas
            GlassPanel(Modifier.fillMaxWidth()) {
                SectionTitle("TEMAS", palette.secondary)
                Spacer(Modifier.height(10.dp))
                NeuroThemes.ALL.forEach { theme ->
                    val unlocked = player.unlockedThemes.contains(theme.id)
                    val selected = player.themeId == theme.id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.dp,
                                if (selected) theme.primary else palette.textDim.copy(alpha = 0.25f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                if (unlocked) onSelectTheme(theme.id) else onBuyTheme(theme.id)
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(theme.void1, theme.primary, theme.secondary)
                                    )
                                )
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                theme.name,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                when {
                                    selected -> "em uso"
                                    unlocked -> "toque para usar"
                                    else -> "${theme.price} neurônios"
                                },
                                color = if (selected) theme.primary else palette.textDim,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = when {
                                selected -> "✓"
                                unlocked -> "•"
                                else -> "🔒"
                            },
                            color = if (selected) theme.primary else palette.textDim,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Neurônios são itens virtuais, não têm valor em dinheiro e não podem " +
                    "ser trocados fora do jogo (veja os Termos de Uso).",
                color = palette.textDim.copy(alpha = 0.75f),
                fontSize = 10.sp
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Bold
    )
}
