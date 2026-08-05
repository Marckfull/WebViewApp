package com.formatfrute.game.core

import kotlin.random.Random

/**
 * A voz das frutas. A arte já é expressiva — aqui ela ganha texto.
 * Falas curtas de propósito: precisam caber num balãozinho sobre a casa.
 */
object FruitVoice {

    private val arrival: Map<Fruit, List<String>> = mapOf(
        Fruit.CEREJA to listOf(
            "Sou pequena, mas começo tudo!",
            "Cadê minha gêmea?",
        ),
        Fruit.MORANGO to listOf(
            "Nasci pra ser sobremesa.",
            "Alguém falou em torta?",
        ),
        Fruit.UVA to listOf(
            "Roxo é cor de realeza.",
            "Vim em cacho, cheguei sozinha.",
        ),
        Fruit.LIMAO to listOf(
            "Sou azedo e tenho orgulho.",
            "A vida te deu eu.",
        ),
        Fruit.BANANA to listOf(
            "Escorreguei até aqui.",
            "Cuidado com a casca!",
        ),
        Fruit.MACA to listOf(
            "Uma por dia e o médico some.",
            "Sou clássica. Aceita.",
        ),
        Fruit.LARANJA to listOf(
            "Cheguei espremida!",
            "Vitamina C ambulante.",
        ),
        Fruit.PERA to listOf(
            "Demorei, mas cheguei.",
            "Esse formato é exclusivo.",
        ),
        Fruit.KIWI to listOf(
            "Peludo fora, doce dentro.",
            "Fui difícil, né? Assume.",
        ),
        Fruit.ABACAXI to listOf(
            "Sou um abacaxi. Resolve aí.",
            "Coroa na cabeça, respeita!",
        ),
        Fruit.PITAYA to listOf(
            "Fruta do dragão chegou! 🐉",
            "Cara na feira, rara aqui.",
        ),
        Fruit.MELANCIA to listOf(
            "CHEGUEI! 🎉",
            "Oito quilos de vitória.",
            "Você conseguiu. Sério mesmo.",
        ),
    )

    private val combo = listOf(
        "Tá pegando fogo! 🔥",
        "Isso virou salada!",
        "Não para não!",
        "O liquidificador enlouqueceu!",
        "A feira inteira te viu.",
    )

    private val defeat = listOf(
        "Ficou pra próxima!",
        "O tabuleiro travou... acontece.",
        "Quase! Bora de novo?",
        "A gente perdoa. Uma vez.",
    )

    private val victory = listOf(
        "Cesta cheia! 🎉",
        "O freguês aplaudiu!",
        "Isso é que é feirante!",
        "Fechou com estilo!",
    )

    private val boss = listOf(
        "Você é doce demais pra mim!",
        "Vou azedar seu dia! 🍋",
        "Argh! Que doçura insuportável!",
        "Fruta podre pra você!",
        "Ninguém me vence... quase.",
    )

    private val greeting = listOf(
        "Bom dia! Bora encher a cesta?",
        "Senti sua falta, viu?",
        "A feira abriu. Chega junto!",
        "Hoje é dia de recorde.",
        "Tem cesta nova te esperando!",
        "Vim aqui só pra te chamar.",
    )

    private val orderDone = listOf(
        "Pedido pronto! 🧾",
        "Manda pro freguês!",
        "Saiu da cozinha!",
    )

    private fun pick(list: List<String>, seed: Long): String =
        list[Random(seed).nextInt(list.size)]

    /** Fala de estreia da fruta na partida. Só vale a pena do limão pra cima. */
    fun onArrival(fruit: Fruit, seed: Long): String? {
        if (fruit.ordinal < Fruit.LIMAO.ordinal) return null
        val lines = arrival[fruit] ?: return null
        return pick(lines, seed)
    }

    fun onCombo(seed: Long): String = pick(combo, seed)

    fun onDefeat(seed: Long): String = pick(defeat, seed)

    fun onVictory(seed: Long): String = pick(victory, seed)

    fun onBoss(seed: Long): String = pick(boss, seed)

    fun onOrderDone(seed: Long): String = pick(orderDone, seed)

    /** A "fruta do dia" que cumprimenta o jogador na tela inicial. */
    fun greetingOfTheDay(day: String): Pair<Fruit, String> {
        val seed = day.hashCode().toLong()
        val rng = Random(seed)
        val fruit = Fruit.entries[rng.nextInt(Fruit.entries.size)]
        return fruit to greeting[rng.nextInt(greeting.size)]
    }
}
