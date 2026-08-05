package com.formatfrute.game.ui.settings

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formatfrute.game.BuildConfig
import com.formatfrute.game.ads.ConsentManager
import com.formatfrute.game.audio.SoundManager
import com.formatfrute.game.data.GameRepository
import com.formatfrute.game.notify.ReminderScheduler
import com.formatfrute.game.ui.components.FruitBackground
import com.formatfrute.game.ui.components.JuicyButton
import com.formatfrute.game.ui.components.PaperCard
import com.formatfrute.game.ui.findActivity
import com.formatfrute.game.ui.legal.LegalTab
import com.formatfrute.game.ui.legal.LegalTexts
import com.formatfrute.game.R
import com.formatfrute.game.ui.theme.Fruta

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLegal: (LegalTab) -> Unit,
    onRestartTutorial: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val repo = remember { GameRepository.get(context) }
    val sound = remember { SoundManager.get(context) }
    val profile by repo.profile.collectAsStateWithLifecycle()

    FruitBackground(profile.boardTheme, density = 5) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
                    stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (profile.boardTheme.dark) Color.White else Fruta.Ink,
                )
            }

            PaperCard(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.96f)) {
                Column(Modifier.padding(6.dp)) {
                    ToggleRow("🎵", stringResource(R.string.settings_music), profile.music) {
                        repo.setMusic(it)
                        sound.refreshMusic()
                    }
                    ToggleRow("🔊", stringResource(R.string.settings_sfx), profile.sfx) { repo.setSfx(it) }
                    ToggleRow("📳", stringResource(R.string.settings_vibration), profile.vibration) { repo.setVibration(it) }
                    ToggleRow("🔔", stringResource(R.string.settings_notifications), profile.notifications) {
                        repo.setNotifications(it)
                        if (it) ReminderScheduler.scheduleAll(context)
                        else ReminderScheduler.cancelAll(context)
                    }
                }
            }

            PaperCard(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.96f)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_game), style = MaterialTheme.typography.titleLarge, color = Fruta.Ink)
                    Spacer(Modifier.height(10.dp))
                    JuicyButton(
                        text = stringResource(R.string.settings_redo_tutorial),
                        emoji = "🎓",
                        color = Fruta.Leaf,
                        height = 48.dp,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRestartTutorial,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatBox(stringResource(R.string.settings_games), "${profile.totalGames}", Modifier.weight(1f))
                        StatBox(stringResource(R.string.settings_merges), "${profile.totalMerges}", Modifier.weight(1f))
                        StatBox(stringResource(R.string.settings_harvests), "${profile.totalHarvests}", Modifier.weight(1f))
                    }
                }
            }

            PaperCard(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.96f)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_privacy_section), style = MaterialTheme.typography.titleLarge, color = Fruta.Ink)
                    Spacer(Modifier.height(10.dp))
                    JuicyButton(
                        text = stringResource(R.string.settings_terms),
                        emoji = "📄",
                        color = Fruta.Grape,
                        height = 46.dp,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onLegal(LegalTab.TERMOS) },
                    )
                    Spacer(Modifier.height(8.dp))
                    JuicyButton(
                        text = stringResource(R.string.settings_privacy),
                        emoji = "🔒",
                        color = Fruta.Sky,
                        height = 46.dp,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onLegal(LegalTab.PRIVACIDADE) },
                    )
                    if (ConsentManager.canShowPrivacyOptions()) {
                        Spacer(Modifier.height(8.dp))
                        JuicyButton(
                            text = stringResource(R.string.settings_ad_privacy),
                            emoji = "⚙️",
                            color = Fruta.InkSoft,
                            height = 46.dp,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { activity?.let { ConsentManager.showPrivacyOptions(it) } },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.settings_contact, LegalTexts.EMAIL),
                        style = MaterialTheme.typography.bodySmall,
                        color = Fruta.InkSoft,
                    )
                    Text(
                        stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelSmall,
                        color = Fruta.InkSoft,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ToggleRow(emoji: String, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = Fruta.Ink, modifier = Modifier.weight(1f))
        FruitSwitch(checked)
    }
}

/** Interruptor com cara de doce, combinando com o resto da UI. */
@Composable
private fun FruitSwitch(checked: Boolean) {
    val offset by animateFloatAsState(if (checked) 1f else 0f, label = "switch")
    Box(
        Modifier
            .size(58.dp, 32.dp)
            .clip(RoundedCornerShape(50))
            .background(if (checked) Fruta.Leaf else Fruta.InkSoft.copy(alpha = 0.4f))
            .border(2.5.dp, Fruta.Ink.copy(alpha = 0.6f), RoundedCornerShape(50)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .size(24.dp)
                .graphicsLayer { translationX = offset * 76f }
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .border(2.dp, Fruta.Ink.copy(alpha = 0.4f), RoundedCornerShape(50)),
        )
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Fruta.Cream)
            .border(2.dp, Fruta.Ink.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Fruta.Ink)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Fruta.InkSoft)
    }
}
