package com.formatfrute.game.ui.legal

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.annotation.StringRes
import com.formatfrute.game.R
import com.formatfrute.game.data.GameRepository
import com.formatfrute.game.ui.components.FruitBackground
import com.formatfrute.game.ui.components.PaperCard
import com.formatfrute.game.ui.theme.Fruta

enum class LegalTab(@StringRes val title: Int) {
    TERMOS(R.string.legal_tab_terms),
    PRIVACIDADE(R.string.legal_tab_privacy),
}

@Composable
fun LegalScreen(initialTab: LegalTab = LegalTab.TERMOS, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { GameRepository.get(context) }
    val profile by repo.profile.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(initialTab) }

    FruitBackground(profile.boardTheme, density = 5) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(3.dp, Fruta.Ink.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⬅", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.legal_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (profile.boardTheme.dark) Color.White else Fruta.Ink,
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LegalTab.entries.forEach { entry ->
                    val selected = entry == tab
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Fruta.Berry else Color.White.copy(alpha = 0.85f))
                            .border(
                                2.5.dp,
                                Fruta.Ink.copy(alpha = 0.6f),
                                RoundedCornerShape(50),
                            )
                            .clickable { tab = entry },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(entry.title),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else Fruta.Ink,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val sections = if (tab == LegalTab.TERMOS) LegalTexts.terms else LegalTexts.privacy
                items(sections.size) { index ->
                    val (titleRes, bodyRes) = sections[index]
                    PaperCard(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.96f)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                stringResource(titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                color = Fruta.Berry,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(bodyRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = Fruta.Ink,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(30.dp)) }
            }
        }
    }
}

/**
 * Índice dos documentos. Os textos vivem em `res/values/strings_legal.xml`;
 * aqui fica só a ordem das seções.
 */
object LegalTexts {

    const val EMAIL = "formatfrute@gmail.com"

    val terms: List<Pair<Int, Int>> = listOf(
        R.string.terms_1_t to R.string.terms_1_b,
        R.string.terms_2_t to R.string.terms_2_b,
        R.string.terms_3_t to R.string.terms_3_b,
        R.string.terms_4_t to R.string.terms_4_b,
        R.string.terms_5_t to R.string.terms_5_b,
        R.string.terms_6_t to R.string.terms_6_b,
        R.string.terms_7_t to R.string.terms_7_b,
        R.string.terms_8_t to R.string.terms_8_b,
        R.string.terms_9_t to R.string.terms_9_b,
        R.string.terms_10_t to R.string.terms_10_b,
    )

    val privacy: List<Pair<Int, Int>> = listOf(
        R.string.privacy_0_t to R.string.privacy_0_b,
        R.string.privacy_1_t to R.string.privacy_1_b,
        R.string.privacy_2_t to R.string.privacy_2_b,
        R.string.privacy_3_t to R.string.privacy_3_b,
        R.string.privacy_4_t to R.string.privacy_4_b,
        R.string.privacy_5_t to R.string.privacy_5_b,
        R.string.privacy_6_t to R.string.privacy_6_b,
        R.string.privacy_7_t to R.string.privacy_7_b,
        R.string.privacy_8_t to R.string.privacy_8_b,
        R.string.privacy_9_t to R.string.privacy_9_b,
    )
}
