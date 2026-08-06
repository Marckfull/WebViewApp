package com.kardiapulse.game.ui.legal

/**
 * Política de Privacidade e Termos de Uso.
 *
 * IMPORTANTE ANTES DE PUBLICAR: troque os campos entre colchetes (contato, empresa, foro) pelos
 * seus dados reais e hospede a mesma Política de Privacidade em uma URL pública — a Play Store
 * exige o link no cadastro do app, além do texto dentro do jogo.
 */
object LegalContent {

    /** Suba este número sempre que o texto mudar de forma relevante: o aceite é pedido de novo. */
    const val VERSION = 1

    const val CONTACT_EMAIL = "[seu-email-de-contato@exemplo.com]"
    const val COMPANY = "[Seu nome ou razão social]"
    const val LAST_UPDATE = "06 de agosto de 2026"

    val PRIVACY: List<LegalSection> = listOf(
        LegalSection(
            "1. Quem somos",
            "Kardia Pulse é um jogo de cartas para Android desenvolvido por $COMPANY. " +
                "Esta política explica quais dados o aplicativo trata, por quê, e o que você pode " +
                "fazer a respeito. Última atualização: $LAST_UPDATE."
        ),
        LegalSection(
            "2. Dados que ficam apenas no seu aparelho",
            "Seu progresso — nível, experiência, Fragmentos, Cristais, poderes, conquistas, " +
                "sequência do Passe Diário, preferências de som, música, vibração e notificações — " +
                "é gravado exclusivamente no armazenamento local do seu dispositivo.\n\n" +
                "Não temos servidor de contas. Não coletamos nome, e-mail, telefone, agenda, fotos, " +
                "localização precisa nem qualquer dado do seu perfil pessoal. Se você desinstalar o " +
                "aplicativo, esses dados são apagados junto."
        ),
        LegalSection(
            "3. Publicidade",
            "O jogo exibe anúncios do Google AdMob para se manter gratuito. Existem dois formatos:\n\n" +
                "• Intersticiais: aparecem apenas ao SAIR de um duelo, nunca durante a partida, e com " +
                "limite de frequência.\n" +
                "• Premiados: são sempre opcionais e iniciados por você, em troca de recompensas no jogo.\n\n" +
                "Para exibir anúncios, o Google pode tratar identificadores do dispositivo (como o ID " +
                "de publicidade), endereço IP aproximado, tipo de aparelho e dados de interação com o " +
                "anúncio. Esse tratamento é feito pelo Google como controlador independente, segundo " +
                "as políticas dele.\n\n" +
                "Nós não temos acesso ao conteúdo desses dados e não os cruzamos com o seu progresso."
        ),
        LegalSection(
            "4. Consentimento e anúncios personalizados",
            "Se você estiver em uma região que exige consentimento (como o Espaço Econômico Europeu, " +
                "Reino Unido ou Suíça), um formulário aparece na primeira execução perguntando se você " +
                "aceita anúncios personalizados.\n\n" +
                "Você pode mudar sua escolha a qualquer momento em Ajustes › Configurações de privacidade. " +
                "Recusar não bloqueia nenhuma parte do jogo: você continua com acesso a todos os modos, " +
                "e os anúncios passam a ser não personalizados."
        ),
        LegalSection(
            "5. Permissões do aplicativo",
            "• INTERNET e ACESSO AO ESTADO DA REDE: usadas apenas para carregar anúncios.\n" +
                "• VIBRAR: retorno tátil durante as jogadas. Pode ser desligada em Ajustes.\n" +
                "• NOTIFICAÇÕES (Android 13+): lembretes do Passe Diário e do Desafio Diário. É opcional, " +
                "pedida no momento certo, e o jogo funciona normalmente se você recusar.\n\n" +
                "O aplicativo não pede câmera, microfone, contatos, armazenamento externo nem localização."
        ),
        LegalSection(
            "6. Crianças",
            "Kardia Pulse não é direcionado a menores de 13 anos e não coleta conscientemente dados " +
                "de crianças. Se você é responsável e acredita que uma criança forneceu dados através " +
                "do aplicativo, entre em contato em $CONTACT_EMAIL para que possamos ajudar."
        ),
        LegalSection(
            "7. Segurança",
            "O progresso salvo é assinado criptograficamente com uma chave HMAC gerada dentro do " +
                "Android Keystore do seu aparelho. Isso protege o save contra edição por aplicativos " +
                "de terceiros. A chave nunca sai do dispositivo e nunca é transmitida.\n\n" +
                "O aplicativo não faz conexões de rede em texto puro."
        ),
        LegalSection(
            "8. Seus direitos",
            "Como o tratamento local não depende de servidor, você exerce seus direitos diretamente:\n\n" +
                "• Apagar tudo: Ajustes › Apagar progresso, ou desinstalar o aplicativo.\n" +
                "• Revisar o consentimento de anúncios: Ajustes › Configurações de privacidade.\n" +
                "• Redefinir o ID de publicidade: nas configurações do próprio Android.\n\n" +
                "Sob a LGPD (Lei 13.709/2018) e o GDPR, você também pode nos escrever em " +
                "$CONTACT_EMAIL para qualquer dúvida sobre este tratamento."
        ),
        LegalSection(
            "9. Serviços de terceiros",
            "O aplicativo utiliza o Google AdMob e a plataforma de mensagens ao usuário (UMP) do Google. " +
                "As políticas aplicáveis estão em policies.google.com/technologies/partner-sites e " +
                "policies.google.com/privacy."
        ),
        LegalSection(
            "10. Mudanças nesta política",
            "Se esta política mudar de forma relevante, a nova versão será exibida dentro do aplicativo " +
                "e o aceite será solicitado novamente. Dúvidas: $CONTACT_EMAIL."
        )
    )

