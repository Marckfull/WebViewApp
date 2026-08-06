package com.kardiapulse.game.ui.extras

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kardiapulse.game.core.model.Element
import com.kardiapulse.game.core.model.Modifier as GameModifier
import com.kardiapulse.game.core.model.PowerType
import com.kardiapulse.game.data.PlayerProfile
import com.kardiapulse.game.ui.common.Caption
import com.kardiapulse.game.ui.common.GlassPanel
import com.kardiapulse.game.ui.common.KardiaBackground
import com.kardiapulse.game.ui.common.ScreenHeader
import com.kardiapulse.game.ui.common.SectionTitle
import com.kardiapulse.game.ui.theme.PulseCyan
import com.kardiapulse.game.ui.theme.PulseGold
import com.kardiapulse.game.ui.theme.PulseViolet
import com.kardiapulse.game.ui.theme.SurfaceCard
import com.kardiapulse.game.ui.theme.SurfaceStroke
import com.kardiapulse.game.ui.theme.TextMuted
import com.kardiapulse.game.ui.theme.TextPrimary
import com.kardiapulse.game.ui.theme.color

/** Uma entrada do Registro, liberada conforme o jogador sobe de nível. */
private data class LoreEntry(val level: Int, val title: String, val text: String)

private val LORE = listOf(
    LoreEntry(
        1, "Registro 01 — O primeiro pulso",
        "O Núcleo não foi construído. Foi encontrado já batendo, no fundo de uma mina de quartzo " +
            "em que nada deveria estar vivo. Quem chegou primeiro anotou uma única frase antes de " +
            "sair correndo: \"ele responde\"."
    ),
    LoreEntry(
        3, "Registro 02 — A direção",
        "Levou onze anos para alguém perceber que o Núcleo não aceita mudar de rumo por capricho. " +
            "Empurre-o para um lado e ele passa a exigir aquele lado. A descoberta custou três " +
            "laboratórios."
    ),
    LoreEntry(
        6, "Registro 03 — Ressonância",
        "A saída apareceu por acidente. Uma técnica deixou cair uma placa do mesmo elemento da " +
            "última leitura, e o Núcleo simplesmente virou. Não foi força: foi concordância."
    ),
    LoreEntry(
        10, "Registro 04 — O Éter",
        "Quatro placas de Éter existem. Não se sabe de que são feitas. Concordam com tudo, e por " +
            "isso valem mais do que qualquer número alto."
    ),
    LoreEntry(
        14, "Registro 05 — Colapso",
        "Ninguém consegue segurar o Núcleo indefinidamente. Quanto mais longa a sessão, menos " +
            "margem ele tolera, até que a única saída seja não ter saída nenhuma."
    ),
    LoreEntry(
        20, "Registro 06 — Duelo",
        "Foi assim que virou jogo. Dois operadores, um Núcleo, e a certeza de que perder é ficar " +
            "sem jogada — nunca ficar sem força."
    ),
    LoreEntry(
        30, "Registro 07 — Kardia",
        "O nome veio de uma anotação de rodapé: kardía, coração. Não porque o Núcleo bata, mas " +
            "porque ele para quando alguém aperta demais."
    )
)

private val TIPS = listOf(
    "Guarde uma carta que ressoa com o último elemento. Ter a inversão na mão vale mais do que " +
        "ter a carta mais alta do baralho.",
    "Cartas baixas são as suas últimas saídas quando a folga aperta. Jogar os números grandes " +
        "cedo costuma ser melhor do que segurá-los.",
    "O Colapso trabalha para quem já está confortável. Se você está em vantagem, deixe a rodada " +
        "se arrastar; se está apertado, force a decisão logo.",
    "O Éter ressoa com tudo, mas só existem quatro no baralho. Gastar um Éter só para virar a " +
        "direção costuma ser desperdício — segure para o momento em que ele salva a rodada.",
    "Descarga é mais forte do que parece: zerar o Núcleo perto do limite devolve toda a rodada " +
        "para o seu lado.",
    "Repare no que o rival descarta. Se ele parou de jogar Água, provavelmente não tem mais " +
        "Água na mão — e não vai conseguir inverter."
)

@Composable
fun ExtrasScreen(profile: PlayerProfile, onBack: () -> Unit) {
    var open by remember { mutableStateOf("codex") }

    KardiaBackground {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            ScreenHeader(title = "Extras", onBack = onBack)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Tab("Códex", open == "codex", Modifier.weight(1f)) { open = "codex" }
                Tab("Estratégia", open == "dicas", Modifier.weight(1f)) { open = "dicas" }
                Tab("Registro", open == "lore", Modifier.weight(1f)) { open = "lore" }
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = open == "codex",
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    SectionTitle("Elementos")
                    Spacer(Modifier.height(8.dp))
                    Caption(
                        "O elemento não muda a força da carta. Ele decide uma coisa só: se a carta " +
                            "ressoa com a última jogada e, portanto, se ela pode inverter a direção."
                    )
                    Spacer(Modifier.height(10.dp))
                    Element.entries.forEach { element ->
                        GlassPanel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(element.glyph, color = element.color, fontSize = 20.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        element.ptName,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Caption(
                                        if (element.isWild)
                                            "Curinga. Ressoa com qualquer elemento. Existem apenas " +
                                                "quatro no baralho, com valores de 1 a 4."
                                        else
                                            "Nove cartas, de 1 a 9. Ressoa com ${element.ptName} e com o Éter."
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    SectionTitle("Poderes")
                    Spacer(Modifier.height(10.dp))
                    PowerType.ALL.forEach { power ->
                        GlassPanel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(power.glyph, color = PulseCyan, fontSize = 20.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        power.ptName,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Caption(power.description)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    SectionTitle("Modificadores do Caos")
                    Spacer(Modifier.height(10.dp))
                    GameModifier.ALL.forEach { modifier ->
                        GlassPanel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(modifier.glyph, color = PulseGold, fontSize = 20.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        modifier.ptName,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Caption(modifier.description)
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = open == "dicas",
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    SectionTitle("Como se joga bem")
                    Spacer(Modifier.height(10.dp))
                    TIPS.forEachIndexed { index, tip ->
                        GlassPanel(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row {
                                Text(
                                    "${index + 1}",
                                    color = PulseViolet,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.width(28.dp)
                                )
                                Caption(tip)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = open == "lore",
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    SectionTitle("Registro do Núcleo")
                    Spacer(Modifier.height(6.dp))
                    Caption("Novas entradas são liberadas conforme você sobe de nível.")
                    Spacer(Modifier.height(10.dp))
                    LORE.forEach { entry ->
                        val unlocked = profile.level >= entry.level
                        GlassPanel(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            borderColor = if (unlocked) SurfaceStroke else SurfaceStroke.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = if (unlocked) entry.title else "Bloqueado",
                                color = if (unlocked) PulseCyan else TextMuted,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Caption(
                                if (unlocked) entry.text
                                else "Alcance o nível ${entry.level} para liberar esta entrada."
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            GlassPanel(Modifier.fillMaxWidth()) {
                Text("Sobre", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Caption(
                    "Kardia Pulse — regras originais, IA com busca em mundos possíveis, e trilha " +
                        "sonora sintetizada em tempo real dentro do próprio aparelho. Nenhum arquivo " +
                        "de áudio foi usado na produção deste jogo."
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Tab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) PulseCyan.copy(alpha = 0.16f) else SurfaceCard.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) PulseCyan else SurfaceStroke
        ),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                color = if (selected) PulseCyan else TextMuted,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
