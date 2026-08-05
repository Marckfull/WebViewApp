package com.formatfrute.game.game

import androidx.annotation.StringRes
import com.formatfrute.game.R

/**
 * O segundo ensino do jogo.
 *
 * O tutorial inicial cobre arrastar, fundir e usar poder. Mas o Modo Receita
 * tem uma regra que ninguém adivinha sozinho: **só conta a fruta que você
 * criar** — a que já está no tabuleiro é matéria-prima. Sem explicar isso, o
 * jogador acha que o contador está quebrado.
 *
 * Como no tutorial grande, o passo seguinte só abre depois que o atual for
 * cumprido de verdade.
 */
enum class RecipeCoachStep(
    @StringRes val title: Int,
    @StringRes val text: Int,
    /** Termina num toque de botão, não numa jogada. */
    val manual: Boolean,
    @StringRes val cta: Int = 0,
) {
    PEDIDO(
        title = R.string.coach_pedido_title,
        text = R.string.coach_pedido_text,
        manual = true,
        cta = R.string.coach_pedido_cta,
    ),
    CRIAR(
        title = R.string.coach_criar_title,
        text = R.string.coach_criar_text,
        manual = false,
    ),
    JOGADAS(
        title = R.string.coach_jogadas_title,
        text = R.string.coach_jogadas_text,
        manual = true,
        cta = R.string.coach_jogadas_cta,
    );

    val stepNumber: Int get() = ordinal + 1
    val totalSteps: Int get() = entries.size
}
