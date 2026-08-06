package com.kardiapulse.game.ui.legal

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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.kardiapulse.game.ui.common.Caption
import com.kardiapulse.game.ui.common.GlassPanel
import com.kardiapulse.game.ui.common.KardiaBackground
import com.kardiapulse.game.ui.common.PulseButton
import com.kardiapulse.game.ui.common.ScreenHeader
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary
import com.kardiapulse.game.ui.theme.TextSecondary

/** Tela de leitura de um documento legal. */
@Composable
fun LegalDocumentScreen(
    title: String,
    sections: List<LegalSection>,
    onBack: () -> Unit
) {
    KardiaBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 18.dp)
        ) {
            ScreenHeader(title = title, onBack = onBack)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sections) { section ->
                    GlassPanel {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = PulseCyan
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = section.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

/**
 * O portão de primeira execução: sem aceitar os Termos e a Política, o jogo não abre.
 *
 * O aceite guarda a versão do documento, então uma atualização relevante do texto faz a tela
 * aparecer de novo — que é exatamente o comportamento que a Play Store espera.
 */
@Composable
fun LegalGateScreen(onAccept: () -> Unit) {
    var reading by remember { mutableStateOf<String?>(null) }

    when (reading) {
        "privacidade" -> {
            LegalDocumentScreen(
                title = "Política de Privacidade",
                sections = LegalContent.PRIVACY,
                onBack = { reading = null }
            )
            return
        }
        "termos" -> {
            LegalDocumentScreen(
                title = "Termos de Uso",
                sections = LegalContent.TERMS,
                onBack = { reading = null }
            )
            return
        }
    }

    KardiaBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Text(
                text = "KARDIA PULSE",
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(10.dp))
            Caption(
                text = "Antes de acender o Núcleo, dê uma olhada em como o jogo trata os seus dados.",
                align = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(22.dp))

            GlassPanel(Modifier.fillMaxWidth()) {
                Bullet("Seu progresso fica só no seu aparelho. Não existe conta nem servidor.")
                Bullet("O jogo exibe anúncios do Google AdMob para se manter gratuito.")
                Bullet("Anúncios premiados são sempre opcionais e nunca bloqueiam um modo de jogo.")
                Bullet("Você pode apagar tudo a qualquer momento em Ajustes.")
            }

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    PulseButton(
                        text = "PRIVACIDADE",
                        onClick = { reading = "privacidade" },
                        primary = false
                    )
                }
                Box(Modifier.weight(1f)) {
                    PulseButton(
                        text = "TERMOS",
                        onClick = { reading = "termos" },
                        primary = false
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            PulseButton(
                text = "ACEITAR E JOGAR",
                subtitle = "você confirma que leu e concorda com os dois documentos",
                onClick = onAccept
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(vertical = 5.dp)) {
        Text("•", color = PulseCyan, modifier = Modifier.padding(end = 8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

/** Rodapé reutilizável com os links legais. */
@Composable
fun LegalFooter(onPrivacy: () -> Unit, onTerms: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Política de Privacidade",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            modifier = Modifier
                .padding(8.dp)
                .clickableText(onPrivacy)
        )
        Text("·", color = TextMuted, modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = "Termos de Uso",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            modifier = Modifier
                .padding(8.dp)
                .clickableText(onTerms)
        )
    }
}

private fun Modifier.clickableText(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
