package com.formatfrute.game.game

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
    val title: String,
    val text: String,
    /** Termina num toque de botão, não numa jogada. */
    val manual: Boolean,
    val cta: String = "",
) {
    PEDIDO(
        title = "O pedido do freguês 🧾",
        text = "Lá em cima está o que ele quer. Cada fruta tem um contador: " +
            "quantas você já entregou e quantas faltam.",
        manual = true,
        cta = "Entendi!",
    ),
    CRIAR(
        title = "Só vale o que você criar",
        text = "As frutas que já estão no tabuleiro são matéria-prima — elas não " +
            "contam sozinhas. Faça uma fusão e veja o contador andar!",
        manual = false,
    ),
    JOGADAS(
        title = "Jogada é contada ⭐",
        text = "Você tem um número certo de jogadas. Terminar o pedido sobrando " +
            "jogada vale estrela: três se sobrar bastante.",
        manual = true,
        cta = "Bora cozinhar!",
    );

    val stepNumber: Int get() = ordinal + 1
    val totalSteps: Int get() = entries.size
}
