package com.chuvadeletras.game.ui.legal

/**
 * Textos de Termos de Uso e Política de Privacidade.
 *
 * IMPORTANTE: este é um modelo de base, escrito para o funcionamento real do
 * app (dados locais + anúncios recompensados). Antes de publicar na Play Store
 * você PRECISA substituir os campos marcados com [ ] e revisar o conteúdo com
 * alguém responsável pela sua operação — a Play Store exige uma política de
 * privacidade acessível por URL pública, além da cópia dentro do app.
 */
object LegalTexts {

    const val COMPANY_PLACEHOLDER = "[NOME DA EMPRESA OU DESENVOLVEDOR]"
    const val EMAIL_PLACEHOLDER = "[seu-email-de-contato@exemplo.com]"
    const val CITY_PLACEHOLDER = "[Cidade/Estado]"
    const val LAST_UPDATE = "27 de julho de 2026"

    val terms: List<LegalSection> = listOf(
        LegalSection(
            "1. Sobre estes Termos",
            "Estes Termos de Uso regulam o uso do aplicativo Chuva de Letras, " +
                "desenvolvido por $COMPANY_PLACEHOLDER. Ao instalar, abrir ou jogar, " +
                "você concorda com estes termos. Se não concordar, desinstale o aplicativo."
        ),
        LegalSection(
            "2. Quem pode jogar",
            "O aplicativo é adequado para o público geral. Se você tem menos de 18 anos, " +
                "peça a um responsável para ler estes termos com você antes de assistir a " +
                "anúncios ou usar qualquer recurso opcional."
        ),
        LegalSection(
            "3. Licença de uso",
            "Concedemos a você uma licença pessoal, gratuita, intransferível e revogável " +
                "para usar o aplicativo para entretenimento. Você não pode copiar, modificar, " +
                "descompilar, revender ou redistribuir o aplicativo ou seu conteúdo."
        ),
        LegalSection(
            "4. Moeda virtual, itens e recompensas",
            "As gotas 💧, os itens (Congelar, Trocar, Revelar, Reparo, Tempo), a experiência, " +
                "os níveis e as conquistas são elementos virtuais sem qualquer valor monetário " +
                "no mundo real. Eles não podem ser vendidos, trocados por dinheiro nem " +
                "transferidos para outra conta ou dispositivo. Podemos ajustar preços, " +
                "quantidades e regras de recompensa a qualquer momento para equilibrar o jogo."
        ),
        LegalSection(
            "5. Anúncios recompensados",
            "O aplicativo exibe anúncios em vídeo que são sempre opcionais: você só assiste " +
                "se tocar no botão correspondente, e recebe a recompensa apenas se o vídeo for " +
                "assistido até o fim. Não somos responsáveis pelo conteúdo dos anúncios " +
                "exibidos por redes de terceiros, nem por produtos ou serviços anunciados."
        ),
        LegalSection(
            "6. Progresso e perda de dados",
            "Seu progresso é salvo apenas no armazenamento do seu próprio aparelho. " +
                "Desinstalar o aplicativo, limpar os dados ou trocar de aparelho apaga esse " +
                "progresso de forma permanente, e não temos como recuperá-lo."
        ),
        LegalSection(
            "7. Uso adequado",
            "Você concorda em não usar ferramentas de trapaça, modificação de memória, " +
                "automação ou engenharia reversa para obter vantagem, recompensas indevidas " +
                "ou para simular a visualização de anúncios."
        ),
        LegalSection(
            "8. Disponibilidade e alterações",
            "O aplicativo é fornecido \"como está\". Podemos atualizar, alterar ou " +
                "descontinuar recursos, modos de jogo e o próprio aplicativo a qualquer momento. " +
                "Faremos o possível para manter tudo funcionando, mas não garantimos " +
                "funcionamento ininterrupto ou livre de falhas."
        ),
        LegalSection(
            "9. Limitação de responsabilidade",
            "Na máxima extensão permitida pela lei brasileira, $COMPANY_PLACEHOLDER não " +
                "responde por danos indiretos, perda de progresso, perda de itens virtuais ou " +
                "lucros cessantes decorrentes do uso do aplicativo."
        ),
        LegalSection(
            "10. Propriedade intelectual",
            "O nome Chuva de Letras, a identidade visual, os textos das dicas, o código e a " +
                "mecânica do jogo pertencem a $COMPANY_PLACEHOLDER. Marcas de terceiros " +
                "citadas pertencem aos seus respectivos donos."
        ),
        LegalSection(
            "11. Lei aplicável",
            "Estes Termos são regidos pelas leis da República Federativa do Brasil, incluindo " +
                "o Código de Defesa do Consumidor e o Marco Civil da Internet. Fica eleito o " +
                "foro de $CITY_PLACEHOLDER para resolver qualquer controvérsia."
        ),
        LegalSection(
            "12. Contato",
            "Dúvidas sobre estes Termos: $EMAIL_PLACEHOLDER."
        )
    )

