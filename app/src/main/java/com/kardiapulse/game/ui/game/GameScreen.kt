package com.kardiapulse.game.ui.game

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kardiapulse.game.LocalServices
import com.kardiapulse.game.core.engine.AiPersona
import com.kardiapulse.game.core.engine.GameEngine
import com.kardiapulse.game.core.model.Difficulty
import com.kardiapulse.game.core.model.GameMode
import com.kardiapulse.game.core.model.GameEvent
import com.kardiapulse.game.core.model.GameState
import com.kardiapulse.game.core.model.Phase
import com.kardiapulse.game.core.model.PowerType
import com.kardiapulse.game.core.model.Side
import com.kardiapulse.game.data.Cosmetics
import com.kardiapulse.game.ui.common.Caption
import com.kardiapulse.game.ui.common.GlassPanel
import com.kardiapulse.game.ui.common.GlowBar
import com.kardiapulse.game.ui.common.KardiaBackground
import com.kardiapulse.game.ui.common.PulseButton
import com.kardiapulse.game.ui.game.components.CardBack
import com.kardiapulse.game.ui.game.components.CardView
import com.kardiapulse.game.ui.game.components.NucleusGauge
import com.kardiapulse.game.ui.game.components.BurstLayer
import com.kardiapulse.game.ui.game.components.FlashLayer
import com.kardiapulse.game.ui.game.components.FloatingDamageLayer
import com.kardiapulse.game.ui.game.components.FlyingCard
import com.kardiapulse.game.ui.game.components.FlyingCardLayer
import com.kardiapulse.game.ui.game.components.NucleusStatusRow
import com.kardiapulse.game.ui.game.components.burstColorFor
import com.kardiapulse.game.ui.game.components.rememberShakeOffset
import com.kardiapulse.game.ui.theme.DangerRed
import com.kardiapulse.game.ui.theme.NegativePole
import com.kardiapulse.game.ui.theme.PositivePole
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.PulseGold
import com.kardiapulse.game.ui.theme.PulseMagenta
import com.kardiapulse.game.ui.theme.PulseViolet
import com.kardiapulse.game.ui.theme.SurfaceCard
import com.kardiapulse.game.ui.theme.SurfaceStroke
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary
import com.kardiapulse.game.ui.theme.TextSecondary
import com.kardiapulse.game.ui.theme.VoidBlack
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    mode: GameMode,
    difficulty: Difficulty,
    onExit: () -> Unit
) {
    val services = LocalServices.current
    val context = LocalContext.current
    val activity = context as? Activity

    val viewModel: GameViewModel = viewModel(
        factory = GameViewModel.Factory(services, mode, difficulty)
    )
    val ui by viewModel.ui.collectAsState()
    val game = ui.game

    // --- Efeitos visuais. Cada evento do motor vira uma camada animada por cima do tabuleiro.
    var flying by remember { mutableStateOf<FlyingCard?>(null) }
    var burstTrigger by remember { mutableStateOf(0) }
    var burstColor by remember { mutableStateOf(PulseCyan) }
    var shakeTrigger by remember { mutableStateOf(0) }
    var flashTrigger by remember { mutableStateOf(0) }
    var damageTrigger by remember { mutableStateOf(0) }
    var damageValue by remember { mutableStateOf<Int?>(null) }
    var damageBlocked by remember { mutableStateOf(false) }
    var fxKey by remember { mutableStateOf(0) }

    LaunchedEffect(ui.fxTick) {
        for (event in ui.fxEvents) {
            when (event) {
                is GameEvent.CardPlayed -> {
                    fxKey++
                    flying = FlyingCard(event.card, event.by, event.sign, fxKey)
                    if (event.flipped || event.chain >= 3) {
                        burstColor = burstColorFor(event.card)
                        burstTrigger++
                    }
                }
                is GameEvent.Overload -> {
                    shakeTrigger++
                    flashTrigger++
                    damageValue = event.damage
                    damageBlocked = event.blockedByShield
                    damageTrigger++
                }
                else -> Unit
            }
        }
    }

    val shake by rememberShakeOffset(shakeTrigger)

    // O aviso passageiro some sozinho.
    LaunchedEffect(ui.toast) {
        if (ui.toast != null) {
            delay(2600)
            viewModel.dismissToast()
        }
    }

    KardiaBackground(intensity = if (game != null && game.headroom <= 4) 1.5f else 1f) {
        if (game == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Acendendo o Núcleo…", color = TextSecondary)
            }
            return@KardiaBackground
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(shake.x.roundToInt(), shake.y.roundToInt()) }
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            RivalBar(
                game = game,
                thinking = ui.aiThinking,
                persona = ui.rivalPersona,
                onExit = onExit
            )

            Spacer(Modifier.height(6.dp))
            RivalHand(game, Cosmetics.cardBackColor(ui.profile.activeCardBack))

            Spacer(Modifier.height(2.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NucleusGauge(
                        nucleus = game.nucleus,
                        baseLimit = game.config.limit,
                        limitNow = game.limitNow,
                        direction = game.direction,
                        lastElement = game.lastElement,
                        chain = game.chain,
                        modifier = Modifier.fillMaxWidth(0.86f)
                    )
                    NucleusStatusRow(game.direction, game.lastElement, game.chain)
                    if (game.config.modifiers.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        ModifierRow(game)
                    }
                }
            }

            AnimatedVisibility(
                visible = ui.toast != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ToastBar(ui.toast.orEmpty())
            }

            if (ui.cornered) {
                Spacer(Modifier.height(6.dp))
                CorneredWarning()
            }

            Spacer(Modifier.height(8.dp))
            ActionBar(
                game = game,
                enabled = ui.yourTurn,
                undoCharges = ui.profile.undoCharges,
                canUndo = ui.canUndo && ui.yourTurn,
                onUse = viewModel::usePower,
                onUndo = viewModel::undo
            )

            Spacer(Modifier.height(10.dp))
            AnimatedVisibility(
                visible = ui.selectedCardId != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                PolarityChooser(
                    game = game,
                    selectedCardId = ui.selectedCardId,
                    onChoose = viewModel::playSelected
                )
            }

            Spacer(Modifier.height(8.dp))
            YourHand(
                game = game,
                selectedCardId = ui.selectedCardId,
                yourTurn = ui.yourTurn,
                onSelect = viewModel::selectCard
            )

            Spacer(Modifier.height(8.dp))
            YourBar(game = game, secondsLeft = ui.secondsLeft)
        }

        // --- Camadas de efeito (não recebem toque, ficam por cima do tabuleiro)

        FlyingCardLayer(flying = flying, onFinished = { flying = null })
        BurstLayer(trigger = burstTrigger, color = burstColor)
        FlashLayer(trigger = flashTrigger)
        FloatingDamageLayer(
            damage = damageValue,
            trigger = damageTrigger,
            blocked = damageBlocked
        )

        // --- Sobreposições

        if (game.phase == Phase.FIM_DE_RODADA && ui.matchResult == null) {
            RoundOverlay(game = game, onContinue = viewModel::continueToNextRound)
        }

        ui.matchResult?.let { result ->
            MatchOverlay(
                result = result,
                mode = mode,
                reviveOffered = ui.reviveOffered,
                rewardedReady = services.ads.rewardedReady,
                onDouble = {
                    activity?.let { act ->
                        services.ads.showRewarded(act) { earned ->
                            if (earned) viewModel.applyDoubledReward()
                        }
                    }
                },
                onRevive = {
                    activity?.let { act ->
                        services.ads.showRewarded(act) { earned ->
                            if (earned) viewModel.revive()
                        }
                    }
                },
                onAgain = viewModel::playAgain,
                onExit = {
                    // O intersticial só aparece ao SAIR do duelo, nunca no meio dele.
                    val profile = ui.profile
                    if (activity != null &&
                        services.ads.shouldShowInterstitial(profile.duelsSinceInterstitial)
                    ) {
                        viewModel.noteInterstitialShown()
                        services.ads.showInterstitial(activity) { onExit() }
                    } else {
                        onExit()
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------- topo

@Composable
private fun RivalBar(
    game: GameState,
    thinking: Boolean,
    persona: AiPersona,
    onExit: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            onClick = onExit,
            shape = RoundedCornerShape(12.dp),
            color = SurfaceCard.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, SurfaceStroke),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("‹", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = game.foe.name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    // O temperamento fica à vista: dá para ler o rival antes de jogar contra ele.
                    text = if (thinking) "pensando…" else persona.ptName.lowercase(),
                    color = if (thinking) PulseViolet else TextMuted,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${game.foe.hp}",
                    color = if (game.foe.hp <= 15) DangerRed else TextSecondary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(4.dp))
            GlowBar(
                progress = game.foe.hp.toFloat() / game.config.startHp,
                height = 6,
                colors = listOf(PulseMagenta, DangerRed)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "rodada ${game.round}",
                color = TextMuted,
                style = MaterialTheme.typography.labelMedium
            )
            if (game.foe.shielded) {
                Text("⬡ escudo", color = PulseCyan, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun RivalHand(game: GameState, cardBackTint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
    ) {
        game.foe.hand.forEachIndexed { index, card ->
            if (game.foeHandVisible) {
                CardView(card = card, width = 40, height = 58, playable = true)
            } else {
                CardBack(index = index, tint = cardBackTint)
            }
        }
    }
}

@Composable
private fun ModifierRow(game: GameState) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        game.config.modifiers.forEach { modifier ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SurfaceCard.copy(alpha = 0.9f))
                    .border(BorderStroke(1.dp, PulseGold.copy(alpha = 0.4f)), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${modifier.glyph} ${modifier.ptName}", color = PulseGold, fontSize = 10.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------- centro

@Composable
private fun ToastBar(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard.copy(alpha = 0.95f))
            .border(BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(text, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CorneredWarning() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DangerRed.copy(alpha = 0.16f))
            .border(BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            "Sem jogada legal. Use um poder de resgate ou sobrecarregue.",
            color = TextPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ActionBar(
    game: GameState,
    enabled: Boolean,
    undoCharges: Int,
    canUndo: Boolean,
    onUse: (PowerType) -> Unit,
    onUndo: () -> Unit
) {
    val owned = PowerType.ALL.filter { game.you.powerCount(it) > 0 }
    if (owned.isEmpty() && !canUndo) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (canUndo) {
            val usable = undoCharges > 0
            Surface(
                onClick = onUndo,
                enabled = usable,
                shape = RoundedCornerShape(12.dp),
                color = SurfaceCard.copy(alpha = if (usable) 0.95f else 0.5f),
                border = BorderStroke(
                    1.dp,
                    if (usable) PulseGold.copy(alpha = 0.6f) else SurfaceStroke
                )
            ) {
                Row(
                    Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("↶", color = PulseGold, fontSize = 15.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Recuo",
                        color = if (usable) TextPrimary else TextMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("×$undoCharges", color = PulseGold, fontSize = 10.sp)
                }
            }
        }

        owned.forEach { power ->
            val count = game.you.powerCount(power)
            Surface(
                onClick = { if (enabled) onUse(power) },
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                color = SurfaceCard.copy(alpha = if (enabled) 0.95f else 0.5f),
                border = BorderStroke(
                    1.dp,
                    if (enabled) PulseCyan.copy(alpha = 0.5f) else SurfaceStroke
                )
            ) {
                Row(
                    Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(power.glyph, color = PulseCyan, fontSize = 15.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        power.ptName,
                        color = if (enabled) TextPrimary else TextMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("×$count", color = PulseGold, fontSize = 10.sp)
                }
            }
        }
    }
}

/**
 * O seletor de polaridade. É aqui que a decisão central do jogo acontece: a mesma carta pode
 * empurrar o Núcleo para cima ou para baixo, e cada lado mostra onde ele vai parar.
 */
@Composable
private fun PolarityChooser(
    game: GameState,
    selectedCardId: Int?,
    onChoose: (Int) -> Unit
) {
    val card = game.you.hand.firstOrNull { it.id == selectedCardId } ?: return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(-1, 1).forEach { sign ->
            val legal = GameEngine.isLegal(game, card, sign)
            val target = game.nucleus + sign * card.value
            val color = if (sign > 0) PositivePole else NegativePole
            val scale by animateFloatAsState(
                targetValue = if (legal) 1f else 0.96f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "polaridade"
            )
            Surface(
                onClick = { if (legal) onChoose(sign) },
                enabled = legal,
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                if (legal) listOf(color.copy(alpha = 0.30f), SurfaceCard)
                                else listOf(SurfaceCard.copy(alpha = 0.5f), VoidBlack.copy(alpha = 0.5f))
                            )
                        )
                        .border(
                            BorderStroke(1.dp, if (legal) color else SurfaceStroke),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (sign > 0) "▲  +${card.value}" else "▼  −${card.value}",
                        color = if (legal) color else TextMuted,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (legal) "Núcleo → $target" else "bloqueado",
                        color = if (legal) TextSecondary else TextMuted,
                        fontSize = 10.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun YourHand(
    game: GameState,
    selectedCardId: Int?,
    yourTurn: Boolean,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        game.you.hand.forEach { card ->
            val playable = yourTurn &&
                (GameEngine.isLegal(game, card, 1) || GameEngine.isLegal(game, card, -1))
            CardView(
                card = card,
                playable = playable,
                resonant = card.resonatesWith(game.lastElement) && game.direction != 0,
                selected = card.id == selectedCardId,
                hideValue = game.config.hasFog && card.id != selectedCardId,
                onClick = { onSelect(card.id) }
            )
        }
    }
}

@Composable
private fun YourBar(game: GameState, secondsLeft: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Você", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                if (game.you.shielded) {
                    Spacer(Modifier.width(6.dp))
                    Text("⬡", color = PulseCyan, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                if (secondsLeft > 0) {
                    Text(
                        text = "${secondsLeft}s",
                        color = if (secondsLeft <= 3) DangerRed else PulseGold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = "${game.you.hp}",
                    color = if (game.you.hp <= 15) DangerRed else TextSecondary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(4.dp))
            GlowBar(
                progress = game.you.hp.toFloat() / game.config.startHp,
                height = 8,
                colors = listOf(PulseCyan, PositivePole)
            )
        }
    }
}

// ---------------------------------------------------------------------- overlays

@Composable
private fun RoundOverlay(game: GameState, onContinue: () -> Unit) {
    val youLost = game.lastRoundLoser == Side.VOCE
    OverlayScrim {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(0.88f),
            borderColor = if (youLost) DangerRed.copy(alpha = 0.6f) else PulseCyan.copy(alpha = 0.6f)
        ) {
            Text(
                text = if (youLost) "SOBRECARGA" else "O RIVAL SOBRECARREGOU",
                color = if (youLost) DangerRed else PulseCyan,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Caption(
                text = if (youLost)
                    "O Núcleo travou em ${game.nucleus} e você ficou sem jogada legal."
                else
                    "${game.foe.name} ficou sem saída com o Núcleo em ${game.nucleus}.",
                modifier = Modifier.fillMaxWidth(),
                align = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat("você", "${game.you.hp}", PulseCyan)
                MiniStat("rival", "${game.foe.hp}", PulseMagenta)
                MiniStat("maior corrente", "${game.maxChainThisRound}", PulseGold)
            }
            Spacer(Modifier.height(16.dp))
            PulseButton(text = "PRÓXIMA RODADA", onClick = onContinue)
        }
    }
}

@Composable
private fun MatchOverlay(
    result: MatchResult,
    mode: GameMode,
    reviveOffered: Boolean,
    rewardedReady: Boolean,
    onDouble: () -> Unit,
    onRevive: () -> Unit,
    onAgain: () -> Unit,
    onExit: () -> Unit
) {
    OverlayScrim {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(0.9f),
            borderColor = if (result.won) PulseCyan else DangerRed.copy(alpha = 0.7f)
        ) {
            Text(
                text = if (result.won) "VITÓRIA" else "DERROTA",
                color = if (result.won) PulseCyan else DangerRed,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            if (mode == GameMode.SOBREVIVENCIA) {
                Caption(
                    text = "Rivais derrotados em sequência: ${result.endlessStreak}",
                    modifier = Modifier.fillMaxWidth(),
                    align = TextAlign.Center,
                    color = PulseGold
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MiniStat("XP", "+${result.rewards.xp}", PulseViolet)
                MiniStat("Fragmentos", "+${result.rewards.shards}", PulseGold)
                if (result.rewards.crystals > 0) {
                    MiniStat("Cristais", "+${result.rewards.crystals}", PulseCyan)
                }
            }
            if (result.rewards.perfect) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "DUELO PERFEITO — sem levar um único arranhão",
                    color = PulseGold,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            if (result.leveledUp) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "NÍVEL AUMENTOU",
                    color = PulseCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            result.newAchievements.forEach { name ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "Conquista desbloqueada: $name",
                    color = PulseGold,
                    fontSize = 11.5.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            if (result.replayCode.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                ReplayCodeBlock(result.replayCode)
            }

            Spacer(Modifier.height(18.dp))

            if (rewardedReady && !result.rewardDoubled) {
                PulseButton(
                    text = "ASSISTIR E DOBRAR",
                    subtitle = "anúncio opcional",
                    onClick = onDouble
                )
                Spacer(Modifier.height(8.dp))
            }
            if (reviveOffered && rewardedReady) {
                PulseButton(
                    text = "CONTINUAR DE PÉ",
                    subtitle = "anúncio · mantém a sequência",
                    onClick = onRevive,
                    primary = false
                )
                Spacer(Modifier.height(8.dp))
            }
            PulseButton(
                text = if (mode == GameMode.SOBREVIVENCIA && result.won) "PRÓXIMO RIVAL" else "JOGAR DE NOVO",
                onClick = onAgain,
                primary = !rewardedReady || result.rewardDoubled
            )
            Spacer(Modifier.height(8.dp))
            PulseButton(text = "VOLTAR AO MENU", onClick = onExit, primary = false)
        }
    }
}

/**
 * O duelo inteiro em uma linha de texto.
 *
 * Como o motor é determinístico, semente mais jogadas bastam para reconstruir a partida carta
 * por carta — então dá para mandar o duelo por mensagem e o outro assistir em Extras › Replay.
 */
@Composable
private fun ReplayCodeBlock(code: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VoidBlack.copy(alpha = 0.6f))
            .border(BorderStroke(1.dp, SurfaceStroke), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            "CÓDIGO DESTE DUELO",
            color = TextMuted,
            fontSize = 8.5.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = code,
            color = TextSecondary,
            fontSize = 9.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        PulseButton(
            text = if (copied) "CÓDIGO COPIADO" else "COPIAR E COMPARTILHAR",
            primary = false,
            onClick = {
                clipboard.setText(AnnotatedString(code))
                copied = true
            }
        )
    }
}

@Composable
private fun OverlayScrim(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Box(
        Modifier
            .fillMaxSize()
            .background(VoidBlack.copy(alpha = 0.86f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = 0.88f)
        ) {
            content()
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, style = MaterialTheme.typography.titleLarge)
        Text(label.uppercase(), color = TextMuted, fontSize = 8.5.sp)
    }
}
