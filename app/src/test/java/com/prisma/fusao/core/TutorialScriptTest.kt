package com.prisma.fusao.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O tutorial é a primeira coisa que um jogador novo vê. Se um dos tabuleiros
 * estiver errado, ele trava logo na abertura — por isso cada passo é verificado:
 * o tabuleiro não pode começar resolvido, a jogada indicada tem que ser válida, e
 * o resultado tem que ser exatamente a lição que o passo promete.
 */
class TutorialScriptTest {

    private fun goalOf(engine: GameEngine, step: TutorialScript.Step): Pair<Pos, Pos>? =
        when (val goal = step.goal) {
            is TutorialScript.Goal.Swap -> goal.from to goal.to
            is TutorialScript.Goal.ActivateKind -> TutorialScript.findActivatable(engine, goal.kind)
        }

    @Test
    fun `o tutorial inteiro pode ser jogado do inicio ao fim`() {
        val engine = GameEngine(TutorialScript.spec)

        TutorialScript.steps.forEachIndexed { index, step ->
            step.layout?.let { TutorialScript.applyLayout(engine, it) }

            if (step.layout != null) {
                assertTrue(
                    "o passo ${index + 1} começa com um match já formado; " +
                        "o tabuleiro cascatearia sozinho e a lição se perderia",
                    !engine.hasMatches(),
                )
            }

            val goal = goalOf(engine, step)
            assertNotNull("o passo ${index + 1} não tem alvo jogável", goal)
            goal!!

            assertTrue(
                "a jogada indicada no passo ${index + 1} não é permitida",
                engine.canSwap(goal.first, goal.second),
            )
            val outcome = engine.trySwap(goal.first, goal.second)
            assertTrue("o passo ${index + 1} foi rejeitado", outcome is MoveOutcome.Accepted)
        }
    }

    @Test
    fun `passo 1 estoura tres gemas sem criar peca especial`() {
        val engine = GameEngine(TutorialScript.spec)
        val step = TutorialScript.steps[0]
        TutorialScript.applyLayout(engine, step.layout!!)
        val goal = step.goal as TutorialScript.Goal.Swap

        val outcome = engine.trySwap(goal.from, goal.to) as MoveOutcome.Accepted
        assertEquals(3, outcome.steps.first().cleared.size)
        assertTrue(outcome.steps.first().created.isEmpty())
    }

    @Test
    fun `passo 2 condensa exatamente uma essencia`() {
        val engine = GameEngine(TutorialScript.spec)
        val step = TutorialScript.steps[1]
        TutorialScript.applyLayout(engine, step.layout!!)
        val goal = step.goal as TutorialScript.Goal.Swap

        val outcome = engine.trySwap(goal.from, goal.to) as MoveOutcome.Accepted
        val first = outcome.steps.first()
        assertEquals("um match de 4 estoura 4 gemas", 4, first.cleared.size)
        assertEquals(
            "um match de 4 condensa exatamente uma essência",
            1,
            first.created.count { it.gem.kind == GemKind.ESSENCE },
        )
    }

    @Test
    fun `passo 3 funde duas essencias numa supernova`() {
        val engine = GameEngine(TutorialScript.spec)
        val step = TutorialScript.steps[2]
        TutorialScript.applyLayout(engine, step.layout!!)
        val goal = step.goal as TutorialScript.Goal.Swap

        val outcome = engine.trySwap(goal.from, goal.to) as MoveOutcome.Accepted
        val results = outcome.steps.flatMap { it.fusions }.map { it.result.kind }
        assertTrue("esperava uma fusão", results.isNotEmpty())
        assertTrue(
            "essências da mesma cor têm que gerar uma supernova",
            results.contains(GemKind.SUPERNOVA),
        )
    }

    @Test
    fun `a supernova do passo 3 sobrevive para o passo 4`() {
        val engine = GameEngine(TutorialScript.spec)
        val third = TutorialScript.steps[2]
        TutorialScript.applyLayout(engine, third.layout!!)
        val goal = third.goal as TutorialScript.Goal.Swap
        engine.trySwap(goal.from, goal.to)

        // O passo 4 não remonta o tabuleiro: ele usa a peça que o jogador criou.
        assertEquals(null, TutorialScript.steps[3].layout)
        val target = TutorialScript.findActivatable(engine, GemKind.SUPERNOVA)
        assertNotNull("a supernova precisa continuar no tabuleiro para o passo 4", target)

        val outcome = engine.trySwap(target!!.first, target.second) as MoveOutcome.Accepted
        assertTrue(
            "detonar a supernova deveria limpar bastante coisa",
            outcome.steps.first().cleared.size >= 8,
        )
    }
}