    val privacy: List<LegalSection> = listOf(
        LegalSection(
            "Resumo rápido",
            "O Chuva de Letras não pede cadastro, não pede seu nome, não pede seu e-mail e " +
                "não envia seu progresso para servidor nenhum. Tudo que você joga fica salvo " +
                "no seu próprio aparelho. A única coleta de dados que existe vem da rede de " +
                "anúncios, e só quando você escolhe assistir a um vídeo."
        ),
        LegalSection(
            "1. Quem é o controlador",
            "$COMPANY_PLACEHOLDER é o controlador dos dados tratados por este aplicativo, " +
                "nos termos da Lei Geral de Proteção de Dados (Lei nº 13.709/2018). " +
                "Contato: $EMAIL_PLACEHOLDER."
        ),
        LegalSection(
            "2. Dados que ficam no seu aparelho",
            "Salvamos localmente, usando o armazenamento do próprio app: seu progresso nos " +
                "modos de jogo, pontuação, estrelas, gotas, itens, nível, experiência, " +
                "ofensiva de dias seguidos, missões, conquistas e suas preferências de som e " +
                "vibração. Esses dados nunca saem do aparelho e são apagados quando você " +
                "desinstala o aplicativo ou limpa os dados dele."
        ),
        LegalSection(
            "3. Dados coletados por anúncios",
            "Quando os anúncios estiverem ativos, a rede de anúncios (por exemplo, o Google " +
                "AdMob) pode coletar identificadores de publicidade, informações do dispositivo, " +
                "endereço IP aproximado e dados de interação com o anúncio, conforme a política " +
                "de privacidade dela. Nós não recebemos nem armazenamos esses dados. " +
                "Consulte: https://policies.google.com/technologies/partner-sites"
        ),
        LegalSection(
            "4. Para que usamos os dados",
            "Os dados locais servem exclusivamente para o jogo funcionar: lembrar onde você " +
                "parou, calcular recompensas e manter sua ofensiva diária. Os dados da rede de " +
                "anúncios servem para exibir e medir os anúncios que você escolheu assistir."
        ),
        LegalSection(
            "5. Compartilhamento",
            "Não vendemos, alugamos nem compartilhamos seus dados com terceiros para fins de " +
                "marketing. O único terceiro envolvido é a rede de anúncios, e apenas no " +
                "momento em que você assiste a um anúncio."
        ),
        LegalSection(
            "6. Seus direitos (LGPD)",
            "Você pode, a qualquer momento, confirmar a existência de tratamento, acessar, " +
                "corrigir, anonimizar ou eliminar seus dados. Como todo o progresso fica no seu " +
                "aparelho, a eliminação completa acontece ao limpar os dados do aplicativo nas " +
                "configurações do Android ou ao desinstalá-lo. Para qualquer outra solicitação, " +
                "escreva para $EMAIL_PLACEHOLDER."
        ),
        LegalSection(
            "7. Publicidade personalizada",
            "Você pode limitar a publicidade personalizada nas configurações do seu Android, " +
                "em Google > Anúncios. Isso não impede a exibição de anúncios, apenas reduz a " +
                "personalização deles."
        ),
        LegalSection(
            "8. Crianças e adolescentes",
            "Não coletamos conscientemente dados pessoais de crianças. Se o aplicativo for " +
                "direcionado ao público infantil na sua loja de aplicativos, a rede de anúncios " +
                "deve ser configurada no modo apropriado para conteúdo infantil. " +
                "Responsáveis que identificarem coleta indevida devem nos contatar em " +
                "$EMAIL_PLACEHOLDER para exclusão imediata."
        ),
        LegalSection(
            "9. Segurança",
            "Como não transmitimos seus dados de jogo pela internet, o risco de vazamento é " +
                "mínimo. Ainda assim, mantenha o sistema do seu aparelho atualizado."
        ),
        LegalSection(
            "10. Alterações nesta política",
            "Podemos atualizar esta política. A data da última atualização aparece no topo " +
                "desta tela. Mudanças relevantes serão comunicadas dentro do aplicativo."
        )
    )
}

data class LegalSection(val title: String, val body: String)
