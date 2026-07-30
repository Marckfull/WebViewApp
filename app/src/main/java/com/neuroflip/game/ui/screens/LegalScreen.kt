package com.neuroflip.game.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflip.game.ui.components.AnimatedNeuroBackground
import com.neuroflip.game.ui.components.GlassPanel
import com.neuroflip.game.ui.components.NeonButton
import com.neuroflip.game.ui.components.NeonVariant
import com.neuroflip.game.ui.theme.LocalNeuro

/** Leitura dos Termos de Uso e da Política de Privacidade. */
@Composable
fun LegalScreen(document: String, onBack: () -> Unit) {
    val palette = LocalNeuro.current
    val isTerms = document == "terms"
    val title = if (isTerms) "TERMOS DE USO" else "POLÍTICA DE PRIVACIDADE"
    val body = if (isTerms) LegalTexts.TERMS else LegalTexts.PRIVACY

    Box(Modifier.fillMaxSize()) {
        AnimatedNeuroBackground(Modifier.fillMaxSize(), intensity = 0.5f)

        Column(
            Modifier
                .fillMaxSize()
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
                    title,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.2.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    Text(
                        text = body,
                        color = palette.textPrimary.copy(alpha = 0.92f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

/**
 * Portão de primeira execução: o jogador precisa aceitar os documentos antes
 * de jogar. Fica registrado no armazenamento local.
 */
@Composable
fun ConsentGateScreen(onAccept: () -> Unit) {
    val palette = LocalNeuro.current
    var reading by remember { mutableStateOf<String?>(null) }

    if (reading != null) {
        LegalScreen(document = reading!!, onBack = { reading = null })
        return
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedNeuroBackground(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(50.dp))
            Text(
                "NEUROFLIP",
                color = palette.primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Text(
                "MEMÓRIA RECODIFICADA",
                color = palette.textDim,
                fontSize = 10.sp,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(34.dp))

            GlassPanel(Modifier.fillMaxWidth()) {
                Text(
                    "ANTES DE COMEÇAR",
                    color = palette.primary,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "O NeuroFlip é gratuito e se mantém com anúncios. Os vídeos " +
                        "premiados são sempre opcionais e sempre entregam algo em troca.\n\n" +
                        "Não pedimos cadastro e seu progresso fica salvo apenas neste aparelho. " +
                        "Onde a lei exige, você verá um formulário de consentimento de anúncios " +
                        "e poderá revê-lo quando quiser nas Configurações.",
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    NeonButton(
                        text = "TERMOS",
                        modifier = Modifier.weight(1f),
                        variant = NeonVariant.GHOST,
                        onClick = { reading = "terms" }
                    )
                    Spacer(Modifier.width(10.dp))
                    NeonButton(
                        text = "PRIVACIDADE",
                        modifier = Modifier.weight(1f),
                        variant = NeonVariant.GHOST,
                        onClick = { reading = "privacy" }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            NeonButton(
                text = "LI E ACEITO",
                glyph = "✓",
                modifier = Modifier.fillMaxWidth(),
                onClick = onAccept
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Ao tocar em “Li e aceito” você concorda com os Termos de Uso e " +
                    "com a Política de Privacidade do NeuroFlip.",
                color = palette.textDim,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}
