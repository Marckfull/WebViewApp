package com.prisma.fusao.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prisma.fusao.PrismaApplication
import com.prisma.fusao.ads.RewardUnavailableReason
import com.prisma.fusao.ads.RewardedOffer
import com.prisma.fusao.core.Campaign
import com.prisma.fusao.core.LevelStatus
import com.prisma.fusao.data.Booster
import com.prisma.fusao.data.PlayerState
import com.prisma.fusao.ui.components.LevelLostDialog
import com.prisma.fusao.ui.components.LevelWonDialog
import com.prisma.fusao.ui.components.OutOfLivesDialog
import com.prisma.fusao.ui.components.PauseDialog
import com.prisma.fusao.ui.components.ProgressBar
import com.prisma.fusao.ui.components.RewardedOfferDialog
import com.prisma.fusao.ui.components.StarfieldBackground
import com.prisma.fusao.ui.components.VSpace
import com.prisma.fusao.ui.components.boosterLabel
import com.prisma.fusao.ui.game.BoardCanvas
import com.prisma.fusao.ui.game.BoardEvent
import com.prisma.fusao.ui.game.BoardVisuals
import com.prisma.fusao.ui.game.GameViewModel
import com.prisma.fusao.ui.game.objectiveMissing
import com.prisma.fusao.ui.game.objectiveShort
import com.prisma.fusao.ui.theme.BrancoGelo
import com.prisma.fusao.ui.theme.DouradoEstrela
import com.prisma.fusao.ui.theme.LilasClaro
import com.prisma.fusao.ui.theme.VerdeConfirma
import kotlinx.coroutines.launch

/**
 * A tela de partida.
 *
 * A regra que orienta o layout: **nada clicável fica perto do tabuleiro**. Os itens
 * ficam numa faixa embaixo, com folga, e nenhum anúncio ocupa a área de jogo — é o
 * que evita clique acidental, exigência das políticas do AdMob.
 */
