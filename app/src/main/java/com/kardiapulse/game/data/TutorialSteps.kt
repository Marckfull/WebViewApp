package com.kardiapulse.game.data

/** O que a etapa exige antes de liberar a próxima. */
enum class TutorialGoal {
    /** Basta ler e tocar em continuar. */
    CONTINUAR,
    JOGAR_CARTA,
    SEGUIR_DIRECAO,
    INVERTER,
    CORRENTE,
    USAR_PODER,
    VENCER_RODADA
}

data class TutorialStep(
    val title: String,
    val body: String,
    val goal: TutorialGoal,
    val hint: String
)

/**
 * O tutorial de Kardia.
 *
 * Cada etapa só libera a seguinte depois que a ação foi realmente executada dentro de um duelo
 * de verdade — nada de slides. O jogador aprende a mecânica fazendo, e a ordem é a mesma em que
 * as regras importam durante uma partida.
 */
object TutorialSteps {

    val ALL: List<TutorialStep> = listOf(
        TutorialStep(
            title = "O Núcleo",
            body = "Tudo em Kardia gira em torno de um único número: o Núcleo. Ele começa em 0 e " +
                "cada carta jogada o empurra para cima ou para baixo. Se ele passar do limite, " +
                "quem estava jogando sobrecarrega e perde vida.",
            goal = TutorialGoal.CONTINUAR,
            hint = "Olhe o medidor no centro da tela."
        ),
        TutorialStep(
            title = "A carta não tem sinal",
            body = "Toque em qualquer carta da sua mão. Repare que aparecem DUAS opções: ▲ para " +
                "somar e ▼ para subtrair. O valor é o mesmo — quem decide a direção é você.",
            goal = TutorialGoal.JOGAR_CARTA,
            hint = "Toque em uma carta e escolha ▲ ou ▼."
        ),
        TutorialStep(
            title = "A direção travou",
            body = "Essa é a regra que muda tudo: a primeira carta da rodada TRAVA a direção. " +
                "Agora todo mundo é obrigado a continuar empurrando o Núcleo para o mesmo lado, " +
                "aproximando ele do limite a cada jogada.",
            goal = TutorialGoal.CONTINUAR,
            hint = "Veja o indicador de direção abaixo do medidor."
        ),
        TutorialStep(
            title = "Siga a direção",
            body = "Jogue mais uma carta seguindo a direção travada. Repare que a opção contrária " +
                "está bloqueada — e que o espaço até o limite está encolhendo.",
            goal = TutorialGoal.SEGUIR_DIRECAO,
            hint = "Só a polaridade que segue a seta está liberada."
        ),
        TutorialStep(
            title = "Ressonância: a saída",
            body = "Existe uma única forma de inverter a direção: jogar uma carta que RESSOA com a " +
                "última jogada — mesmo elemento, ou um Éter, que ressoa com tudo.\n\n" +
                "As cartas que podem inverter agora estão com a borda brilhando na sua mão.",
            goal = TutorialGoal.INVERTER,
            hint = "Procure a carta com borda pulsante e jogue no sentido contrário."
        ),
        TutorialStep(
            title = "Corrente",
            body = "Ressonâncias seguidas formam uma Corrente, e correntes longas rendem poderes de " +
                "graça. Encadeie ressonâncias até a Corrente chegar a 3.",
            goal = TutorialGoal.CORRENTE,
            hint = "Jogue cartas que ressoam com a anterior, uma atrás da outra."
        ),
        TutorialStep(
            title = "O Colapso",
            body = "Quanto mais a rodada se arrasta, mais o campo aperta: o limite do Núcleo encolhe " +
                "sozinho, e as faixas vermelhas nas pontas do medidor mostram o espaço já perdido.\n\n" +
                "É por isso que nenhuma rodada dura para sempre.",
            goal = TutorialGoal.CONTINUAR,
            hint = "Repare nas pontas vermelhas do medidor."
        ),
        TutorialStep(
            title = "Poderes",
            body = "Poderes são a sua carta na manga e não gastam o turno. A Descarga zera o Núcleo, " +
                "o Inversor destrava a direção, o Escudo anula um dano.\n\n" +
                "Use um poder agora.",
            goal = TutorialGoal.USAR_PODER,
            hint = "Toque em um poder na barra acima da sua mão."
        ),
        TutorialStep(
            title = "Encurrale o rival",
            body = "Você não vence fazendo pontos: vence deixando o rival SEM JOGADA LEGAL. Empurre " +
                "o Núcleo para a beirada guardando uma ressonância na mão e feche a saída dele.",
            goal = TutorialGoal.VENCER_RODADA,
            hint = "Deixe o Núcleo perto do limite quando for a vez dele."
        )
    )

    const val COMPLETION_SHARDS = 250
    const val COMPLETION_CRYSTALS = 2
}
