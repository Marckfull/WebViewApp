package com.neuroflip.game.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflip.game.data.PlayerState
import com.neuroflip.game.ui.components.AnimatedNeuroBackground
import com.neuroflip.game.ui.components.GlassPanel
import com.neuroflip.game.ui.components.NeonButton
import com.neuroflip.game.ui.components.NeonVariant
import com.neuroflip.game.ui.theme.LocalNeuro

@Composable
fun SettingsScreen(
    player: PlayerState,
    privacyOptionsRequired: Boolean,
    onBack: () -> Unit,
    onMusic: (Boolean) -> Unit,
    onSfx: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onPrivacyOptions: () -> Unit,
    onLegal: (String) -> Unit
) {
    val palette = LocalNeuro.current

    Box(Modifier.fillMaxSize()) {
        AnimatedNeuroBackground(Modifier.fillMaxSize(), intensity = 0.5f)

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
                    "CONFIGURAÇÕES",
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(Modifier.height(18.dp))

            GlassPanel(Modifier.fillMaxWidth()) {
                ToggleRow("🎵", "Música", "Trilha sonora original gerada pelo app", player.musicEnabled, onMusic)
                ToggleRow("🔊", "Efeitos sonoros", "Sons de carta, combo e sobrecarga", player.sfxEnabled, onSfx)
                ToggleRow("📳", "Vibração", "Retorno tátil em acertos, erros e combos", player.hapticsEnabled, onHaptics)
            }

            Spacer(Modifier.height(16.dp))

            GlassPanel(Modifier.fillMaxWidth()) {
                Text(
                    "PRIVACIDADE E ANÚNCIOS",
                    color = palette.primary,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "O jogo é gratuito e mantido por anúncios. Vídeos premiados são " +
                        "sempre opcionais.",
                    color = palette.textDim,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                if (privacyOptionsRequired) {
                    NeonButton(
                        text = "OPÇÕES DE PRIVACIDADE",
                        glyph = "🛡",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onPrivacyOptions
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Row(Modifier.fillMaxWidth()) {
                    NeonButton(
                        text = "TERMOS",
                        modifier = Modifier.weight(1f),
                        variant = NeonVariant.GHOST,
                        onClick = { onLegal("terms") }
                    )
                    Spacer(Modifier.width(10.dp))
                    NeonButton(
                        text = "PRIVACIDADE",
                        modifier = Modifier.weight(1f),
                        variant = NeonVariant.GHOST,
                        onClick = { onLegal("privacy") }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            GlassPanel(Modifier.fillMaxWidth()) {
                Text(
                    "SOBRE",
                    color = palette.primary,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                InfoRow("Versão", "1.0.0")
                InfoRow("Níveis concluídos", "${player.starsByLevel.size}")
                InfoRow("Estrelas", "${player.totalStars}")
                InfoRow("Recorde Blitz", "${player.blitzBest}")
                InfoRow("Contato", LegalTexts.CONTACT_EMAIL)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Trilha e efeitos sonoros são sintetizados pelo próprio app — " +
                        "nenhum áudio de terceiros é distribuído com o jogo.",
                    color = palette.textDim.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    glyph: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    val palette = LocalNeuro.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(glyph, fontSize = 20.sp)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(title, color = palette.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = palette.textDim, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.void0,
                checkedTrackColor = palette.primary,
                uncheckedThumbColor = palette.textDim,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val palette = LocalNeuro.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(label, color = palette.textDim, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = palette.textPrimary, fontSize = 12.sp)
    }
}
