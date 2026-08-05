package com.formatfrute.game.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.formatfrute.game.ads.AdsManager
import com.formatfrute.game.ads.RewardReason
import com.formatfrute.game.core.Fruit
import com.formatfrute.game.core.GameMode
import com.formatfrute.game.core.Order
import com.formatfrute.game.core.Power
import com.formatfrute.game.data.Achievement
import com.formatfrute.game.data.GameRepository
import com.formatfrute.game.diag.CrashReporter
import com.formatfrute.game.game.GameStatus
import com.formatfrute.game.game.GameUi
import com.formatfrute.game.game.GameViewModel
import com.formatfrute.game.ui.components.ChunkyBar
import com.formatfrute.game.ui.components.Confetti
import com.formatfrute.game.ui.components.FruitBackground
import com.formatfrute.game.ui.components.JuicyButton
import com.formatfrute.game.ui.components.OutlinedTitle
import com.formatfrute.game.ui.components.PaperCard
import com.formatfrute.game.ui.components.Pulse
import com.formatfrute.game.ui.components.RollingNumber
import com.formatfrute.game.ui.components.StatPill
import com.formatfrute.game.ui.components.lighten
import com.formatfrute.game.ui.findActivity
import com.formatfrute.game.ui.formatClock
import com.formatfrute.game.ui.formatScore
import com.formatfrute.game.ui.theme.Fruta

@Composable
fun GameScreen(
    mode: GameMode,
    tutorial: Boolean,
    recipeNumber: Int = 1,
    resume: Boolean = false,
    onExit: () -> Unit,
    onShop: () -> Unit,
    vm: GameViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val repo = remember { GameRepository.get(context) }
    val profile by repo.profile.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val theme = profile.boardTheme

    var showPause by remember { mutableStateOf(false) }

    LaunchedEffect(mode, tutorial, recipeNumber) {
        CrashReporter.breadcrumb(context, "jogo:${mode.id}:$recipeNumber")
        if (resume) vm.resumeSaved() else if (mode.isCampaign) vm.startRecipe(recipeNumber)
        else vm.start(mode, tutorial)
    }

    // Partida guardada quando o app vai para o fundo — ligação não custa o jogo.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) vm.saveProgress()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Baque de fruta grande: a tela inteira sente o impacto.
    val thump = remember { Animatable(0f) }
    LaunchedEffect(ui.impactToken) {
        if (ui.impactToken == 0L) return@LaunchedEffect
        thump.snapTo(1f)
        thump.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(ui.hint) {
        if (ui.hint != null) {
            kotlinx.coroutines.delay(2400)
            vm.clearHint()
        }
    }

    BackHandler {
        if (ui.status == GameStatus.PLAYING) {
            showPause = true
            vm.setPaused(true)
        } else {
            vm.leave()
            onExit()
        }
    }

    FruitBackground(theme, density = 7) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val wave = kotlin.math.sin(thump.value * 15f) * thump.value
                    translationY = wave * 16f * ui.impactPower
                    val zoom = 1f + thump.value * 0.012f * ui.impactPower
                    scaleX = zoom
                    scaleY = zoom
                }
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GameHud(
                ui = ui,
                best = profile.bestOf(ui.mode),
                coins = profile.coins,
                onPause = {
                    showPause = true
                    vm.setPaused(true)
                },
            )

            Spacer(Modifier.height(10.dp))

            ModeStatus(ui)

            Spacer(Modifier.height(10.dp))

            BoardView(
                ui = ui,
                theme = theme,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onSwipe = vm::swipe,
                onCellTap = vm::tapCell,
            )

            Spacer(Modifier.height(10.dp))

            AnimatedVisibility(
                visible = ui.hint != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
            ) {
                Text(
                    text = ui.hint.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Fruta.Ink.copy(alpha = 0.82f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            Spacer(Modifier.height(6.dp))

            PowerBar(
                ui = ui,
                counts = Power.entries.associateWith { profile.powerCount(it) },
                onPower = vm::requestPower,
                onCancel = vm::cancelPendingPower,
            )
        }

        // ---- Camadas por cima -------------------------------------------
        if (ui.tutorial != null) {
            TutorialOverlay(
                step = ui.tutorial!!,
                onAdvance = vm::completeTutorialStep,
                onSkip = vm::skipTutorial,
            )
        }

        ui.rewardRequest?.let { request ->
            RewardDialog(
                reason = request.reason,
                power = request.power,
                coins = profile.coins,
                onWatch = {
                    val act = activity
                    if (act == null) {
                        vm.onRewardResult(false)
                    } else {
                        AdsManager.showRewarded(act) { granted -> vm.onRewardResult(granted) }
                    }
                },
                onBuy = request.power?.let { power -> { vm.buyPowerWithCoins(power) } },
                onDismiss = vm::dismissReward,
            )
        }

        if (showPause) {
            PauseDialog(
                mode = ui.mode,
                onResume = {
                    showPause = false
                    vm.setPaused(false)
                },
                onRestart = {
                    showPause = false
                    vm.setPaused(false)
                    vm.restart()
                },
                onShop = {
                    showPause = false
                    vm.setPaused(false)
                    onShop()
                },
                onExit = {
                    showPause = false
                    vm.leave()
                    onExit()
                },
            )
        }

        ui.unlocked?.let { achievement ->
            AchievementDialog(achievement = achievement, onClose = vm::dismissAchievement)
        }

        if (ui.status != GameStatus.PLAYING && ui.unlocked == null) {
            EndDialog(
                ui = ui,
                best = profile.bestOf(ui.mode),
                onRevive = vm::offerRevive,
                onDouble = vm::offerDoubleCoins,
                onAgain = {
                    val act = activity
                    val restart = { vm.restart() }
                    if (act == null || !AdsManager.maybeShowInterstitial(act) { restart() }) restart()
                },
                onNext = if (ui.recipe != null && vm.hasNextRecipe) {
                    {
                        val act = activity
                        val next = { vm.nextRecipe() }
                        if (act == null || !AdsManager.maybeShowInterstitial(act) { next() }) next()
                    }
                } else {
                    null
                },
                onExit = {
                    val act = activity
                    val leave = {
                        vm.leave()
                        onExit()
                    }
                    if (act == null || !AdsManager.maybeShowInterstitial(act) { leave() }) leave()
                },
            )
        }
    }
}

// ------------------------------------------------------------------- HUD

@Composable
private fun GameHud(ui: GameUi, best: Int, coins: Int, onPause: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(3.dp, Fruta.Ink.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                .clickable(onClick = onPause),
            contentAlignment = Alignment.Center,
        ) {
            Text("⏸", style = MaterialTheme.typography.titleMedium)
        }

        ScoreBox("PONTOS", ui.state.score, ui.mode.accent, Modifier.weight(1f))
        ScoreBox("RECORDE", best, Fruta.Grape, Modifier.weight(1f))
        StatPill("🌱", formatScore(coins))
    }
}

