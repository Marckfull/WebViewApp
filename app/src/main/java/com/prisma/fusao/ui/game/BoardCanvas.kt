package com.prisma.fusao.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.prisma.fusao.core.GemColor
import com.prisma.fusao.core.GemKind
import com.prisma.fusao.core.Pos
import com.prisma.fusao.ui.theme.highlight
import com.prisma.fusao.ui.theme.primary
import com.prisma.fusao.ui.theme.shade
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Desenha o tabuleiro inteiro no Canvas. Nenhuma imagem é usada: as gemas são
 * formas vetoriais com degradê, o que deixa o app leve e escala para qualquer tela.
 */
@Composable
fun BoardCanvas(
    visuals: BoardVisuals,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSwipe: (Pos, Pos) -> Unit,
    onTap: (Pos) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    positionAt(offset, size.width, size.height, visuals)?.let(onTap)
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                var start: Pos? = null
                var accumulated = Offset.Zero
                detectDragGestures(
                    onDragStart = { offset ->
                        start = positionAt(offset, size.width, size.height, visuals)
                        accumulated = Offset.Zero
                    },
                    onDragEnd = { start = null },
                    onDragCancel = { start = null },
                ) { change, drag ->
                    change.consume()
                    val from = start ?: return@detectDragGestures
                    accumulated += drag
                    val cell = cellSizeOf(size.width.toFloat(), size.height.toFloat(), visuals)
                    // Um terço da casa já basta para decidir a direção: o arrasto
                    // responde rápido, como o jogador espera.
                    if (accumulated.getDistance() < cell * 0.33f) return@detectDragGestures
                    val target = if (abs(accumulated.x) > abs(accumulated.y)) {
                        Pos(from.r, from.c + if (accumulated.x > 0) 1 else -1)
                    } else {
                        Pos(from.r + if (accumulated.y > 0) 1 else -1, from.c)
                    }
                    start = null
                    if (target.r in 0 until visuals.rows && target.c in 0 until visuals.cols) {
                        onSwipe(from, target)
                    }
                }
            },
    ) {
        val cell = cellSizeOf(size.width, size.height, visuals)
        val boardWidth = cell * visuals.cols
        val boardHeight = cell * visuals.rows
        val originX = (size.width - boardWidth) / 2f
        val originY = (size.height - boardHeight) / 2f

        // Ler `frame` aqui é o que inscreve o Canvas para redesenhar a cada quadro
        // da simulação: é estado observável do Compose.
        val tick = visuals.frame
        val shakeX = if (visuals.shake > 0f) sin(tick * 0.9f) * visuals.shake * cell * 0.06f else 0f
        val shakeY = if (visuals.shake > 0f) cos(tick * 1.1f) * visuals.shake * cell * 0.05f else 0f

        translate(originX + shakeX, originY + shakeY) {
            drawCells(visuals, cell)
            drawBeams(visuals, cell)
            drawSprites(visuals, cell)
            drawParticles(visuals, cell)
            drawHints(visuals, cell)
            drawFloatingScores(visuals, cell, textMeasurer)
        }
    }
}

private fun cellSizeOf(width: Float, height: Float, visuals: BoardVisuals): Float =
    minOf(width / visuals.cols, height / visuals.rows)

/** Converte um toque em coordenada de casa, ou null se caiu fora do tabuleiro. */
private fun positionAt(offset: Offset, width: Int, height: Int, visuals: BoardVisuals): Pos? {
    val cell = cellSizeOf(width.toFloat(), height.toFloat(), visuals)
    val originX = (width - cell * visuals.cols) / 2f
    val originY = (height - cell * visuals.rows) / 2f
    val c = ((offset.x - originX) / cell).toInt()
    val r = ((offset.y - originY) / cell).toInt()
    if (r !in 0 until visuals.rows || c !in 0 until visuals.cols) return null
    if (!visuals.playable[r][c]) return null
    return Pos(r, c)
}

// --------------------------------------------------------------------- camadas

