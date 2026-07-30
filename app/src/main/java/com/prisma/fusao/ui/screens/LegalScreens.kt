package com.prisma.fusao.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prisma.fusao.ui.components.PrismaButton
import com.prisma.fusao.ui.components.PrismaCard
import com.prisma.fusao.ui.components.PrismaTextButton
import com.prisma.fusao.ui.components.StarfieldBackground
import com.prisma.fusao.ui.components.VSpace
import com.prisma.fusao.ui.theme.BrancoGelo
import com.prisma.fusao.ui.theme.LilasClaro

/** Os dois documentos que o app precisa exibir. */
enum class LegalDocument(val title: String) {
    TERMS("Termos de Uso"),
    PRIVACY("Política de Privacidade"),
}

/**
 * Porta de entrada: sem aceite explícito o jogo não começa.
 *
 * O aceite é registrado com um número de versão. Se os documentos mudarem de forma
 * relevante, basta subir `GameRepository.CURRENT_TERMS_VERSION` para pedir de novo.
 */
@Composable
fun ConsentScreen(
    onAccept: () -> Unit,
    onOpenDocument: (LegalDocument) -> Unit,
) {
    var accepted by remember { mutableStateOf(false) }

    StarfieldBackground {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            PrismaCard {
                Text("Antes de começar", style = MaterialTheme.typography.headlineMedium, color = BrancoGelo)
                VSpace(12)
                Text(
                    "PRISMA é gratuito e se mantém com anúncios. Para jogar, você precisa " +
                        "concordar com os Termos de Uso e com a Política de Privacidade.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LilasClaro,
                )
                VSpace(16)
                Text(
                    "Em resumo, e sem letras miúdas:",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrancoGelo,
                )
                VSpace(8)
                listOf(
                    "Seu progresso fica salvo apenas no seu aparelho.",
                    "Não pedimos cadastro, e-mail, telefone nem localização.",
                    "Os anúncios são do Google AdMob; você escolhe as preferências de privacidade.",
                    "Vídeos premiados são sempre opcionais — recusar nunca trava o jogo.",
                    "Você pode apagar todos os seus dados a qualquer momento nos Ajustes.",
                ).forEach { line ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text("•  ", color = LilasClaro)
                        Text(line, style = MaterialTheme.typography.bodyMedium, color = LilasClaro)
                    }
                }

                VSpace(16)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = accepted,
                        onCheckedChange = { accepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = LilasClaro),
                    )
                    Text(
                        "Li e concordo com os Termos de Uso e a Política de Privacidade.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrancoGelo,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PrismaTextButton("Ler os Termos") { onOpenDocument(LegalDocument.TERMS) }
                    PrismaTextButton("Ler a Política") { onOpenDocument(LegalDocument.PRIVACY) }
                }

                VSpace(8)
                PrismaButton(
                    text = "Aceitar e jogar",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = accepted,
                    pulsing = accepted,
                    onClick = onAccept,
                )
            }
        }
    }
}

/** Exibe um documento por inteiro, rolável. */
@Composable
fun LegalScreen(document: LegalDocument, onBack: () -> Unit) {
    StarfieldBackground {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text(document.title, style = MaterialTheme.typography.headlineMedium, color = BrancoGelo)
            VSpace(12)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = when (document) {
                        LegalDocument.TERMS -> LegalTexts.TERMS
                        LegalDocument.PRIVACY -> LegalTexts.PRIVACY
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = LilasClaro,
                )
                VSpace(24)
            }
            PrismaButton("Voltar", Modifier.fillMaxWidth(), onClick = onBack)
        }
    }
}

/**
 * Textos legais.
 *
 * IMPORTANTE PARA PUBLICAÇÃO: os campos entre colchetes precisam ser preenchidos com
 * os dados reais do responsável pelo app antes de enviar à Play Store, e o mesmo
 * conteúdo deve estar hospedado numa URL pública (a Play exige um link para a
 * política de privacidade na ficha do app). Ver docs/PUBLICACAO.md.
 */
object LegalTexts {

    const val LAST_UPDATE = "30 de julho de 2026"

