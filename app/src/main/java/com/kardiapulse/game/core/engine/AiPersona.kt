package com.kardiapulse.game.core.engine

/**
 * O temperamento de um rival.
 *
 * A dificuldade controla o quanto a IA **enxerga** (profundidade da busca e número de mundos
 * simulados). A persona controla o que ela **quer**: os pesos da função de avaliação.
 *
 * Duas IAs na mesma dificuldade com personas diferentes jogam de formas visivelmente distintas —
 * uma cola no limite para sufocar, a outra recua e deixa o Colapso trabalhar. É isso que faz a
 * Sobrevivência parecer dez adversários em vez de um mesmo adversário em dez velocidades.
 */
enum class AiPersona(
    val ptName: String,
    val tell: String,
    /** Peso da diferença de vida. */
    val hpWeight: Int,
    /** Peso do aperto do campo. Alto = quer o Núcleo colado no limite. */
    val squeezeWeight: Int,
    /** Peso do número de jogadas disponíveis. Alto = odeia ficar sem opção. */
    val optionsWeight: Int,
    /** Peso de guardar cartas que ressoam. Alto = economiza inversões. */
    val resonanceWeight: Int,
    /** Peso de negar ressonância ao oponente. */
    val denyWeight: Int,
    /** Peso de manter cartas que ainda cabem na folga. */
    val flexWeight: Int,
    /** Peso dos poderes no inventário. */
    val powerWeight: Int,
    /** Ruído somado à avaliação. Alto = joga de um jeito menos previsível. */
    val noise: Int
) {
    EQUILIBRADO(
        ptName = "Equilibrado",
        tell = "Joga o livro. Não erra e não arrisca.",
        hpWeight = 40, squeezeWeight = 6, optionsWeight = 3,
        resonanceWeight = 18, denyWeight = 15, flexWeight = 4, powerWeight = 25, noise = 0
    ),
    AGRESSIVO(
        ptName = "Agressivo",
        tell = "Empurra o Núcleo para a beirada desde a primeira carta.",
        hpWeight = 30, squeezeWeight = 14, optionsWeight = 1,
        resonanceWeight = 10, denyWeight = 22, flexWeight = 1, powerWeight = 15, noise = 0
    ),
    AVARENTO(
        ptName = "Avarento",
        tell = "Segura ressonâncias como se fossem ouro. Inverte só quando dói.",
        hpWeight = 38, squeezeWeight = 4, optionsWeight = 4,
        resonanceWeight = 34, denyWeight = 10, flexWeight = 6, powerWeight = 30, noise = 0
    ),
    PACIENTE(
        ptName = "Paciente",
        tell = "Evita o confronto e deixa o Colapso fazer o trabalho sujo.",
        hpWeight = 45, squeezeWeight = 2, optionsWeight = 7,
        resonanceWeight = 20, denyWeight = 8, flexWeight = 10, powerWeight = 28, noise = 0
    ),
    IMPREVISIVEL(
        ptName = "Imprevisível",
        tell = "Faz escolhas estranhas que às vezes funcionam bem demais.",
        hpWeight = 34, squeezeWeight = 8, optionsWeight = 2,
        resonanceWeight = 14, denyWeight = 12, flexWeight = 3, powerWeight = 20, noise = 90
    );

    companion object {
        val ALL: List<AiPersona> = entries.toList()

        /** Persona estável para um índice — o rival N da Sobrevivência é sempre o mesmo sujeito. */
        fun forIndex(index: Int): AiPersona = ALL[((index % ALL.size) + ALL.size) % ALL.size]
    }
}
