package com.chuvadeletras.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chuvadeletras.game.ads.AdState

/**
 * Overlay do anúncio recompensado.
 *
 * Com o `SimulatedRewardedAdHost` ele mostra a contagem regressiva de mentirinha.
 * Com o AdMob de verdade o SDK abre a própria tela por cima desta — o overlay
 * então só cobre o intervalo de carregamento.
 */
@Composable
fun AdOverlay(state: AdState, modifier: Modifier = Modifier) {
    if (state is AdState.Idle) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .blockTouches(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (state) {
                is AdState.Loading -> {
                    Text(state.placement.emoji, fontSize = 44.sp)
                    Text(
                        "Carregando anúncio…",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    CircularProgressIndicator()
                }

                is AdState.Playing -> {
                    Text(state.placement.emoji, fontSize = 44.sp)
                    Text(
                        state.placement.title,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Prêmio: ${state.placement.rewardLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "${state.secondsLeft}",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Anúncio de demonstração — troque pelo AdMob antes de publicar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                AdState.Idle -> Unit
            }
        }
    }
}
