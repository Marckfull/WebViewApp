package com.kardiapulse.game.core.model

/** Dificuldade da IA. Controla a profundidade da busca e o número de determinizações. */
enum class Difficulty(val ptName: String, val depth: Int, val samples: Int, val blunderChance: Int) {
    FACIL("Fácil", depth = 1, samples = 1, blunderChance = 45),
    NORMAL("Normal", depth = 2, samples = 2, blunderChance = 15),
    DIFICIL("Difícil", depth = 3, samples = 6, blunderChance = 4),
    MESTRE("Mestre", depth = 4, samples = 8, blunderChance = 0);

    companion object {
        val ALL: List<Difficulty> = entries.toList()
    }
}

/** Modificadores de regra usados no modo Caos e nos desafios diários. */
enum class Modifier(val ptName: String, val description: String, val glyph: String) {
    GRAVIDADE(
        "Gravidade",
        "O Núcleo desliza sozinho 1 ponto na direção atual antes de cada turno.",
        "↓"
    ),
    NEVOA(
        "Névoa",
        "Os valores da sua mão ficam ocultos até você tocar na carta.",
        "≋"
    ),
    ESPELHO(
        "Espelho",
        "O limite de sobrecarga cai de 20 para 13. Tudo fica apertado.",
        "◫"
    ),
    FRENESI(
        "Frenesi",
        "Mão reduzida para 3 cartas. Menos opções, mais nervo.",
        "⚡"
    ),
    TEMPESTADE(
        "Tempestade",
        "Depois de cada jogada o elemento do Núcleo muda sozinho. Ressonância vira aposta.",
        "✳"
    );

    companion object {
        val ALL: List<Modifier> = entries.toList()
    }
}

/** Modos de jogo disponíveis no menu. */
enum class GameMode(
    val ptName: String,
    val tagline: String,
    val description: String,
    val glyph: String
) {
    DUELO(
        "Duelo",
        "O confronto clássico",
        "Um contra um até alguém sobrecarregar. Melhor forma de aprender a ler o Núcleo.",
        "⚔"
    ),
    BLITZ(
        "Blitz",
        "12 segundos por turno",
        "O relógio não perdoa. Se o tempo acabar, uma carta aleatória é jogada por você.",
        "⏱"
    ),
    SOBREVIVENCIA(
        "Sobrevivência",
        "Quantos você aguenta?",
        "Adversários em sequência, cada um mais afiado. Sua vida NÃO é restaurada entre duelos.",
        "∞"
    ),
    CAOS(
        "Caos",
        "As regras mudam",
        "Dois modificadores aleatórios por duelo. Nada é o que parece duas vezes seguidas.",
        "✳"
    ),
    DIARIO(
        "Desafio Diário",
        "O mesmo para todo mundo",
        "Um duelo com semente fixa do dia. Todo jogador do mundo recebe exatamente as mesmas cartas.",
        "◈"
    );

    companion object {
        val ALL: List<GameMode> = entries.toList()
    }
}
