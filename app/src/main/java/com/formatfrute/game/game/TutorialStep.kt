package com.formatfrute.game.game

import com.formatfrute.game.core.Direction

enum class TutorialSpot { TABULEIRO, PODERES, PREMIADO, PLACAR }

/**
 * Tutorial guiado. Cada passo so libera o proximo depois que o jogador faz de
 * verdade o que foi pedido — nada de "pular tudo e se perder depois".
 */
enum class TutorialStep(
    val title: String,
    val text: String,
    val requiredDirection: Direction? = null,
    val blockHint: String? = null,
    val spot: TutorialSpot = TutorialSpot.TABULEIRO,
    /** Passo que termina num toque de botao, nao numa jogada. */
    val manual: Boolean = false,
    val cta: String = "",
) {
    ARRASTAR(
        title = "Passo 1 — Arraste!",
        text = "Deslize o dedo para a DIREITA. Todas as frutas correm para esse lado.",
        requiredDirection = Direction.RIGHT,
        blockHint = "Agora só vale para a direita! ➡️",
    ),
    FUNDIR(
        title = "Passo 2 — Junte iguais",
        text = "Duas cerejas encostando viram um morango. Faça a fusão acontecer!",
        blockHint = "Encoste duas frutas iguais para elas virarem uma só.",
    ),
    EVOLUIR(
        title = "Passo 3 — Faça crescer",
        text = "Continue juntando até aparecer uma UVA. A escada da fruta é assim: " +
            "cereja, morango, uva…",
    ),
    PODER(
        title = "Passo 4 — Use um poder",
        text = "Toque no Martelinho lá embaixo e esmague qualquer fruta. Poderes salvam " +
            "tabuleiro travado.",
        spot = TutorialSpot.PODERES,
    ),
    PREMIADO(
        title = "Passo 5 — Poder de graça",
        text = "Acabaram os poderes? Assista um vídeo rapidinho e ganhe na hora, sem " +
            "gastar semente nenhuma.",
        spot = TutorialSpot.PREMIADO,
        manual = true,
        cta = "Entendi!",
    ),
    FIM(
        title = "Pronto, feirante! 🎉",
        text = "Você ganhou 150 sementes, um Martelinho e um Voltar no Tempo. Agora é " +
            "com você: encha essa cesta!",
        spot = TutorialSpot.PLACAR,
        manual = true,
        cta = "Bora jogar!",
    );

    val stepNumber: Int get() = ordinal + 1
    val totalSteps: Int get() = entries.size
}