    val TERMS = """
        Última atualização: $LAST_UPDATE

        1. QUEM SOMOS
        PRISMA — Fusão Cromática ("o Jogo") é desenvolvido e distribuído por
        [NOME DO DESENVOLVEDOR/EMPRESA], contato [E-MAIL DE CONTATO].

        2. ACEITE
        Ao instalar, abrir ou usar o Jogo, você concorda com estes Termos. Se não
        concordar, basta não usar o Jogo e desinstalá-lo.

        3. LICENÇA DE USO
        Concedemos a você uma licença pessoal, gratuita, não exclusiva e revogável
        para usar o Jogo em aparelhos que você controla, para fins de entretenimento
        pessoal e não comercial.

        4. O QUE VOCÊ NÃO PODE FAZER
        Você concorda em não: (a) modificar, descompilar ou fazer engenharia reversa
        do Jogo, salvo onde a lei permitir; (b) usar programas que alterem o
        funcionamento do Jogo, automatizem jogadas ou manipulem pontuação, moedas ou
        itens; (c) redistribuir, vender ou sublicenciar o Jogo; (d) usar o Jogo para
        qualquer finalidade ilegal.

        5. MOEDAS E ITENS VIRTUAIS
        Moedas, vidas, itens e progresso são conteúdo virtual, sem valor monetário
        real. Não são de sua propriedade, não podem ser trocados por dinheiro nem
        transferidos entre contas ou aparelhos. Podemos ajustar preços, quantidades e
        regras de balanceamento a qualquer momento.

        6. ANÚNCIOS
        O Jogo é gratuito e se mantém com publicidade fornecida pelo Google AdMob.
        Os vídeos premiados são sempre opcionais: assistir é uma escolha sua, e
        recusar nunca impede o avanço no Jogo. Anúncios em tela cheia só aparecem em
        pausas naturais, nunca durante uma partida.

        7. PROGRESSO E PERDA DE DADOS
        Seu progresso é gravado apenas no armazenamento do seu aparelho. Desinstalar
        o Jogo, limpar os dados do aplicativo ou trocar de aparelho apaga o progresso
        de forma permanente. Não mantemos cópia em servidor e não conseguimos
        restaurar progresso perdido.

        8. IDADE MÍNIMA
        O Jogo não é dirigido a crianças menores de 13 anos. Se você tem menos de 18
        anos, use o Jogo com a supervisão de um responsável.

        9. DISPONIBILIDADE E MUDANÇAS
        Podemos atualizar, alterar ou descontinuar o Jogo, no todo ou em parte, a
        qualquer momento. Podemos também alterar estes Termos; mudanças relevantes
        serão apresentadas para novo aceite dentro do Jogo.

        10. GARANTIAS E RESPONSABILIDADE
        O Jogo é fornecido "no estado em que se encontra". Na máxima extensão
        permitida pela lei aplicável, não respondemos por danos indiretos,
        incidentais ou lucros cessantes decorrentes do uso do Jogo. Nada nestes
        Termos limita direitos que a lei garanta a você como consumidor, incluindo os
        do Código de Defesa do Consumidor.

        11. LEI APLICÁVEL
        Estes Termos são regidos pelas leis da República Federativa do Brasil. Fica
        eleito o foro do domicílio do consumidor para dirimir controvérsias.

        12. CONTATO
        Dúvidas sobre estes Termos: [E-MAIL DE CONTATO].
    """.trimIndent()

    val PRIVACY = """
        Última atualização: $LAST_UPDATE

        1. RESUMO
        PRISMA — Fusão Cromática não pede cadastro, não coleta nome, e-mail,
        telefone, contatos, fotos nem localização precisa. Seu progresso fica salvo
        somente no seu aparelho. A única coleta de dados acontece por meio da
        plataforma de anúncios do Google.

        2. CONTROLADOR
        [NOME DO DESENVOLVEDOR/EMPRESA], contato [E-MAIL DE CONTATO].

        3. DADOS GUARDADOS NO SEU APARELHO
        Guardamos localmente, sem enviar a servidor nenhum: progresso das fases,
        estrelas e melhores pontuações; moedas, vidas e itens; missões e sequência de
        prêmio diário; preferências de música, efeitos e vibração; e o registro de que
        você aceitou estes documentos. Esses dados são apagados quando você desinstala
        o Jogo, limpa os dados do aplicativo ou usa "Apagar meus dados" nos Ajustes.

        4. DADOS TRATADOS PELA PUBLICIDADE
        Para exibir anúncios usamos o Google AdMob. Dependendo das suas escolhas de
        privacidade, o Google pode tratar: identificador de publicidade do aparelho,
        endereço IP, modelo do aparelho, sistema operacional, idioma, país e
        interações com os anúncios. Esses dados são tratados pelo Google como
        controlador ou operador, conforme o caso, segundo as políticas dele.
        Política de privacidade do Google: https://policies.google.com/privacy
        Como o Google usa dados de parceiros: https://policies.google.com/technologies/partner-sites

        5. SUAS ESCOLHAS DE PRIVACIDADE
        Na primeira execução mostramos um formulário de consentimento fornecido pelo
        Google (User Messaging Platform), onde você decide sobre anúncios
        personalizados. Você pode rever essa decisão a qualquer momento em
        Ajustes > Opções de privacidade. Também é possível redefinir ou excluir o
        identificador de publicidade nas configurações do Android.

        6. BASE LEGAL
        Tratamos dados locais com base na execução do serviço que você solicitou
        (jogar). O tratamento publicitário se apoia no seu consentimento, quando ele
        for exigido pela legislação aplicável, ou no legítimo interesse de manter o
        Jogo gratuito, sempre respeitando sua escolha no formulário de privacidade.

        7. COMPARTILHAMENTO
        Não vendemos seus dados. Não compartilhamos dados com terceiros além do que
        for necessário para a exibição de anúncios pelo Google AdMob e para o
        cumprimento de obrigações legais.

        8. RETENÇÃO
        Os dados locais permanecem no aparelho enquanto o Jogo estiver instalado e
        você não os apagar. Os prazos de retenção da plataforma de anúncios seguem as
        políticas do Google.

        9. CRIANÇAS
        O Jogo não é dirigido a menores de 13 anos e não coletamos conscientemente
        dados dessa faixa etária. Se você é responsável e acredita que uma criança
        forneceu dados, entre em contato para que sejam removidos.

        10. SEUS DIREITOS
        Conforme a LGPD (Lei 13.709/2018) e, quando aplicável, o GDPR, você pode
        pedir confirmação de tratamento, acesso, correção, portabilidade, eliminação e
        revogação do consentimento. Como não mantemos cadastro nem servidor, a forma
        mais direta e imediata de exercer a eliminação é usar "Apagar meus dados" nos
        Ajustes do próprio Jogo. Para os demais pedidos, escreva para
        [E-MAIL DE CONTATO]; responderemos no prazo legal.

        11. SEGURANÇA
        Os dados locais ficam na área privada do aplicativo, protegida pelo sistema
        Android. Nenhum método é infalível, mas não trafegamos dados pessoais seus
        para servidores próprios.

        12. MUDANÇAS
        Se esta Política mudar de forma relevante, apresentaremos o novo texto para
        aceite dentro do Jogo.

        13. CONTATO
        Encarregado de dados / dúvidas de privacidade: [E-MAIL DE CONTATO].
    """.trimIndent()
}
