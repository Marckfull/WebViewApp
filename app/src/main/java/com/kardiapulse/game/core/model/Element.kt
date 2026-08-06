package com.kardiapulse.game.core.model

/**
 * Os cinco elementos de Kardia. O elemento importa apenas para a RESSONÂNCIA:
 * duas cartas ressoam quando compartilham o elemento, e o Éter ressoa com tudo.
 *
 * Ressonância é a única forma de inverter a direção do Núcleo — é o coração da mecânica.
 */
enum class Element(val ptName: String, val glyph: String) {
    FOGO("Fogo", "▲"),
    AGUA("Água", "▼"),
    TERRA("Terra", "■"),
    AR("Ar", "◆"),
    ETER("Éter", "✦");

    val isWild: Boolean get() = this == ETER

    companion object {
        /** Elementos comuns (sem o curinga), usados na montagem do baralho. */
        val COMMON: List<Element> = listOf(FOGO, AGUA, TERRA, AR)

        /** Duas cartas ressoam se compartilham elemento ou se uma delas é Éter. */
        fun resonates(a: Element, b: Element): Boolean = a == b || a.isWild || b.isWild
    }
}
