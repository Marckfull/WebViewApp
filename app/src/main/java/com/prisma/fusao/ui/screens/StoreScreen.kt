package com.prisma.fusao.ui.screens

import android.app.Activity
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prisma.fusao.PrismaApplication
import com.prisma.fusao.ads.RewardedOffer
import com.prisma.fusao.data.Booster
import com.prisma.fusao.data.PlayerState
import com.prisma.fusao.ui.components.PrismaButton
import com.prisma.fusao.ui.components.PrismaCard
import com.prisma.fusao.ui.components.RewardedOfferDialog
import com.prisma.fusao.ui.components.StarfieldBackground
import com.prisma.fusao.ui.components.VSpace
import com.prisma.fusao.ui.components.boosterDescription
import com.prisma.fusao.ui.components.boosterLabel
import com.prisma.fusao.ui.theme.BrancoGelo
import com.prisma.fusao.ui.theme.DouradoEstrela
import com.prisma.fusao.ui.theme.LilasClaro
import com.prisma.fusao.ui.theme.VermelhoAlerta
import kotlinx.coroutines.launch

/**
 * Loja. Tudo aqui é comprável com moedas ganhas jogando — nenhum item é exclusivo
 * de quem assiste a anúncio ou paga, o que mantém a campanha inteira acessível.
 */
@Composable
fun StoreScreen(activity: Activity, onBack: () -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current
        .applicationContext as PrismaApplication
    val player by app.repository.state.collectAsStateWithLifecycle(initialValue = PlayerState())
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var pendingOffer by remember { mutableStateOf<RewardedOffer?>(null) }

    StarfieldBackground {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Loja", style = MaterialTheme.typography.headlineMedium, color = BrancoGelo)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("◈ ", color = DouradoEstrela, fontSize = 16.sp)
                    Text(
                        "%,d".format(player.coins),
                        color = BrancoGelo,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            VSpace(14)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
            ) {
                // --------------------------------------------------- moedas grátis
                PrismaCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Moedas grátis",
                        style = MaterialTheme.typography.titleLarge,
                        color = DouradoEstrela,
                    )
                    VSpace(6)
                    Text(
                        "Assista a um vídeo curto e ganhe 150 moedas. Totalmente opcional: " +
                            "dá para conseguir tudo que está aqui só jogando.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LilasClaro,
                    )
                    VSpace(12)
                    val underLimit = player.rewardedWatchedToday < PlayerState.MAX_REWARDED_PER_DAY
                    PrismaButton(
                        text = if (underLimit) {
                            "Ver vídeo e ganhar 150 moedas"
                        } else {
                            "Limite de vídeos de hoje atingido"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = underLimit,
                        colors = listOf(Color(0xFF22C55E), Color(0xFF15803D)),
                        onClick = { pendingOffer = RewardedOffer.DOUBLE_COINS },
                    )
                }

                VSpace(14)

                // ------------------------------------------------------------ vidas
                PrismaCard(Modifier.fillMaxWidth()) {
                    Text("Vidas", style = MaterialTheme.typography.titleLarge, color = BrancoGelo)
                    VSpace(6)
                    Text(
                        "Você tem ${player.lives} de ${PlayerState.MAX_LIVES}. " +
                            "Uma vida nova a cada 20 minutos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LilasClaro,
                    )
                    VSpace(12)
                    PrismaButton(
                        text = "Encher as vidas — 400 ◈",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = player.coins >= 400 && player.lives < PlayerState.MAX_LIVES,
                        onClick = {
                            scope.launch {
                                if (app.repository.spendCoins(400)) {
                                    app.repository.grantLives(PlayerState.MAX_LIVES)
                                    message = "Vidas cheias!"
                                } else {
                                    message = "Moedas insuficientes."
                                }
                            }
                        },
                    )
                    VSpace(8)
                    PrismaButton(
                        text = "Vidas infinitas por 1 hora — 900 ◈",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = player.coins >= 900,
                        colors = listOf(Color(0xFFEC4899), Color(0xFF9D174D)),
                        onClick = {
                            scope.launch {
                                if (app.repository.spendCoins(900)) {
                                    app.repository.grantInfiniteLives(60 * 60_000L)
                                    message = "Vidas infinitas por 1 hora!"
                                } else {
                                    message = "Moedas insuficientes."
                                }
                            }
                        },
                    )
                }

                VSpace(14)

                // ----------------------------------------------------------- itens
                Text("Itens", style = MaterialTheme.typography.titleLarge, color = BrancoGelo)
                VSpace(8)
                Booster.entries.forEach { booster ->
                    BoosterRow(
                        booster = booster,
                        owned = player.boosterCount(booster),
                        canAfford = player.coins >= booster.price,
                        onBuy = {
                            scope.launch {
                                message = if (app.repository.buyBooster(booster)) {
                                    "${boosterLabel(booster)} comprado!"
                                } else {
                                    "Moedas insuficientes."
                                }
                            }
                        },
                    )
                    VSpace(8)
                }

                VSpace(6)
                Text(
                    "Compras com dinheiro real ainda não estão ativas nesta versão. " +
                        "Todo o conteúdo do jogo pode ser obtido jogando.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LilasClaro.copy(alpha = 0.7f),
                )
                VSpace(20)
            }

            PrismaButton("Voltar", Modifier.fillMaxWidth(), onClick = onBack)
        }
    }

    message?.let { text ->
        androidx.compose.runtime.LaunchedEffect(text) {
            kotlinx.coroutines.delay(1800)
            message = null
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Text(
                text,
                color = BrancoGelo,
                modifier = Modifier
                    .padding(bottom = 96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }

    pendingOffer?.let { offer ->
        val ready by app.adsManager.rewardedReady.collectAsStateWithLifecycle()
        RewardedOfferDialog(
            offer = offer,
            available = ready,
            unavailableMessage = if (!ready) {
                "Nenhum vídeo disponível agora. Tente de novo em instantes."
            } else {
                null
            },
            onWatch = {
                app.adsManager.showRewarded(
                    activity = activity,
                    watchedToday = player.rewardedWatchedToday,
                    onReward = {
                        scope.launch {
                            app.repository.registerRewardedWatched()
                            app.repository.grantCoins(150)
                            message = "+150 moedas!"
                        }
                    },
                    onUnavailable = { message = "Não foi possível carregar o vídeo." },
                    onDismissed = { pendingOffer = null },
                )
            },
            onDecline = { pendingOffer = null },
        )
    }
}

@Composable
private fun BoosterRow(
    booster: Booster,
    owned: Int,
    canAfford: Boolean,
    onBuy: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    boosterLabel(booster),
                    style = MaterialTheme.typography.titleMedium,
                    color = BrancoGelo,
                )
                if (owned > 0) {
                    Text(
                        "  (você tem $owned)",
                        style = MaterialTheme.typography.bodySmall,
                        color = LilasClaro,
                    )
                }
            }
            Text(
                boosterDescription(booster),
                style = MaterialTheme.typography.bodySmall,
                color = LilasClaro,
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (canAfford) DouradoEstrela.copy(alpha = 0.9f)
                    else Color.White.copy(alpha = 0.08f)
                )
                .clickable(enabled = canAfford, onClick = onBuy)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                "${booster.price} ◈",
                color = if (canAfford) Color(0xFF2A1A00) else VermelhoAlerta,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}
