package com.formatfrute.game.ui.pass

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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formatfrute.game.ads.AdsManager
import com.formatfrute.game.audio.Haptics
import com.formatfrute.game.audio.Sfx
import com.formatfrute.game.audio.SoundManager
import com.formatfrute.game.data.GameRepository
import com.formatfrute.game.data.PassReward
import com.formatfrute.game.data.SeasonPass
import com.formatfrute.game.ui.components.ChunkyBar
import com.formatfrute.game.ui.components.Confetti
import com.formatfrute.game.ui.components.FruitBackground
import com.formatfrute.game.ui.components.JuicyButton
import com.formatfrute.game.ui.components.PaperCard
import com.formatfrute.game.ui.components.Pulse
import com.formatfrute.game.ui.components.StatPill
import com.formatfrute.game.ui.findActivity
import com.formatfrute.game.R
import com.formatfrute.game.ui.label
import com.formatfrute.game.ui.theme.Fruta

/**
 * Passe da Feira. Duas faixas por degrau: a de cima abre só jogando, a de
 * baixo abre assistindo um vídeo — degrau por degrau, sem cobrar nada.
 */
@Composable
fun PassScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val repo = remember { GameRepository.get(context) }
    val sound = remember { SoundManager.get(context) }
    val haptics = remember { Haptics.get(context) }
    val profile by repo.profile.collectAsStateWithLifecycle()

    var toast by remember { mutableStateOf<String?>(null) }
    var celebrate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { repo.ensureSeasonFresh() }
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

    val tier = profile.passTier
    val (into, need) = SeasonPass.progressInTier(profile.passPoints)
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { listState.scrollToItem((tier - 1).coerceAtLeast(0)) }

    fun claim(level: Int, premium: Boolean) {
        val entry = SeasonPass.tier(level)
        val reward = if (premium) entry.premium else entry.free
        if (repo.claimTier(level, premium)) {
            sound.play(Sfx.COIN)
            haptics.win()
            celebrate = true
            toast = context.getString(R.string.pass_claimed, reward.label(context))
        } else {
            haptics.error()
            toast = context.getString(R.string.pass_locked)
        }
    }

    FruitBackground(profile.boardTheme, density = 6) {
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
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.pass_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (profile.boardTheme.dark) Color.White else Fruta.Ink,
                    )
                    Text(
                        stringResource(R.string.pass_season, stringResource(SeasonPass.seasonMonth())),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (profile.boardTheme.dark) Color.White.copy(alpha = 0.8f)
                        else Fruta.InkSoft,
                    )
                }
                StatPill("⏳", stringResource(R.string.pass_days_left, SeasonPass.daysLeft()))
            }

            PaperCard(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.95f), corner = 20.dp) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.pass_tier, tier, SeasonPass.TIERS),
                            style = MaterialTheme.typography.titleMedium,
                            color = Fruta.Ink,
                        )
                        Text(
                            stringResource(R.string.pass_tokens, into, need),
                            style = MaterialTheme.typography.labelMedium,
                            color = Fruta.InkSoft,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    ChunkyBar(into.toFloat() / need, color = Fruta.Berry, height = 14.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.pass_how),
                        style = MaterialTheme.typography.bodySmall,
                        color = Fruta.InkSoft,
                    )
                    if (profile.passTitle.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        StatPill("🏅", profile.passTitle)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(SeasonPass.TIERS) { index ->
                    val level = index + 1
                    val entry = SeasonPass.tier(level)
                    TierRow(
                        level = level,
                        reached = tier >= level,
                        free = entry.free,
                        premium = entry.premium,
                        freeClaimed = repo.isTierClaimed(level, premium = false),
                        premiumClaimed = repo.isTierClaimed(level, premium = true),
                        onClaimFree = { claim(level, premium = false) },
                        onClaimPremium = {
                            val act = activity
                            if (act == null) {
                                toast = context.getString(R.string.shop_video_unavailable)
                                return@TierRow
                            }
                            AdsManager.showRewarded(act) { granted ->
                                if (granted) claim(level, premium = true)
                                else toast = context.getString(R.string.shop_video_failed)
                            }
                        },
                    )
                }
                item { Spacer(Modifier.height(28.dp)) }
            }
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
private fun TierRow(
    level: Int,
    reached: Boolean,
    free: PassReward,
    premium: PassReward,
    freeClaimed: Boolean,
    premiumClaimed: Boolean,
    onClaimFree: () -> Unit,
    onClaimPremium: () -> Unit,
) {
    PaperCard(
        Modifier.fillMaxWidth(),
        color = if (reached) Color.White else Color.White.copy(alpha = 0.75f),
        corner = 20.dp,
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (reached) Brush.verticalGradient(listOf(Fruta.Sun, Fruta.Peach))
                        else Brush.verticalGradient(
                            listOf(Fruta.InkSoft.copy(alpha = 0.3f), Fruta.InkSoft.copy(alpha = 0.2f))
                        )
                    )
                    .border(3.dp, Fruta.Ink.copy(alpha = 0.55f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text("$level", style = MaterialTheme.typography.titleMedium, color = Fruta.Ink)
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RewardSlot(
                    tag = stringResource(R.string.pass_free),
                    tagColor = Fruta.Leaf,
                    reward = free,
                    reached = reached,
                    claimed = freeClaimed,
                    cta = stringResource(R.string.pass_claim),
                    onClaim = onClaimFree,
                )
                RewardSlot(
                    tag = stringResource(R.string.pass_premium),
                    tagColor = Fruta.Grape,
                    reward = premium,
                    reached = reached,
                    claimed = premiumClaimed,
                    cta = stringResource(R.string.pass_claim_video),
                    onClaim = onClaimPremium,
                )
            }
        }
    }
}

@Composable
private fun RewardSlot(
    tag: String,
    tagColor: Color,
    reward: PassReward,
    reached: Boolean,
    claimed: Boolean,
    cta: String,
    onClaim: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (claimed) Fruta.Leaf.copy(alpha = 0.14f) else Fruta.Cream)
            .border(2.dp, tagColor.copy(alpha = if (reached) 0.7f else 0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tag,
            style = MaterialTheme.typography.labelSmall,
            color = tagColor,
            modifier = Modifier.width(52.dp),
        )
        Text(reward.emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(6.dp))
        Text(
            reward.label(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (reached) Fruta.Ink else Fruta.InkSoft,
            modifier = Modifier.weight(1f),
        )
        when {
            claimed -> Text("✅", style = MaterialTheme.typography.titleMedium)
            reached -> Box(contentAlignment = Alignment.Center) {
                Pulse(tagColor.copy(alpha = 0.45f), Modifier.size(88.dp, 36.dp), corner = 16.dp)
                JuicyButton(
                    text = cta,
                    color = tagColor,
                    height = 32.dp,
                    modifier = Modifier.width(88.dp),
                    onClick = onClaim,
                )
            }
            else -> Text("🔒", style = MaterialTheme.typography.titleMedium)
        }
    }
}
