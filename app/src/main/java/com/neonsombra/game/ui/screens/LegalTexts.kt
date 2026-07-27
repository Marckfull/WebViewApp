package com.neonsombra.game.ui.screens

/**
 * Textos legais do jogo.
 *
 * Ficam em Kotlin, e nao em strings.xml, porque sao blocos longos de varios
 * paragrafos -- aqui eles continuam legiveis e sem escapes de XML.
 *
 * Importante: eles descrevem exatamente o que o app faz hoje. Se um dia o jogo
 * passar a usar internet, anuncios ou placar online, estes textos precisam ser
 * atualizados junto.
 */
object LegalTexts {

    const val VERSION = "1.0"
    const val LAST_UPDATE = "Julho de 2026"

    val TERMS = """
        1. ACEITACAO

        Ao instalar e usar o NEON SOMBRA voce concorda com estes Termos de Uso.
        Se nao concordar, basta desinstalar o aplicativo.

        2. O QUE E O JOGO

        O NEON SOMBRA e um jogo de encaixe de blocos, gratuito e para
        entretenimento. Ele funciona inteiramente no seu aparelho e nao precisa
        de conexao com a internet para ser jogado.

        3. USO PERMITIDO

        Voce pode jogar quantas vezes quiser, no seu aparelho, para uso pessoal.
        Nao e permitido revender o aplicativo, cobrar por ele, modificar o
        pacote instalado para distribuir como se fosse seu, nem usar o jogo para
        qualquer finalidade ilegal.

        4. CONTA E APELIDO

        O jogo pede um apelido apenas para mostrar no painel e ao lado do
        recorde. Esse apelido fica salvo somente no seu aparelho. Nao existe
        cadastro, login, senha ou servidor.

        5. RECORDES

        O recorde e guardado localmente. Apagar os dados do aplicativo,
        desinstala-lo ou limpar o armazenamento apaga o recorde para sempre.

        6. SAUDE E BOM SENSO

        O jogo tem luzes fortes, cores vivas e efeitos piscantes. Se voce tem
        sensibilidade a estimulos luminosos, jogue com moderacao e em ambiente
        iluminado. Faca pausas regulares.

        7. GARANTIA

        O jogo e oferecido "como esta". Fazemos o possivel para que funcione bem
        em todos os aparelhos, mas nao garantimos que estara livre de falhas ou
        que rodara igual em todo modelo de celular.

        8. LIMITE DE RESPONSABILIDADE

        Na medida permitida pela lei, os responsaveis pelo jogo nao respondem
        por perdas indiretas decorrentes do uso do aplicativo, como perda de
        recordes ou de tempo de jogo.

        9. MUDANCAS

        Estes termos podem ser atualizados em novas versoes do jogo. A versao
        vigente e sempre a que aparece dentro do aplicativo.

        10. LEI APLICAVEL

        Estes termos seguem a legislacao brasileira.
    """.trimIndent()

    val PRIVACY = """
        RESUMO RAPIDO

        O NEON SOMBRA nao coleta, nao envia e nao vende nenhum dado seu. Tudo o
        que o jogo guarda fica dentro do seu proprio aparelho.

        1. O QUE E GUARDADO

        - O apelido que voce digita.
        - A sua maior pontuacao.
        - Suas preferencias de musica, efeitos sonoros, vibracao e peca fantasma.
        - Se voce ja aceitou estes termos e ja viu o tutorial.

        Esses dados ficam nas preferencias locais do aplicativo, no seu aparelho.

        2. O QUE NAO E COLETADO

        O jogo nao coleta nome real, e-mail, telefone, contatos, fotos,
        localizacao, identificadores de publicidade nem dados de navegacao.

        3. INTERNET

        O aplicativo nao pede permissao de internet e nao envia informacao para
        nenhum servidor. Nao existem anuncios nem ferramentas de analise de uso
        dentro do jogo.

        4. PERMISSOES

        A unica permissao pedida e a de vibracao (VIBRATE), usada apenas para o
        retorno tatil durante a partida. Voce pode desligar a vibracao a
        qualquer momento nas configuracoes do jogo.

        5. CRIANCAS

        O jogo nao coleta dados de ninguem, de qualquer idade, e por isso pode
        ser usado por criancas com o acompanhamento dos responsaveis.

        6. COMO APAGAR SEUS DADOS

        Basta limpar os dados do aplicativo nas configuracoes do Android ou
        desinstalar o jogo. Nao ha copia em nenhum outro lugar.

        7. CONTATO

        Duvidas sobre privacidade podem ser enviadas ao responsavel pela
        publicacao do aplicativo na loja onde voce o baixou.
    """.trimIndent()
}
