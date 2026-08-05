package com.formatfrute.game.ui.legal

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.formatfrute.game.data.GameRepository
import com.formatfrute.game.ui.components.FruitBackground
import com.formatfrute.game.ui.components.PaperCard
import com.formatfrute.game.ui.theme.Fruta

enum class LegalTab(val title: String) { TERMOS("Termos de Uso"), PRIVACIDADE("Privacidade") }

@Composable
fun LegalScreen(initialTab: LegalTab = LegalTab.TERMOS, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { GameRepository.get(context) }
    val profile by repo.profile.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(initialTab) }

    FruitBackground(profile.boardTheme, density = 5) {
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
                Text(
                    "Documentos",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (profile.boardTheme.dark) Color.White else Fruta.Ink,
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LegalTab.entries.forEach { entry ->
                    val selected = entry == tab
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Fruta.Berry else Color.White.copy(alpha = 0.85f))
                            .border(
                                2.5.dp,
                                Fruta.Ink.copy(alpha = 0.6f),
                                RoundedCornerShape(50),
                            )
                            .clickable { tab = entry },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            entry.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else Fruta.Ink,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val sections = if (tab == LegalTab.TERMOS) LegalTexts.terms else LegalTexts.privacy
                items(sections.size) { index ->
                    val (title, body) = sections[index]
                    PaperCard(Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.96f)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(title, style = MaterialTheme.typography.titleMedium, color = Fruta.Berry)
                            Spacer(Modifier.height(6.dp))
                            Text(body, style = MaterialTheme.typography.bodySmall, color = Fruta.Ink)
                        }
                    }
                }
                item { Spacer(Modifier.height(30.dp)) }
            }
        }
    }
}

/**
 * Textos legais do app. Escritos em portugues simples, descrevendo exatamente
 * o que o Format Frute faz — nada de clausula generica copiada.
 */
object LegalTexts {

    const val EMAIL = "formatfrute@gmail.com"
    const val UPDATED = "5 de agosto de 2026"

    val terms: List<Pair<String, String>> = listOf(
        "1. Quem somos" to
            "Format Frute é um jogo de quebra-cabeça para celular, feito para diversão de " +
            "toda a família. Ao instalar e jogar, você concorda com estes Termos de Uso. " +
            "Se não concordar, basta desinstalar o aplicativo.\n\nContato: $EMAIL",

        "2. Licença de uso" to
            "Damos a você uma licença pessoal, gratuita, não exclusiva e intransferível para " +
            "usar o Format Frute em seus dispositivos. O jogo, o nome, os desenhos das frutas, " +
            "os sons e o código continuam sendo nossos.",

        "3. O que você não pode fazer" to
            "• Copiar, vender, alugar ou redistribuir o jogo;\n" +
            "• Modificar, descompilar ou fazer engenharia reversa do aplicativo;\n" +
            "• Usar programas para trapacear, alterar pontuação, sementes ou poderes;\n" +
            "• Usar o jogo para qualquer finalidade ilegal.\n\n" +
            "Contas ou dispositivos que burlarem as regras podem perder o progresso.",

        "4. Sementes, poderes e itens" to
            "As Sementes e os poderes são itens virtuais, sem valor monetário fora do jogo. " +
            "Eles não podem ser trocados por dinheiro, transferidos entre dispositivos ou " +
            "resgatados de qualquer outra forma. Podemos ajustar preços, recompensas e " +
            "equilíbrio do jogo para manter a experiência justa.",

        "5. Anúncios" to
            "O Format Frute é gratuito e se mantém com anúncios exibidos pelo Google AdMob. " +
            "Existem dois tipos:\n\n" +
            "• Anúncio entre partidas (intersticial), mostrado com intervalos para não " +
            "atrapalhar o jogo;\n" +
            "• Anúncio premiado (vídeo), sempre opcional: você escolhe assistir para ganhar " +
            "poderes, sementes, tempo extra ou continuar a partida.\n\n" +
            "Nunca é obrigatório assistir a um vídeo premiado para avançar no jogo.",

        "6. Progresso salvo no aparelho" to
            "Seu progresso, recordes, sementes e itens ficam guardados no próprio aparelho. " +
            "Se você desinstalar o jogo, limpar os dados do app ou trocar de celular, esse " +
            "progresso pode ser perdido e não temos como recuperá-lo.",

        "7. Disponibilidade e mudanças" to
            "Podemos atualizar o jogo, adicionar ou remover modos, poderes e recompensas, e " +
            "também interromper o serviço a qualquer momento. Sempre que possível, avisaremos " +
            "dentro do próprio aplicativo.",

        "8. Garantias e responsabilidade" to
            "O jogo é fornecido \"como está\". Não garantimos que ele funcionará sem falhas em " +
            "todos os aparelhos. Na medida permitida pela lei, não nos responsabilizamos por " +
            "danos indiretos decorrentes do uso do aplicativo. Nada aqui afasta os direitos " +
            "garantidos pelo Código de Defesa do Consumidor.",

        "9. Crianças" to
            "O jogo tem tema infantil e pode ser jogado por crianças com a supervisão de " +
            "pais ou responsáveis. Recomendamos que o responsável configure os controles " +
            "parentais da loja de aplicativos e acompanhe o uso.",

        "10. Lei aplicável" to
            "Estes Termos são regidos pelas leis do Brasil. Dúvidas, sugestões ou reclamações " +
            "podem ser enviadas para $EMAIL.\n\nÚltima atualização: $UPDATED",
    )

