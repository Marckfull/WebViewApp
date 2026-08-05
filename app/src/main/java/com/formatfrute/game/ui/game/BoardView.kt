package com.formatfrute.game.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.formatfrute.game.core.Direction
import com.formatfrute.game.core.Tile
import com.formatfrute.game.core.TileKind
import com.formatfrute.game.data.BoardTheme
import com.formatfrute.game.game.Burst
import com.formatfrute.game.game.Floater
import com.formatfrute.game.game.GameUi
import com.formatfrute.game.game.Speech
import com.formatfrute.game.ui.components.darken
import com.formatfrute.game.ui.components.lighten
import kotlinx.coroutines.delay
import kotlin.math.abs

private val BoardPadding = 10.dp
private val CellGap = 8.dp

/**
 * O tabuleiro. Cada fruta e um composable com identidade estavel (a chave e o
 * id da peca), entao deslizar, fundir e nascer viram animacoes de verdade em
 * vez de redesenho seco.
 */
@Composable
fun BoardView(
    ui: GameUi,
    theme: BoardTheme,
    modifier: Modifier = Modifier,
    onSwipe: (Direction) -> Unit,
    onCellTap: (Int, Int) -> Unit,
) {
    val n = ui.state.size
    val shake = remember { Animatable(0f) }

    LaunchedEffect(ui.shakeToken) {
        if (ui.shakeToken == 0L) return@LaunchedEffect
        shake.snapTo(0f)
        shake.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }

    // O tabuleiro e sempre quadrado e cabe no menor lado disponivel: em tela
    // baixa ele encolhe em vez de empurrar a barra de poderes para fora.
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = min(maxWidth, maxHeight)
        val cell = (side - BoardPadding * 2 - CellGap * (n - 1)) / n
        val density = LocalDensity.current
        val threshold = with(density) { 34.dp.toPx() }
        val cellPx = with(density) { cell.toPx() }
        val gapPx = with(density) { CellGap.toPx() }
        val padPx = with(density) { BoardPadding.toPx() }

        Box(
            Modifier
                .size(side)
                .graphicsLayer {
                    val wave = kotlin.math.sin(shake.value * 18f) * (1f - shake.value)
                    translationX = wave * 14f
                    rotationZ = wave * 0.7f
                }
                .clip(RoundedCornerShape(30.dp))
                .background(theme.board)
                .border(5.dp, theme.frame, RoundedCornerShape(30.dp))
                .pointerInput(ui.status, ui.paused, ui.pendingPower) {
                    if (ui.pendingPower != null) return@pointerInput
                    var fired = false
                    var total = Offset.Zero
                    detectDragGestures(
                        onDragStart = {
                            fired = false
                            total = Offset.Zero
                        },
                        onDragCancel = { fired = false },
                        onDragEnd = { fired = false },
                    ) { change, delta ->
                        change.consume()
                        total += delta
                        if (fired) return@detectDragGestures
                        if (total.getDistance() < threshold) return@detectDragGestures
                        fired = true
                        onSwipe(
                            if (abs(total.x) > abs(total.y)) {
                                if (total.x > 0) Direction.RIGHT else Direction.LEFT
                            } else {
                                if (total.y > 0) Direction.DOWN else Direction.UP
                            }
                        )
                    }
                }
                .pointerInput(ui.pendingPower) {
                    if (ui.pendingPower == null) return@pointerInput
                    detectTapGestures { offset ->
                        val col = ((offset.x - padPx) / (cellPx + gapPx)).toInt()
                        val row = ((offset.y - padPx) / (cellPx + gapPx)).toInt()
                        if (row in 0 until n && col in 0 until n) onCellTap(row, col)
                    }
                },
        ) {
            // Casas vazias
            for (r in 0 until n) for (c in 0 until n) {
                Box(
                    Modifier
                        .offset(x = cellX(c, cell), y = cellX(r, cell))
                        .size(cell)
                        .clip(RoundedCornerShape(cell * 0.22f))
                        .background(theme.cell),
                )
            }

            // Frutas: mortas primeiro (ficam por baixo), depois as vivas
            val all = ui.state.dying.map { it to true } + ui.state.tiles.map { it to false }
            all.forEach { (tile, dying) ->
                key(tile.id) {
                    TileView(
                        tile = tile,
                        cell = cell,
                        dying = dying,
                        highlighted = ui.pendingPower != null,
                        hinted = tile.id in ui.hintTiles,
                    )
                }
            }

            ui.bursts.forEach { burst ->
                key(burst.id) { BurstView(burst, cell) }
            }

            ui.floaters.forEach { floater ->
                key(floater.id) { FloaterView(floater, cell) }
            }

            ui.speech?.let { speech ->
                SpeechBubble(speech, cell, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

/**
 * O balãozinho da fruta. Fica acima da casa que falou (ou abaixo, se ela estiver
 * na primeira linha) e é centralizado no tabuleiro para nunca vazar da borda.
 */
@Composable
private fun SpeechBubble(speech: Speech, cell: Dp, modifier: Modifier = Modifier) {
    val pop = remember(speech.id) { Animatable(0.5f) }
    val fade = remember(speech.id) { Animatable(0f) }

    LaunchedEffect(speech.id) {
        fade.animateTo(1f, tween(140))
        pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 520f))
        delay(1900)
        fade.animateTo(0f, tween(320))
    }

    val above = speech.row > 0
    val y = if (above) {
        cellX(speech.row, cell) - cell * 0.58f
    } else {
        cellX(speech.row, cell) + cell * 1.04f
    }

    Row(
        modifier = modifier
            .offset(y = y)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                alpha = fade.value
                transformOrigin = TransformOrigin(0.5f, if (above) 1f else 0f)
            }
            .clip(RoundedCornerShape(18.dp))
            .background(if (speech.villain) Color(0xFF4A5628) else Color.White)
            .border(
                3.dp,
                if (speech.villain) Color(0xFF2C3417) else Color(0xFF3D2B1F),
                RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (speech.fruit != null) {
            Image(
                painter = painterResource(speech.fruit.art),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
        } else {
            Text("🧃", style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            text = speech.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (speech.villain) Color.White else Color(0xFF3D2B1F),
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

private fun cellX(index: Int, cell: Dp): Dp = BoardPadding + (cell + CellGap) * index

@Composable
private fun TileView(
    tile: Tile,
    cell: Dp,
    dying: Boolean,
    highlighted: Boolean,
    hinted: Boolean,
) {
    val targetX = cellX(tile.col, cell)
    val targetY = cellX(tile.row, cell)

    val x by androidx.compose.animation.core.animateDpAsState(
        targetValue = targetX,
        animationSpec = tween(125, easing = FastOutSlowInEasing),
        label = "x",
    )
    val y by androidx.compose.animation.core.animateDpAsState(
        targetValue = targetY,
        animationSpec = tween(125, easing = FastOutSlowInEasing),
        label = "y",
    )

    val scale = remember { Animatable(if (tile.spawned) 0.15f else 1f) }
    val alpha = remember { Animatable(1f) }
    val flash = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (tile.spawned) {
            scale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 700f))
        }
    }
    LaunchedEffect(tile.level, tile.merged) {
        if (tile.merged) {
            // O clarão vem antes do pulo: é ele que vende o impacto da fusão.
            flash.snapTo(0.85f)
            scale.animateTo(1.24f, tween(90))
            flash.animateTo(0f, tween(220))
            scale.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 650f))
        }
    }
    LaunchedEffect(dying) {
        if (dying) {
            alpha.animateTo(0f, tween(170))
            scale.animateTo(0.6f, tween(170))
        }
    }

    val pulse = if ((highlighted || hinted) && !dying) {
        val transition = rememberInfiniteTransition(label = "sel")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = if (hinted) 1.1f else 1.06f,
            animationSpec = infiniteRepeatable(tween(if (hinted) 400 else 520), RepeatMode.Reverse),
            label = "selp",
        ).value
    } else {
        1f
    }

    // Squash & stretch: a fruta que ainda está viajando estica no eixo do
    // movimento e achata no outro. É o detalhe que separa deslize gostoso de
    // deslize duro — e sai de graça, só com a distância que falta percorrer.
    val travelX = (targetX - x).value
    val travelY = (targetY - y).value
    val stretch = (kotlin.math.abs(travelX) + kotlin.math.abs(travelY)) / cell.value
    val horizontal = kotlin.math.abs(travelX) >= kotlin.math.abs(travelY)
    val squash = (stretch * 0.22f).coerceAtMost(0.20f)

    Box(
        Modifier
            .offset(x = x, y = y)
            .size(cell)
            .graphicsLayer {
                val base = scale.value * pulse
                scaleX = base * (if (horizontal) 1f + squash else 1f - squash)
                scaleY = base * (if (horizontal) 1f - squash else 1f + squash)
                this.alpha = alpha.value
            },
        contentAlignment = Alignment.Center,
    ) {
        when (tile.kind) {
            TileKind.RAINBOW -> RainbowTile(cell)
            TileKind.ROTTEN -> RottenTile(tile, cell)
            TileKind.FRUIT -> FruitTile(tile, cell)
        }
        if (tile.frozen) IceOverlay(cell)
        if (hinted) HintRing(cell)
        if (flash.value > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cell * 0.22f))
                    .background(Color.White.copy(alpha = flash.value)),
            )
        }
    }
}

