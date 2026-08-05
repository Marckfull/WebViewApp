package com.formatfrute.game.ui.shop

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formatfrute.game.ads.AdsManager
import com.formatfrute.game.audio.Haptics
import com.formatfrute.game.audio.Sfx
import com.formatfrute.game.audio.SoundManager
import com.formatfrute.game.core.Power
import com.formatfrute.game.data.BoardTheme
import com.formatfrute.game.data.GameRepository
import com.formatfrute.game.ui.components.Confetti
import com.formatfrute.game.ui.components.FruitBackground
import com.formatfrute.game.ui.components.JuicyButton
import com.formatfrute.game.ui.components.PaperCard
import com.formatfrute.game.ui.components.Pulse
import com.formatfrute.game.ui.components.StatPill
import com.formatfrute.game.ui.findActivity
import com.formatfrute.game.ui.formatScore
import com.formatfrute.game.ui.theme.Fruta

private const val AD_COINS = 150

/**
 * A Barraquinha. Tudo aqui pode ser comprado com Sementes, mas o caminho mais
 * curto e sempre o video premiado — e a loja deixa isso obvio.
 */
@Composable
fun ShopScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val repo = remember { GameRepository.get(context) }
    val sound = remember { SoundManager.get(context) }
    val haptics = remember { Haptics.get(context) }
    val profile by repo.profile.collectAsStateWithLifecycle()

    var toast by remember { mutableStateOf<String?>(null) }
    var celebrate by remember { mutableStateOf(false) }

    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2200)
            toast = null
        }
    }
    LaunchedEffect(celebrate) {
        if (celebrate) {
            kotlinx.coroutines.delay(2400)
            celebrate = false
        }
    }

    FruitBackground(profile.boardTheme, density = 6) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackChip(onBack)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Barraquinha",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (profile.boardTheme.dark) Color.White else Fruta.Ink,
                        modifier = Modifier.weight(1f),
                    )
                    StatPill("🌱", formatScore(profile.coins))
                }
            }

            item {
                FreeCoinsCard(
                    onWatch = {
                        val act = activity
                        if (act == null) {
                            toast = "Vídeo indisponível agora."
                            return@FreeCoinsCard
                        }
                        AdsManager.showRewarded(act) { granted ->
                            if (granted) {
                                repo.addCoins(AD_COINS)
                                sound.play(Sfx.COIN)
                                haptics.win()
                                celebrate = true
                                toast = "+$AD_COINS sementes na conta!"
                            } else {
                                toast = "O vídeo não completou. Tente de novo."
                            }
                        }
                    },
                )
            }

            item { ShopSection("Poderes", profile.boardTheme.dark) }

            items(Power.entries.toList()) { power ->
                PowerRow(
                    power = power,
                    owned = profile.powerCount(power),
                    canAfford = profile.coins >= power.price,
                    onBuy = {
                        if (repo.buyPower(power)) {
                            sound.play(Sfx.COIN)
                            haptics.tap()
                            toast = "${power.title} comprado!"
                        } else {
                            haptics.error()
                            toast = "Faltam sementes. Assista um vídeo!"
                        }
                    },
                    onWatch = {
                        val act = activity
                        if (act == null) {
                            toast = "Vídeo indisponível agora."
                            return@PowerRow
                        }
                        AdsManager.showRewarded(act) { granted ->
                            if (granted) {
                                repo.addPower(power)
                                sound.play(Sfx.POWER)
                                haptics.power()
                                toast = "${power.title} liberado!"
                            } else {
                                toast = "O vídeo não completou. Tente de novo."
                            }
                        }
                    },
                )
            }

            item { ShopSection("Peles do tabuleiro", profile.boardTheme.dark) }

            items(BoardTheme.entries.toList()) { theme ->
                ThemeRow(
                    theme = theme,
                    unlocked = theme.id in profile.unlockedThemes,
                    selected = theme.id == profile.theme,
                    canAfford = profile.coins >= theme.price,
                    onBuy = {
                        if (repo.buyTheme(theme)) {
                            repo.selectTheme(theme)
                            sound.play(Sfx.COIN)
                            celebrate = true
                            toast = "${theme.title} desbloqueada!"
                        } else {
                            haptics.error()
                            toast = "Faltam ${theme.price - profile.coins} sementes."
                        }
                    },
                    onSelect = {
                        repo.selectTheme(theme)
                        sound.play(Sfx.BUTTON)
                    },
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }

        toast?.let { message ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Fruta.Ink.copy(alpha = 0.9f))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }

        Confetti(active = celebrate)
    }
}

