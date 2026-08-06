package com.kardiapulse.game.core.engine

/**
 * Gerador xorshift64* determinístico e serializável em um único Long.
 *
 * Guardar o estado do RNG dentro do [com.kardiapulse.game.core.model.GameState] permite
 * reproduzir uma partida inteira a partir da semente — é isso que faz o Desafio Diário ser
 * idêntico para todos os jogadores e permite que a IA simule futuros sem contaminar o jogo real.
 */
class PulseRandom(seed: Long) {

    var state: Long = if (seed == 0L) FALLBACK_SEED else seed
        private set

    fun nextLong(): Long {
        var x = state
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        state = x
        return x * MULTIPLIER
    }

    /** Inteiro em [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound deve ser positivo" }
        val v = nextLong() ushr 1
        return (v % bound).toInt()
    }

    fun nextInt(fromInclusive: Int, toInclusive: Int): Int =
        fromInclusive + nextInt(toInclusive - fromInclusive + 1)

    fun nextBoolean(): Boolean = nextLong() and 1L == 1L

    fun nextFloat(): Float = (nextLong() ushr 11).toFloat() / (1L shl 53).toFloat()

    /** Sorteia um elemento da lista. */
    fun <T> pick(items: List<T>): T = items[nextInt(items.size)]

    companion object {
        private const val FALLBACK_SEED = 0x2545F4914F6CDD1DL
        private const val MULTIPLIER = 0x2545F4914F6CDD1DL
    }
}

/** Fisher-Yates determinístico usando o [PulseRandom]. */
fun <T> List<T>.shuffledWith(rng: PulseRandom): List<T> {
    val out = this.toMutableList()
    for (i in out.size - 1 downTo 1) {
        val j = rng.nextInt(i + 1)
        val tmp = out[i]
        out[i] = out[j]
        out[j] = tmp
    }
    return out
}