/** Anel dourado do Olho Bom em volta das duas frutas indicadas. */
@Composable
private fun HintRing(cell: Dp) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cell * 0.22f))
            .border(4.dp, Color(0xFFFFD54F), RoundedCornerShape(cell * 0.22f)),
    )
}

@Composable
private fun FruitTile(tile: Tile, cell: Dp) {
    val fruit = tile.fruit
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cell * 0.22f))
            .background(
                Brush.verticalGradient(
                    listOf(fruit.glow.lighten(0.45f), fruit.glow.lighten(0.1f)),
                )
            )
            .border(2.5.dp, fruit.skin.darken(0.15f).copy(alpha = 0.55f), RoundedCornerShape(cell * 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(fruit.art),
            contentDescription = fruit.label,
            modifier = Modifier
                .fillMaxSize()
                .padding(cell * 0.08f),
        )
    }
}

@Composable
private fun RainbowTile(cell: Dp) {
    val transition = rememberInfiniteTransition(label = "rainbow")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4200, easing = androidx.compose.animation.core.LinearEasing)),
        label = "spin",
    )
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cell * 0.22f))
            .background(
                Brush.sweepGradient(
                    listOf(
                        Color(0xFFFF4D6D), Color(0xFFFFC531), Color(0xFF43C463),
                        Color(0xFF3FA9F5), Color(0xFF7B61FF), Color(0xFFFF4D6D),
                    )
                )
            )
            .border(3.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(cell * 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "🌈",
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.graphicsLayer { rotationZ = spin * 0.15f },
        )
    }
}

