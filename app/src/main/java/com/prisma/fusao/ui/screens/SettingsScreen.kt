package com.prisma.fusao.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prisma.fusao.BuildConfig
import com.prisma.fusao.PrismaApplication
import com.prisma.fusao.data.PlayerState
import com.prisma.fusao.ui.components.InfoRow
import com.prisma.fusao.ui.components.PrismaButton
import com.prisma.fusao.ui.components.PrismaCard
import com.prisma.fusao.ui.components.StarfieldBackground
import com.prisma.fusao.ui.components.ToggleRow
import com.prisma.fusao.ui.components.VSpace
import com.prisma.fusao.ui.theme.BrancoGelo
import com.prisma.fusao.ui.theme.LilasClaro
import com.prisma.fusao.ui.theme.VermelhoAlerta
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    activity: Activity,
    onBack: () -> Unit,
    onOpenDocument: (LegalDocument) -> Unit,
    onDataErased: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as PrismaApplication
    val player by app.repository.state.collectAsStateWithLifecycle(initialValue = PlayerState())
    val privacyRequired by app.adsManager.privacyOptionsRequired.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmErase by remember { mutableStateOf(false) }

    StarfieldBackground {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Text("Ajustes", style = MaterialTheme.typography.headlineMedium, color = BrancoGelo)
            VSpace(14)

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                PrismaCard(Modifier.fillMaxWidth()) {
                    Text("Som", style = MaterialTheme.typography.titleLarge, color = BrancoGelo)
                    VSpace(6)
                    ToggleRow("Música", player.musicEnabled) { enabled ->
                        scope.launch {
                            app.repository.setMusicEnabled(enabled)
                            app.soundEngine.musicEnabled = enabled
                        }
                    }
                    ToggleRow("Efeitos sonoros", player.sfxEnabled) { enabled ->
                        scope.launch {
                            app.repository.setSfxEnabled(enabled)
                            app.soundEngine.sfxEnabled = enabled
                        }
                    }
                    ToggleRow("Vibração", player.vibrationEnabled) { enabled ->
                        scope.launch { app.repository.setVibrationEnabled(enabled) }
                    }
                }

                VSpace(14)
                PrismaCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Privacidade e anúncios",
                        style = MaterialTheme.typography.titleLarge,
                        color = BrancoGelo,
                    )
                    VSpace(6)
                    Text(
                        "Você pode rever a qualquer momento como seus dados são usados para " +
                            "anúncios.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LilasClaro,
                    )
                    VSpace(12)
                    if (privacyRequired) {
                        PrismaButton(
                            text = "Opções de privacidade",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { app.adsManager.showPrivacyOptions(activity) },
                        )
                        VSpace(8)
                    }
                    LinkRow("Termos de Uso") { onOpenDocument(LegalDocument.TERMS) }
                    LinkRow("Política de Privacidade") { onOpenDocument(LegalDocument.PRIVACY) }
                }

                VSpace(14)
                PrismaCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Seus dados",
                        style = MaterialTheme.typography.titleLarge,
                        color = BrancoGelo,
                    )
                    VSpace(6)
                    Text(
                        "Todo o seu progresso fica apenas neste aparelho. Apagar é imediato e " +
                            "não tem como desfazer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LilasClaro,
                    )
                    VSpace(12)
                    PrismaButton(
                        text = "Apagar meus dados",
                        modifier = Modifier.fillMaxWidth(),
                        colors = listOf(Color(0xFFB91C1C), Color(0xFF7F1D1D)),
                        onClick = { confirmErase = true },
                    )
                }

                VSpace(14)
                PrismaCard(Modifier.fillMaxWidth()) {
                    Text("Sobre", style = MaterialTheme.typography.titleLarge, color = BrancoGelo)
                    VSpace(6)
                    InfoRow("Versão", BuildConfig.VERSION_NAME)
                    InfoRow("Fases concluídas", "${player.levels.count { it.value.stars > 0 }}/150")
                    InfoRow("Estrelas", "${player.totalStars}/450")
                    InfoRow("Fusões realizadas", "${player.totalFusions}")
                    InfoRow("Gemas estouradas", "%,d".format(player.totalGemsCleared))
                }
                VSpace(20)
            }

            PrismaButton("Voltar", Modifier.fillMaxWidth(), onClick = onBack)
        }
    }

    if (confirmErase) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.padding(26.dp)) {
                PrismaCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Apagar tudo?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = VermelhoAlerta,
                    )
                    VSpace(10)
                    Text(
                        "Isso remove progresso, estrelas, moedas e itens deste aparelho. " +
                            "Não é possível recuperar depois.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LilasClaro,
                    )
                    VSpace(18)
                    PrismaButton(
                        text = "Sim, apagar tudo",
                        modifier = Modifier.fillMaxWidth(),
                        colors = listOf(Color(0xFFB91C1C), Color(0xFF7F1D1D)),
                        onClick = {
                            scope.launch {
                                app.repository.eraseAllData()
                                confirmErase = false
                                onDataErased()
                            }
                        },
                    )
                    VSpace(10)
                    PrismaButton(
                        text = "Cancelar",
                        modifier = Modifier.fillMaxWidth(),
                        colors = listOf(Color(0xFF4C4670), Color(0xFF322C4D)),
                        onClick = { confirmErase = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = BrancoGelo)
        Text("›", style = MaterialTheme.typography.titleLarge, color = LilasClaro)
    }
}
