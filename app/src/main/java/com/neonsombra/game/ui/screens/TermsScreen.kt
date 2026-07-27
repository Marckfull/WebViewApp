package com.neonsombra.game.ui.screens

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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neonsombra.game.ui.LocalSound
import com.neonsombra.game.ui.components.NeonBackground
import com.neonsombra.game.ui.components.NeonButton
import com.neonsombra.game.ui.components.NeonPanel
import com.neonsombra.game.ui.components.NeonText
import com.neonsombra.game.ui.theme.NeonCyan
import com.neonsombra.game.ui.theme.NeonLime
import com.neonsombra.game.ui.theme.NeonMagenta
import com.neonsombra.game.ui.theme.NeonTextMuted
import com.neonsombra.game.ui.theme.NeonTextPrimary

/**
 * Termos de uso e politica de privacidade.
 *
 * Na primeira abertura a tela exige o aceite ([requireAcceptance]); quando
 * chamada pelo menu, e apenas leitura.
 */
@Composable
fun TermsScreen(
    requireAcceptance: Boolean,
    onAccept: () -> Unit,
    onBack: () -> Unit,
) {
    val sound = LocalSound.current
    var accepted by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    NeonBackground(modifier = Modifier.fillMaxSize(), showSkyline = false, intensity = 0.7f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp),
        ) {
            NeonText(
                text = "TERMOS E PRIVACIDADE",
                color = NeonCyan,
                glowRadius = 20f,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Versao ${LegalTexts.VERSION} - ${LegalTexts.LAST_UPDATE}",
                color = NeonTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                style = MaterialTheme.typography.labelSmall,
            )

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TabChip(
                    text = "TERMOS DE USO",
                    selected = !showPrivacy,
                    modifier = Modifier.weight(1f),
                ) {
                    sound.click()
                    showPrivacy = false
                }
                TabChip(
                    text = "PRIVACIDADE",
                    selected = showPrivacy,
                    modifier = Modifier.weight(1f),
                ) {
                    sound.click()
                    showPrivacy = true
                }
            }

            Spacer(Modifier.height(14.dp))

            NeonPanel(
                accent = if (showPrivacy) NeonMagenta else NeonCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = 16.dp,
            ) {
                Text(
                    text = if (showPrivacy) LegalTexts.PRIVACY else LegalTexts.TERMS,
                    color = NeonTextPrimary.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.verticalScroll(scroll),
                )
            }

            Spacer(Modifier.height(16.dp))

            if (requireAcceptance) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            sound.click()
                            accepted = !accepted
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeonCheckbox(checked = accepted)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Li e aceito os Termos de Uso e a Politica de Privacidade.",
                        color = NeonTextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(Modifier.height(16.dp))

                NeonButton(
                    text = "ACEITAR E JOGAR",
                    onClick = onAccept,
                    accent = NeonLime,
                    enabled = accepted,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                NeonButton(
                    text = "VOLTAR",
                    onClick = onBack,
                    accent = NeonCyan,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TabChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = if (selected) NeonCyan else NeonTextMuted
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .background(accent.copy(alpha = if (selected) 0.18f else 0.05f), shape)
            .border(1.dp, accent.copy(alpha = if (selected) 0.8f else 0.3f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) NeonTextPrimary else NeonTextMuted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun NeonCheckbox(checked: Boolean) {
    val accent = if (checked) NeonLime else NeonTextMuted
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(accent.copy(alpha = if (checked) 0.25f else 0.06f), RoundedCornerShape(7.dp))
            .border(1.5.dp, accent, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            NeonText(
                text = "X",
                color = NeonLime,
                glowRadius = 12f,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
