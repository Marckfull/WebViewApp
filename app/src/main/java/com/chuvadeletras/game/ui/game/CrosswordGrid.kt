package com.chuvadeletras.game.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chuvadeletras.game.domain.model.CellState
import com.chuvadeletras.game.domain.model.CellStatus
import com.chuvadeletras.game.domain.model.GameState
import com.chuvadeletras.game.domain.model.GridPos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CrosswordGrid(
    state: GameState,
    modifier: Modifier = Modifier,
    onCellTap: (GridPos) -> Unit,
    highlightLocked: Boolean = false
) {
    val puzzle = state.puzzle
    val activeCells = remember(state.activeEntryId, puzzle) {
        state.activeEntry?.cells?.toSet() ?: emptySet()
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val spacing = 3.dp
        val byWidth = (maxWidth - spacing * (puzzle.cols - 1)) / puzzle.cols
        val byHeight = (maxHeight - spacing * (puzzle.rows - 1)) / puzzle.rows
        val cellSize: Dp = minOf(byWidth, byHeight).coerceIn(18.dp, 46.dp)

        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            for (row in 0 until puzzle.rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    for (col in 0 until puzzle.cols) {
                        val pos = GridPos(row, col)
                        val cell = state.cells[pos]
                        if (cell == null) {
                            // Parede: espaço vazio que mantém o alinhamento da grade.
                            Box(modifier = Modifier.size(cellSize))
                        } else {
                            CrosswordCell(
                                cell = cell,
                                size = cellSize,
                                number = puzzle.startNumbers[pos],
                                selected = state.selected == pos,
                                inActiveWord = pos in activeCells,
                                pulseLocked = highlightLocked,
                                onTap = { onCellTap(pos) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CrosswordCell(
    cell: CellState,
    size: Dp,
    number: Int?,
    selected: Boolean,
    inActiveWord: Boolean,
    pulseLocked: Boolean,
    onTap: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    val background by animateColorAsState(
        targetValue = when {
            cell.status == CellStatus.LOCKED -> scheme.error.copy(alpha = 0.22f)
            cell.status == CellStatus.WRONG -> scheme.error.copy(alpha = 0.35f)
            selected -> scheme.primary
            cell.status == CellStatus.CORRECT && cell.revealed -> scheme.tertiary.copy(alpha = 0.35f)
            cell.status == CellStatus.CORRECT -> scheme.primary.copy(alpha = 0.20f)
            inActiveWord -> scheme.primaryContainer.copy(alpha = 0.45f)
            else -> scheme.surface
        },
        animationSpec = tween(220),
        label = "cellBg"
    )

    val letterColor = when {
        selected -> scheme.onPrimary
        cell.status == CellStatus.WRONG -> scheme.error
        else -> scheme.onSurface
    }

    val selectionScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
        label = "cellScale"
    )

    // Sacudida ao errar: dispara toda vez que a casa entra em WRONG.
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(cell.status, cell.input) {
        if (cell.status == CellStatus.WRONG) {
            shakeAnim.snapTo(0f)
            shakeAnim.animateTo(1f, tween(420))
        }
    }
    val shake = sin(shakeAnim.value * 6f * Math.PI.toFloat()) * (1f - shakeAnim.value) * 9f

    val lockedPulse by animateFloatAsState(
        targetValue = if (pulseLocked && cell.status == CellStatus.LOCKED) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.3f),
        label = "lockPulse"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(selectionScale * lockedPulse)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(shake.roundToInt(), 0)
                }
            }
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .border(
                BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = when {
                        selected -> scheme.primary
                        cell.status == CellStatus.LOCKED -> scheme.error.copy(alpha = 0.6f)
                        else -> scheme.outline.copy(alpha = 0.5f)
                    }
                ),
                RoundedCornerShape(6.dp)
            )
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        if (number != null) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontSize = (size.value * 0.24f).coerceIn(7f, 11f).sp,
                color = letterColor.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 1.dp)
            )
        }

        if (cell.status == CellStatus.LOCKED) {
            Text(
                text = "🔒",
                fontSize = (size.value * 0.42f).coerceIn(10f, 20f).sp,
                textAlign = TextAlign.Center
            )
        } else {
            AnimatedContent(
                targetState = cell.input,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = 0.4f, stiffness = 500f)) + fadeIn())
                        .togetherWith(scaleOut(tween(120)) + fadeOut(tween(120)))
                },
                label = "letter"
            ) { letter ->
                Text(
                    text = letter?.toString() ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = (size.value * 0.52f).coerceIn(11f, 24f).sp,
                    fontWeight = FontWeight.Black,
                    color = letterColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
