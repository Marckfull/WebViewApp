package com.kardiapulse.game.core.model

/**
 * Uma carta de Kardia.
 *
 * [value] é a intensidade do pulso (1..9). A carta não tem sinal próprio: quem joga escolhe
 * a POLARIDADE na hora, empurrando o Núcleo para cima (+) ou para baixo (-).
 */
data class Card(
    val id: Int,
    val value: Int,
    val element: Element
) {
    fun resonatesWith(other: Element?): Boolean =
        other != null && Element.resonates(element, other)
}

/** Polaridade escolhida no momento da jogada. */
object Polarity {
    const val POSITIVA = 1
    const val NEGATIVA = -1
}
