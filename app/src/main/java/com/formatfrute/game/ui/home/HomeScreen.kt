package com.formatfrute.game.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formatfrute.game.core.Fruit
import com.formatfrute.game.core.FruitVoice
import com.formatfrute.game.core.GameMode
import com.formatfrute.game.data.Achievements
import com.formatfrute.game.data.DailyRewards
import com.formatfrute.game.data.GameRepository
import com.formatfrute.game.data.Mission
import com.formatfrute.game.data.Ranks
import com.formatfrute.game.data.SeasonPass
import com.formatfrute.game.ui.components.ChunkyBar
import com.formatfrute.game.ui.components.Confetti
import com.formatfrute.game.ui.components.FruitBackground
import com.formatfrute.game.ui.components.JuicyButton
import com.formatfrute.game.ui.components.OutlinedTitle
import com.formatfrute.game.ui.components.PaperCard
import com.formatfrute.game.ui.components.Pulse
import com.formatfrute.game.ui.components.StatPill
import com.formatfrute.game.ui.components.lighten
import com.formatfrute.game.ui.formatScore
import com.formatfrute.game.ui.theme.Fruta

@Composable
fun HomeScreen(
    onPlay: (GameMode) -> Unit,
    onTutorial: () -> Unit,
    onShop: () -> Unit,
    onSettings: () -> Unit,
    onRecipes: () -> Unit,
    onPass: () -> Unit,
    onAchievements: () -> Unit,
    onDailyRecipe: () -> Unit,
    onResume: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { GameRepository.get(context) }
    val profile by repo.profile.collectAsStateWithLifecycle()
    val theme = profile.boardTheme

    var showDaily by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repo.ensureMissionsFresh()
        repo.ensureSeasonFresh()
        if (repo.pendingDailyStreak() > 0) showDaily = true
    }

    val mascot = remember { FruitVoice.greetingOfTheDay(GameRepository.today()) }
    val saved = remember(profile) { repo.loadResume() }
    val pendingAchievements = remember(profile) { Achievements.pending(profile).size }

    val missions = remember(profile.missionsDay, profile.missionProgress) { repo.missions() }

    FruitBackground(theme) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                PlayerBar(
                    level = profile.level,
                    rank = profile.rank,
                    xp = profile.xp,
                    coins = profile.coins,
                    onSettings = onSettings,
                )
            }

            item { Logo(theme.dark) }

            item { Mascot(fruit = mascot.first, line = mascot.second) }

            if (saved != null) {
                item {
                    ResumeCard(
                        mode = saved.mode.title,
                        score = saved.state.score,
                        onClick = onResume,
                    )
                }
            }

            if (!profile.tutorialDone) {
                item {
                    JuicyButton(
                        text = "Começar o tutorial",
                        emoji = "🎓",
                        color = Fruta.Leaf,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onTutorial,
                    )
                }
            }

            item {
                RecipeCard(
                    next = profile.nextRecipe,
                    cleared = profile.recipeCleared,
                    stars = profile.recipeStarTotal,
                    onClick = onRecipes,
                )
            }

            item {
                PassCard(
                    tier = profile.passTier,
                    progress = SeasonPass.progressInTier(profile.passPoints),
                    daysLeft = SeasonPass.daysLeft(),
                    onClick = onPass,
                )
            }

            item {
                DailyRecipeCard(
                    done = profile.dailyRecipeDay == GameRepository.today(),
                    onClick = onDailyRecipe,
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    JuicyButton(
                        text = "Barraquinha",
                        emoji = "🛒",
                        color = Fruta.Grape,
                        height = 54.dp,
                        modifier = Modifier.weight(1f),
                        onClick = onShop,
                    )
                    JuicyButton(
                        text = if (repo.pendingDailyStreak() > 0) "Presente!" else "Presente",
                        emoji = "🎁",
                        color = if (repo.pendingDailyStreak() > 0) Fruta.Sun else Fruta.InkSoft,
                        textColor = if (repo.pendingDailyStreak() > 0) Fruta.Ink else Color.White,
                        height = 54.dp,
                        modifier = Modifier.weight(1f),
                        onClick = { showDaily = true },
                    )
                }
            }

            item {
                Box {
                    JuicyButton(
                        text = "Conquistas",
                        emoji = "🏅",
                        color = Fruta.Sky,
                        height = 54.dp,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onAchievements,
                    )
                    if (pendingAchievements > 0) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 2.dp, end = 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Fruta.Berry)
                                .border(2.dp, Color.White, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 1.dp),
                        ) {
                            Text(
                                "$pendingAchievements",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle("Modos avulsos", theme.dark)
            }

            items(GameMode.arcade) { mode ->
                ModeCard(
                    mode = mode,
                    best = profile.bestOf(mode),
                    locked = !profile.tutorialDone && mode != GameMode.POMAR,
                    onClick = { onPlay(mode) },
                )
            }

            item { SectionTitle("Missões do dia", theme.dark) }

            items(missions) { mission ->
                MissionRow(
                    mission = mission,
                    progress = repo.missionProgress(mission),
                    claimed = repo.isMissionClaimed(mission),
                    onClaim = {
                        if (repo.claimMission(mission) > 0) celebrate = true
                    },
                )
            }

            item { CollectionCard(profile.highestFruit) }

            item {
                Text(
                    "Format Frute • formatfrute@gmail.com",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (theme.dark) Color.White.copy(alpha = 0.7f) else Fruta.Ink.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            item { Spacer(Modifier.height(20.dp)) }
        }

        if (showDaily) {
            DailyGiftDialog(
                streakDay = repo.pendingDailyStreak(),
                currentStreak = profile.streak,
                onClaim = {
                    if (repo.claimDaily() > 0) celebrate = true
                    showDaily = false
                },
                onDismiss = { showDaily = false },
            )
        }

        Confetti(active = celebrate)
        if (celebrate) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2600)
                celebrate = false
            }
        }
    }
}