private fun DrawScope.drawCells(visuals: BoardVisuals, cell: Float) {
    for (r in 0 until visuals.rows) {
        for (c in 0 until visuals.cols) {
            if (!visuals.playable[r][c]) continue
            val topLeft = Offset(c * cell, r * cell)
            val inset = cell * 0.04f
            // Xadrez sutil ajuda o olho a alinhar linhas e colunas.
            val base = if ((r + c) % 2 == 0) Color(0x22FFFFFF) else Color(0x14FFFFFF)
            drawRoundRect(
                color = base,
                topLeft = topLeft + Offset(inset, inset),
                size = Size(cell - inset * 2, cell - inset * 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.16f),
            )
            val layers = visuals.ice[r][c]
            if (layers > 0) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0x99BFE9FF), Color(0x66FFFFFF)),
                        start = topLeft,
                        end = topLeft + Offset(cell, cell),
                    ),
                    topLeft = topLeft + Offset(inset, inset),
                    size = Size(cell - inset * 2, cell - inset * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.16f),
                )
                // Trincas: uma diagonal para cada camada restante.
                repeat(layers) { layer ->
                    val y = topLeft.y + cell * (0.32f + layer * 0.3f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.75f),
                        start = Offset(topLeft.x + cell * 0.16f, y),
                        end = Offset(topLeft.x + cell * 0.84f, y - cell * 0.16f),
                        strokeWidth = cell * 0.045f,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBeams(visuals: BoardVisuals, cell: Float) {
    for (beam in visuals.beams) {
        val progress = 1f - beam.life / beam.maxLife
        val alpha = (1f - progress).coerceIn(0f, 1f)
        for (pos in beam.cells) {
            // As casas mais distantes acendem um pouco depois: dá direção ao feixe.
            val distance = maxOf(abs(pos.r - beam.origin.r), abs(pos.c - beam.origin.c))
            val delay = distance * 0.045f
            val local = ((progress - delay) / (1f - delay).coerceAtLeast(0.05f)).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val radius = cell * (0.16f + 0.34f * local)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(beam.color.copy(alpha = 0.85f * alpha), Color.Transparent),
                    center = Offset((pos.c + 0.5f) * cell, (pos.r + 0.5f) * cell),
                    radius = radius,
                ),
                radius = radius,
                center = Offset((pos.c + 0.5f) * cell, (pos.r + 0.5f) * cell),
            )
        }
    }
}

private fun DrawScope.drawSprites(visuals: BoardVisuals, cell: Float) {
    for (sprite in visuals.sprites.values) {
        if (sprite.alpha <= 0.01f || sprite.scale <= 0.01f) continue
        val center = Offset((sprite.x + 0.5f) * cell, (sprite.y + 0.5f) * cell)
        val radius = cell * 0.40f * sprite.scale
        when (sprite.kind) {
            GemKind.NORMAL -> drawNormalGem(center, radius, sprite.color!!, sprite.alpha)
            GemKind.ESSENCE -> drawEssence(center, radius, sprite.color!!, sprite.alpha, sprite.spin, sprite.glow)
            GemKind.PRISM -> drawPrism(center, radius, sprite.alpha, sprite.spin, sprite.glow)
            GemKind.SUPERNOVA -> drawSupernova(center, radius, sprite.color, sprite.alpha, sprite.spin, sprite.glow)
            GemKind.PRISMOID -> drawPrismoid(center, radius, sprite.alpha, sprite.spin)
            GemKind.STONE -> drawStone(center, radius, sprite.alpha, sprite.hp)
        }
    }
}

private fun DrawScope.drawParticles(visuals: BoardVisuals, cell: Float) {
    for (particle in visuals.particles) {
        val alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f)
        drawCircle(
            color = particle.color.copy(alpha = alpha),
            radius = particle.radius * cell * (0.5f + alpha),
            center = Offset(particle.x * cell, particle.y * cell),
        )
    }
}

