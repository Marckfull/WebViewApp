package com.neonsombra.game.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import com.neonsombra.game.game.COLS
import com.neonsombra.game.game.GameSnapshot
import com.neonsombra.game.game.ROWS
import com.neonsombra.game.game.TetrominoType
import com.neonsombra.game.ui.theme.NeonCyan
import com.neonsombra.game.ui.theme.NeonMagenta
import com.neonsombra.game.ui.theme.NeonPurple
import com.neonsombra.game.ui.theme.ShadowBlock
import com.neonsombra.game.ui.theme.ShadowEdge
import com.neonsombra.game.ui.theme.colorForCode
import com.neonsombra.game.ui.theme.neonColor
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * O tabuleiro. Desenha, de tras para frente: fundo, grade, o espelho sombrio,
 * as pecas presas, a peca fantasma e a peca que esta caindo.
 *
 * Os gestos sao os tres do projeto:
 *  - arrastar na horizontal move a peca coluna a coluna;
 *  - segurar acelera a queda;
 *  - tocar rapidinho gira a peca.
 */
@Composable
fun TetrisBoard(
    snapshot: GameSnapshot,
    showGhost: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onHorizontalStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "tabuleiro")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "pulso",
    )

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val cellPx = with(density) {
            min(maxWidth.toPx() / COLS, maxHeight.toPx() / ROWS)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cellPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        onPressStart()
                        var accumulated = 0f
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) break
                                val dx = change.positionChange().x
                                if (dx != 0f) {
                                    accumulated += dx
                                    while (accumulated >= cellPx) {
                                        onHorizontalStep(1)
                                        accumulated -= cellPx
                                    }
                                    while (accumulated <= -cellPx) {
                                        onHorizontalStep(-1)
                                        accumulated += cellPx
                                    }
                                    change.consume()
                                }
                            }
                        } finally {
                            onPressEnd()
                        }
                    }
                },
        ) {
            val boardWidth = cellPx * COLS
            val boardHeight = cellPx * ROWS
            val originX = (size.width - boardWidth) / 2f
            val originY = (size.height - boardHeight) / 2f

            drawArena(originX, originY, boardWidth, boardHeight, cellPx)
            drawShadowSide(snapshot, originX, originY, cellPx, pulse)
            drawLockedCells(snapshot, originX, originY, cellPx)
            drawClearingRows(snapshot, originX, originY, boardWidth, cellPx, pulse)
            if (showGhost) drawGhost(snapshot, originX, originY, cellPx)
            drawActivePiece(snapshot, originX, originY, cellPx, pulse)
            drawFrame(originX, originY, boardWidth, boardHeight)
        }
    }
}

// ------------------------------------------------------------------ camadas

private fun DrawScope.drawArena(
    originX: Float,
    originY: Float,
    width: Float,
    height: Float,
    cell: Float,
) {
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xF20A0320), Color(0xF2160636)),
            startY = originY,
            endY = originY + height,
        ),
        topLeft = Offset(originX, originY),
        size = Size(width, height),
        cornerRadius = CornerRadius(cell * 0.4f),
    )
    for (column in 1 until COLS) {
        val x = originX + column * cell
        drawLine(
            color = NeonPurple.copy(alpha = 0.13f),
            start = Offset(x, originY),
            end = Offset(x, originY + height),
            strokeWidth = 1f,
        )
    }
    for (row in 1 until ROWS) {
        val y = originY + row * cell
        drawLine(
            color = NeonPurple.copy(alpha = 0.10f),
            start = Offset(originX, y),
            end = Offset(originX + width, y),
            strokeWidth = 1f,
        )
    }
}

/**
 * O lado sombrio: tudo o que esta empilhado embaixo aparece espelhado em cima,
 * em blocos quase pretos. Nao colide com nada -- so rouba a visibilidade do
 * jogador, e fica mais denso conforme o nivel sobe.
 */
private fun DrawScope.drawShadowSide(
    snapshot: GameSnapshot,
    originX: Float,
    originY: Float,
    cell: Float,
    pulse: Float,
) {
    val breathing = 0.75f + 0.25f * abs(sin(pulse * 2f * Math.PI.toFloat()))
    for (row in 0 until ROWS) {
        for (column in 0 until COLS) {
            if (snapshot.shadowCodeAt(row, column) == 0) continue
            val x = originX + column * cell
            val y = originY + row * cell
            val inset = cell * 0.04f
            val topLeft = Offset(x + inset, y + inset)
            val boxSize = Size(cell - inset * 2, cell - inset * 2)
            val radius = CornerRadius(cell * 0.2f)

            drawRoundRect(
                color = ShadowBlock.copy(alpha = snapshot.shadowStrength),
                topLeft = topLeft,
                size = boxSize,
                cornerRadius = radius,
            )
            drawRoundRect(
                color = ShadowEdge.copy(alpha = snapshot.shadowStrength * 0.85f * breathing),
                topLeft = topLeft,
                size = boxSize,
                cornerRadius = radius,
                style = Stroke(width = cell * 0.07f),
            )
            drawRoundRect(
                color = NeonMagenta.copy(alpha = 0.10f * breathing),
                topLeft = topLeft,
                size = boxSize,
                cornerRadius = radius,
                style = Stroke(width = cell * 0.03f),
            )
        }
    }
}

private fun DrawScope.drawLockedCells(
    snapshot: GameSnapshot,
    originX: Float,
    originY: Float,
    cell: Float,
) {
    for (row in 0 until ROWS) {
        for (column in 0 until COLS) {
            val code = snapshot.codeAt(row, column)
            if (code == 0) continue
            drawBlock(
                x = originX + column * cell,
                y = originY + row * cell,
                cell = cell,
                color = colorForCode(code),
            )
        }
    }
}