@Composable
private fun PlayerBar(level: Int, rank: String, xp: Int, coins: Int, onSettings: () -> Unit) {
    val (into, need) = Ranks.progress(xp)
    PaperCard(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.93f), corner = 22.dp) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(Fruta.Sun, Fruta.Peach)))
                    .border(3.dp, Fruta.Ink.copy(alpha = 0.8f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text("$level", style = MaterialTheme.typography.titleMedium, color = Fruta.Ink)
            }
            Column(Modifier.weight(1f)) {
                Text(rank, style = MaterialTheme.typography.titleMedium, color = Fruta.Ink)
                Spacer(Modifier.height(4.dp))
                ChunkyBar(into.toFloat() / need, color = Fruta.Leaf, height = 12.dp)
            }
            StatPill("🌱", formatScore(coins))
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Fruta.Cream)
                    .border(2.5.dp, Fruta.Ink.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onSettings),
                contentAlignment = Alignment.Center,
            ) {
                Text("⚙️", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun Logo(dark: Boolean) {
    val transition = rememberInfiniteTransition(label = "logo")
    val bob by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "bob",
    )
    val tilt by transition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(2100), RepeatMode.Reverse),
        label = "tilt",
    )

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(Fruit.CEREJA, Fruit.LIMAO, Fruit.MELANCIA).forEachIndexed { index, fruit ->
                Image(
                    painter = painterResource(fruit.art),
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .graphicsLayer {
                            translationY = bob * (if (index % 2 == 0) 1f else -1f)
                            rotationZ = tilt * (if (index % 2 == 0) 1f else -1.4f)
                        },
                )
            }
        }
        Box(Modifier.graphicsLayer { rotationZ = tilt * 0.5f }) {
            OutlinedTitle(
                text = "FORMAT FRUTE",
                color = Fruta.Sun,
                style = MaterialTheme.typography.displayMedium,
            )
        }
        Text(
            "junte as frutas, encha a cesta",
            style = MaterialTheme.typography.bodyMedium,
            color = if (dark) Color.White.copy(alpha = 0.85f) else Fruta.Ink.copy(alpha = 0.75f),
        )
    }
}