private fun DrawScope.drawHints(visuals: BoardVisuals, cell: Float) {
    visuals.selected?.let { pos ->
        drawRoundRect(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(pos.c * cell, pos.r * cell) + Offset(cell * 0.05f, cell * 0.05f),
            size = Size(cell * 0.9f, cell * 0.9f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.18f),
            style = Stroke(width = cell * 0.06f),
        )
    }
    for (pos in visuals.hints) {
        val pulse = 0.55f + 0.45f * abs(sin(visuals.frame * 0.08f))
        drawRoundRect(
            color = Color(0xFFFFD166).copy(alpha = pulse),
            topLeft = Offset(pos.c * cell, pos.r * cell) + Offset(cell * 0.03f, cell * 0.03f),
            size = Size(cell * 0.94f, cell * 0.94f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.2f),
            style = Stroke(width = cell * 0.07f),
        )
    }
}

private fun DrawScope.drawFloatingScores(
    visuals: BoardVisuals,
    cell: Float,
    measurer: TextMeasurer,
) {
    for (score in visuals.floatingScores) {
        val alpha = (score.life / score.maxLife).coerceIn(0f, 1f)
        val layout = measurer.measure(
            score.text,
            TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Black, color = score.color.copy(alpha = alpha)),
        )
        drawText(
            layout,
            topLeft = Offset(
                (score.x + 0.5f) * cell - layout.size.width / 2f,
                (score.y + 0.5f) * cell - layout.size.height / 2f,
            ),
        )
    }
}

// ----------------------------------------------------------------- as peças

private fun hexPath(center: Offset, radius: Float): Path {
    val path = Path()
    for (i in 0 until 6) {
        val angle = PI / 6 + i * PI / 3
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun DrawScope.drawNormalGem(center: Offset, radius: Float, color: GemColor, alpha: Float) {
    val path = hexPath(center, radius)
    drawPath(
        path,
        Brush.radialGradient(
            listOf(color.highlight().copy(alpha = alpha), color.primary().copy(alpha = alpha)),
            center = center - Offset(radius * 0.3f, radius * 0.35f),
            radius = radius * 1.8f,
        ),
    )
    drawPath(path, color.shade().copy(alpha = alpha * 0.85f), style = Stroke(width = radius * 0.13f))
    // Reflexo: é o que faz a peça parecer de vidro e não um adesivo chapado.
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.55f),
        radius = radius * 0.17f,
        center = center - Offset(radius * 0.33f, radius * 0.4f),
    )
}

private fun DrawScope.drawEssence(
    center: Offset,
    radius: Float,
    color: GemColor,
    alpha: Float,
    spin: Float,
    glow: Float,
) {
    val aura = radius * (1.5f + glow * 0.9f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(color.primary().copy(alpha = alpha * (0.4f + glow * 0.5f)), Color.Transparent),
            center = center,
            radius = aura,
        ),
        radius = aura,
        center = center,
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = alpha), color.primary().copy(alpha = alpha)),
            center = center - Offset(radius * 0.22f, radius * 0.26f),
            radius = radius * 1.5f,
        ),
        radius = radius * 0.78f,
        center = center,
    )
    // Três satélites em órbita: sinal visual de que a peça é "instável" e quer fundir.
    repeat(3) { i ->
        val angle = spin + i * 2f * PI.toFloat() / 3f
        val orbit = radius * 1.05f
        drawCircle(
            color = color.highlight().copy(alpha = alpha),
            radius = radius * 0.15f,
            center = center + Offset(cos(angle) * orbit, sin(angle) * orbit),
        )
    }
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.9f),
        radius = radius * 0.78f,
        center = center,
        style = Stroke(width = radius * 0.09f),
    )
}

private fun DrawScope.drawPrism(center: Offset, radius: Float, alpha: Float, spin: Float, glow: Float) {
    val aura = radius * (1.6f + glow)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = alpha * 0.45f), Color.Transparent),
            center = center,
            radius = aura,
        ),
        radius = aura,
        center = center,
    )
    rotate(degrees = spin * 26f, pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x + radius * 0.92f, center.y + radius * 0.72f)
            lineTo(center.x - radius * 0.92f, center.y + radius * 0.72f)
            close()
        }
        drawPath(
            path,
            Brush.linearGradient(
                listOf(
                    Color(0xFFFF6B9D).copy(alpha = alpha),
                    Color(0xFFFFD166).copy(alpha = alpha),
                    Color(0xFF4ADE80).copy(alpha = alpha),
                    Color(0xFF48BFE3).copy(alpha = alpha),
                    Color(0xFFB185FF).copy(alpha = alpha),
                ),
                start = Offset(center.x - radius, center.y - radius),
                end = Offset(center.x + radius, center.y + radius),
            ),
        )
        drawPath(path, Color.White.copy(alpha = alpha * 0.9f), style = Stroke(width = radius * 0.12f))
    }
}

