package com.kardiapulse.game.ui.shop

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiapulse.game.LocalServices
import com.kardiapulse.game.core.model.PowerType
import com.kardiapulse.game.data.Cosmetic
import com.kardiapulse.game.data.Cosmetics
import com.kardiapulse.game.data.Economy
import com.kardiapulse.game.data.PlayerProfile
import com.kardiapulse.game.ui.common.Caption
import com.kardiapulse.game.ui.common.GlassPanel
import com.kardiapulse.game.ui.common.KardiaBackground
import com.kardiapulse.game.ui.common.PulseButton
import com.kardiapulse.game.ui.common.ResourceChip
import com.kardiapulse.game.ui.common.ScreenHeader
import com.kardiapulse.game.ui.common.SectionTitle
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.PulseGold
import com.kardiapulse.game.ui.theme.SurfaceCard
import com.kardiapulse.game.ui.theme.SurfaceStroke
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/** Preço de uma carga de Recuo, em Fragmentos. */
private const val UNDO_PRICE = 110

@Composable
fun ShopScreen(profile: PlayerProfile, onBack: () -> Unit) {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? Activity
    var message by remember { mutableStateOf<String?>(null) }

    fun buyPower(power: PowerType) {
        if (profile.shards < power.shardPrice) {
            message = "Fragmentos insuficientes para ${power.ptName}."
            return
        }
        scope.launch {
            services.repository.update {
                it.copy(shards = it.shards - power.shardPrice).withPower(power, 1)
            }
            services.audio.play(com.kardiapulse.game.audio.Sfx.COIN)
            services.haptics.perform(com.kardiapulse.game.haptics.HapticPattern.REWARD)
            message = "${power.ptName} adicionado ao inventário."
        }
    }

    fun buyCosmetic(item: Cosmetic) {
        if (item.id in profile.cosmetics) {
            scope.launch {
                services.repository.update {
                    if (item.kind == Cosmetic.Kind.DORSO) it.copy(activeCardBack = item.id)
                    else it.copy(activeBoard = item.id)
                }
                message = "${item.name} equipado."
            }
            return
        }
        if (profile.crystals < item.crystalPrice) {
            message = "Cristais insuficientes para ${item.name}."
            return
        }
        scope.launch {
            services.repository.update {
                val owned = it.copy(
                    crystals = it.crystals - item.crystalPrice,
                    cosmetics = it.cosmetics + item.id
                )
                if (item.kind == Cosmetic.Kind.DORSO) owned.copy(activeCardBack = item.id)
                else owned.copy(activeBoard = item.id)
            }
            services.audio.play(com.kardiapulse.game.audio.Sfx.COIN)
            message = "${item.name} desbloqueado e equipado."
        }
    }

    KardiaBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            ScreenHeader(title = "Loja", onBack = onBack)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResourceChip("◈", profile.shards.toString(), PulseGold)
                ResourceChip("✧", profile.crystals.toString(), PulseCyan)
            }

            if (message != null) {
                Spacer(Modifier.height(12.dp))
                GlassPanel(Modifier.fillMaxWidth(), borderColor = PulseCyan.copy(alpha = 0.5f)) {
                    Text(message!!, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // --- Anúncio premiado
            Spacer(Modifier.height(18.dp))
            SectionTitle("Fragmentos grátis")
            Spacer(Modifier.height(10.dp))
            GlassPanel(Modifier.fillMaxWidth(), borderColor = PulseGold.copy(alpha = 0.5f)) {
                Caption(
                    "Assista a um anúncio curto e leve ${Economy.REWARDED_AD_SHARDS} Fragmentos. " +
                        "É opcional: tudo aqui também é comprável só jogando."
                )
                Spacer(Modifier.height(12.dp))
                PulseButton(
                    text = if (services.ads.rewardedReady) "ASSISTIR ANÚNCIO" else "ANÚNCIO INDISPONÍVEL",
                    enabled = services.ads.rewardedReady && activity != null,
                    onClick = {
                        activity?.let { act ->
                            services.ads.showRewarded(act) { earned ->
                                if (earned) {
                                    scope.launch {
                                        services.repository.update {
                                            it.copy(shards = it.shards + Economy.REWARDED_AD_SHARDS)
                                        }
                                        services.audio.play(com.kardiapulse.game.audio.Sfx.COIN)
                                        message = "+${Economy.REWARDED_AD_SHARDS} Fragmentos."
                                    }
                                } else {
                                    message = "O anúncio não foi concluído."
                                }
                            }
                        }
                    }
                )
            }

            // --- Poderes
            Spacer(Modifier.height(20.dp))
            SectionTitle("Poderes")
            Spacer(Modifier.height(4.dp))
            Caption("Consumíveis. Usar um poder não gasta o seu turno.")
            Spacer(Modifier.height(10.dp))
            PowerType.ALL.forEach { power ->
                PowerRow(
                    power = power,
                    owned = profile.powerCount(power),
                    affordable = profile.shards >= power.shardPrice,
                    onBuy = { buyPower(power) }
                )
                Spacer(Modifier.height(8.dp))
            }

            // --- Recuo
            Spacer(Modifier.height(20.dp))
            SectionTitle("Recuo")
            Spacer(Modifier.height(10.dp))
            GlassPanel(Modifier.fillMaxWidth(), borderColor = PulseGold.copy(alpha = 0.45f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↶", color = PulseGold, fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Recuo",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("×${profile.undoCharges}", color = PulseGold, fontSize = 11.sp)
                        }
                        Caption(
                            "Desfaz a sua última ação e tudo que o rival respondeu depois dela. " +
                                "Uma carga por uso."
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                PulseButton(
                    text = "COMPRAR · $UNDO_PRICE ◈",
                    enabled = profile.shards >= UNDO_PRICE,
                    primary = false,
                    onClick = {
                        scope.launch {
                            services.repository.update {
                                it.copy(
                                    shards = it.shards - UNDO_PRICE,
                                    undoCharges = it.undoCharges + 1
                                )
                            }
                            services.audio.play(com.kardiapulse.game.audio.Sfx.COIN)
                            message = "Uma carga de Recuo adicionada."
                        }
                    }
                )
            }

            // --- Cosméticos
            Spacer(Modifier.height(20.dp))
            SectionTitle("Dorsos de carta")
            Spacer(Modifier.height(10.dp))
            CosmeticGrid(
                items = Cosmetics.CARD_BACKS,
                profile = profile,
                activeId = profile.activeCardBack,
                onPick = ::buyCosmetic
            )

            Spacer(Modifier.height(18.dp))
            SectionTitle("Mesas")
            Spacer(Modifier.height(10.dp))
            CosmeticGrid(
                items = Cosmetics.BOARDS,
                profile = profile,
                activeId = profile.activeBoard,
                onPick = ::buyCosmetic
            )

            // --- Remover anúncios
            Spacer(Modifier.height(20.dp))
            SectionTitle("Sem anúncios")
            Spacer(Modifier.height(10.dp))
            GlassPanel(Modifier.fillMaxWidth()) {
                Caption(
                    if (profile.removeAds)
                        "Os anúncios intersticiais estão desligados. Os premiados continuam " +
                            "disponíveis caso você queira as recompensas extras."
                    else
                        "Desliga os anúncios intersticiais para sempre. Custa 40 Cristais, que você " +
                            "ganha vencendo duelos perfeitos e no Passe Diário."
                )
                Spacer(Modifier.height(12.dp))
                PulseButton(
                    text = if (profile.removeAds) "JÁ ATIVADO" else "REMOVER ANÚNCIOS · 40 ✧",
                    enabled = !profile.removeAds && profile.crystals >= 40,
                    primary = false,
                    onClick = {
                        scope.launch {
                            services.repository.update {
                                it.copy(crystals = it.crystals - 40, removeAds = true)
                            }
                            services.ads.adsRemoved = true
                            message = "Anúncios intersticiais desativados."
                        }
                    }
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PowerRow(
    power: PowerType,
    owned: Int,
    affordable: Boolean,
    onBuy: () -> Unit
) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(power.glyph, color = PulseCyan, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        power.ptName,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (owned > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text("×$owned", color = PulseGold, fontSize = 11.sp)
                    }
                }
                Caption(power.description)
            }
        }
        Spacer(Modifier.height(10.dp))
        PulseButton(
            text = "COMPRAR · ${power.shardPrice} ◈",
            enabled = affordable,
            primary = false,
            onClick = onBuy
        )
    }
}

@Composable
private fun CosmeticGrid(
    items: List<Cosmetic>,
    profile: PlayerProfile,
    activeId: String,
    onPick: (Cosmetic) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { item ->
                    CosmeticTile(
                        item = item,
                        owned = item.id in profile.cosmetics,
                        active = item.id == activeId,
                        modifier = Modifier.weight(1f),
                        onClick = { onPick(item) }
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CosmeticTile(
    item: Cosmetic,
    owned: Boolean,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, if (active) item.color else SurfaceStroke),
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(item.color.copy(alpha = 0.6f))
                        .border(BorderStroke(1.dp, item.color), RoundedCornerShape(7.dp))
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    item.name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    active -> "equipado"
                    owned -> "toque para equipar"
                    else -> "${item.crystalPrice} ✧"
                },
                color = when {
                    active -> item.color
                    owned -> PulseCyan
                    else -> TextMuted
                },
                fontSize = 10.5.sp
            )
        }
    }
}