/** A fruta do dia dando bom dia — a arte ganhando voz logo na abertura. */
@Composable
private fun Mascot(fruit: Fruit, line: String) {
    val transition = rememberInfiniteTransition(label = "mascot")
    val hop by transition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "hop",
    )
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(fruit.art),
            contentDescription = fruit.label,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { translationY = hop },
        )
        Spacer(Modifier.width(4.dp))
        PaperCard(color = Color.White.copy(alpha = 0.95f), corner = 20.dp) {
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = Fruta.Ink,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

/** Cartão de entrada do Modo Receita: mostra onde o jogador parou. */
@Composable
private fun RecipeCard(next: Int, cleared: Int, stars: Int, onClick: () -> Unit) {
    PaperCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
        corner = 24.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Fruta.Berry, Color(0xFFFFA3D1))))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🧾", style = MaterialTheme.typography.displayMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Modo Receita", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    "Monte o pedido do freguês",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.92f),
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatPill("🍳", "Fase $next", color = Color.White.copy(alpha = 0.9f))
                    StatPill("⭐", "$stars", color = Color.White.copy(alpha = 0.9f))
                }
            }
            Text("▶", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }
    }
}

/** Convite para voltar exatamente de onde parou. */
@Composable
private fun ResumeCard(mode: String, score: Int, onClick: () -> Unit) {
    PaperCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
        corner = 22.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Fruta.Leaf, Color(0xFFB7EFC5))))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⏸️", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Continuar partida", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    "$mode • ${formatScore(score)} pontos",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }
            Text("▶", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }
    }
}

/** A Receita do Dia: a mesma fase para todo mundo, trocada à meia-noite. */
@Composable
private fun DailyRecipeCard(done: Boolean, onClick: () -> Unit) {
    PaperCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
        corner = 22.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        if (done) listOf(Fruta.InkSoft, Color(0xFFB8A99C))
                        else listOf(Color(0xFF7B61FF), Color(0xFFC4B5FF))
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (done) "✅" else "📅", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Receita do Dia", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    if (done) "Fechada hoje! Volta amanhã pra próxima."
                    else "Mesma fase para todo mundo. Vale sementes extras.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }
            Text(if (done) "↻" else "▶", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }
    }
}