@Composable
private fun BackChip(onBack: () -> Unit) {
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
}

@Composable
private fun ShopSection(title: String, dark: Boolean) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        color = if (dark) Color.White else Fruta.Ink,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun FreeCoinsCard(onWatch: () -> Unit) {
    PaperCard(Modifier.fillMaxWidth(), color = Color.White, corner = 24.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Fruta.Leaf, Fruta.Sun)))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Pulse(Color.White.copy(alpha = 0.5f), Modifier.size(62.dp), corner = 50.dp)
                Text("🎬", style = MaterialTheme.typography.displayMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Sementes grátis", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    "Um vídeo rapidinho = $AD_COINS 🌱. Pode repetir sempre.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }
            JuicyButton(
                text = "Assistir",
                color = Color.White,
                textColor = Fruta.Ink,
                height = 44.dp,
                modifier = Modifier.width(104.dp),
                onClick = onWatch,
            )
        }
    }
}

@Composable
private fun PowerRow(
    power: Power,
    owned: Int,
    canAfford: Boolean,
    onBuy: () -> Unit,
    onWatch: () -> Unit,
) {
    PaperCard(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.96f), corner = 22.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(power.color.copy(alpha = 0.18f))
                    .border(2.5.dp, power.color, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(power.emoji, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(power.title, style = MaterialTheme.typography.titleMedium, color = Fruta.Ink)
                    if (owned > 0) {
                        Spacer(Modifier.width(6.dp))
                        StatPill("📦", "$owned")
                    }
                }
                Text(power.desc, style = MaterialTheme.typography.bodySmall, color = Fruta.InkSoft)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                JuicyButton(
                    text = "${power.price} 🌱",
                    color = if (canAfford) Fruta.Sun else Fruta.InkSoft,
                    textColor = if (canAfford) Fruta.Ink else Color.White,
                    height = 38.dp,
                    modifier = Modifier.width(96.dp),
                    onClick = onBuy,
                )
                Spacer(Modifier.height(6.dp))
                JuicyButton(
                    text = "Vídeo",
                    emoji = "🎬",
                    color = Fruta.Leaf,
                    height = 38.dp,
                    modifier = Modifier.width(96.dp),
                    onClick = onWatch,
                )
            }
        }
    }
}

@Composable
private fun ThemeRow(
    theme: BoardTheme,
    unlocked: Boolean,
    selected: Boolean,
    canAfford: Boolean,
    onBuy: () -> Unit,
    onSelect: () -> Unit,
) {
    PaperCard(
        Modifier.fillMaxWidth(),
        color = if (selected) Fruta.Leaf.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.96f),
        corner = 22.dp,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(theme.top, theme.bottom)))
                    .border(3.dp, theme.frame, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.cell),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(theme.title, style = MaterialTheme.typography.titleMedium, color = Fruta.Ink)
                Text(theme.desc, style = MaterialTheme.typography.bodySmall, color = Fruta.InkSoft)
            }
            Spacer(Modifier.width(8.dp))
            when {
                selected -> Text("✅", style = MaterialTheme.typography.headlineSmall)
                unlocked -> JuicyButton(
                    text = "Usar",
                    color = Fruta.Grape,
                    height = 40.dp,
                    modifier = Modifier.width(92.dp),
                    onClick = onSelect,
                )
                else -> JuicyButton(
                    text = "${theme.price} 🌱",
                    color = if (canAfford) Fruta.Sun else Fruta.InkSoft,
                    textColor = if (canAfford) Fruta.Ink else Color.White,
                    height = 40.dp,
                    modifier = Modifier.width(92.dp),
                    onClick = onBuy,
                )
            }
        }
    }
}
