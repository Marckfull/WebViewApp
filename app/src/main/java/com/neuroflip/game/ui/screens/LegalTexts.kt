package com.neuroflip.game.ui.screens

/**
 * Textos legais exibidos dentro do app.
 *
 * IMPORTANTE ANTES DE PUBLICAR: troque os campos entre colchetes pelos seus
 * dados reais (nome/razão social, e-mail de contato e data de vigência) e
 * hospede as mesmas versões numa URL pública — a Google Play exige um link
 * para a Política de Privacidade na ficha do app.
 */
object LegalTexts {

    const val CONTACT_EMAIL = "[seu-email-de-contato@exemplo.com]"
    const val COMPANY = "[Seu Nome ou Razão Social]"
    const val EFFECTIVE_DATE = "[dd/mm/aaaa]"

    val TERMS = """
        TERMOS DE USO — NEUROFLIP

        Vigência: $EFFECTIVE_DATE

        1. ACEITAÇÃO
        Ao instalar, abrir ou usar o NeuroFlip ("o Jogo"), você concorda com estes
        Termos de Uso. Se não concordar, desinstale o aplicativo e não o utilize.

        2. LICENÇA DE USO
        $COMPANY concede a você uma licença pessoal, limitada, não exclusiva,
        intransferível e revogável para usar o Jogo em dispositivos que você
        controla, exclusivamente para fins de entretenimento pessoal.

        3. O QUE VOCÊ NÃO PODE FAZER
        • Modificar, descompilar ou fazer engenharia reversa do Jogo;
        • Usar programas, scripts ou aparelhos para burlar as regras, o placar,
          a economia de neurônios ou a exibição de anúncios;
        • Redistribuir, vender ou sublicenciar o Jogo ou partes dele;
        • Usar o Jogo para qualquer finalidade ilegal.

        4. MOEDA VIRTUAL (NEURÔNIOS)
        Os "neurônios" são itens virtuais sem valor monetário. Eles não são
        dinheiro, não podem ser trocados por dinheiro, transferidos entre contas
        nem resgatados fora do Jogo. Nós podemos ajustar preços, recompensas e
        regras de obtenção a qualquer momento para equilibrar o Jogo.
        Os neurônios ficam salvos apenas no seu aparelho: se você desinstalar o
        app ou limpar os dados, o progresso e o saldo podem ser perdidos.

        5. ANÚNCIOS E RECOMPENSAS POR VÍDEO
        O Jogo é gratuito e se sustenta com publicidade. Existem dois tipos:
        • Anúncios comuns, exibidos entre partidas;
        • Vídeos premiados (rewarded), que são SEMPRE opcionais. Ao assistir um
          vídeo até o fim, você recebe a recompensa anunciada (neurônios, tempo
          extra, itens ou bônus em dobro).
        Se o vídeo não for concluído, a recompensa não é entregue. Se nenhum
        anúncio estiver disponível (por exemplo, sem internet), o Jogo pode
        oferecer uma recompensa menor de consolo, a nosso critério.
        Nenhuma recompensa do Jogo tem valor em dinheiro e nada aqui constitui
        promessa de ganho financeiro, sorteio, aposta ou prêmio em dinheiro.

        6. COMPRAS
        Esta versão do Jogo não vende itens com dinheiro real. Caso compras
        sejam adicionadas no futuro, elas serão processadas pela loja de
        aplicativos e seguirão as regras de reembolso dela.

        7. DISPONIBILIDADE
        O Jogo é fornecido "no estado em que se encontra". Podemos alterar,
        suspender ou encerrar funcionalidades a qualquer momento, inclusive
        anúncios, níveis e recompensas.

        8. LIMITAÇÃO DE RESPONSABILIDADE
        Na máxima extensão permitida pela lei, $COMPANY não se responsabiliza
        por perdas de progresso, danos indiretos, lucros cessantes ou problemas
        decorrentes do uso do Jogo. Nada nestes Termos afasta direitos que o
        Código de Defesa do Consumidor garante a você.

        9. USO SAUDÁVEL
        Jogue com moderação. Faça pausas regulares. O Jogo usa efeitos de luz,
        som e vibração — se você tem sensibilidade a estímulos luminosos,
        desative as animações do sistema ou evite sessões longas.

        10. MENORES DE IDADE
        O Jogo não é direcionado a crianças menores de 13 anos. Se você tem
        menos de 18 anos, use o Jogo com a supervisão de um responsável.

        11. ALTERAÇÕES
        Podemos atualizar estes Termos. A versão vigente estará sempre dentro do
        app. O uso continuado após a atualização significa concordância.

        12. LEI APLICÁVEL
        Estes Termos são regidos pelas leis da República Federativa do Brasil.

        13. CONTATO
        Dúvidas: $CONTACT_EMAIL
    """.trimIndent()

