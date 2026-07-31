package com.prisma.fusao.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prisma.fusao.PrismaApplication
import com.prisma.fusao.data.PlayerState
import com.prisma.fusao.ui.components.PrismaButton
import com.prisma.fusao.ui.components.PrismaCard
import com.prisma.fusao.ui.components.ProgressBar
import com.prisma.fusao.ui.components.StarfieldBackground
import com.prisma.fusao.ui.components.VSpace
import com.prisma.fusao.ui.theme.BrancoGelo
import com.prisma.fusao.ui.theme.DouradoEstrela
import com.prisma.fusao.ui.theme.LilasClaro
import com.prisma.fusao.ui.theme.VerdeConfirma
import kotlinx.coroutines.launch

/** Missões do dia: três metas curtas que renovam à meia-noite. */
@Composable
fun MissionsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as PrismaApplication
    val player by app.repository.state.collectAsStateWithLifecycle(initialValue = PlayerState())
    val scope = rememberCoroutineScope()

    StarfieldBackground {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Text("Missões do dia", style = MaterialTheme.typography.headlineMedium, color = BrancoGelo)
            VSpace(4)
            Text(
                "Renovam todo dia. Complete e receba as moedas.",
                style = MaterialTheme.typography.bodyMedium,
                color = LilasClaro,
            )
            VSpace(16)

            Column(Modifier.weight(1f)) {
                if (player.missions.isEmpty()) {
                    Text(
                        "As missões de hoje estão sendo preparadas. Volte em instantes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LilasClaro,
                    )
                }
                player.missions.forEach { mission ->
                    PrismaCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                mission.description,
                                style = MaterialTheme.typography.titleMedium,
                                color = BrancoGelo,
                            )
                            Text(
                                "+${mission.reward} ◈",
                                color = DouradoEstrela,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        VSpace(10)
                        ProgressBar(
                            progress = mission.progress.toFloat() / mission.target.coerceAtLeast(1),
                            modifier = Modifier.fillMaxWidth(),
                            color = if (mission.complete) VerdeConfirma else LilasClaro,
                        )
                        VSpace(6)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${mission.progress}/${mission.target}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LilasClaro,
                            )
                            when {
                                mission.claimed -> Text(
                                    "Recebido",
                                    color = VerdeConfirma,
                                    fontSize = 13.sp,
                                )
                                mission.complete -> Box(
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DouradoEstrela)
                                        .clickable {
                                            scope.launch { app.repository.claimMission(mission.id) }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        "Receber",
                                        color = Color(0xFF2A1A00),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    )
                                }
                                else -> Box(
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.14f),
                                            RoundedCornerShape(12.dp),
                                        )
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                ) {
                                    Text("Em andamento", color = LilasClaro, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    VSpace(12)
                }
            }

            PrismaButton("Voltar", Modifier.fillMaxWidth(), onClick = onBack)
        }
    }
}
