package com.prisma.fusao.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberInfiniteTransition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prisma.fusao.ads.RewardedOffer
import com.prisma.fusao.data.Booster
import com.prisma.fusao.data.DailyReward
import com.prisma.fusao.ui.theme.BrancoGelo
import com.prisma.fusao.ui.theme.DouradoEstrela
import com.prisma.fusao.ui.theme.LilasClaro
import com.prisma.fusao.ui.theme.VerdeConfirma

/** Fundo escurecido padrão dos diálogos. */
@Composable
private fun DialogScrim(onDismiss: (() -> Unit)?, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .then(
                if (onDismiss != null) Modifier.clickable(onClick = onDismiss) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.padding(24.dp)) { content() }
    }
}

/**
 * Convite para assistir a um vídeo premiado.
 *
 * O formato segue as políticas do AdMob: o prêmio é descrito antes, a decisão é do
 * jogador, e o botão de recusar tem o mesmo destaque do de aceitar — nada de
 * "não, obrigado" escondido em cinza minúsculo.
 */
@Composable
fun RewardedOfferDialog(
    offer: RewardedOffer,
    available: Boolean,
    unavailableMessage: String? = null,
    onWatch: () -> Unit,
    onDecline: () -> Unit,
) {
    DialogScrim(onDismiss = onDecline) {
        PrismaCard(Modifier.fillMaxWidth()) {
            Text(offer.title, style = MaterialTheme.typography.headlineMedium, color = BrancoGelo)
            VSpace(10)
            Text(
                offer.description,
                style = MaterialTheme.typography.bodyMedium,
                color = LilasClaro,
            )
            VSpace(6)
            Text(
                "Você verá um anúncio em vídeo. Pode fechar quando quiser, mas o prêmio " +
                    "só é liberado se o vídeo chegar ao fim.",
                style = MaterialTheme.typography.bodySmall,
                color = LilasClaro.copy(alpha = 0.75f),
            )
            if (!available && unavailableMessage != null) {
                VSpace(10)
                Text(
                    unavailableMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = DouradoEstrela,
                )
            }
            VSpace(18)
            PrismaButton(
                text = offer.buttonLabel,
                modifier = Modifier.fillMaxWidth(),
                enabled = available,
                pulsing = available,
                colors = listOf(Color(0xFF22C55E), Color(0xFF15803D)),
                onClick = onWatch,
            )
            VSpace(10)
            // Mesmo tamanho e mesmo peso visual do botão de aceitar.
            PrismaButton(
                text = "Agora não",
                modifier = Modifier.fillMaxWidth(),
                colors = listOf(Color(0xFF4C4670), Color(0xFF322C4D)),
                onClick = onDecline,
            )
        }
    }
}

/** Tela de vitória, com estrelas animadas e as opções de prêmio. */
@Composable
fun LevelWonDialog(
    level: Int,
    score: Int,
    stars: Int,
    coins: Int,
    canDoubleCoins: Boolean,
    onDoubleCoins: () -> Unit,
    onNext: () -> Unit,
    onMap: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "vitoria")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(tween(820), RepeatMode.Reverse),
        label = "estrelas",
    )

    DialogScrim(onDismiss = null) {
        PrismaCard(Modifier.fillMaxWidth()) {
            Text(
                "Fase $level concluída!",
                style = MaterialTheme.typography.headlineMedium,
                color = BrancoGelo,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            VSpace(16)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StarRow(stars, size = 46.dp, modifier = Modifier.scale(pulse))
            }
            VSpace(16)
            InfoRow("Pontuação", "%,d".format(score))
            InfoRow("Moedas ganhas", "+$coins")
            VSpace(18)
            if (canDoubleCoins) {
                PrismaButton(
                    text = "Ver vídeo e dobrar as moedas",
                    modifier = Modifier.fillMaxWidth(),
                    colors = listOf(Color(0xFF22C55E), Color(0xFF15803D)),
                    pulsing = true,
                    onClick = onDoubleCoins,
                )
                VSpace(10)
            }
            PrismaButton("Próxima fase", Modifier.fillMaxWidth(), onClick = onNext)
            VSpace(8)
            PrismaTextButton("Voltar ao mapa", Modifier.fillMaxWidth(), onClick = onMap)
        }
    }
}

