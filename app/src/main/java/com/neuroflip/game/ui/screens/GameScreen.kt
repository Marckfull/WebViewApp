package com.neuroflip.game.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflip.game.data.PlayerState
import com.neuroflip.game.data.PowerUp
import com.neuroflip.game.domain.CardKind
import com.neuroflip.game.domain.GameEvent
import com.neuroflip.game.domain.GameMode
import com.neuroflip.game.domain.GameState
import com.neuroflip.game.domain.GameStatus
import com.neuroflip.game.domain.LevelCatalog
import com.neuroflip.game.ui.GameViewModel
import com.neuroflip.game.ui.components.AnimatedNeuroBackground
import com.neuroflip.game.ui.components.GlassPanel
import com.neuroflip.game.ui.components.MemoryCardView
import com.neuroflip.game.ui.components.NeonBar
import com.neuroflip.game.ui.components.NeonButton
import com.neuroflip.game.ui.components.NeonVariant
import com.neuroflip.game.ui.components.ParticleBurst
import com.neuroflip.game.ui.components.RewardedAdButton
import com.neuroflip.game.ui.components.StarRow
import com.neuroflip.game.ui.theme.LocalNeuro
import kotlinx.coroutines.delay
import kotlin.math.sin

private data class BurstFx(val id: Long, val index: Int, val gold: Boolean)
private data class SlideFx(val id: Long, val from: Int, val to: Int)