    val TERMS: List<LegalSection> = listOf(
        LegalSection(
            "1. Aceitação",
            "Ao instalar ou usar Kardia Pulse você concorda com estes Termos de Uso e com a Política " +
                "de Privacidade. Se não concordar, não utilize o aplicativo. Última atualização: $LAST_UPDATE."
        ),
        LegalSection(
            "2. Licença de uso",
            "$COMPANY concede a você uma licença pessoal, gratuita, não exclusiva, intransferível e " +
                "revogável para usar o aplicativo em dispositivos que você controla, para fins de " +
                "entretenimento pessoal e não comercial."
        ),
        LegalSection(
            "3. O que não é permitido",
            "• Modificar, descompilar ou fazer engenharia reversa do aplicativo, salvo onde a lei " +
                "expressamente permitir.\n" +
                "• Alterar o arquivo de progresso, a memória do processo ou usar aplicativos de trapaça " +
                "para obter Fragmentos, Cristais, poderes ou conquistas.\n" +
                "• Automatizar interações, gerar cliques artificiais em anúncios ou tentar fraudar a " +
                "rede de publicidade. Isso viola também as políticas do Google e pode encerrar seu acesso.\n" +
                "• Redistribuir, revender ou publicar o aplicativo ou partes dele sem autorização."
        ),
        LegalSection(
            "4. Moedas e itens virtuais",
            "Fragmentos, Cristais, poderes e cosméticos são itens virtuais licenciados para uso dentro " +
                "do jogo. Eles NÃO têm valor monetário, não são propriedade do jogador, não podem ser " +
                "trocados por dinheiro e não são transferíveis entre dispositivos ou contas.\n\n" +
                "Itens perdidos por desinstalação, troca de aparelho, falha do dispositivo ou limpeza " +
                "de dados não são repostos."
        ),
        LegalSection(
            "5. Publicidade",
            "O aplicativo é sustentado por anúncios. Os anúncios premiados são sempre opcionais: " +
                "recusá-los não impede o acesso a nenhum modo de jogo, e todas as recompensas também " +
                "podem ser obtidas jogando. A disponibilidade de anúncios depende da rede do Google e " +
                "pode variar."
        ),
        LegalSection(
            "6. Propriedade intelectual",
            "O nome Kardia Pulse, a identidade visual, as regras originais do jogo, os textos, a " +
                "trilha sonora sintetizada e o código-fonte pertencem a $COMPANY, salvo componentes " +
                "de terceiros licenciados separadamente."
        ),
        LegalSection(
            "7. Isenção de garantias",
            "O aplicativo é fornecido \"no estado em que se encontra\". Não garantimos que ele estará " +
                "livre de erros, disponível ininterruptamente ou compatível com todos os aparelhos " +
                "Android existentes."
        ),
        LegalSection(
            "8. Limitação de responsabilidade",
            "Na máxima extensão permitida pela lei aplicável, $COMPANY não responde por danos " +
                "indiretos, incidentais ou consequentes decorrentes do uso do aplicativo, incluindo " +
                "perda de progresso ou de itens virtuais.\n\n" +
                "Nada nestes Termos afasta direitos que o Código de Defesa do Consumidor assegure a você."
        ),
        LegalSection(
            "9. Alterações e encerramento",
            "Podemos atualizar o aplicativo e estes Termos a qualquer momento. Mudanças relevantes " +
                "serão exibidas dentro do jogo. Podemos encerrar a licença de uso em caso de violação " +
                "grave destes Termos."
        ),
        LegalSection(
            "10. Contato e foro",
            "Dúvidas, pedidos e reclamações: $CONTACT_EMAIL.\n\n" +
                "Estes Termos são regidos pelas leis da República Federativa do Brasil. Fica eleito o " +
                "foro de [sua comarca], salvo o foro do domicílio do consumidor, quando aplicável."
        )
    )
}

data class LegalSection(val title: String, val body: String)
