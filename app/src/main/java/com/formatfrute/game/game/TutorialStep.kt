package com.formatfrute.game.game

import androidx.annotation.StringRes
import com.formatfrute.game.R
import com.formatfrute.game.core.Direction

enum class TutorialSpot { TABULEIRO, PODERES, PREMIADO, PLACAR }

/**
 * Tutorial guiado. Cada passo so libera o proximo depois que o jogador faz de
 * verdade o que foi pedido — nada de "pular tudo e se perder depois".
 */
enum class TutorialStep(
    @StringRes val title: Int,
    @StringRes val text: Int,
    val requiredDirection: Direction? = null,
    @StringRes val blockHint: Int? = null,
    val spot: TutorialSpot = TutorialSpot.TABULEIRO,
    /** Passo que termina num toque de botao, nao numa jogada. */
    val manual: Boolean = false,
    @StringRes val cta: Int = 0,
) {
    ARRASTAR(
        title = R.string.tut_arrastar_title,
        text = R.string.tut_arrastar_text,
        requiredDirection = Direction.RIGHT,
        blockHint = R.string.tut_arrastar_block,
    ),
    FUNDIR(
        title = R.string.tut_fundir_title,
        text = R.string.tut_fundir_text,
        blockHint = R.string.tut_fundir_block,
    ),
    EVOLUIR(
        title = R.string.tut_evoluir_title,
        text = R.string.tut_evoluir_text,
    ),
    PODER(
        title = R.string.tut_poder_title,
        text = R.string.tut_poder_text,
        spot = TutorialSpot.PODERES,
    ),
    PREMIADO(
        title = R.string.tut_premiado_title,
        text = R.string.tut_premiado_text,
        spot = TutorialSpot.PREMIADO,
        manual = true,
        cta = R.string.tut_premiado_cta,
    ),
    FIM(
        title = R.string.tut_fim_title,
        text = R.string.tut_fim_text,
        spot = TutorialSpot.PLACAR,
        manual = true,
        cta = R.string.tut_fim_cta,
    );

    val stepNumber: Int get() = ordinal + 1
    val totalSteps: Int get() = entries.size
}
