package com.prisma.fusao.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberInfiniteTransition
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prisma.fusao.core.Campaign
import com.prisma.fusao.data.PlayerState
import com.prisma.fusao.ui.components.PrismaButton
import com.prisma.fusao.ui.components.PrismaCard
import com.prisma.fusao.ui.components.PrismaTextButton
import com.prisma.fusao.ui.components.ProgressBar
import com.prisma.fusao.ui.components.StarRow
import com.prisma.fusao.ui.components.StarfieldBackground
import com.prisma.fusao.ui.components.VSpace
import com.prisma.fusao.ui.game.objectiveText
import com.prisma.fusao.ui.theme.BrancoGelo
import com.prisma.fusao.ui.theme.DouradoEstrela
import com.prisma.fusao.ui.theme.LilasClaro
import com.prisma.fusao.ui.theme.RoxoBorda
import com.prisma.fusao.ui.theme.VermelhoAlerta
import kotlin.math.sin

/**
 * Mapa da campanha: uma trilha serpenteando por 150 fases, agrupadas em mundos.
 * A fase atual pulsa para o olho encontrá-la sem procurar.
 */
@Composable
fun HomeScreen(
    player: PlayerState,
    onPlayLevel: (Int) -> Unit,
    onOpenStore: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMissions: () -> Unit,
    onOpenDailyReward: () -> Unit,
    canClaimDaily: Boolean,
    millisToNextLife: Long?,
) {
    var selectedLevel by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    // Abre o mapa já na fase que o jogador vai jogar agora.
    LaunchedEffect(player.highestUnlocked) {
        val target = (player.highestUnlocked - 2).coerceAtLeast(0)
        listState.scrollToItem(target)
    }

    StarfieldBackground {
        Column(Modifier.fillMaxSize()) {
            TopHud(
                player = player,
                millisToNextLife = millisToNextLife,
                onOpenStore = onOpenStore,
                onOpenSettings = onOpenSettings,
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(Campaign.levels, key = { it.index }) { level ->
                    val world = Campaign.worldOf(level.index)
                    if ((level.index - 1) % 25 == 0) {
                        WorldHeader(Campaign.worldNames[world], level.index)
                    }
                    LevelNode(
                        index = level.index,
                        stars = player.stars(level.index),
                        unlocked = level.index <= player.highestUnlocked,
                        isCurrent = level.index == player.highestUnlocked,
                        boss = Campaign.isBoss(level.index),
                        onClick = { selectedLevel = level.index },
                    )
                }
            }

            BottomBar(
                canClaimDaily = canClaimDaily,
                missionsReady = player.missions.any { it.complete && !it.claimed },
                onOpenMissions = onOpenMissions,
                onOpenDailyReward = onOpenDailyReward,
                onOpenStore = onOpenStore,
            )
        }
    }

    selectedLevel?.let { index ->
        LevelDetailDialog(
            index = index,
            stars = player.stars(index),
            bestScore = player.bestScore(index),
            onPlay = {
                selectedLevel = null
                onPlayLevel(index)
            },
            onDismiss = { selectedLevel = null },
        )
    }
}

@Composable
private fun TopHud(
    player: PlayerState,
    millisToNextLife: Long?,
    onOpenStore: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.28f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Pill(
                text = livesLabel(player, millisToNextLife),
                icon = "♥",
                iconColor = VermelhoAlerta,
                onClick = onOpenStore,
            )
            Pill(
                text = "%,d".format(player.coins),
                icon = "◈",
                iconColor = DouradoEstrela,
                onClick = onOpenStore,
            )
            Pill(text = "${player.totalStars}", icon = "★", iconColor = DouradoEstrela)
            Pill(text = "⚙", icon = "", iconColor = LilasClaro, onClick = onOpenSettings)
        }
        VSpace(8)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Nível ${player.playerLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = LilasClaro,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            ProgressBar(
                progress = player.xpIntoLevel.toFloat() / player.xpForNextLevel.coerceAtLeast(1),
                modifier = Modifier.weight(1f),
                height = 7.dp,
            )
        }
    }
}

