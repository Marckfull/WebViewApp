package com.prisma.fusao.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberInfiniteTransition
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prisma.fusao.PrismaApplication
import com.prisma.fusao.audio.Sfx
import com.prisma.fusao.core.GameEngine
import com.prisma.fusao.core.GemKind
import com.prisma.fusao.core.MoveOutcome
import com.prisma.fusao.core.Pos
import com.prisma.fusao.core.ResolveStep
import com.prisma.fusao.core.TutorialScript
import com.prisma.fusao.data.Booster
import com.prisma.fusao.ui.components.PrismaButton
import com.prisma.fusao.ui.components.PrismaCard
import com.prisma.fusao.ui.components.StarfieldBackground
import com.prisma.fusao.ui.components.VSpace
import com.prisma.fusao.ui.game.BoardCanvas
import com.prisma.fusao.ui.game.BoardVisuals
import com.prisma.fusao.ui.theme.BrancoGelo
import com.prisma.fusao.ui.theme.DouradoEstrela
import com.prisma.fusao.ui.theme.LilasClaro
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tutorial obrigatório da primeira execução.
 *
 * Ele ensina na ordem em que a mecânica foi pensada: primeiro o match comum, que o
 * jogador já conhece de outros jogos, e depois o que o PRISMA faz de diferente —
 * a essência, o transporte livre dela e a fusão.
 *
 * A regra central é que **o tutorial não avança sozinho**: nenhum "próximo", nenhum
 * toque genérico na tela. Só a jogada certa destrava o passo seguinte. Errar não
 * pune: a peça sacode e o destaque continua ali.
 *
 * Os tabuleiros vivem em [TutorialScript] e são verificados por teste.
 */
