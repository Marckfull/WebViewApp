package com.neonsombra.game.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neonsombra.game.ui.components.NeonBackground
import com.neonsombra.game.ui.components.NeonButton
import com.neonsombra.game.ui.components.NeonPanel
import com.neonsombra.game.ui.components.NeonText
import com.neonsombra.game.ui.theme.NeonCyan
import com.neonsombra.game.ui.theme.NeonLime
import com.neonsombra.game.ui.theme.NeonMagenta
import com.neonsombra.game.ui.theme.NeonPurple
import com.neonsombra.game.ui.theme.NeonTextMuted
import com.neonsombra.game.ui.theme.NeonTextPrimary
import com.neonsombra.game.ui.theme.NeonYellow
import com.neonsombra.game.ui.theme.ShadowBlock
import com.neonsombra.game.ui.theme.ShadowEdge
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.launch

private data class TutorialPage(
    val title: String,
    val body: String,
    val accent: Color,
    val illustration: Illustration,
)

private enum class Illustration { OBJETIVO, CONTROLES, SOMBRA, PONTOS }

private val pages = listOf(
    TutorialPage(
        title = "O OBJETIVO",
        body = "As pecas caem do alto do tabuleiro. Encaixe uma na outra ate " +
            "preencher uma linha inteira, de ponta a ponta. Linha cheia some e " +
            "vira ponto. Se a pilha chegar no topo, a partida acaba.",
        accent = NeonCyan,
        illustration = Illustration.OBJETIVO,
    ),
    TutorialPage(
        title = "OS CONTROLES",
        body = "ARRASTE o dedo para os lados: a peca anda coluna a coluna.\n\n" +
            "SEGURE o dedo na tela: a peca desce bem mais rapido e voce " +
            "ainda ganha um ponto por linha descida.\n\n" +
            "TOQUE rapidinho: a peca gira. Sao tres posicoes novas antes de " +
            "voltar ao formato original.",
        accent = NeonLime,
        illustration = Illustration.CONTROLES,
    ),
    TutorialPage(
        title = "O LADO SOMBRIO",
        body = "Aqui esta o diferencial do NEON SOMBRA: tudo o que voce empilha " +
            "embaixo ganha um espelho preto na parte de cima do tabuleiro.\n\n" +
            "A sombra nao bate em nada, nao empurra nada -- ela so atrapalha a " +
            "sua visao. Quanto mais alta a pilha, mais escuro fica o caminho " +
            "das pecas novas. Jogar limpo e a unica forma de enxergar bem.",
        accent = NeonMagenta,
        illustration = Illustration.SOMBRA,
    ),
    TutorialPage(
        title = "PONTOS E NIVEIS",
        body = "1 linha vale 100, duas valem 300, tres valem 500 e quatro de uma " +
            "vez valem 800 -- tudo multiplicado pelo nivel.\n\n" +
            "A cada 10 linhas voce sobe de nivel: as pecas caem mais rapido e a " +
            "sombra fica mais densa. Seu recorde fica guardado no aparelho.",
        accent = NeonYellow,
        illustration = Illustration.PONTOS,
    ),
)

/**
 * Tutorial mostrado na primeira vez que o jogo abre (e sempre que o jogador
 * pedir "como jogar" no menu).
 */