    val PRIVACY = """
        POLÍTICA DE PRIVACIDADE — NEUROFLIP

        Vigência: $EFFECTIVE_DATE
        Controlador: $COMPANY — $CONTACT_EMAIL

        1. RESUMO EM UMA FRASE
        O NeuroFlip não pede cadastro, não coleta seu nome, e-mail, telefone,
        agenda, fotos nem localização precisa. Seu progresso fica no aparelho.
        O que sai do aparelho é apenas o necessário para exibir anúncios.

        2. DADOS GUARDADOS NO SEU APARELHO
        Salvamos localmente (sem enviar a nenhum servidor nosso):
        • progresso da campanha, estrelas e recordes;
        • saldo de neurônios e itens;
        • preferências de música, efeitos, vibração e tema;
        • data do último bônus diário e o aceite dos Termos.
        Apagar os dados do app ou desinstalá-lo remove essas informações.

        3. DADOS TRATADOS PELA PUBLICIDADE (GOOGLE ADMOB)
        Para exibir anúncios, usamos o Google AdMob, que pode tratar:
        • identificador de publicidade do aparelho (Android Advertising ID);
        • endereço IP e localização aproximada derivada dele;
        • modelo do aparelho, sistema operacional e idioma;
        • interações com o anúncio (visualização, clique, conclusão do vídeo).
        Esse tratamento é feito pelo Google como controlador independente.
        Política do Google: https://policies.google.com/privacy
        Como o Google usa dados de parceiros:
        https://policies.google.com/technologies/partner-sites

        4. BASE LEGAL E SEU CONSENTIMENTO (LGPD / GDPR)
        Onde a lei exige, mostramos um formulário de consentimento antes de
        qualquer anúncio personalizado (Google UMP). Você pode aceitar apenas
        anúncios não personalizados — o Jogo continua funcionando igual.
        Você pode revisar ou mudar sua escolha a qualquer momento em
        Configurações › Opções de privacidade.
        Bases legais usadas: consentimento (anúncios personalizados) e legítimo
        interesse (funcionamento do app, prevenção a fraude e medição básica).

        5. PERMISSÕES DO APLICATIVO
        • INTERNET e ESTADO DA REDE: baixar anúncios;
        • VIBRAR: retorno tátil do jogo (pode ser desligado nas Configurações);
        • com.google.android.gms.permission.AD_ID: identificador de publicidade.
        Não pedimos câmera, microfone, contatos, arquivos nem localização GPS.

        6. CRIANÇAS
        O Jogo não é direcionado a menores de 13 anos e não coleta
        conscientemente dados dessa faixa etária. Se você é responsável e
        acredita que uma criança forneceu dados, escreva para $CONTACT_EMAIL
        que faremos a exclusão.

        7. COMPARTILHAMENTO
        Não vendemos dados. O compartilhamento se limita ao Google AdMob e aos
        serviços do Google Play necessários para o app funcionar.

        8. RETENÇÃO
        Os dados locais permanecem enquanto o app estiver instalado. Os dados
        de publicidade seguem as políticas de retenção do Google.

        9. SEUS DIREITOS (art. 18 da LGPD)
        Você pode pedir confirmação de tratamento, acesso, correção, anonimização,
        portabilidade, eliminação e revogação do consentimento. Para dados sob
        nosso controle, escreva para $CONTACT_EMAIL — respondemos em até 15 dias.
        Para os dados de publicidade, use também os controles do Google:
        Configurações do Android › Google › Anúncios (redefinir ou excluir o
        identificador de publicidade).

        10. SEGURANÇA
        Não mantemos servidores com seus dados. As informações locais ficam na
        área privada do aplicativo, protegida pelo sistema Android.

        11. ALTERAÇÕES
        Se esta política mudar, publicaremos a nova versão dentro do app com
        nova data de vigência.

        12. CONTATO
        $COMPANY — $CONTACT_EMAIL
    """.trimIndent()
}