@Composable
fun TutorialScreen(onFinished: () -> Unit) {
    val app = LocalContext.current.applicationContext as PrismaApplication
    val scope = rememberCoroutineScope()

    val engine = remember { GameEngine(TutorialScript.spec) }
    val visuals = remember { BoardVisuals(TutorialScript.ROWS, TutorialScript.COLS) }

    var stepIndex by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(true) }
    var celebrating by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }
    var goalCells by remember { mutableStateOf<Pair<Pos, Pos>?>(null) }

    val step = TutorialScript.steps.getOrNull(stepIndex)

    LaunchedEffect(visuals) {
        var previous = 0L
        while (true) {
            withFrameNanos { now ->
                if (previous != 0L) visuals.update(((now - previous) / 1_000_000_000.0).toFloat())
                previous = now
            }
        }
    }

    // Monta o tabuleiro do passo e calcula o alvo destacado.
    LaunchedEffect(stepIndex) {
        val current = TutorialScript.steps.getOrNull(stepIndex) ?: return@LaunchedEffect
        current.layout?.let {
            TutorialScript.applyLayout(engine, it, startId = 900_000L + stepIndex * 1_000L)
            visuals.syncFrom(engine.cells())
        }
        val target = when (val goal = current.goal) {
            is TutorialScript.Goal.Swap -> goal.from to goal.to
            is TutorialScript.Goal.ActivateKind -> TutorialScript.findActivatable(engine, goal.kind)
        }
        goalCells = target
        visuals.hints = target?.let { setOf(it.first, it.second) } ?: emptySet()
        // Rede de segurança: se por algum motivo a peça do passo não existir mais,
        // seguimos em frente em vez de deixar o jogador preso.
        if (target == null) {
            delay(600)
            if (stepIndex + 1 < TutorialScript.steps.size) stepIndex++ else finished = true
            return@LaunchedEffect
        }
        busy = false
    }

    StarfieldBackground {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.34f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Passo ${stepIndex + 1} de ${TutorialScript.steps.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DouradoEstrela,
                )
                VSpace(4)
                Text(
                    step?.title ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = BrancoGelo,
                    textAlign = TextAlign.Center,
                )
                VSpace(6)
                Text(
                    step?.instruction ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LilasClaro,
                    textAlign = TextAlign.Center,
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                BoardCanvas(
                    visuals = visuals,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                    enabled = !busy && celebrating == null && !finished,
                    onSwipe = { from, to ->
                        val goal = goalCells ?: return@BoardCanvas
                        val correct = (from == goal.first && to == goal.second) ||
                            (from == goal.second && to == goal.first)
                        if (!correct) {
                            app.soundEngine.play(Sfx.INVALID, volume = 0.5f)
                            visuals.rejectSwap(from, to)
                            return@BoardCanvas
                        }
                        busy = true
                        scope.launch {
                            val outcome = engine.trySwap(from, to)
                            if (outcome is MoveOutcome.Accepted) {
                                app.soundEngine.play(Sfx.SWAP, volume = 0.7f)
                                playTutorialSteps(app, visuals, outcome.steps)
                                visuals.reconcile(engine.cells())
                            }
                            visuals.hints = emptySet()
                            celebrating = TutorialScript.steps[stepIndex].celebration
                            delay(1700)
                            celebrating = null
                            if (stepIndex + 1 < TutorialScript.steps.size) {
                                stepIndex++
                            } else {
                                finished = true
                            }
                        }
                    },
                    onTap = { },
                )

                celebrating?.let { text ->
                    val transition = rememberInfiniteTransition(label = "festa")
                    val glow by transition.animateFloat(
                        initialValue = 0.72f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                        label = "brilho",
                    )
                    Box(
                        Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text,
                            style = MaterialTheme.typography.headlineMedium,
                            color = DouradoEstrela,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .alpha(glow)
                                .background(Color.Black.copy(alpha = 0.66f))
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Siga o destaque dourado — o tutorial avança quando você faz a jogada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LilasClaro,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (finished) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.padding(26.dp)) {
                PrismaCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Você aprendeu o PRISMA!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = BrancoGelo,
                    )
                    VSpace(12)
                    Text(
                        "Guarde o essencial: quatro em linha condensam uma essência, a " +
                            "essência anda livre pelo tabuleiro, e duas essências encostadas " +
                            "se fundem. Cores iguais viram Supernova; cores diferentes viram " +
                            "Prisma.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LilasClaro,
                    )
                    VSpace(14)
                    Text(
                        "Prêmio de boas-vindas: 200 moedas e 1 martelo.",
                        style = MaterialTheme.typography.titleMedium,
                        color = DouradoEstrela,
                    )
                    VSpace(18)
                    PrismaButton(
                        text = "Começar a campanha",
                        modifier = Modifier.fillMaxWidth(),
                        pulsing = true,
                        onClick = {
                            scope.launch {
                                app.repository.completeTutorial()
                                app.repository.grantCoins(200)
                                app.repository.grantBooster(Booster.HAMMER)
                                onFinished()
                            }
                        },
                    )
                }
            }
        }
    }
}

/** Reproduz a resolução da jogada, com o mesmo ritmo da tela de partida. */
private suspend fun playTutorialSteps(
    app: PrismaApplication,
    visuals: BoardVisuals,
    steps: List<ResolveStep>,
) {
    for (step in steps) {
        when {
            step.fusions.isNotEmpty() -> app.soundEngine.play(Sfx.FUSION)
            step.blasts.any { it.kind == GemKind.SUPERNOVA } -> app.soundEngine.play(Sfx.SUPERNOVA)
            step.blasts.any { it.kind == GemKind.PRISM } -> app.soundEngine.play(Sfx.PRISM)
            step.created.isNotEmpty() -> app.soundEngine.play(Sfx.ESSENCE)
            step.cleared.isNotEmpty() -> app.soundEngine.playCascade(step.cascade)
        }
        visuals.playClearPhase(step)
        delay(280)
        visuals.playFallPhase(step)
        delay(320)
    }
}
