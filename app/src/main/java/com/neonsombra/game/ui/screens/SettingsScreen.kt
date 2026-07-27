package com.neonsombra.game.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neonsombra.game.audio.Sfx
import com.neonsombra.game.ui.LocalPrefs
import com.neonsombra.game.ui.LocalSound
import com.neonsombra.game.ui.components.NeonBackground
import com.neonsombra.game.ui.components.NeonButton
import com.neonsombra.game.ui.components.NeonPanel
import com.neonsombra.game.ui.components.NeonText
import com.neonsombra.game.ui.theme.NeonCyan
import com.neonsombra.game.ui.theme.NeonLime
import com.neonsombra.game.ui.theme.NeonMagenta
import com.neonsombra.game.ui.theme.NeonPurple
import com.neonsombra.game.ui.theme.NeonTextMuted
import com.neonsombra.game.ui.theme.NeonTextPrimary
import com.neonsombra.game.ui.theme.NeonYellow

/** Musica, efeitos, vibracao e peca fantasma -- tudo pode ser desligado aqui. */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val prefs = LocalPrefs.current
    val sound = LocalSound.current
    var confirmingReset by remember { mutableStateOf(false) }

    NeonBackground(modifier = Modifier.fillMaxSize(), showSkyline = false, intensity = 0.8f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            NeonText(
                text = "CONFIGURACOES",
                color = NeonPurple,
                glowRadius = 20f,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(22.dp))

            NeonPanel(accent = NeonCyan, modifier = Modifier.fillMaxWidth()) {
                SettingRow(
                    title = "MUSICA",
                    description = "Trilha eletronica do jogo",
                    checked = prefs.musicEnabled,
                    accent = NeonCyan,
                ) { enabled ->
                    prefs.musicEnabled = enabled
                    sound.syncMusicWithPreferences()
                    if (enabled) sound.click()
                }
                Divider()
                SettingRow(
                    title = "EFEITOS",
                    description = "Sons de encaixe, giro e linha",
                    checked = prefs.soundEnabled,
                    accent = NeonLime,
                ) { enabled ->
                    prefs.soundEnabled = enabled
                    if (enabled) sound.play(Sfx.CLEAR)
                }
                Divider()
                SettingRow(
                    title = "VIBRACAO",
                    description = "Retorno tatil durante a partida",
                    checked = prefs.vibrationEnabled,
                    accent = NeonMagenta,
                ) { enabled ->
                    prefs.vibrationEnabled = enabled
                    if (enabled) sound.vibrate(35)
                }
                Divider()
                SettingRow(
                    title = "PECA FANTASMA",
                    description = "Marca onde a peca vai cair",
                    checked = prefs.ghostEnabled,
                    accent = NeonYellow,
                ) { enabled ->
                    prefs.ghostEnabled = enabled
                }
            }

            Spacer(Modifier.height(20.dp))

            NeonPanel(accent = NeonMagenta, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "RECORDE",
                    color = NeonTextMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (prefs.highScore > 0) {
                        "${prefs.highScore} pontos por ${prefs.highScoreOwner.ifBlank { "JOGADOR" }}"
                    } else {
                        "Nenhum recorde ainda."
                    },
                    color = NeonTextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                NeonButton(
                    text = if (confirmingReset) "CONFIRMAR APAGAR" else "APAGAR RECORDE",
                    accent = if (confirmingReset) NeonMagenta else NeonTextMuted,
                    enabled = prefs.highScore > 0,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (confirmingReset) {
                            prefs.highScore = 0
                            prefs.highScoreOwner = ""
                            confirmingReset = false
                        } else {
                            confirmingReset = true
                        }
                    },
                )
            }

            Spacer(Modifier.height(24.dp))

            NeonButton(
                text = "VOLTAR",
                onClick = onBack,
                accent = NeonCyan,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .height(1.dp)
            .background(NeonPurple.copy(alpha = 0.25f)),
    )
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (checked) NeonTextPrimary else NeonTextMuted,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = description,
                color = NeonTextMuted,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
            )
        }
        Spacer(Modifier.width(12.dp))
        NeonSwitch(checked = checked, accent = accent)
    }
}

/** Interruptor com cara de neon, no lugar do Switch padrao. */
@Composable
private fun NeonSwitch(checked: Boolean, accent: Color) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = tween(180),
        label = "knob",
    )
    val color = if (checked) accent else NeonTextMuted
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(26.dp)
            .background(color.copy(alpha = if (checked) 0.22f else 0.08f), RoundedCornerShape(13.dp))
            .border(1.5.dp, color.copy(alpha = 0.85f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(20.dp)
                .background(color, CircleShape),
        )
    }
}