    val privacy: List<Pair<String, String>> = listOf(
        "Resumo rápido" to
            "Não pedimos cadastro, não pedimos e-mail, não pedimos telefone e não criamos " +
            "conta. Seu progresso fica no seu aparelho. Os únicos dados que saem do celular " +
            "são os usados pelo Google AdMob para mostrar anúncios.\n\nContato: $EMAIL",

        "1. Dados que ficam só no seu aparelho" to
            "Guardamos localmente, usando as preferências do Android:\n" +
            "• Pontuação, recordes e melhor fruta alcançada;\n" +
            "• Sementes, poderes, peles compradas e nível do jogador;\n" +
            "• Missões do dia, sequência de presentes e progresso do tutorial;\n" +
            "• Suas preferências de música, efeitos, vibração e notificações.\n\n" +
            "Esses dados não são enviados para nenhum servidor nosso — nós nem temos servidor.",

        "2. Dados usados pelos anúncios" to
            "Os anúncios são fornecidos pelo Google AdMob. Para exibi-los, o Google pode " +
            "coletar e tratar informações como identificador de publicidade do dispositivo, " +
            "endereço IP, modelo do aparelho, versão do sistema e interações com o anúncio.\n\n" +
            "Esse tratamento é feito pelo Google, conforme a política dele: " +
            "https://policies.google.com/technologies/ads",

        "3. Consentimento e anúncios personalizados" to
            "Se você estiver na Europa (EEE/Reino Unido) ou em outra região que exija, " +
            "mostramos uma tela de consentimento antes dos anúncios, usando a plataforma de " +
            "mensagens do Google (UMP). Você pode rever essa escolha a qualquer momento em " +
            "Ajustes › Opções de privacidade.\n\n" +
            "No Android, você também pode limitar ou apagar o identificador de publicidade " +
            "em Configurações › Google › Anúncios.",

        "4. Notificações" to
            "Enviamos lembretes divertidos sobre presentes do dia e desafios novos. Elas são " +
            "geradas no próprio aparelho, não usam servidor e podem ser desligadas a qualquer " +
            "momento em Ajustes › Notificações ou nas configurações do Android.",

        "5. Permissões que o app pede" to
            "• Notificações: para os lembretes (Android 13 ou superior);\n" +
            "• Vibração: para o retorno tátil durante o jogo;\n" +
            "• Internet: apenas para carregar os anúncios.\n\n" +
            "Não pedimos acesso a contatos, câmera, microfone, fotos nem localização.",

        "6. Crianças e famílias" to
            "O Format Frute foi pensado para o público familiar. Não coletamos " +
            "intencionalmente dados pessoais de crianças. Quando o app é distribuído no " +
            "programa para famílias do Google Play, as chamadas de anúncio são marcadas como " +
            "dirigidas a crianças e o conteúdo é limitado a classificações apropriadas. " +
            "Se você é responsável e acredita que uma criança forneceu algum dado pessoal, " +
            "escreva para $EMAIL e apagaremos o que estiver ao nosso alcance.",

        "7. Seus direitos (LGPD)" to
            "Você pode pedir confirmação, acesso, correção ou exclusão dos dados tratados por " +
            "nós. Como o progresso fica apenas no aparelho, a exclusão completa acontece ao " +
            "limpar os dados do aplicativo ou desinstalá-lo. Para qualquer pedido ou dúvida " +
            "sobre privacidade, fale com $EMAIL — respondemos em até 15 dias.",

        "8. Mudanças nesta política" to
            "Se algo mudar, atualizaremos este texto dentro do aplicativo e alteraremos a data " +
            "abaixo. Mudanças relevantes serão avisadas na tela inicial.\n\n" +
            "Última atualização: $UPDATED",
    )
}