private fun DrawScope.drawClearingRows(
    snapshot: GameSnapshot,
    originX: Float,
    originY: Float,
    width: Float,
    cell: Float,
    pulse: Float,
) {
    if (snapshot.clearingRows.isEmpty()) return
    val flash = 0.45f + 0.55f * abs(sin(pulse * 8f * Math.PI.toFloat()))
    snapshot.clearingRows.forEach { row ->
        drawRect(
            color = Color.White.copy(alpha = flash),
            topLeft = Offset(originX, originY + row * cell),
            size = Size(width, cell),
        )
    }
}

private fun DrawScope.drawGhost(
    snapshot: GameSnapshot,
    originX: Float,
    originY: Float,
    cell: Float,
) {
    val piece = snapshot.active ?: return
    if (snapshot.ghostY == piece.y) return
    val color = piece.type.neonColor
    for (offset in piece.type.cells(piece.rotation)) {
        val column = piece.x + offset[0]
        val row = snapshot.ghostY + offset[1]
        if (row < 0 || row >= ROWS || column < 0 || column >= COLS) continue
        val inset = cell * 0.14f
        drawRoundRect(
            color = color.copy(alpha = 0.28f),
            topLeft = Offset(originX + column * cell + inset, originY + row * cell + inset),
            size = Size(cell - inset * 2, cell - inset * 2),
            cornerRadius = CornerRadius(cell * 0.18f),
            style = Stroke(width = cell * 0.07f),
        )
    }
}

private fun DrawScope.drawActivePiece(
    snapshot: GameSnapshot,
    originX: Float,
    originY: Float,
    cell: Float,
    pulse: Float,
) {
    val piece = snapshot.active ?: return
    val glow = 0.8f + 0.2f * abs(sin(pulse * 2f * Math.PI.toFloat()))
    for (offset in piece.type.cells(piece.rotation)) {
        val column = piece.x + offset[0]
        val row = piece.y + offset[1]
        if (row < 0 || row >= ROWS || column < 0 || column >= COLS) continue
        drawBlock(
            x = originX + column * cell,
            y = originY + row * cell,
            cell = cell,
            color = piece.type.neonColor,
            haloScale = if (snapshot.softDropping) 1.5f * glow else glow,
        )
    }
}

private fun DrawScope.drawFrame(
    originX: Float,
    originY: Float,
    width: Float,
    height: Float,
) {
    val radius = CornerRadius(width * 0.035f)
    drawRoundRect(
        color = NeonCyan.copy(alpha = 0.16f),
        topLeft = Offset(originX - 4f, originY - 4f),
        size = Size(width + 8f, height + 8f),
        cornerRadius = radius,
        style = Stroke(width = 9f),
    )
    drawRoundRect(
        color = NeonCyan.copy(alpha = 0.85f),
        topLeft = Offset(originX, originY),
        size = Size(width, height),
        cornerRadius = radius,
        style = Stroke(width = 2.5f),
    )
}

// -------------------------------------------------------------------- blocos

/** Bloco de neon: halo, corpo em degrade, contorno claro e reflexo no topo. */
internal fun DrawScope.drawBlock(
    x: Float,
    y: Float,
    cell: Float,
    color: Color,
    alpha: Float = 1f,
    haloScale: Float = 1f,
) {
    val inset = cell * 0.06f
    val topLeft = Offset(x + inset, y + inset)
    val boxSize = Size(cell - inset * 2, cell - inset * 2)
    val radius = CornerRadius(cell * 0.22f)

    drawRoundRect(
        color = color.copy(alpha = 0.22f * alpha * haloScale),
        topLeft = Offset(x - cell * 0.05f, y - cell * 0.05f),
        size = Size(cell * 1.1f, cell * 1.1f),
        cornerRadius = CornerRadius(cell * 0.3f),
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0.5f * alpha)),
            start = topLeft,
            end = Offset(topLeft.x + boxSize.width, topLeft.y + boxSize.height),
        ),
        topLeft = topLeft,
        size = boxSize,
        cornerRadius = radius,
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.5f * alpha),
        topLeft = topLeft,
        size = boxSize,
        cornerRadius = radius,
        style = Stroke(width = cell * 0.06f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.22f * alpha),
        topLeft = Offset(x + cell * 0.2f, y + cell * 0.17f),
        size = Size(cell * 0.6f, cell * 0.14f),
        cornerRadius = CornerRadius(cell * 0.07f),
    )
}

/**
 * Miniatura de uma peca, usada no painel "proximas pecas".
 */
@Composable
fun PiecePreview(
    type: TetrominoType?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (type == null) return@Canvas
        val cells = type.cells(0)
        val minX = cells.minOf { it[0] }
        val maxX = cells.maxOf { it[0] }
        val minY = cells.minOf { it[1] }
        val maxY = cells.maxOf { it[1] }
        val wide = maxX - minX + 1
        val tall = maxY - minY + 1
        val cell = min(size.width / (wide + 0.6f), size.height / (tall + 0.6f))
        val offsetX = (size.width - cell * wide) / 2f
        val offsetY = (size.height - cell * tall) / 2f

        cells.forEach { offset ->
            drawBlock(
                x = offsetX + (offset[0] - minX) * cell,
                y = offsetY + (offset[1] - minY) * cell,
                cell = cell,
                color = type.neonColor,
            )
        }
    }
}