/** Cartão de entrada do Passe da Feira, com o degrau atual. */
@Composable
private fun PassCard(tier: Int, progress: Pair<Int, Int>, daysLeft: Int, onClick: () -> Unit) {
    PaperCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
        corner = 24.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Fruta.Sun, Fruta.Peach)))
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎟️", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Passe da Feira", style = MaterialTheme.typography.titleLarge, color = Fruta.Ink)
                    Text(
                        "Degrau $tier de ${SeasonPass.TIERS} • acaba em ${daysLeft}d",
                        style = MaterialTheme.typography.bodySmall,
                        color = Fruta.Ink.copy(alpha = 0.8f),
                    )
                }
                Text("▶", style = MaterialTheme.typography.headlineMedium, color = Fruta.Ink)
            }
            Spacer(Modifier.height(8.dp))
            ChunkyBar(
                progress = progress.first.toFloat() / progress.second,
                color = Fruta.Berry,
                height = 12.dp,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, dark: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = if (dark) Color.White else Fruta.Ink,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun ModeCard(
    mode: GameMode,
    best: Int,
    locked: Boolean,
    onClick: () -> Unit,
) {
    PaperCard(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !locked, onClick = onClick),
        color = Color.White,
        corner = 24.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(mode.accent, mode.accent2)))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(mode.emblem.art),
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .graphicsLayer { alpha = if (locked) 0.4f else 1f },
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(mode.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    if (locked) "Faça o tutorial para liberar" else mode.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
                if (best > 0) {
                    Spacer(Modifier.height(4.dp))
                    StatPill("🏆", formatScore(best), color = Color.White.copy(alpha = 0.85f))
                }
            }
            Text(
                if (locked) "🔒" else "▶",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun MissionRow(mission: Mission, progress: Int, claimed: Boolean, onClaim: () -> Unit) {
    val done = progress >= mission.target
    PaperCard(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.95f), corner = 20.dp) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(mission.emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(mission.title, style = MaterialTheme.typography.titleMedium, color = Fruta.Ink)
                Spacer(Modifier.height(4.dp))
                ChunkyBar(
                    progress = progress.toFloat() / mission.target,
                    color = if (done) Fruta.Leaf else Fruta.Sun,
                    height = 12.dp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$progress / ${mission.target}  •  +${mission.reward} 🌱",
                    style = MaterialTheme.typography.labelSmall,
                    color = Fruta.InkSoft,
                )
            }
            Spacer(Modifier.width(8.dp))
            when {
                claimed -> Text("✅", style = MaterialTheme.typography.headlineSmall)
                done -> Box {
                    Pulse(Fruta.Leaf.copy(alpha = 0.5f), Modifier.size(84.dp, 44.dp), corner = 20.dp)
                    JuicyButton(
                        text = "Pegar",
                        color = Fruta.Leaf,
                        height = 38.dp,
                        modifier = Modifier.width(84.dp),
                        onClick = onClaim,
                    )
                }
                else -> Text("⏳", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Album de figurinhas: mostra ate onde o jogador ja chegou na escada da fruta. */
@Composable
private fun CollectionCard(highest: Int) {
    PaperCard(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.95f), corner = 22.dp) {
        Column(Modifier.padding(14.dp)) {
            Text("Álbum de frutas", style = MaterialTheme.typography.titleLarge, color = Fruta.Ink)
            Text(
                "Você já chegou na ${Fruit.of(highest).label}. Faltam ${Fruit.MAX - highest} para completar!",
                style = MaterialTheme.typography.bodySmall,
                color = Fruta.InkSoft,
            )
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Fruit.entries.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { fruit ->
                            val unlocked = fruit.ordinal <= highest
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (unlocked) fruit.glow.lighten(0.4f)
                                        else Fruta.InkSoft.copy(alpha = 0.18f)
                                    )
                                    .border(2.dp, Fruta.Ink.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (unlocked) {
                                    Image(
                                        painter = painterResource(fruit.art),
                                        contentDescription = fruit.label,
                                        modifier = Modifier.size(38.dp),
                                    )
                                } else {
                                    Text("?", style = MaterialTheme.typography.titleMedium, color = Fruta.InkSoft)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyGiftDialog(
    streakDay: Int,
    currentStreak: Int,
    onClaim: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC1B1310)),
        contentAlignment = Alignment.Center,
    ) {
        PaperCard(Modifier.padding(24.dp), color = Color.White) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Presente do dia 🎁", style = MaterialTheme.typography.headlineSmall, color = Fruta.Ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (streakDay > 0) "Dia $streakDay da sua sequência!"
                    else "Você já pegou o presente de hoje. Volte amanhã!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Fruta.InkSoft,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DailyRewards.table.forEachIndexed { index, value ->
                        val day = index + 1
                        val taken = day < streakDay || (streakDay == 0 && day <= currentStreak)
                        val today = day == streakDay
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(38.dp, 46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            today -> Fruta.Sun
                                            taken -> Fruta.Leaf.copy(alpha = 0.4f)
                                            else -> Fruta.InkSoft.copy(alpha = 0.15f)
                                        }
                                    )
                                    .border(2.dp, Fruta.Ink.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (taken) "✅" else "$value",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Fruta.Ink,
                                )
                            }
                            Text("D$day", style = MaterialTheme.typography.labelSmall, color = Fruta.InkSoft)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                if (streakDay > 0) {
                    JuicyButton(
                        text = "Pegar ${DailyRewards.rewardFor(streakDay)} sementes",
                        emoji = "🌱",
                        color = Fruta.Leaf,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onClaim,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                JuicyButton(
                    text = "Fechar",
                    color = Fruta.InkSoft,
                    height = 46.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                )
            }
        }
    }
}
