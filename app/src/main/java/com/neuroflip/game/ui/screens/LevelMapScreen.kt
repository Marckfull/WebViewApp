package com.neuroflip.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuroflip.game.data.PlayerState
import com.neuroflip.game.domain.LevelCatalog
import com.neuroflip.game.ui.components.AnimatedNeuroBackground
import com.neuroflip.game.ui.components.StarRow
import com.neuroflip.game.ui.theme.LocalNeuro

@Composable
fun LevelMapScreen(
    player: PlayerState,
    onPlay: (Int) -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalNeuro.current

    Box(Modifier.fillMaxSize()) {
        AnimatedNeuroBackground(Modifier.fillMaxSize(), intensity = 0.7f)

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹",
                    color = palette.primary,
                    fontSize = 30.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 12.dp)
                )
                Column {
                    Text(
                        "CAMPANHA SINAPSE",
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        "${player.totalStars}/${LevelCatalog.TOTAL_LEVELS * 3} estrelas",
                        color = palette.textDim,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items((1..LevelCatalog.TOTAL_LEVELS).toList()) { level ->
                    val unlocked = level <= player.unlockedLevel
                    val stars = player.starsByLevel[level] ?: 0
                    LevelNode(
                        level = level,
                        stars = stars,
                        unlocked = unlocked,
                        onClick = { if (unlocked) onPlay(level) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelNode(level: Int, stars: Int, unlocked: Boolean, onClick: () -> Unit) {
    val palette = LocalNeuro.current
    val config = LevelCatalog.level(level)
    val accent = if (stars == 3) palette.gold else palette.primary

    Column(
        modifier = Modifier
            .aspectRatio(0.82f)
            .alpha(if (unlocked) 1f else 0.35f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (unlocked) {
                    Brush.linearGradient(
                        listOf(palette.surface, accent.copy(alpha = 0.12f))
                    )
                } else {
                    Brush.linearGradient(listOf(palette.surface, palette.surface))
                }
            )
            .border(1.2.dp, accent.copy(alpha = if (unlocked) 0.6f else 0.2f), RoundedCornerShape(16.dp))
            .clickable(enabled = unlocked) { onClick() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (unlocked) "$level" else "🔒",
            color = if (unlocked) palette.textPrimary else palette.textDim,
            fontSize = if (unlocked) 22.sp else 16.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "${config.columns}×${config.rows}",
            color = palette.textDim,
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        StarRow(stars = stars, size = 11)
    }
}
