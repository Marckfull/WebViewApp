package com.prisma.fusao.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prisma.fusao.ui.theme.DouradoEstrela
import com.prisma.fusao.ui.theme.LilasClaro
import com.prisma.fusao.ui.theme.NoiteMedia
import com.prisma.fusao.ui.theme.NoiteProfunda
import com.prisma.fusao.ui.theme.RoxoBorda
import com.prisma.fusao.ui.theme.RoxoCartao
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Fundo animado usado em todas as telas: um céu com estrelas que respiram e duas
 * auroras que giram devagar. Custa pouco e dá vida às telas paradas.
 */
@Composable
fun StarfieldBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    val transition = rememberInfiniteTransition(label = "fundo")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Reverse),
        label = "brilho",
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(40_000), RepeatMode.Restart),
        label = "aurora",
    )

    // As posições são sorteadas uma única vez e ficam estáveis entre recomposições.
    val stars = remember {
        val rng = Random(20)
        List(90) {
            Triple(rng.nextFloat(), rng.nextFloat(), 0.6f + rng.nextFloat() * 1.9f)
        }
    }

    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NoiteProfunda, NoiteMedia)))) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x33B185FF), Color.Transparent),
                    center = Offset(size.width * (0.25f + 0.1f * cos(drift)), size.height * 0.22f),
                    radius = size.minDimension * 0.7f,
                ),
                radius = size.minDimension * 0.7f,
                center = Offset(size.width * (0.25f + 0.1f * cos(drift)), size.height * 0.22f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x2648BFE3), Color.Transparent),
                    center = Offset(size.width * (0.8f + 0.1f * sin(drift)), size.height * 0.78f),
                    radius = size.minDimension * 0.6f,
                ),
                radius = size.minDimension * 0.6f,
                center = Offset(size.width * (0.8f + 0.1f * sin(drift)), size.height * 0.78f),
            )
            stars.forEachIndexed { index, (x, y, r) ->
                val twinkle = 0.35f + 0.65f * kotlin.math.abs(sin(drift * 2f + index))
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f + 0.5f * twinkle * phase),
                    radius = r,
                    center = Offset(x * size.width, y * size.height),
                )
            }
        }
        content()
    }
}

/** Botão principal, com leve pulsação para chamar o toque. */
@Composable
fun PrismaButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pulsing: Boolean = false,
    colors: List<Color> = listOf(Color(0xFF8B5CF6), Color(0xFF5B21B6)),
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "pulso")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulsing) 1.045f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "escala",
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .scale(if (pulsing) pulse else 1f)
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (enabled) Brush.verticalGradient(colors)
                    else Brush.verticalGradient(listOf(Color(0xFF3A3050), Color(0xFF2A2340)))
                )
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Botão secundário discreto. */
@Composable
fun PrismaTextButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, color = LilasClaro, style = MaterialTheme.typography.labelLarge)
    }
}

/** Cartão translúcido usado em painéis e diálogos. */
@Composable
fun PrismaCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(RoxoCartao.copy(alpha = 0.92f))
            .border(1.dp, RoxoBorda, RoundedCornerShape(24.dp))
            .padding(20.dp),
        content = content,
    )
}

/** Fileira de três estrelas, com as conquistadas preenchidas. */
@Composable
fun StarRow(stars: Int, size: androidx.compose.ui.unit.Dp = 28.dp, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            Canvas(Modifier.size(size)) {
                drawStar(
                    filled = index < stars,
                    color = if (index < stars) DouradoEstrela else Color.White.copy(alpha = 0.16f),
                )
            }
        }
    }
}

/** Desenha uma estrela de cinco pontas ocupando todo o espaço disponível. */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(filled: Boolean, color: Color) {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val angle = -PI / 2 + i * PI / 5
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    if (filled) {
        drawPath(path, Brush.verticalGradient(listOf(Color(0xFFFFE49A), color)))
    } else {
        drawPath(path, color)
    }
}

/** Barra de progresso arredondada com brilho. */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = LilasClaro,
    height: androidx.compose.ui.unit.Dp = 10.dp,
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.35f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color))),
        )
    }
}

/** Espaçador vertical curto, usado o suficiente para valer um atalho. */
@Composable
fun VSpace(dp: Int) = Spacer(Modifier.height(dp.dp))

@Composable
fun HSpace(dp: Int) = Spacer(Modifier.width(dp.dp))