/** Caixinha de placar que da um "pulo" toda vez que o numero muda. */
@Composable
private fun ScoreBox(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    val pop = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(value) {
        if (value > 0) {
            pop.animateTo(1.12f, tween(90))
            pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = 700f))
        }
    }
    PaperCard(
        modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
        color = color,
        corner = 18.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
            RollingNumber(value = value, format = ::formatScore)
        }
    }
}

/** A faixa que muda conforme o modo: relógio, jogadas, vida do monstro, meta. */
@Composable
private fun ModeStatus(ui: GameUi) {
    val mode = ui.mode
    PaperCard(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.9f), corner = 18.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ui.recipe?.let { "Fase ${it.number} · ${it.title}" } ?: mode.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = mode.accent,
                )
                if (ui.combo >= 3) {
                    val transition = rememberInfiniteTransition(label = "combo")
                    val pop by transition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.14f,
                        animationSpec = infiniteRepeatable(tween(320), RepeatMode.Reverse),
                        label = "cp",
                    )
                    Text(
                        "🔥 COMBO x${ui.combo}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Fruta.Danger,
                        modifier = Modifier.graphicsLayer { scaleX = pop; scaleY = pop },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            when {
                ui.recipe != null -> {
                    val recipe = ui.recipe!!
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        recipe.orders.forEach { order ->
                            OrderChip(
                                order = order,
                                done = (ui.produced[order.level] ?: 0).coerceAtMost(order.count),
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "🎯 ${ui.movesLeft}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (ui.movesLeft <= 5) Fruta.Danger else Fruta.Ink,
                            )
                            Text(
                                "jogadas",
                                style = MaterialTheme.typography.labelSmall,
                                color = Fruta.InkSoft,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    ChunkyBar(
                        progress = ui.movesLeft / ui.movesTotal.toFloat().coerceAtLeast(1f),
                        color = if (ui.movesLeft <= 5) Fruta.Danger else mode.accent,
                        height = 12.dp,
                    )
                }

                mode.boss -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (ui.bossAngry) "😡" else "🧃", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(8.dp))
                        ChunkyBar(
                            progress = ui.bossHp.toFloat() / ui.bossMaxHp,
                            color = if (ui.bossAngry) Fruta.Danger else Fruta.Leaf,
                            modifier = Modifier.weight(1f),
                            height = 18.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${ui.bossHp}", style = MaterialTheme.typography.labelMedium, color = Fruta.Ink)
                    }
                }

                mode.hasClock -> {
                    val urgent = ui.timeLeft <= 10
                    val transition = rememberInfiniteTransition(label = "clock")
                    val beat by transition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (urgent) 1.16f else 1f,
                        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
                        label = "beat",
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "⏱️ ${formatClock(ui.timeLeft)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (urgent) Fruta.Danger else Fruta.Ink,
                            modifier = Modifier.graphicsLayer { scaleX = beat; scaleY = beat },
                        )
                        Spacer(Modifier.width(10.dp))
                        ChunkyBar(
                            progress = ui.timeLeft / mode.timeLimit.toFloat().coerceAtLeast(1f),
                            color = if (urgent) Fruta.Danger else Fruta.Sun,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                mode.moveLimit > 0 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "🎯 ${ui.movesLeft} jogadas",
                            style = MaterialTheme.typography.titleMedium,
                            color = Fruta.Ink,
                        )
                        Spacer(Modifier.width(10.dp))
                        ChunkyBar(
                            progress = ui.movesLeft / ui.movesTotal.toFloat().coerceAtLeast(1f),
                            color = mode.accent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(ui.goal, style = MaterialTheme.typography.bodySmall, color = Fruta.InkSoft)
                }

                else -> {
                    Text(ui.goal, style = MaterialTheme.typography.bodyMedium, color = Fruta.InkSoft)
                }
            }
        }
    }
}