/** Tela de derrota. A oferta de vídeo aparece, mas nunca bloqueia a saída. */
@Composable
fun LevelLostDialog(
    objectivesMissing: List<String>,
    extraMovesBoosters: Int,
    onWatchForMoves: () -> Unit,
    onUseBooster: () -> Unit,
    onRetry: () -> Unit,
    onMap: () -> Unit,
) {
    DialogScrim(onDismiss = null) {
        PrismaCard(Modifier.fillMaxWidth()) {
            Text(
                "Acabaram as jogadas",
                style = MaterialTheme.typography.headlineMedium,
                color = BrancoGelo,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            VSpace(12)
            if (objectivesMissing.isNotEmpty()) {
                Text("Faltou:", style = MaterialTheme.typography.titleMedium, color = LilasClaro)
                VSpace(4)
                objectivesMissing.forEach {
                    Text("•  $it", style = MaterialTheme.typography.bodyMedium, color = LilasClaro)
                }
                VSpace(14)
            }
            PrismaButton(
                text = "Ver vídeo e ganhar 5 jogadas",
                modifier = Modifier.fillMaxWidth(),
                colors = listOf(Color(0xFF22C55E), Color(0xFF15803D)),
                pulsing = true,
                onClick = onWatchForMoves,
            )
            if (extraMovesBoosters > 0) {
                VSpace(10)
                PrismaButton(
                    text = "Usar item: +5 jogadas ($extraMovesBoosters)",
                    modifier = Modifier.fillMaxWidth(),
                    colors = listOf(Color(0xFF8B5CF6), Color(0xFF5B21B6)),
                    onClick = onUseBooster,
                )
            }
            VSpace(10)
            PrismaButton(
                text = "Tentar de novo",
                modifier = Modifier.fillMaxWidth(),
                colors = listOf(Color(0xFF4C4670), Color(0xFF322C4D)),
                onClick = onRetry,
            )
            VSpace(8)
            PrismaTextButton("Voltar ao mapa", Modifier.fillMaxWidth(), onClick = onMap)
        }
    }
}

/** Sem vidas: espera, vídeo premiado ou voltar. */
@Composable
fun OutOfLivesDialog(
    timeToNextLife: String,
    onWatchForLife: () -> Unit,
    onDismiss: () -> Unit,
) {
    DialogScrim(onDismiss = onDismiss) {
        PrismaCard(Modifier.fillMaxWidth()) {
            Text("Sem vidas", style = MaterialTheme.typography.headlineMedium, color = BrancoGelo)
            VSpace(10)
            Text(
                "A próxima vida chega em $timeToNextLife. Você também pode assistir a um " +
                    "vídeo curto e continuar agora.",
                style = MaterialTheme.typography.bodyMedium,
                color = LilasClaro,
            )
            VSpace(18)
            PrismaButton(
                text = "Ver vídeo e ganhar 1 vida",
                modifier = Modifier.fillMaxWidth(),
                colors = listOf(Color(0xFF22C55E), Color(0xFF15803D)),
                pulsing = true,
                onClick = onWatchForLife,
            )
            VSpace(10)
            PrismaButton(
                text = "Esperar",
                modifier = Modifier.fillMaxWidth(),
                colors = listOf(Color(0xFF4C4670), Color(0xFF322C4D)),
                onClick = onDismiss,
            )
        }
    }
}

/** Prêmio diário, com o ciclo de sete dias visível. */
@Composable
fun DailyRewardDialog(
    streak: Int,
    reward: DailyReward,
    canDouble: Boolean,
    onDouble: () -> Unit,
    onClose: () -> Unit,
) {
    DialogScrim(onDismiss = null) {
        PrismaCard(Modifier.fillMaxWidth()) {
            Text("Prêmio diário", style = MaterialTheme.typography.headlineMedium, color = BrancoGelo)
            VSpace(4)
            Text(
                "Dia $streak de 7 seguidos",
                style = MaterialTheme.typography.bodyMedium,
                color = LilasClaro,
            )
            VSpace(14)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (day in 1..7) {
                    Box(
                        Modifier
                            .weight(1f)
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (day <= streak) DouradoEstrela.copy(alpha = 0.85f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                1.dp,
                                if (day == streak) Color.White else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$day",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (day <= streak) Color(0xFF2A1A00) else LilasClaro,
                        )
                    }
                }
            }
            VSpace(16)
            if (reward.coins > 0) InfoRow("Moedas", "+${reward.coins}")
            reward.booster?.let { InfoRow("Item", boosterLabel(it)) }
            if (reward.infiniteLivesMinutes > 0) {
                InfoRow("Vidas infinitas", "${reward.infiniteLivesMinutes} min")
            }
            VSpace(18)
            if (canDouble && reward.coins > 0) {
                PrismaButton(
                    text = "Ver vídeo e dobrar as moedas",
                    modifier = Modifier.fillMaxWidth(),
                    colors = listOf(Color(0xFF22C55E), Color(0xFF15803D)),
                    pulsing = true,
                    onClick = onDouble,
                )
                VSpace(10)
            }
            PrismaButton("Receber", Modifier.fillMaxWidth(), onClick = onClose)
        }
    }
}

