package com.chuvadeletras.game.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chuvadeletras.game.ui.components.ChuvaCard
import com.chuvadeletras.game.ui.components.ScreenHeader

@Composable
fun LegalScreen(
    title: String,
    intro: String,
    sections: List<LegalSection>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        ScreenHeader(title = title, onBack = onBack)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "$intro\n\nÚltima atualização: ${LegalTexts.LAST_UPDATE}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(sections) { section ->
                ChuvaCard {
                    Text(section.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        section.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun TermsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    LegalScreen(
        title = "Termos de Uso",
        intro = "Estas são as regras do jogo fora do jogo. Leitura rápida, sem letra miúda escondida.",
        sections = LegalTexts.terms,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    LegalScreen(
        title = "Política de Privacidade",
        intro = "Resumindo: seu progresso fica no seu aparelho e a gente não pede nenhum dado seu.",
        sections = LegalTexts.privacy,
        onBack = onBack,
        modifier = modifier
    )
}
