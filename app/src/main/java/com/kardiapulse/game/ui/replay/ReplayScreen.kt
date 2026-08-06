package com.kardiapulse.game.ui.replay

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiapulse.game.core.engine.ReplayCode
import com.kardiapulse.game.core.model.GameState
import com.kardiapulse.game.core.model.Side
import com.kardiapulse.game.ui.common.Caption
import com.kardiapulse.game.ui.common.GlassPanel
import com.kardiapulse.game.ui.common.GlowBar
import com.kardiapulse.game.ui.common.KardiaBackground
import com.kardiapulse.game.ui.common.PulseButton
import com.kardiapulse.game.ui.common.ScreenHeader
import com.kardiapulse.game.ui.game.components.CardBack
import com.kardiapulse.game.ui.game.components.CardView
import com.kardiapulse.game.ui.game.components.NucleusGauge
import com.kardiapulse.game.ui.game.components.NucleusStatusRow
import com.kardiapulse.game.ui.theme.DangerRed
import com.kardiapulse.game.ui.theme.PositivePole
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.PulseGold
import com.kardiapulse.game.ui.theme.PulseMagenta
import com.kardiapulse.game.ui.theme.SurfaceCard
import com.kardiapulse.game.ui.theme.SurfaceStroke
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary
import com.kardiapulse.game.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Assistir a um duelo a partir de um código.
 *
 * Não existe vídeo nem gravação de tela: o código traz a semente e a lista de jogadas, e o
 * próprio motor recalcula a partida inteira aqui na hora. É por isso que um duelo completo cabe
 * em uma linha de texto.
 */
@Composable
fun ReplayScreen(onBack: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var code by remember { mutableStateOf("") }
    var timeline by remember { mutableStateOf<List<GameState>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var frame by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }

    fun load(raw: String) {
        val replay = ReplayCode.decode(raw)
        if (replay == null) {
            errorMessage = "Código inválido. Confira se ele foi copiado inteiro."
            timeline = emptyList()
            return
        }
        val states = ReplayCode.rebuild(replay)
        if (states.size <= 1) {
            errorMessage = "O código é válido, mas não contém jogadas."
            timeline = emptyList()
            return
        }
        errorMessage = null
        timeline = states
        frame = 0
        playing = true
    }

    // Reprodução automática, uma jogada por vez.
    LaunchedEffect(playing, timeline, frame) {
        if (!playing || timeline.isEmpty()) return@LaunchedEffect
        if (frame >= timeline.lastIndex) {
            playing = false
            return@LaunchedEffect
        }
        delay(900)
        frame++
    }

    KardiaBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            ScreenHeader(title = "Replay", onBack = onBack)

            if (timeline.isEmpty()) {
                GlassPanel(Modifier.fillMaxWidth()) {
                    Caption(
                        "Cole aqui o código de um duelo. Ele aparece na tela de resultado, no fim " +
                            "de qualquer partida, com um botão de copiar."
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Código do duelo", color = TextMuted) },
                        singleLine = false,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextSecondary,
                            focusedBorderColor = PulseCyan,
                            unfocusedBorderColor = SurfaceStroke,
                            cursorColor = PulseCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage!!, color = DangerRed, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            PulseButton(
                                text = "COLAR",
                                primary = false,
                                onClick = {
                                    code = clipboard.getText()?.text.orEmpty().ifEmpty { code }
                                }
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            PulseButton(
                                text = "ASSISTIR",
                                enabled = code.isNotBlank(),
                                onClick = { load(code) }
                            )
                        }
                    }
                }
            } else {
                val state = timeline[frame.coerceIn(0, timeline.lastIndex)]
                ReplayBoard(state)

                Spacer(Modifier.height(12.dp))
                GlassPanel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "jogada ${frame} de ${timeline.lastIndex}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "rodada ${state.round}",
                            color = PulseGold,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    GlowBar(progress = frame.toFloat() / timeline.lastIndex.coerceAtLeast(1))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StepButton("◀", Modifier.weight(1f)) {
                            playing = false
                            if (frame > 0) frame--
                        }
                        StepButton(if (playing) "❚❚" else "▶", Modifier.weight(1f)) {
                            if (frame >= timeline.lastIndex) frame = 0
                            playing = !playing
                        }
                        StepButton("▶", Modifier.weight(1f)) {
                            playing = false
                            if (frame < timeline.lastIndex) frame++
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    PulseButton(
                        text = "OUTRO CÓDIGO",
                        primary = false,
                        onClick = {
                            timeline = emptyList()
                            playing = false
                            code = ""
                        }
                    )
                }

                // O registro escrito da partida, útil para entender a jogada que você não viu.
                if (state.log.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    GlassPanel(Modifier.fillMaxWidth()) {
                        state.log.takeLast(4).forEach { entry ->
                            Text(
                                entry.text,
                                color = when (entry.side) {
                                    Side.VOCE -> PulseCyan
                                    Side.RIVAL -> PulseMagenta
                                    else -> TextMuted
                                },
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Tabuleiro somente-leitura: as mesmas peças do duelo, sem nenhuma interação. */
@Composable
private fun ReplayBoard(state: GameState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HpRow(
            name = state.foe.name,
            hp = state.foe.hp,
            max = state.config.startHp,
            color = PulseMagenta
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            state.foe.hand.forEachIndexed { index, _ -> CardBack(index = index, width = 34, height = 48) }
        }

        Spacer(Modifier.height(6.dp))
        NucleusGauge(
            nucleus = state.nucleus,
            baseLimit = state.config.limit,
            limitNow = state.limitNow,
            direction = state.direction,
            lastElement = state.lastElement,
            chain = state.chain,
            modifier = Modifier.fillMaxWidth(0.78f)
        )
        NucleusStatusRow(state.direction, state.lastElement, state.chain)

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            state.you.hand.forEach { card ->
                CardView(card = card, width = 40, height = 58, playable = true)
            }
        }
        Spacer(Modifier.height(6.dp))
        HpRow(
            name = state.you.name,
            hp = state.you.hp,
            max = state.config.startHp,
            color = PulseCyan
        )
    }
}

@Composable
private fun HpRow(name: String, hp: Int, max: Int, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text("$hp", color = if (hp <= 15) DangerRed else TextSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(3.dp))
        GlowBar(
            progress = hp.toFloat() / max.coerceAtLeast(1),
            height = 5,
            colors = listOf(color, PositivePole)
        )
    }
}

@Composable
private fun StepButton(glyph: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, SurfaceStroke),
        modifier = modifier
    ) {
        Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(glyph, color = PulseCyan, fontSize = 15.sp)
        }
    }
}