/** Pausa durante a partida. */
@Composable
fun PauseDialog(
    musicEnabled: Boolean,
    sfxEnabled: Boolean,
    onToggleMusic: (Boolean) -> Unit,
    onToggleSfx: (Boolean) -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
    onResume: () -> Unit,
) {
    DialogScrim(onDismiss = onResume) {
        PrismaCard(Modifier.fillMaxWidth()) {
            Text("Pausa", style = MaterialTheme.typography.headlineMedium, color = BrancoGelo)
            VSpace(14)
            ToggleRow("Música", musicEnabled, onToggleMusic)
            ToggleRow("Efeitos sonoros", sfxEnabled, onToggleSfx)
            VSpace(16)
            PrismaButton("Continuar", Modifier.fillMaxWidth(), pulsing = true, onClick = onResume)
            VSpace(10)
            PrismaButton(
                text = "Recomeçar a fase (custa 1 vida)",
                modifier = Modifier.fillMaxWidth(),
                colors = listOf(Color(0xFF4C4670), Color(0xFF322C4D)),
                onClick = onRestart,
            )
            VSpace(8)
            PrismaTextButton("Sair para o mapa", Modifier.fillMaxWidth(), onClick = onQuit)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LilasClaro)
        Text(value, style = MaterialTheme.typography.titleMedium, color = BrancoGelo)
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = BrancoGelo)
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VerdeConfirma,
            ),
        )
    }
}

fun boosterLabel(booster: Booster): String = when (booster) {
    Booster.HAMMER -> "Martelo"
    Booster.BOMB -> "Bomba"
    Booster.COLOR_BLAST -> "Raio cromático"
    Booster.SHUFFLE -> "Embaralhar"
    Booster.EXTRA_MOVES -> "+5 jogadas"
}

fun boosterDescription(booster: Booster): String = when (booster) {
    Booster.HAMMER -> "Remove uma peça qualquer do tabuleiro."
    Booster.BOMB -> "Estoura uma área de 3x3 casas."
    Booster.COLOR_BLAST -> "Remove todas as gemas de uma cor."
    Booster.SHUFFLE -> "Redistribui o tabuleiro inteiro."
    Booster.EXTRA_MOVES -> "Devolve 5 jogadas quando elas acabam."
}
