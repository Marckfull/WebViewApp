package com.formatfrute.game.ui.recipe

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formatfrute.game.core.RecipeBook
import com.formatfrute.game.data.GameRepository
import com.formatfrute.game.ui.components.ChunkyBar
import com.formatfrute.game.ui.components.FruitBackground
import com.formatfrute.game.ui.components.PaperCard
import com.formatfrute.game.ui.components.StatPill
import com.formatfrute.game.ui.theme.Fruta

/**
 * O caminho da feira: as 60 fases em trilha, com as estrelas conquistadas.
 * Abre já na próxima fase a jogar — ninguém precisa procurar onde parou.
 */
@Composable
fun RecipeMapScreen(onPlay: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { GameRepository.get(context) }
    val profile by repo.profile.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        listState.scrollToItem((profile.nextRecipe - 2).coerceAtLeast(0))
    }

    FruitBackground(profile.boardTheme, density = 6) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(3.dp, Fruta.Ink.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⬅", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Modo Receita",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (profile.boardTheme.dark) Color.White else Fruta.Ink,
                    )
                    Text(
                        "${profile.recipeCleared} de ${RecipeBook.TOTAL} fases",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (profile.boardTheme.dark) Color.White.copy(alpha = 0.8f)
                        else Fruta.InkSoft,
                    )
                }
                StatPill("⭐", "${profile.recipeStarTotal}/${RecipeBook.TOTAL * 3}")
            }

            ChunkyBar(
                progress = profile.recipeCleared.toFloat() / RecipeBook.TOTAL,
                color = Fruta.Berry,
                height = 14.dp,
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(RecipeBook.TOTAL) { index ->
                    val number = index + 1
                    LevelCard(
                        number = number,
                        stars = profile.starsOf(number),
                        unlocked = profile.isRecipeUnlocked(number),
                        current = number == profile.nextRecipe,
                        indent = index % 4,
                        onClick = { onPlay(number) },
                    )
                }
                item { Spacer(Modifier.height(28.dp)) }
            }
        }
    }
}

@Composable
private fun LevelCard(
    number: Int,
    stars: Int,
    unlocked: Boolean,
    current: Boolean,
    indent: Int,
    onClick: () -> Unit,
) {
    val recipe = remember(number) { RecipeBook.recipe(number) }
    // a trilha serpenteia: cada fase entra um pouco diferente da anterior
    val offset = listOf(0, 26, 40, 26)[indent].dp

    val glow = if (current) {
        val transition = rememberInfiniteTransition(label = "current")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "cg",
        ).value
    } else {
        1f
    }

    PaperCard(
        Modifier
            .fillMaxWidth()
            .padding(start = offset)
            .graphicsLayer { scaleX = glow; scaleY = glow }
            .clickable(enabled = unlocked, onClick = onClick),
        color = Color.White,
        corner = 22.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    if (unlocked) {
                        Brush.horizontalGradient(
                            listOf(Fruta.Berry, Fruta.Peach)
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(Fruta.InkSoft.copy(alpha = 0.55f), Fruta.InkSoft.copy(alpha = 0.35f))
                        )
                    }
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.9f))
                    .border(3.dp, Fruta.Ink.copy(alpha = 0.6f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                if (unlocked) {
                    Text("$number", style = MaterialTheme.typography.titleLarge, color = Fruta.Ink)
                } else {
                    Text("🔒", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                val detail = buildString {
                    append("${recipe.moves} jogadas • ${recipe.size}×${recipe.size}")
                    if (recipe.iceBlocks > 0) append(" • ❄️")
                    if (recipe.rottenEvery > 0) append(" • 🤢")
                }
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    recipe.orders.forEach { order ->
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.28f))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(order.fruit.art),
                                contentDescription = order.fruit.label,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                "×${order.count}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⭐".repeat(stars).ifEmpty { if (unlocked) "▶" else "" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                if (current) {
                    Text(
                        "JOGAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