@Composable
fun GameScreen(
    levelIndex: Int,
    activity: Activity,
    onExit: () -> Unit,
    onOpenStore: () -> Unit,
    onNextLevel: (Int) -> Unit,
) {
    val app = LocalContext.current.applicationContext as PrismaApplication
    val viewModel: GameViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by app.repository.state.collectAsStateWithLifecycle(initialValue = PlayerState())
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val level = Campaign.level(levelIndex)
    val visuals = remember(levelIndex) { BoardVisuals(level.rows, level.cols) }

    var paused by remember { mutableStateOf(false) }
    var pendingOffer by remember { mutableStateOf<RewardedOffer?>(null) }
    var offerMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(levelIndex) { viewModel.startLevel(levelIndex) }

    // Uma única volta de quadro alimenta toda a animação do tabuleiro.
    LaunchedEffect(visuals) {
        var previous = 0L
        while (true) {
            withFrameNanos { now ->
                if (previous != 0L) visuals.update(((now - previous) / 1_000_000_000.0).toFloat())
                previous = now
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BoardEvent.Reset -> visuals.syncFrom(event.cells)
                is BoardEvent.Clear -> visuals.playClearPhase(event.step)
                is BoardEvent.Fall -> visuals.playFallPhase(event.step)
                is BoardEvent.Reject -> visuals.rejectSwap(event.a, event.b)
                is BoardEvent.Settle -> visuals.reconcile(event.cells)
            }
        }
    }

    LaunchedEffect(selection) { visuals.selected = selection }

    BackHandler(enabled = state.status == LevelStatus.PLAYING) { paused = true }

    StarfieldBackground {
        Column(Modifier.fillMaxSize()) {
            GameHud(
                levelIndex = levelIndex,
                score = state.score,
                movesLeft = state.movesLeft,
                objectives = state.objectives.map { objectiveShort(it) to it.complete },
                starProgress = starProgress(state.score, level.starScores),
                onPause = { paused = true },
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                BoardCanvas(
                    visuals = visuals,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                    enabled = !state.busy && state.status == LevelStatus.PLAYING && !paused,
                    onSwipe = viewModel::onSwipe,
                    onTap = viewModel::onTap,
                )

                state.toast?.let { message ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            message,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = DouradoEstrela,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                state.armedBooster?.let { booster ->
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { viewModel.cancelBooster() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "${boosterLabel(booster)}: toque numa peça  •  toque aqui para cancelar",
                            color = BrancoGelo,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            BoosterBar(
                player = player,
                armed = state.armedBooster,
                enabled = !state.busy && state.status == LevelStatus.PLAYING,
                onUse = viewModel::armBooster,
                onOpenStore = onOpenStore,
            )
        }
    }

    // ------------------------------------------------------------- diálogos

    if (paused) {
        PauseDialog(
            musicEnabled = player.musicEnabled,
            sfxEnabled = player.sfxEnabled,
            onToggleMusic = {
                scope.launch {
                    app.repository.setMusicEnabled(it)
                    app.soundEngine.musicEnabled = it
                }
            },
            onToggleSfx = {
                scope.launch {
                    app.repository.setSfxEnabled(it)
                    app.soundEngine.sfxEnabled = it
                }
            },
            onRestart = { paused = false; viewModel.restart() },
            onQuit = { paused = false; onExit() },
            onResume = { paused = false },
        )
    }

    if (state.outOfLives) {
        OutOfLivesDialog(
            timeToNextLife = formatDuration(player.millisToNextLife(System.currentTimeMillis())),
            onWatchForLife = { pendingOffer = RewardedOffer.EXTRA_LIFE },
            onDismiss = onExit,
        )
    }

    if (state.status == LevelStatus.WON) {
        LevelWonDialog(
            level = levelIndex,
            score = state.score,
            stars = state.stars,
            coins = state.coinsAwarded,
            canDoubleCoins = state.coinsAwarded > 0 &&
                player.rewardedWatchedToday < PlayerState.MAX_REWARDED_PER_DAY,
            onDoubleCoins = { pendingOffer = RewardedOffer.DOUBLE_COINS },
            onNext = {
                // Intersticial só aqui: transição de tela, com a partida encerrada.
                // `registerInterstitialShown` só vale se o anúncio realmente apareceu —
                // marcar sem exibir adiaria os próximos sem motivo.
                if (app.adsManager.maybeShowInterstitial(activity, levelIndex, player)) {
                    scope.launch { app.repository.registerInterstitialShown() }
                }
                onNextLevel((levelIndex + 1).coerceAtMost(Campaign.LEVEL_COUNT))
            },
            onMap = {
                if (app.adsManager.maybeShowInterstitial(activity, levelIndex, player)) {
                    scope.launch { app.repository.registerInterstitialShown() }
                }
                onExit()
            },
        )
    }

    if (state.status == LevelStatus.LOST) {
        LevelLostDialog(
            objectivesMissing = state.objectives.filter { !it.complete }.map { objectiveMissing(it) },
            extraMovesBoosters = player.boosterCount(Booster.EXTRA_MOVES),
            onWatchForMoves = { pendingOffer = RewardedOffer.EXTRA_MOVES },
            onUseBooster = { viewModel.consumeExtraMovesBooster { } },
            onRetry = { viewModel.restart() },
            onMap = onExit,
        )
    }

    pendingOffer?.let { offer ->
        val ready by app.adsManager.rewardedReady.collectAsStateWithLifecycle()
        val underLimit = player.rewardedWatchedToday < PlayerState.MAX_REWARDED_PER_DAY
        RewardedOfferDialog(
            offer = offer,
            available = ready && underLimit,
            unavailableMessage = when {
                !underLimit -> "Você já assistiu ao máximo de vídeos premiados por hoje. " +
                    "Amanhã eles voltam."
                !ready -> "Nenhum vídeo disponível agora. Tente de novo em instantes."
                else -> null
            },
            onWatch = {
                app.adsManager.showRewarded(
                    activity = activity,
                    watchedToday = player.rewardedWatchedToday,
                    onReward = {
                        scope.launch {
                            app.repository.registerRewardedWatched()
                            when (offer) {
                                RewardedOffer.EXTRA_MOVES -> viewModel.grantExtraMoves(5)
                                RewardedOffer.EXTRA_LIFE -> {
                                    app.repository.grantLives(1)
                                    viewModel.startLevel(levelIndex)
                                }
                                RewardedOffer.DOUBLE_COINS -> viewModel.doubleCoins()
                                RewardedOffer.FREE_BOOSTER ->
                                    app.repository.grantBooster(Booster.HAMMER)
                                RewardedOffer.DAILY_BONUS -> Unit
                            }
                        }
                    },
                    onUnavailable = { reason ->
                        offerMessage = when (reason) {
                            RewardUnavailableReason.DAILY_LIMIT ->
                                "Limite de vídeos de hoje atingido."
                            RewardUnavailableReason.NO_CONSENT ->
                                "Os anúncios estão desativados nas suas opções de privacidade."
                            RewardUnavailableReason.NOT_LOADED ->
                                "Não conseguimos carregar o vídeo agora."
                        }
                    },
                    onDismissed = { pendingOffer = null },
                )
            },
            onDecline = { pendingOffer = null },
        )
    }

    offerMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2500)
            offerMessage = null
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Text(
                message,
                color = BrancoGelo,
                modifier = Modifier
                    .padding(bottom = 120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

/** Quanto falta para a próxima estrela — a barra que empurra o jogador a continuar. */
private fun starProgress(score: Int, thresholds: List<Int>): Float {
    val next = thresholds.firstOrNull { score < it } ?: return 1f
    val previous = thresholds.filter { score >= it }.maxOrNull() ?: 0
    return ((score - previous).toFloat() / (next - previous).coerceAtLeast(1)).coerceIn(0f, 1f)
}

private fun formatDuration(millis: Long?): String {
    if (millis == null) return "instantes"
    val seconds = millis / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun GameHud(
    levelIndex: Int,
    score: Int,
    movesLeft: Int,
    objectives: List<Pair<String, Boolean>>,
    starProgress: Float,
    onPause: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onPause),
                contentAlignment = Alignment.Center,
            ) {
                Text("II", color = BrancoGelo, fontWeight = FontWeight.Black)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FASE $levelIndex", fontSize = 11.sp, color = LilasClaro)
                Text(
                    "%,d".format(score),
                    style = MaterialTheme.typography.titleLarge,
                    color = BrancoGelo,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("JOGADAS", fontSize = 11.sp, color = LilasClaro)
                Text(
                    "$movesLeft",
                    style = MaterialTheme.typography.titleLarge,
                    // Vermelho nas últimas cinco: aviso sem precisar de texto.
                    color = if (movesLeft <= 5) Color(0xFFFF6B6B) else BrancoGelo,
                )
            }
        }

        VSpace(8)
        ProgressBar(starProgress, Modifier.fillMaxWidth(), color = DouradoEstrela, height = 6.dp)
        VSpace(8)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            objectives.forEach { (text, complete) ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (complete) VerdeConfirma.copy(alpha = 0.22f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (complete) "✓ $text" else text,
                        fontSize = 12.sp,
                        color = if (complete) VerdeConfirma else BrancoGelo,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoosterBar(
    player: PlayerState,
    armed: Booster?,
    enabled: Boolean,
    onUse: (Booster) -> Unit,
    onOpenStore: () -> Unit,
) {
    val usable = listOf(Booster.HAMMER, Booster.BOMB, Booster.COLOR_BLAST, Booster.SHUFFLE)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.34f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        usable.forEach { booster ->
            val count = player.boosterCount(booster)
            Box(
                Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (armed == booster) {
                            Brush.verticalGradient(listOf(Color(0xFFFFC53D), Color(0xFFE08900)))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFF3B2270), Color(0xFF2A1B52)))
                        },
                    )
                    .border(
                        1.dp,
                        if (armed == booster) Color.White else Color.White.copy(alpha = 0.14f),
                        RoundedCornerShape(16.dp),
                    )
                    .clickable(enabled = enabled) {
                        if (count > 0) onUse(booster) else onOpenStore()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        boosterIcon(booster),
                        fontSize = 20.sp,
                        color = if (armed == booster) Color(0xFF2A1A00) else BrancoGelo,
                    )
                    Text(
                        if (count > 0) "x$count" else "+",
                        fontSize = 11.sp,
                        color = if (armed == booster) Color(0xFF2A1A00) else LilasClaro,
                    )
                }
            }
        }
    }
}

private fun boosterIcon(booster: Booster): String = when (booster) {
    Booster.HAMMER -> "🔨"
    Booster.BOMB -> "💣"
    Booster.COLOR_BLAST -> "🌈"
    Booster.SHUFFLE -> "🔀"
    Booster.EXTRA_MOVES -> "➕"
}
