package com.kardiapulse.game.notifications

/**
 * As provocações que aparecem nas notificações.
 *
 * Regra de ouro do tom: a piada é sempre com o Núcleo, com o rival ou com a própria situação —
 * nunca com o jogador de um jeito que magoe de verdade. Provocar é convidar para a revanche,
 * não fazer ninguém desinstalar o app.
 */
data class Taunt(val title: String, val body: String)

object Taunts {

    /** Para quem não abre o jogo há um ou dois dias. */
    private val saudade = listOf(
        Taunt("O Núcleo esfriou", "Sem você ele fica parado no zero. Que tristeza."),
        Taunt("Seu baralho mandou notícias", "Disse que está bem, mas a gente sabe que não está."),
        Taunt("Alguém aí lembra de jogar?", "O Éter perguntou por você. Falei que estava ocupado."),
        Taunt("Reunião de emergência", "As cartas votaram: você está de licença não autorizada."),
        Taunt("Silêncio suspeito", "Nenhuma inversão registrada hoje. O rival agradece."),
        Taunt("O Núcleo está de mau humor", "Ele só melhora quando alguém joga uma Corrente decente.")
    )

    /** Para quem está prestes a perder a sequência do Passe Diário. */
    private val sequencia = listOf(
        Taunt("Sua sequência está por um fio", "Um dia a mais parado e ela vira poeira de Fragmento."),
        Taunt("Faltam poucas horas", "Sua sequência olha para o relógio. Depois olha para você."),
        Taunt("Aquela sequência bonita...", "Seria uma pena se alguém simplesmente esquecesse dela."),
        Taunt("Recompensa esperando", "O Passe Diário está aberto e ninguém veio buscar.")
    )

    /** Para quem perdeu o último duelo. */
    private val revanche = listOf(
        Taunt("O rival continua comemorando", "Já faz um tempo. Alguém precisa interromper isso."),
        Taunt("Aquela sobrecarga ainda dói?", "A boa notícia é que dá para desforrar agora."),
        Taunt("Consta nos registros", "Sua última derrota está muito bem documentada. Podemos apagar."),
        Taunt("O rival mandou um oi", "Um oi bem convencido, para ser sincero.")
    )

    /** Para quem venceu o último duelo. */
    private val convite = listOf(
        Taunt("Invicto por enquanto", "Palavra-chave: por enquanto."),
        Taunt("O ranking te chamou", "Disse que tem uma vaga mais em cima com o seu nome."),
        Taunt("Mão quente", "Seria um desperdício deixar essa sequência esfriar."),
        Taunt("Novo rival disponível", "Esse aí diz que lê o Núcleo melhor que você.")
    )

    /** Para o Desafio Diário do dia, que ainda não foi jogado. */
    private val desafio = listOf(
        Taunt("Desafio Diário no ar", "As mesmas cartas para o mundo inteiro. Sem desculpa."),
        Taunt("O desafio de hoje é cruel", "E é exatamente por isso que você vai querer tentar."),
        Taunt("Todo mundo recebeu a mesma mão", "A diferença vai ser só quem joga melhor.")
    )

    enum class Kind { SAUDADE, SEQUENCIA, REVANCHE, CONVITE, DESAFIO }

    /**
     * Escolhe uma provocação. O índice vem de fora (dia atual, por exemplo) para a mesma
     * mensagem não repetir dois dias seguidos.
     */
    fun pick(kind: Kind, rotation: Int): Taunt {
        val pool = when (kind) {
            Kind.SAUDADE -> saudade
            Kind.SEQUENCIA -> sequencia
            Kind.REVANCHE -> revanche
            Kind.CONVITE -> convite
            Kind.DESAFIO -> desafio
        }
        val index = ((rotation % pool.size) + pool.size) % pool.size
        return pool[index]
    }

    fun all(): List<Taunt> = saudade + sequencia + revanche + convite + desafio
}