private fun DrawScope.drawSupernova(
    center: Offset,
    radius: Float,
    color: GemColor?,
    alpha: Float,
    spin: Float,
    glow: Float,
) {
    val base = color?.primary() ?: Color.White
    val aura = radius * (1.8f + glow)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(base.copy(alpha = alpha * 0.6f), Color.Transparent),
            center = center,
            radius = aura,
        ),
        radius = aura,
        center = center,
    )
    rotate(degrees = spin * 34f, pivot = center) {
        val path = Path()
        val points = 8
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) radius * 1.18f else radius * 0.46f
            val angle = i * PI / points
            val x = center.x + (r * cos(angle)).toFloat()
            val y = center.y + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(
            path,
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = alpha), base.copy(alpha = alpha)),
                center = center,
                radius = radius * 1.3f,
            ),
        )
    }
    drawCircle(color = Color.White.copy(alpha = alpha), radius = radius * 0.3f, center = center)
}

private fun DrawScope.drawPrismoid(center: Offset, radius: Float, alpha: Float, spin: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFFE49A).copy(alpha = alpha * 0.55f), Color.Transparent),
            center = center,
            radius = radius * 1.7f,
        ),
        radius = radius * 1.7f,
        center = center,
    )
    val path = Path().apply {
        moveTo(center.x, center.y - radius * 1.1f)
        lineTo(center.x + radius * 0.7f, center.y)
        lineTo(center.x, center.y + radius * 1.1f)
        lineTo(center.x - radius * 0.7f, center.y)
        close()
    }
    drawPath(
        path,
        Brush.verticalGradient(
            listOf(Color(0xFFFFF3C4).copy(alpha = alpha), Color(0xFFF0A500).copy(alpha = alpha)),
            startY = center.y - radius,
            endY = center.y + radius,
        ),
    )
    drawPath(path, Color.White.copy(alpha = alpha * 0.8f), style = Stroke(width = radius * 0.1f))
    // Faísca girando, para o olho achar o prismoide no meio do tabuleiro.
    val sparkle = Offset(cos(spin * 2f) * radius * 0.45f, sin(spin * 2f) * radius * 0.45f)
    drawCircle(Color.White.copy(alpha = alpha), radius * 0.11f, center + sparkle)
}

private fun DrawScope.drawStone(center: Offset, radius: Float, alpha: Float, hp: Int) {
    val path = hexPath(center, radius * 1.02f)
    drawPath(
        path,
        Brush.verticalGradient(
            listOf(Color(0xFF8A8AA3).copy(alpha = alpha), Color(0xFF4A4A63).copy(alpha = alpha)),
            startY = center.y - radius,
            endY = center.y + radius,
        ),
    )
    drawPath(path, Color(0xFF2C2C3D).copy(alpha = alpha), style = Stroke(width = radius * 0.14f))
    // Uma pedra já trincada mostra a rachadura: o jogador vê que falta um golpe.
    if (hp <= 1) {
        drawLine(
            color = Color(0xFF22222F).copy(alpha = alpha),
            start = center + Offset(-radius * 0.5f, -radius * 0.3f),
            end = center + Offset(radius * 0.2f, radius * 0.55f),
            strokeWidth = radius * 0.12f,
        )
        drawLine(
            color = Color(0xFF22222F).copy(alpha = alpha),
            start = center + Offset(radius * 0.2f, radius * 0.55f),
            end = center + Offset(radius * 0.55f, radius * 0.05f),
            strokeWidth = radius * 0.1f,
        )
    }
}
