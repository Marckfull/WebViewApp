package com.neonsombra.game.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neonsombra.game.ui.LocalPrefs
import com.neonsombra.game.ui.components.NeonBackground
import com.neonsombra.game.ui.components.NeonButton
import com.neonsombra.game.ui.components.NeonLabel
import com.neonsombra.game.ui.components.NeonLogo
import com.neonsombra.game.ui.components.NeonPanel
import com.neonsombra.game.ui.components.NeonText
import com.neonsombra.game.ui.theme.NeonCyan
import com.neonsombra.game.ui.theme.NeonLime
import com.neonsombra.game.ui.theme.NeonMagenta
import com.neonsombra.game.ui.theme.NeonPurple
import com.neonsombra.game.ui.theme.NeonTextMuted
import com.neonsombra.game.ui.theme.NeonTextPrimary
import com.neonsombra.game.ui.theme.NeonYellow

private const val MAX_NAME_LENGTH = 12

/** Menu principal: apelido do jogador, recorde e os caminhos do jogo. */
@Composable
fun MenuScreen(
    onPlay: () -> Unit,
    onTutorial: () -> Unit,
    onSettings: () -> Unit,
    onTerms: () -> Unit,
) {
    val prefs = LocalPrefs.current
    var name by remember { mutableStateOf(prefs.playerName) }

    NeonBackground(modifier = Modifier.fillMaxSize(), intensity = 0.9f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(10.dp))
            NeonLogo(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))

            NeonPanel(
                accent = NeonMagenta,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 14.dp,
            ) {
                NeonLabel(text = "MAIOR PONTUACAO", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeonText(
                        text = prefs.highScore.toString(),
                        color = NeonYellow,
                        glowRadius = 16f,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp),
                    )
                    Text(
                        text = prefs.highScoreOwner.ifBlank { "ninguem ainda" },
                        color = NeonTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            NeonPanel(
                accent = NeonCyan,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 14.dp,
            ) {
                NeonLabel(text = "SEU APELIDO", modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { typed ->
                        name = typed.take(MAX_NAME_LENGTH).filter { it != '\n' }
                        prefs.playerName = name.trim()
                    },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "JOGADOR",
                            color = NeonTextMuted,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    textStyle = TextStyle(
                        color = NeonTextPrimary,
                        fontSize = 16.sp,
                        fontFamily = MaterialTheme.typography.titleMedium.fontFamily,
                        letterSpacing = 2.sp,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = NeonCyan.copy(alpha = 0.4f),
                        cursorColor = NeonMagenta,
                        focusedContainerColor = NeonCyan.copy(alpha = 0.06f),
                        unfocusedContainerColor = NeonCyan.copy(alpha = 0.03f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(24.dp))

            NeonButton(
                text = "JOGAR",
                onClick = onPlay,
                accent = NeonLime,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            NeonButton(
                text = "COMO JOGAR",
                onClick = onTutorial,
                accent = NeonCyan,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            NeonButton(
                text = "CONFIGURACOES",
                onClick = onSettings,
                accent = NeonPurple,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            NeonButton(
                text = "TERMOS E PRIVACIDADE",
                onClick = onTerms,
                accent = NeonMagenta,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "ARRASTE PARA MOVER  -  SEGURE PARA CAIR  -  TOQUE PARA GIRAR",
                color = NeonTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
