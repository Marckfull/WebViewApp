package com.neuroflip.game.domain

/**
 * Catálogo da campanha SINAPSE: 30 níveis com dificuldade e mecânicas progressivas.
 * As mecânicas entram uma a uma para o jogador aprender sem susto.
 */
object LevelCatalog {

    private data class Band(
        val columns: Int,
        val rows: Int,
        val seconds: Int,
        val mutation: Int,
        val specials: Set<CardKind>,
        val eco: Boolean
    )

    private val titles = listOf(
        "Primeiro Contato", "Faísca", "Eco Inicial", "Ruído Branco", "Sinapse Viva",
        "Deriva", "Curto-Circuito", "Corrente Fria", "Ouro Neural", "Espelho Quebrado",
        "Fio Solto", "Reflexo", "Sobrecarga", "Frequência", "Névoa de Dados",
        "Fantasma na Rede", "Reação em Cadeia", "Núcleo Instável", "Vórtice", "Pulso Duplo",
        "Labirinto Sináptico", "Zona Morta", "Tempestade", "Anomalia", "Colapso",
        "Mente Espelhada", "Radiação", "Limiar", "Overdrive", "Córtex Final"
    )

    private fun bandFor(level: Int): Band = when (level) {
        in 1..2 -> Band(3, 4, 0, 0, emptySet(), eco = false)
        in 3..4 -> Band(4, 4, 90, 0, emptySet(), eco = true)
        in 5..7 -> Band(4, 5, 100, 8, emptySet(), eco = true)
        in 8..9 -> Band(4, 6, 120, 7, setOf(CardKind.CRYO), eco = true)
        in 10..11 -> Band(4, 6, 115, 6, setOf(CardKind.CRYO, CardKind.GOLDEN), eco = true)
        in 12..14 -> Band(4, 7, 130, 6, setOf(CardKind.GOLDEN, CardKind.MIRROR), eco = true)
        in 15..17 -> Band(5, 6, 140, 5, setOf(CardKind.CRYO, CardKind.MIRROR), eco = true)
        in 18..20 -> Band(5, 6, 135, 5, setOf(CardKind.GOLDEN, CardKind.PHANTOM), eco = true)
        in 21..23 -> Band(4, 8, 150, 5, setOf(CardKind.PHANTOM, CardKind.MIRROR), eco = true)
        in 24..26 -> Band(6, 6, 165, 4, setOf(CardKind.UNSTABLE, CardKind.CRYO), eco = true)
        in 27..28 -> Band(5, 8, 180, 4, setOf(CardKind.UNSTABLE, CardKind.GOLDEN, CardKind.PHANTOM), eco = true)
        else -> Band(
            6, 7, 190, 3,
            setOf(CardKind.UNSTABLE, CardKind.GOLDEN, CardKind.PHANTOM, CardKind.MIRROR, CardKind.CRYO),
            eco = true
        )
    }

    const val TOTAL_LEVELS = 30

    fun level(id: Int): LevelConfig {
        val safeId = id.coerceIn(1, TOTAL_LEVELS)
        val band = bandFor(safeId)
        val pairs = band.columns * band.rows / 2
        // Par de jogadas: quanto maior o tabuleiro, mais folga proporcional.
        val par = (pairs * 1.6f).toInt() + 2
        return LevelConfig(
            id = safeId,
            title = titles.getOrElse(safeId - 1) { "Nível $safeId" },
            columns = band.columns,
            rows = band.rows,
            timeLimitMs = band.seconds * 1000L,
            mutationEveryMoves = band.mutation,
            specials = band.specials,
            parMoves = par,
            ecoEnabled = band.eco,
            mode = GameMode.CAMPAIGN
        )
    }

    /** BLITZ: 90 segundos, tabuleiro grande, mutação agressiva. Pontuação livre. */
    fun blitz(): LevelConfig = LevelConfig(
        id = 1001,
        title = "Blitz 90",
        columns = 5,
        rows = 6,
        timeLimitMs = 90_000L,
        mutationEveryMoves = 3,
        specials = setOf(CardKind.GOLDEN, CardKind.CRYO, CardKind.PHANTOM),
        parMoves = 0,
        ecoEnabled = true,
        mode = GameMode.BLITZ
    )

    /** ZEN: sem cronômetro, sem mutação. Para relaxar e treinar memória. */
    fun zen(): LevelConfig = LevelConfig(
        id = 1002,
        title = "Zen",
        columns = 4,
        rows = 6,
        timeLimitMs = 0L,
        mutationEveryMoves = 0,
        specials = setOf(CardKind.MIRROR),
        parMoves = 0,
        ecoEnabled = true,
        mode = GameMode.ZEN
    )

    /** Mecânicas novas apresentadas em cada nível (para o balão de tutorial). */
    fun unlockHint(level: Int): String? = when (level) {
        3 -> "ECO ativado: ao acertar um par, as cartas vizinhas piscam por um instante."
        5 -> "MUTAÇÃO: de vez em quando duas cartas trocam de lugar. Siga o rastro!"
        8 -> "Carta CRIO ❄ congela o cronômetro e devolve tempo."
        10 -> "Carta DOURADA ★ vale 3x pontos."
        12 -> "Carta ESPELHO ◈ marca um par escondido pra você."
        18 -> "Carta FANTASMA ⌭ foge de lugar quando você erra com ela."
        24 -> "Carta INSTÁVEL ☢ sacode o tabuleiro se você a esquecer."
        else -> null
    }
}