@Composable
fun TutorialScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    NeonBackground(modifier = Modifier.fillMaxSize(), showSkyline = false, intensity = 0.75f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp),
        ) {
            NeonText(
                text = "COMO JOGAR",
                color = NeonPurple,
                glowRadius = 20f,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    NeonPanel(
                        accent = page.accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = 16.dp,
                    ) {
                        NeonText(
                            text = page.title,
                            color = page.accent,
                            glowRadius = 18f,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(14.dp))
                        TutorialArt(
                            illustration = page.illustration,
                            accent = page.accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = page.body,
                            color = NeonTextPrimary.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                pages.indices.forEach { index ->
                    val selected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .size(if (selected) 11.dp else 7.dp)
                            .background(
                                color = if (selected) NeonCyan else NeonTextMuted.copy(alpha = 0.4f),
                                shape = CircleShape,
                            ),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NeonButton(
                    text = "PULAR",
                    onClick = onFinished,
                    accent = NeonTextMuted,
                    modifier = Modifier.weight(1f),
                )
                NeonButton(
                    text = if (isLast) "COMECAR" else "PROXIMO",
                    accent = if (isLast) NeonLime else NeonCyan,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (isLast) {
                            onFinished()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                )
            }
        }
    }
}

// ------------------------------------------------------------------ desenhos

@Composable
private fun TutorialArt(
    illustration: Illustration,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "arte")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "tempo",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (illustration) {
            Illustration.CONTROLES -> ControlsArt(time)
            else -> Canvas(modifier = Modifier.fillMaxSize()) {
                when (illustration) {
                    Illustration.OBJETIVO -> drawObjectiveArt(time, accent)
                    Illustration.SOMBRA -> drawShadowArt(time)
                    Illustration.PONTOS -> drawScoreArt(time, accent)
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun ControlsArt(time: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ControlCard("ARRASTAR", "< >", NeonCyan, Modifier.weight(1f), time)
        ControlCard("SEGURAR", "v", NeonMagenta, Modifier.weight(1f), time)
        ControlCard("TOCAR", "@", NeonLime, Modifier.weight(1f), time)
    }
}

@Composable
private fun ControlCard(
    title: String,
    glyph: String,
    accent: Color,
    modifier: Modifier = Modifier,
    time: Float,
) {
    val pulse = 0.6f + 0.4f * abs(sin(time * 2f * Math.PI.toFloat()))
    NeonPanel(accent = accent, modifier = modifier, contentPadding = 8.dp) {
        NeonText(
            text = glyph,
            color = accent,
            glowRadius = 18f * pulse,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 22.sp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            color = NeonTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
        )
    }
}

private fun DrawScope.drawObjectiveArt(time: Float, accent: Color) {
    val columns = 8
    val rows = 5
    val cell = minOf(size.width / columns, size.height / rows)
    val originX = (size.width - cell * columns) / 2f
    val originY = (size.height - cell * rows) / 2f

    drawMiniGrid(originX, originY, cell, columns, rows)

    // Linha quase completa, com a peca que vai fechar tudo descendo.
    for (column in 0 until columns - 1) {
        drawMiniBlock(originX + column * cell, originY + (rows - 1) * cell, cell, accent)
    }
    val fall = (time * 3f).coerceAtMost(1f)
    val y = originY + (rows - 1) * cell * fall
    drawMiniBlock(originX + (columns - 1) * cell, y, cell, NeonYellow)

    if (fall >= 1f) {
        drawRect(
            color = Color.White.copy(alpha = 0.5f * abs(sin(time * 12f))),
            topLeft = Offset(originX, originY + (rows - 1) * cell),
            size = Size(cell * columns, cell),
        )
    }
}

private fun DrawScope.drawShadowArt(time: Float) {
    val columns = 8
    val rows = 6
    val cell = minOf(size.width / columns, size.height / rows)
    val originX = (size.width - cell * columns) / 2f
    val originY = (size.height - cell * rows) / 2f

    drawMiniGrid(originX, originY, cell, columns, rows)

    // Pilha embaixo...
    val stack = listOf(0 to 5, 1 to 5, 2 to 5, 3 to 5, 5 to 5, 6 to 5, 2 to 4, 3 to 4, 6 to 4)
    stack.forEach { (column, row) ->
        drawMiniBlock(originX + column * cell, originY + row * cell, cell, NeonCyan)
    }

    // ...e o espelho preto em cima, respirando.
    val breathing = 0.6f + 0.4f * abs(sin(time * 2f * Math.PI.toFloat()))
    stack.forEach { (column, row) ->
        val mirrored = rows - 1 - row
        val x = originX + column * cell
        val y = originY + mirrored * cell
        drawRoundRect(
            color = ShadowBlock.copy(alpha = 0.9f),
            topLeft = Offset(x + 1f, y + 1f),
            size = Size(cell - 2f, cell - 2f),
            cornerRadius = CornerRadius(cell * 0.2f),
        )
        drawRoundRect(
            color = ShadowEdge.copy(alpha = breathing),
            topLeft = Offset(x + 1f, y + 1f),
            size = Size(cell - 2f, cell - 2f),
            cornerRadius = CornerRadius(cell * 0.2f),
            style = Stroke(width = cell * 0.08f),
        )
    }

    // Seta ligando o que esta embaixo ao que aparece em cima.
    val arrowX = originX + cell * columns + cell * 0.2f
    if (arrowX < size.width) {
        drawLine(
            color = NeonMagenta.copy(alpha = 0.7f),
            start = Offset(arrowX, originY + cell * rows),
            end = Offset(arrowX, originY),
            strokeWidth = 2f,
        )
    }
}

private fun DrawScope.drawScoreArt(time: Float, accent: Color) {
    val bars = 4
    val gap = size.width * 0.04f
    val barWidth = (size.width - gap * (bars - 1)) / bars
    val values = floatArrayOf(0.3f, 0.5f, 0.7f, 1f)
    values.forEachIndexed { index, value ->
        val grow = ((time * 2f) - index * 0.15f).coerceIn(0f, 1f)
        val height = size.height * value * grow
        val x = index * (barWidth + gap)
        drawRoundRect(
            color = accent.copy(alpha = 0.22f),
            topLeft = Offset(x, size.height - height),
            size = Size(barWidth, height),
            cornerRadius = CornerRadius(barWidth * 0.2f),
        )
        drawRoundRect(
            color = accent,
            topLeft = Offset(x, size.height - height),
            size = Size(barWidth, height),
            cornerRadius = CornerRadius(barWidth * 0.2f),
            style = Stroke(width = 2f),
        )
    }
}

private fun DrawScope.drawMiniGrid(
    originX: Float,
    originY: Float,
    cell: Float,
    columns: Int,
    rows: Int,
) {
    drawRoundRect(
        color = Color(0x800A0320),
        topLeft = Offset(originX, originY),
        size = Size(cell * columns, cell * rows),
        cornerRadius = CornerRadius(cell * 0.3f),
    )
    for (column in 0..columns) {
        drawLine(
            color = NeonPurple.copy(alpha = 0.18f),
            start = Offset(originX + column * cell, originY),
            end = Offset(originX + column * cell, originY + cell * rows),
            strokeWidth = 1f,
        )
    }
    for (row in 0..rows) {
        drawLine(
            color = NeonPurple.copy(alpha = 0.18f),
            start = Offset(originX, originY + row * cell),
            end = Offset(originX + cell * columns, originY + row * cell),
            strokeWidth = 1f,
        )
    }
}

private fun DrawScope.drawMiniBlock(x: Float, y: Float, cell: Float, color: Color) {
    drawRoundRect(
        color = color.copy(alpha = 0.85f),
        topLeft = Offset(x + 1.5f, y + 1.5f),
        size = Size(cell - 3f, cell - 3f),
        cornerRadius = CornerRadius(cell * 0.22f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.45f),
        topLeft = Offset(x + 1.5f, y + 1.5f),
        size = Size(cell - 3f, cell - 3f),
        cornerRadius = CornerRadius(cell * 0.22f),
        style = Stroke(width = cell * 0.07f),
    )
}
