package com.kardiapulse.game.ui.settings

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kardiapulse.game.BuildConfig
import com.kardiapulse.game.LocalServices
import com.kardiapulse.game.audio.MusicTrack
import com.kardiapulse.game.audio.Sfx
import com.kardiapulse.game.data.PlayerProfile
import com.kardiapulse.game.haptics.HapticPattern
import com.kardiapulse.game.notifications.TauntWorker
import com.kardiapulse.game.ui.common.Caption
import com.kardiapulse.game.ui.common.GlassPanel
import com.kardiapulse.game.ui.common.KardiaBackground
import com.kardiapulse.game.ui.common.PulseButton
import com.kardiapulse.game.ui.common.ScreenHeader
import com.kardiapulse.game.ui.common.SectionTitle
import com.kardiapulse.game.ui.theme.DangerRed
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.SurfaceCard
import com.kardiapulse.game.ui.theme.SurfaceStroke
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary
import com.kardiapulse.game.ui.theme.VoidBlack
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    profile: PlayerProfile,
    onBack: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onShowPrivacyOptions: () -> Unit,
    onProgressReset: () -> Unit
) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var confirmingReset by remember { mutableStateOf(false) }

    KardiaBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            ScreenHeader(title = "Ajustes", onBack = onBack)

            SectionTitle("Som e toque")
            Spacer(Modifier.height(10.dp))
            GlassPanel(Modifier.fillMaxWidth()) {
                ToggleRow(
                    title = "Efeitos sonoros",
                    subtitle = "Cartas, inversões, sobrecargas.",
                    checked = profile.soundOn
                ) { value ->
                    services.audio.soundEnabled = value
                    if (value) services.audio.play(Sfx.TAP)
                    scope.launch { services.repository.update { it.copy(soundOn = value) } }
                }
                Divider()
                ToggleRow(
                    title = "Música",
                    subtitle = "Trilha sintetizada, em loop contínuo.",
                    checked = profile.musicOn
                ) { value ->
                    services.audio.musicEnabled = value
                    if (value) services.audio.play(MusicTrack.MENU)
                    scope.launch { services.repository.update { it.copy(musicOn = value) } }
                }
                Divider()
                ToggleRow(
                    title = "Vibração",
                    subtitle = "Retorno tátil em cada jogada.",
                    checked = profile.hapticsOn
                ) { value ->
                    services.haptics.enabled = value
                    if (value) services.haptics.perform(HapticPattern.FLIP)
                    scope.launch { services.repository.update { it.copy(hapticsOn = value) } }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Notificações")
            Spacer(Modifier.height(10.dp))
            GlassPanel(Modifier.fillMaxWidth()) {
                ToggleRow(
                    title = "Lembretes provocadores",
                    subtitle = "No máximo um por dia, e só quando você não apareceu.",
                    checked = profile.notificationsOn
                ) { value ->
                    scope.launch { services.repository.update { it.copy(notificationsOn = value) } }
                    if (value) {
                        onRequestNotificationPermission()
                        TauntWorker.schedule(context)
                    } else {
                        TauntWorker.cancel(context)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Privacidade")
            Spacer(Modifier.height(10.dp))
            GlassPanel(Modifier.fillMaxWidth()) {
                Caption(
                    "Seu progresso fica somente neste aparelho, assinado com uma chave do Android " +
                        "Keystore. Nenhum dado de perfil é enviado para servidores."
                )
                Spacer(Modifier.height(12.dp))
                PulseButton(
                    text = "CONFIGURAÇÕES DE ANÚNCIOS",
                    subtitle = "revisar o consentimento",
                    primary = false,
                    onClick = onShowPrivacyOptions
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        PulseButton(text = "PRIVACIDADE", primary = false, onClick = onPrivacy)
                    }
                    Column(Modifier.weight(1f)) {
                        PulseButton(text = "TERMOS", primary = false, onClick = onTerms)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Dados")
            Spacer(Modifier.height(10.dp))
            GlassPanel(
                Modifier.fillMaxWidth(),
                borderColor = if (confirmingReset) DangerRed else SurfaceCard
            ) {
                Caption(
                    if (confirmingReset)
                        "Isso apaga nível, Fragmentos, Cristais, poderes, conquistas e a sequência " +
                            "do Passe Diário. Não tem como desfazer."
                    else
                        "Apaga todo o progresso guardado neste aparelho."
                )
                Spacer(Modifier.height(12.dp))
                PulseButton(
                    text = if (confirmingReset) "CONFIRMAR: APAGAR TUDO" else "APAGAR PROGRESSO",
                    primary = false,
                    onClick = {
                        if (!confirmingReset) {
                            confirmingReset = true
                        } else {
                            scope.launch {
                                services.repository.reset()
                                confirmingReset = false
                                onProgressReset()
                            }
                        }
                    }
                )
                if (confirmingReset) {
                    Spacer(Modifier.height(8.dp))
                    PulseButton(
                        text = "CANCELAR",
                        primary = false,
                        onClick = { confirmingReset = false }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "Kardia Pulse ${BuildConfig.VERSION_NAME}",
                color = TextMuted,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Caption(subtitle)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VoidBlack,
                checkedTrackColor = PulseCyan,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceCard
            )
        )
    }
}

/** Fio fino entre as opções de um mesmo painel. */
@Composable
private fun Divider() {
    Spacer(Modifier.height(6.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SurfaceStroke.copy(alpha = 0.6f))
    )
    Spacer(Modifier.height(6.dp))
}
