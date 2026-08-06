package com.kardiapulse.game.core.model

/**
 * Poderes consumíveis. São o recurso que o jogador ganha assistindo anúncios premiados
 * ou comprando com Fragmentos. Usar um poder NÃO encerra o turno.
 */
enum class PowerType(
    val ptName: String,
    val description: String,
    val glyph: String,
    val shardPrice: Int
) {
    INVERSOR(
        ptName = "Inversor",
        description = "Destrava a direção do Núcleo. Sua próxima carta pode ir para qualquer lado, sem precisar de ressonância.",
        glyph = "⇄",
        shardPrice = 120
    ),
    DESCARGA(
        ptName = "Descarga",
        description = "Zera o Núcleo na hora, mantendo a direção. Perfeito para escapar de uma sobrecarga iminente.",
        glyph = "⊘",
        shardPrice = 150
    ),
    VISAO(
        ptName = "Visão",
        description = "Revela a mão do oponente pelos próximos dois turnos.",
        glyph = "◉",
        shardPrice = 90
    ),
    ECO(
        ptName = "Eco",
        description = "Sua próxima carta não encerra o turno. Jogue duas vezes seguidas.",
        glyph = "≡",
        shardPrice = 200
    ),
    ESCUDO(
        ptName = "Escudo",
        description = "Anula por completo o próximo dano que você sofreria.",
        glyph = "⬡",
        shardPrice = 170
    );

    companion object {
        val ALL: List<PowerType> = entries.toList()
    }
}