/** Item do pedido: a fruta e quanto já saiu da cozinha. */
@Composable
private fun OrderChip(order: Order, done: Int) {
    val complete = done >= order.count
    val pop = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(done) {
        if (done > 0) {
            pop.animateTo(1.18f, tween(90))
            pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = 700f))
        }
    }
    Row(
        Modifier
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .clip(RoundedCornerShape(14.dp))
            .background(if (complete) Fruta.Leaf.copy(alpha = 0.22f) else Fruta.Cream)
            .border(
                2.5.dp,
                if (complete) Fruta.Leaf else Fruta.Ink.copy(alpha = 0.35f),
                RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(order.fruit.art),
            contentDescription = order.fruit.label,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (complete) "✓" else "$done/${order.count}",
            style = MaterialTheme.typography.labelMedium,
            color = if (complete) Fruta.Leaf else Fruta.Ink,
        )
    }
}

// --------------------------------------------------------- barra de poderes

@Composable
private fun PowerBar(
    ui: GameUi,
    counts: Map<Power, Int>,
    onPower: (Power) -> Unit,
    onCancel: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (ui.pendingPower != null) {
            JuicyButton(
                text = "Cancelar ${ui.pendingPower!!.title}",
                color = Fruta.InkSoft,
                height = 44.dp,
                modifier = Modifier.fillMaxWidth(),
                onClick = onCancel,
            )
            Spacer(Modifier.height(8.dp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Power.entries.forEach { power ->
                PowerChip(
                    power = power,
                    count = counts[power] ?: 0,
                    selected = ui.pendingPower == power,
                    onClick = { onPower(power) },
                )
            }
        }
    }
}

@Composable
private fun PowerChip(power: Power, count: Int, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.08f else 1f, label = "chip")
    Box(contentAlignment = Alignment.TopEnd) {
        Column(
            Modifier
                .width(74.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (selected) power.color.lighten(0.1f) else Color.White.copy(alpha = 0.94f)
                )
                .border(
                    3.dp,
                    if (selected) Fruta.Ink else power.color.copy(alpha = 0.8f),
                    RoundedCornerShape(20.dp),
                )
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(power.emoji, style = MaterialTheme.typography.headlineSmall)
            Text(
                power.title,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.White else Fruta.Ink,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
        Box(
            Modifier
                .padding(top = 2.dp, end = 2.dp)
                .clip(RoundedCornerShape(50))
                .background(if (count > 0) Fruta.Leaf else Fruta.Berry)
                .border(2.dp, Color.White, RoundedCornerShape(50))
                .padding(horizontal = 7.dp, vertical = 1.dp),
        ) {
            Text(
                if (count > 0) "$count" else "🎬",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

// ------------------------------------------------------------------ dialogos

@Composable
private fun Scrim(onDismiss: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC1B1310))
            .pointerInput(onDismiss) {
                detectTapGestures { onDismiss?.invoke() }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Absorve o toque para o cartao nao fechar o dialogo por engano.
        Box(
            Modifier
                .padding(24.dp)
                .pointerInput(Unit) { detectTapGestures { } },
        ) { content() }
    }
}

@Composable
private fun RewardDialog(
    reason: RewardReason,
    power: Power?,
    coins: Int,
    onWatch: () -> Unit,
    onBuy: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    Scrim(onDismiss = onDismiss) {
        PaperCard(color = Color.White) {
            Column(
                Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Pulse(Fruta.Sun.copy(alpha = 0.5f), Modifier.size(96.dp), corner = 50.dp)
                    Text(power?.emoji ?: reason.emoji, style = MaterialTheme.typography.displayLarge)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    power?.title ?: reason.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Fruta.Ink,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    power?.desc ?: reason.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Fruta.InkSoft,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                JuicyButton(
                    text = "Assistir vídeo",
                    emoji = "🎬",
                    color = Fruta.Leaf,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onWatch,
                )
                if (onBuy != null && power != null) {
                    Spacer(Modifier.height(8.dp))
                    JuicyButton(
                        text = "Comprar por ${power.price} 🌱",
                        color = if (coins >= power.price) Fruta.Sun else Fruta.InkSoft,
                        textColor = Fruta.Ink,
                        height = 50.dp,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onBuy,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Agora não",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Fruta.InkSoft,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun PauseDialog(
    mode: GameMode,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onShop: () -> Unit,
    onExit: () -> Unit,
) {
    Scrim {
        PaperCard(color = Color.White) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Pausa pro cafezinho ☕", style = MaterialTheme.typography.headlineSmall, color = Fruta.Ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    mode.howTo,
                    style = MaterialTheme.typography.bodySmall,
                    color = Fruta.InkSoft,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                JuicyButton("Continuar", Modifier.fillMaxWidth(), Fruta.Leaf, emoji = "▶️", onClick = onResume)
                Spacer(Modifier.height(8.dp))
                JuicyButton("Recomeçar", Modifier.fillMaxWidth(), Fruta.Sun, Fruta.Ink, "🔄", height = 50.dp, onClick = onRestart)
                Spacer(Modifier.height(8.dp))
                JuicyButton("Barraquinha", Modifier.fillMaxWidth(), Fruta.Grape, emoji = "🛒", height = 50.dp, onClick = onShop)
                Spacer(Modifier.height(8.dp))
                JuicyButton("Sair da partida", Modifier.fillMaxWidth(), Fruta.InkSoft, emoji = "🚪", height = 50.dp, onClick = onExit)
            }
        }
    }
}

@Composable
private fun EndDialog(
    ui: GameUi,
    best: Int,
    onRevive: () -> Unit,
    onDouble: () -> Unit,
    onAgain: () -> Unit,
    onNext: (() -> Unit)?,
    onExit: () -> Unit,
) {
    val won = ui.status == GameStatus.WON
    val recipe = ui.recipe
    Box(Modifier.fillMaxSize()) {
        Scrim {
            PaperCard(color = Color.White) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    OutlinedTitle(
                        text = when {
                            won && recipe != null -> "PEDIDO PRONTO!"
                            won -> "VOCÊ VENCEU!"
                            recipe != null -> "Faltou pouco!"
                            else -> "Fim de feira!"
                        },
                        color = if (won) Fruta.Sun else Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                    )

                    if (recipe != null && won) {
                        Spacer(Modifier.height(12.dp))
                        StarRow(ui.stars)
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = when {
                            recipe != null && won ->
                                "Fase ${recipe.number} — ${recipe.title} está fechada!"
                            recipe != null ->
                                "As jogadas acabaram antes do pedido. Tenta de novo?"
                            won -> "Você fechou o ${ui.mode.title}!"
                            else -> "O tabuleiro travou. Acontece até com os melhores."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Fruta.InkSoft,
                        textAlign = TextAlign.Center,
                    )

                    if (recipe != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            recipe.orders.forEach { order ->
                                OrderChip(
                                    order = order,
                                    done = (ui.produced[order.level] ?: 0).coerceAtMost(order.count),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ResultChip("Pontos", formatScore(ui.state.score), ui.mode.accent)
                        if (recipe == null) {
                            ResultChip("Recorde", formatScore(maxOf(best, ui.state.score)), Fruta.Grape)
                        }
                        ResultChip("Sementes", "+${ui.coinsEarned}", Fruta.Leaf)
                    }

                    if (ui.newRecord) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (recipe != null) "⭐ FASE NOVA LIBERADA!" else "🏆 RECORDE NOVO!",
                            style = MaterialTheme.typography.titleMedium,
                            color = Fruta.Sun,
                        )
                    }

                    if (recipe == null) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Melhor fruta: ", style = MaterialTheme.typography.bodySmall, color = Fruta.InkSoft)
                            Text(
                                Fruit.of(ui.state.highestLevel).label,
                                style = MaterialTheme.typography.titleMedium,
                                color = Fruit.of(ui.state.highestLevel).skin,
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    if (!won && !ui.reviveUsed) {
                        JuicyButton(
                            text = if (recipe != null) "+5 jogadas" else "Continuar jogando",
                            emoji = "🎬",
                            color = Fruta.Leaf,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onRevive,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (won && onNext != null) {
                        JuicyButton(
                            text = "Próxima fase",
                            emoji = "➡️",
                            color = Fruta.Leaf,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onNext,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    JuicyButton(
                        text = "Dobrar sementes",
                        emoji = "🎬",
                        color = Fruta.Sun,
                        textColor = Fruta.Ink,
                        height = 50.dp,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDouble,
                    )
                    Spacer(Modifier.height(8.dp))
                    JuicyButton(
                        text = if (recipe != null) "Tentar de novo" else "Jogar de novo",
                        emoji = "🔄",
                        color = ui.mode.accent,
                        height = 50.dp,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onAgain,
                    )
                    Spacer(Modifier.height(8.dp))
                    JuicyButton(
                        text = if (recipe != null) "Voltar ao mapa" else "Voltar pra feira",
                        emoji = if (recipe != null) "🗺️" else "🏠",
                        color = Fruta.InkSoft,
                        height = 50.dp,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onExit,
                    )
                }
            }
        }
        Confetti(active = won || ui.newRecord)
    }
}

/** Comemoração da conquista — entra antes do placar final, com confete. */
@Composable
private fun AchievementDialog(achievement: Achievement, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Scrim {
            PaperCard(color = Color.White) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "CONQUISTA!",
                        style = MaterialTheme.typography.labelLarge,
                        color = Fruta.InkSoft,
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(contentAlignment = Alignment.Center) {
                        Pulse(Fruta.Sun.copy(alpha = 0.55f), Modifier.size(110.dp), corner = 50.dp)
                        Text(achievement.emoji, style = MaterialTheme.typography.displayLarge)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        achievement.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Fruta.Berry,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        achievement.desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Fruta.InkSoft,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    StatPill("🌱", "+${achievement.reward}")
                    Spacer(Modifier.height(18.dp))
                    JuicyButton(
                        text = "Boa!",
                        emoji = "🎉",
                        color = Fruta.Leaf,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onClose,
                    )
                }
            }
        }
        Confetti(active = true)
    }
}

/** As três estrelas da fase, caindo uma a uma. */
@Composable
fun StarRow(stars: Int, size: androidx.compose.ui.unit.Dp = 46.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { index ->
            val earned = index < stars
            val scale = remember(stars, index) { androidx.compose.animation.core.Animatable(if (earned) 0f else 1f) }
            LaunchedEffect(stars, index) {
                if (earned) {
                    kotlinx.coroutines.delay(index * 220L)
                    scale.animateTo(1.3f, tween(140))
                    scale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.36f, stiffness = 520f))
                }
            }
            Text(
                text = if (earned) "⭐" else "☆",
                style = MaterialTheme.typography.displayMedium,
                color = if (earned) Fruta.Sun else Fruta.InkSoft.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(size)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
            )
        }
    }
}

@Composable
private fun ResultChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(color)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Fruta.InkSoft)
    }
}