@Composable
private fun RottenTile(tile: Tile, cell: Dp) {
    val transition = rememberInfiniteTransition(label = "rotten")
    val wobble by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "wob",
    )
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cell * 0.22f))
            .background(Color(0xFF7A8B4A))
            .border(2.5.dp, Color(0xFF4A5628), RoundedCornerShape(cell * 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(tile.fruit.art),
            contentDescription = "Fruta podre",
            modifier = Modifier
                .fillMaxSize()
                .padding(cell * 0.12f)
                .graphicsLayer {
                    alpha = 0.45f
                    rotationZ = wobble
                },
        )
        Text("🤢", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun IceOverlay(cell: Dp) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cell * 0.22f))
            .background(
                Brush.linearGradient(
                    listOf(Color(0x99BEE9FF), Color(0x66FFFFFF), Color(0xAA8FD8FF)),
                )
            )
            .border(3.dp, Color(0xCCFFFFFF), RoundedCornerShape(cell * 0.22f)),
        contentAlignment = Alignment.TopEnd,
    ) {
        Text("❄️", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(4.dp))
    }
}

/** Estouro de "suco" quando duas frutas se juntam. */
@Composable
private fun BurstView(burst: Burst, cell: Dp) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }

    val drops = remember(burst.id) {
        List(if (burst.big) 12 else 7) { index ->
            val angle = (index * 360f / (if (burst.big) 12 else 7)) + burst.id % 30
            angle to (0.5f + (index % 3) * 0.22f)
        }
    }

    val reach = with(LocalDensity.current) { (cell * 0.95f).toPx() }

    Box(
        Modifier
            .offset(x = cellX(burst.col, cell), y = cellX(burst.row, cell))
            .size(cell),
        contentAlignment = Alignment.Center,
    ) {
        drops.forEach { (angle, dist) ->
            val rad = Math.toRadians(angle.toDouble())
            val dx = (kotlin.math.cos(rad) * dist).toFloat()
            val dy = (kotlin.math.sin(rad) * dist).toFloat()
            Box(
                Modifier
                    .size(if (burst.big) 13.dp else 9.dp)
                    .graphicsLayer {
                        val p = progress.value
                        translationX = dx * reach * p
                        translationY = dy * reach * p
                        alpha = 1f - p
                        scaleX = 1f - p * 0.5f
                        scaleY = 1f - p * 0.5f
                    }
                    .clip(RoundedCornerShape(50))
                    .background(burst.color),
            )
        }
    }
}

/** "+64", "COMBO x5", "COLHEITA!" subindo do tabuleiro. */
@Composable
private fun FloaterView(floater: Floater, cell: Dp) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(850, easing = FastOutSlowInEasing)) }

    Box(
        Modifier
            .offset(x = cellX(floater.col, cell), y = cellX(floater.row, cell))
            .size(cell),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = floater.text,
            style = MaterialTheme.typography.titleMedium,
            color = floater.color,
            modifier = Modifier.graphicsLayer {
                translationY = -progress.value * 90f
                alpha = 1f - progress.value * progress.value
                val pop = 1f + (1f - progress.value) * 0.35f
                scaleX = pop
                scaleY = pop
            },
        )
    }
}
