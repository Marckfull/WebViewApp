package com.chuvadeletras.game.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chuvadeletras.game.domain.model.PowerUp
import com.chuvadeletras.game.domain.model.TrayTile

/**
 * A bandeja: as letras da rodada. Entram caindo de cima (é chuva, afinal) e
 * saem encolhendo quando são usadas.
 */
@Composable
fun LetterTray(
    tiles: List<TrayTile>,
    movesLeft: Int,
    modifier: Modifier = Modifier,
    pendingPowerUp: PowerUp? = null,
    enabled: Boolean = true,
    onTileTap: (Long) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when {
                    pendingPowerUp == PowerUp.CONGELAR -> "❄️ Toque na letra que quer segurar"
                    movesLeft <= 0 -> "Rodada encerrada — vem chuva nova"
                    else -> "Sua bandeja"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MovesIndicator(movesLeft = movesLeft)
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
        ) {
            items(tiles.size, key = { tiles[it].id }) { index ->
                val tile = tiles[index]
                LetterTile(
                    tile = tile,
                    enabled = enabled && (movesLeft > 0 || pendingPowerUp == PowerUp.CONGELAR),
                    highlighted = pendingPowerUp == PowerUp.CONGELAR,
                    modifier = Modifier.animateItem(
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    onTap = { onTileTap(tile.id) }
                )
            }
        }
    }
}

@Composable
private fun LetterTile(
    tile: TrayTile,
    enabled: Boolean,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val pressScale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "tileScale"
    )

    // Letra inútil (nenhuma casa aberta precisa dela) fica apagada de propósito:
    // é uma dica visual de que dá para deixar essa evaporar sem dó.
    val contentAlpha = when {
        !enabled -> 0.45f
        !tile.useful -> 0.55f
        else -> 1f
    }

    // Entrada: a peça cai de cima e quica ao pousar.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }

    Box(
        modifier = modifier
            .size(56.dp)
            .graphicsLayer {
                translationY = (1f - appear.value) * -160f
                val entry = 0.6f + 0.4f * appear.value
                scaleX = entry * pressScale
                scaleY = entry * pressScale
                this.alpha = contentAlpha * appear.value.coerceIn(0f, 1f)
            }
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    tile.frozen -> scheme.tertiary.copy(alpha = 0.35f)
                    highlighted -> scheme.secondaryContainer
                    else -> scheme.surfaceVariant
                }
            )
            .border(
                width = if (tile.frozen || highlighted) 2.dp else 1.dp,
                color = when {
                    tile.frozen -> scheme.tertiary
                    highlighted -> scheme.secondary
                    else -> scheme.outline.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled) { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tile.letter.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            color = scheme.onSurface
        )
        if (tile.frozen) {
            Text(
                text = "❄️",
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
            )
        }
    }
}

/** Bolinhas que mostram quantas jogadas ainda restam na rodada. */
@Composable
fun MovesIndicator(movesLeft: Int, modifier: Modifier = Modifier, total: Int = 4) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Jogadas",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        repeat(maxOf(total, movesLeft)) { index ->
            val active = index < movesLeft
            val scale by animateFloatAsState(
                targetValue = if (active) 1f else 0.6f,
                animationSpec = spring(dampingRatio = 0.4f),
                label = "move$index"
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        }
                    )
            )
        }
    }
}

/** Barra de itens do rodapé da partida. */
@Composable
fun PowerUpBar(
    counts: Map<PowerUp, Int>,
    pending: PowerUp?,
    modifier: Modifier = Modifier,
    showTime: Boolean,
    onPowerUp: (PowerUp) -> Unit
) {
    val items = PowerUp.entries.filter { showTime || it != PowerUp.TEMPO }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { powerUp ->
            val count = counts[powerUp] ?: 0
            val selected = pending == powerUp
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        },
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onPowerUp(powerUp) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    Text(powerUp.emoji, fontSize = 20.sp, modifier = Modifier.alpha(if (count > 0) 1f else 0.4f))
                }
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (count > 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