@Composable
fun GameScreen(
    vm: GameViewModel,
    player: PlayerState,
    onExit: () -> Unit,
    onNextLevel: (Int) -> Unit
) {
    val palette = LocalNeuro.current
    val context = LocalContext.current
    val activity = context as? Activity

    val state by vm.state.collectAsState()
    val result by vm.result.collectAsState()
    val hint by vm.hint.collectAsState()

    var paused by remember { mutableStateOf(false) }
    var comboBanner by remember { mutableStateOf<String?>(null) }
    val bursts = remember { mutableStateListOf<BurstFx>() }
    var slide by remember { mutableStateOf<SlideFx?>(null) }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is GameEvent.Matched -> {
                    val stamp = System.nanoTime()
                    bursts += BurstFx(stamp, event.a, event.kind == CardKind.GOLDEN)
                    bursts += BurstFx(stamp + 1, event.b, event.kind == CardKind.GOLDEN)
                    comboBanner = "+${event.points}"
                }
                is GameEvent.Combo -> comboBanner = "COMBO ×${event.level}"
                is GameEvent.Mutation -> slide = SlideFx(System.nanoTime(), event.from, event.to)
                GameEvent.OverloadStarted -> comboBanner = "SOBRECARGA!"
                else -> Unit
            }
        }
    }

    LaunchedEffect(comboBanner) {
        if (comboBanner != null) {
            delay(900)
            comboBanner = null
        }
    }

    val current = state
    Box(Modifier.fillMaxSize()) {
        AnimatedNeuroBackground(
            Modifier.fillMaxSize(),
            intensity = if (current?.overloadActive == true) 2f else 0.8f
        )

        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Carregando…", color = palette.textDim)
            }
            return@Box
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            GameHud(
                state = current,
                onPause = {
                    paused = true
                    vm.pause()
                },
                onOverload = { vm.activateOverload() }
            )

            Spacer(Modifier.height(10.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Board(
                    state = current,
                    bursts = bursts,
                    slide = slide,
                    onSlideDone = { slide = null },
                    onBurstDone = { id -> bursts.removeAll { it.id == id } },
                    onFlip = { vm.flip(it) }
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = comboBanner != null,
                    enter = fadeIn(tween(120)),
                    exit = fadeOut(tween(220))
                ) {
                    Text(
                        text = comboBanner.orEmpty(),
                        color = palette.gold,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            PowerUpBar(
                player = player,
                enabled = current.status == GameStatus.RUNNING,
                onUse = { vm.usePowerUp(it) },
                onWatchAd = { kind -> activity?.let { vm.usePowerUpFromAd(it, kind) } }
            )
        }

        // Balão de tutorial da mecânica nova do nível
        AnimatedVisibility(
            visible = hint != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            HintBalloon(text = hint.orEmpty(), onDismiss = { vm.dismissHint() })
        }
    }

    if (paused) {
        PauseDialog(
            onResume = {
                paused = false
                vm.resume()
            },
            onRestart = {
                paused = false
                vm.restart()
            },
            onQuit = {
                paused = false
                vm.quit()
                onExit()
            }
        )
    }

    result?.let { res ->
        ResultDialog(
            result = res,
            onNext = {
                if (res.mode == GameMode.CAMPAIGN && res.levelId < LevelCatalog.TOTAL_LEVELS) {
                    onNextLevel(res.levelId + 1)
                } else {
                    vm.quit()
                    onExit()
                }
            },
            onRetry = { vm.restart() },
            onMenu = {
                vm.quit()
                onExit()
            },
            onDouble = { activity?.let { vm.doubleRewardWithAd(it) } },
            onContinue = { activity?.let { vm.continueWithAd(it) } }
        )
    }
}

// ---------------------------------------------------------------------- HUD

@Composable
private fun GameHud(state: GameState, onPause: () -> Unit, onOverload: () -> Unit) {
    val palette = LocalNeuro.current
    val timed = state.config.timeLimitMs > 0
    val seconds = (state.timeLeftMs / 1000L).toInt()
    val timeProgress = if (timed) {
        (state.timeLeftMs.toFloat() / state.config.timeLimitMs).coerceIn(0f, 1f)
    } else {
        1f
    }
    val urgent = timed && seconds <= 10

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "⏸",
                fontSize = 20.sp,
                color = palette.textDim,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onPause() }
                    .padding(8.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    state.config.title.uppercase(),
                    color = palette.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    "jogadas ${state.moves} · pares ${state.matches}/${state.config.pairCount}",
                    color = palette.textDim,
                    fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${state.score}",
                    color = palette.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                if (state.combo >= 2) {
                    Text(
                        "×${"%.2f".format(state.comboMultiplier)}",
                        color = palette.gold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (timed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.frozen) "❄" else "⏱",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
                NeonBar(
                    progress = timeProgress,
                    modifier = Modifier.weight(1f),
                    height = 8.dp,
                    brush = Brush.horizontalGradient(
                        if (urgent) {
                            listOf(palette.danger, palette.secondary)
                        } else {
                            listOf(palette.primary, palette.accent)
                        }
                    )
                )
                Text(
                    text = "${seconds}s",
                    color = if (urgent) palette.danger else palette.textDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚡", fontSize = 13.sp, modifier = Modifier.padding(end = 6.dp))
            NeonBar(
                progress = state.synapse,
                modifier = Modifier.weight(1f),
                height = 8.dp,
                brush = Brush.horizontalGradient(listOf(palette.secondary, palette.gold))
            )
            Spacer(Modifier.size(8.dp))
            OverloadButton(
                ready = state.overloadReady,
                active = state.overloadActive,
                onClick = onOverload
            )
        }
    }
}

@Composable
private fun OverloadButton(ready: Boolean, active: Boolean, onClick: () -> Unit) {
    val palette = LocalNeuro.current
    val label = when {
        active -> "ATIVA"
        ready -> "SOBRECARGA"
        else -> "SINAPSE"
    }
    val color = when {
        active -> palette.gold
        ready -> palette.secondary
        else -> palette.textDim
    }
    Text(
        text = label,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable(enabled = ready) { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

// ---------------------------------------------------------------------- tabuleiro

@Composable
private fun Board(
    state: GameState,
    bursts: List<BurstFx>,
    slide: SlideFx?,
    onSlideDone: () -> Unit,
    onBurstDone: (Long) -> Unit,
    onFlip: (Int) -> Unit
) {
    val palette = LocalNeuro.current
    val columns = state.config.columns
    val rows = state.config.rows
    val gap = 8.dp

    val slideAnim = remember(slide?.id) { Animatable(0f) }
    LaunchedEffect(slide?.id) {
        if (slide != null) {
            slideAnim.snapTo(0f)
            slideAnim.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
            onSlideDone()
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cell: Dp = minOf(
            (maxWidth - gap * (columns - 1)) / columns,
            (maxHeight - gap * (rows - 1)) / rows
        )
        val step = cell + gap

        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                repeat(rows) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(columns) { col ->
                            val index = row * columns + col
                            val card = state.cards.getOrNull(index)
                            if (card == null) {
                                Spacer(Modifier.size(cell))
                                return@repeat
                            }

                            // Deslocamento da MUTAÇÃO: a carta chega deslizando da posição antiga.
                            var dx = 0f
                            var dy = 0f
                            if (slide != null && (index == slide.from || index == slide.to)) {
                                val other = if (index == slide.from) slide.to else slide.from
                                val otherCol = other % columns
                                val otherRow = other / columns
                                val t = 1f - slideAnim.value
                                dx = (otherCol - col) * step.value * t
                                dy = (otherRow - row) * step.value * t -
                                    sin(Math.PI * slideAnim.value).toFloat() * 14f
                            }

                            MemoryCardView(
                                card = card,
                                faceVisible = card.isVisible(state.nowMs),
                                previewing = state.nowMs < card.previewUntilMs && !card.faceUp,
                                cellSize = cell,
                                modifier = Modifier.graphicsLayer {
                                    translationX = dx * density
                                    translationY = dy * density
                                },
                                onClick = { onFlip(index) }
                            )
                        }
                    }
                }
            }

            // Explosões de partículas nas cartas resolvidas
            bursts.forEach { burst ->
                key(burst.id) {
                    val anim = remember { Animatable(0f) }
                    LaunchedEffect(burst.id) {
                        anim.animateTo(1f, tween(520))
                        onBurstDone(burst.id)
                    }
                    val col = burst.index % columns
                    val row = burst.index / columns
                    ParticleBurst(
                        progress = anim.value,
                        color = if (burst.gold) palette.gold else palette.primary,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = step * col, y = step * row)
                            .size(cell)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------- power-ups

@Composable
private fun PowerUpBar(
    player: PlayerState,
    enabled: Boolean,
    onUse: (PowerUp) -> Unit,
    onWatchAd: (PowerUp) -> Unit
) {
    val palette = LocalNeuro.current
    val counts = mapOf(
        PowerUp.SCAN to player.scanCount,
        PowerUp.WILD to player.wildCount,
        PowerUp.TIME to player.timeCount
    )

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PowerUp.entries.forEach { kind ->
            val available = counts[kind] ?: 0
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surface.copy(alpha = 0.6f))
                    .border(
                        1.dp,
                        (if (available > 0) palette.primary else palette.gold).copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = enabled) {
                        if (available > 0) onUse(kind) else onWatchAd(kind)
                    }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(kind.glyph, fontSize = 18.sp)
                Text(
                    text = kind.label,
                    color = palette.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (available > 0) "×$available" else "▶ vídeo",
                    color = if (available > 0) palette.textDim else palette.gold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun HintBalloon(text: String, onDismiss: () -> Unit) {
    val palette = LocalNeuro.current
    Row(
        modifier = Modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .border(1.dp, palette.accent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable { onDismiss() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("💡", fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
        Text(text, color = palette.textPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("✕", color = palette.textDim, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

// ---------------------------------------------------------------------- diálogos

@Composable
private fun PauseDialog(onResume: () -> Unit, onRestart: () -> Unit, onQuit: () -> Unit) {
    val palette = LocalNeuro.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(Modifier.fillMaxWidth(0.86f)) {
            Text(
                "PAUSA",
                color = palette.primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(18.dp))
            NeonButton("CONTINUAR", Modifier.fillMaxWidth(), glyph = "▶", onClick = onResume)
            Spacer(Modifier.height(10.dp))
            NeonButton(
                "REINICIAR",
                Modifier.fillMaxWidth(),
                glyph = "↺",
                variant = NeonVariant.GHOST,
                onClick = onRestart
            )
            Spacer(Modifier.height(10.dp))
            NeonButton(
                "SAIR",
                Modifier.fillMaxWidth(),
                glyph = "⏻",
                variant = NeonVariant.DANGER,
                onClick = onQuit
            )
        }
    }
}

@Composable
private fun ResultDialog(
    result: com.neuroflip.game.ui.GameResult,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onMenu: () -> Unit,
    onDouble: () -> Unit,
    onContinue: () -> Unit
) {
    val palette = LocalNeuro.current
    val starAnim = remember { Animatable(0f) }
    LaunchedEffect(result.won) {
        if (result.won) starAnim.animateTo(1f, tween(700))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            Modifier.fillMaxWidth(0.9f),
            borderColor = if (result.won) palette.gold else palette.danger
        ) {
            Text(
                text = if (result.won) "NÍVEL CONCLUÍDO" else "TEMPO ESGOTADO",
                color = if (result.won) palette.gold else palette.danger,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(14.dp))

            if (result.won && result.mode == GameMode.CAMPAIGN) {
                Box(Modifier.alpha(starAnim.value)) {
                    StarRow(stars = result.stars, size = 34)
                }
                Spacer(Modifier.height(14.dp))
            }

            ResultLine("Pontos", "${result.score}")
            ResultLine("Melhor combo", "×${result.bestCombo}")
            if (result.won) {
                ResultLine(
                    "Neurônios",
                    "+${result.neurons}${if (result.rewardDoubled) " (2×)" else ""}",
                    highlight = true
                )
            }

            Spacer(Modifier.height(16.dp))

            if (result.won && !result.rewardDoubled) {
                RewardedAdButton(
                    label = "Dobrar recompensa",
                    reward = "+${result.neurons} neurônios extras",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDouble
                )
                Spacer(Modifier.height(10.dp))
            }

            if (!result.won && result.canContinue) {
                RewardedAdButton(
                    label = "Continuar de onde parou",
                    reward = "+30 segundos",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onContinue
                )
                Spacer(Modifier.height(10.dp))
            }

            if (result.won) {
                NeonButton(
                    text = if (result.mode == GameMode.CAMPAIGN &&
                        result.levelId < LevelCatalog.TOTAL_LEVELS
                    ) {
                        "PRÓXIMO NÍVEL"
                    } else {
                        "CONTINUAR"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    glyph = "▶",
                    onClick = onNext
                )
                Spacer(Modifier.height(10.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton(
                    "REPETIR",
                    Modifier.weight(1f),
                    glyph = "↺",
                    variant = NeonVariant.GHOST,
                    onClick = onRetry
                )
                NeonButton(
                    "MENU",
                    Modifier.weight(1f),
                    glyph = "⌂",
                    variant = NeonVariant.GHOST,
                    onClick = onMenu
                )
            }
        }
    }
}

@Composable
private fun ResultLine(label: String, value: String, highlight: Boolean = false) {
    val palette = LocalNeuro.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(label, color = palette.textDim, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            color = if (highlight) palette.gold else palette.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