private fun livesLabel(player: PlayerState, millisToNextLife: Long?): String {
    val now = System.currentTimeMillis()
    if (player.hasInfiniteLives(now)) {
        val minutes = ((player.infiniteLivesUntil - now) / 60_000L).coerceAtLeast(0)
        return "∞ ${minutes}min"
    }
    if (player.lives >= PlayerState.MAX_LIVES || millisToNextLife == null) {
        return "${player.lives}/${PlayerState.MAX_LIVES}"
    }
    val totalSeconds = millisToNextLife / 1000
    return "${player.lives}  %d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun Pill(
    text: String,
    icon: String,
    iconColor: Color,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.10f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon.isNotEmpty()) {
            Text(icon, color = iconColor, fontSize = 15.sp)
            androidx.compose.foundation.layout.Spacer(Modifier.width(5.dp))
        }
        Text(text, color = BrancoGelo, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WorldHeader(name: String, firstLevel: Int) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RoxoBorda),
        )
        VSpace(10)
        Text(name.uppercase(), style = MaterialTheme.typography.titleLarge, color = DouradoEstrela)
        Text(
            "fases $firstLevel a ${(firstLevel + 24).coerceAtMost(Campaign.LEVEL_COUNT)}",
            style = MaterialTheme.typography.bodySmall,
            color = LilasClaro,
        )
        VSpace(6)
    }
}

@Composable
private fun LevelNode(
    index: Int,
    stars: Int,
    unlocked: Boolean,
    isCurrent: Boolean,
    boss: Boolean,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "no")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "pulso",
    )
    // A trilha serpenteia: o deslocamento é uma senoide do índice da fase.
    val offsetX = (sin(index * 0.85f) * 78f).dp

    Box(
        Modifier
            .fillMaxWidth()
            .height(84.dp)
            .offset(x = offsetX),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(if (boss) 68.dp else 58.dp)
                    .scale(if (isCurrent) pulse else 1f)
                    .clip(CircleShape)
                    .background(
                        when {
                            !unlocked -> Brush.verticalGradient(
                                listOf(Color(0xFF2A2340), Color(0xFF1B1730)),
                            )
                            boss -> Brush.verticalGradient(
                                listOf(Color(0xFFFF9F45), Color(0xFFC2410C)),
                            )
                            else -> Brush.verticalGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFF5B21B6)),
                            )
                        },
                    )
                    .border(
                        width = if (isCurrent) 3.dp else 1.dp,
                        color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.2f),
                        shape = CircleShape,
                    )
                    .clickable(enabled = unlocked, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (unlocked) "$index" else "🔒",
                    color = if (unlocked) Color.White else LilasClaro.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Black,
                    fontSize = if (boss) 21.sp else 18.sp,
                )
            }
            if (unlocked && stars > 0) {
                VSpace(3)
                StarRow(stars, size = 13.dp)
            }
        }
    }
}

@Composable
private fun BottomBar(
    canClaimDaily: Boolean,
    missionsReady: Boolean,
    onOpenMissions: () -> Unit,
    onOpenDailyReward: () -> Unit,
    onOpenStore: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.34f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BottomAction("Prêmio\ndiário", canClaimDaily, Modifier.weight(1f), onOpenDailyReward)
        BottomAction("Missões", missionsReady, Modifier.weight(1f), onOpenMissions)
        BottomAction("Loja", false, Modifier.weight(1f), onOpenStore)
    }
}

@Composable
private fun BottomAction(
    label: String,
    highlighted: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (highlighted) Brush.verticalGradient(listOf(Color(0xFFFFC53D), Color(0xFFE08900)))
                else Brush.verticalGradient(listOf(Color(0xFF3B2270), Color(0xFF2A1B52)))
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (highlighted) Color(0xFF2A1A00) else BrancoGelo,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun LevelDetailDialog(
    index: Int,
    stars: Int,
    bestScore: Int,
    onPlay: () -> Unit,
    onDismiss: () -> Unit,
) {
    val level = Campaign.level(index)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.padding(28.dp)) {
            PrismaCard(Modifier.fillMaxWidth()) {
                Text(
                    if (Campaign.isBoss(index)) "Fase $index — Desafio" else "Fase $index",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BrancoGelo,
                )
                VSpace(8)
                StarRow(stars, size = 26.dp)
                VSpace(14)
                Text("Objetivos", style = MaterialTheme.typography.titleMedium, color = LilasClaro)
                VSpace(4)
                level.objectives.forEach {
                    Text(
                        "•  ${objectiveText(it)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrancoGelo,
                    )
                }
                VSpace(12)
                Text(
                    "Jogadas: ${level.moves}" +
                        if (bestScore > 0) "   •   Recorde: ${"%,d".format(bestScore)}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = LilasClaro,
                )
                VSpace(18)
                PrismaButton("Jogar", Modifier.fillMaxWidth(), pulsing = true, onClick = onPlay)
                VSpace(6)
                PrismaTextButton("Voltar", Modifier.fillMaxWidth(), onClick = onDismiss)
            }
        }
    }
}
