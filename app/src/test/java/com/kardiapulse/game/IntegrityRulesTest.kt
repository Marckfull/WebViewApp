package com.kardiapulse.game

import com.kardiapulse.game.core.model.PowerType
import com.kardiapulse.game.data.PlayerProfile
import com.kardiapulse.game.security.IntegrityRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O objetivo destes testes é dizer que a barreira funciona **sem** atrapalhar quem joga normal.
 * Por isso metade deles verifica que ganhos legítimos passam intactos.
 */
class IntegrityRulesTest {

    private val base = PlayerProfile()

    @Test
    fun `ganho normal de um duelo passa intacto`() {
        val after = base.copy(
            xp = base.xp + 240,
            shards = base.shards + 130,
            crystals = base.crystals + 1,
            duelsPlayed = 1,
            duelsWon = 1,
            bestChain = 4,
            totalFlips = 7
        )
        val result = IntegrityRules.validate(base, after)
        assertTrue("Ganho legítimo foi barrado: ${result.violations}", result.clean)
        assertEquals(after, result.profile)
    }

    @Test
    fun `recompensa grande de conquistas em lote ainda passa`() {
        // Pior caso realista: várias conquistas caindo na mesma gravação.
        val after = base.copy(shards = base.shards + 4_500, xp = base.xp + 3_000)
        val result = IntegrityRules.validate(base, after)
        assertTrue("Lote de conquistas foi barrado: ${result.violations}", result.clean)
    }

    @Test
    fun `gastar na loja passa intacto`() {
        val rich = base.copy(shards = 5_000, crystals = 60)
        val after = rich.copy(shards = 4_800, crystals = 20)
        val result = IntegrityRules.validate(rich, after)
        assertTrue("Compra legítima foi barrada: ${result.violations}", result.clean)
        assertEquals(4_800, result.profile.shards)
        assertEquals(20, result.profile.crystals)
    }

    @Test
    fun `o classico 999999999 de fragmentos e rejeitado`() {
        val after = base.copy(shards = 999_999_999)
        val result = IntegrityRules.validate(base, after)
        assertFalse(result.clean)
        assertEquals("O valor anterior deve ser mantido", base.shards, result.profile.shards)
        assertTrue(result.profile.suspicious)
    }

    @Test
    fun `salto absurdo de xp e rejeitado`() {
        val result = IntegrityRules.validate(base, base.copy(xp = 50_000_000))
        assertFalse(result.clean)
        assertEquals(base.xp, result.profile.xp)
    }

    @Test
    fun `valores negativos sao rejeitados`() {
        val result = IntegrityRules.validate(base, base.copy(shards = -1, crystals = -99))
        assertFalse(result.clean)
        assertEquals(base.shards, result.profile.shards)
        assertEquals(base.crystals, result.profile.crystals)
    }

    @Test
    fun `mais vitorias do que duelos e impossivel`() {
        val previous = base.copy(duelsPlayed = 10, duelsWon = 4)
        val result = IntegrityRules.validate(previous, previous.copy(duelsWon = 900))
        assertFalse(result.clean)
        assertTrue(result.profile.duelsWon <= result.profile.duelsPlayed)
    }

    @Test
    fun `contador de duelos nao anda para tras nem pula`() {
        val previous = base.copy(duelsPlayed = 10)
        assertFalse(IntegrityRules.validate(previous, previous.copy(duelsPlayed = 3)).clean)
        assertFalse(IntegrityRules.validate(previous, previous.copy(duelsPlayed = 500)).clean)
        // Um duelo a mais é exatamente o esperado.
        assertTrue(IntegrityRules.validate(previous, previous.copy(duelsPlayed = 11)).clean)
    }

    @Test
    fun `pilha de poderes e aparada em vez de bloquear o jogador`() {
        val after = base.copy(powers = mapOf(PowerType.ECO to 5_000))
        val result = IntegrityRules.validate(base, after)
        assertFalse(result.clean)
        assertEquals(IntegrityRules.MAX_POWER_STACK, result.profile.powers[PowerType.ECO])
    }

    @Test
    fun `cosmetico equipado sem ter sido comprado volta para o padrao`() {
        val after = base.copy(activeCardBack = "dorso_eter", activeBoard = "mesa_forja")
        val result = IntegrityRules.validate(base, after)
        assertEquals("dorso_padrao", result.profile.activeCardBack)
        assertEquals("mesa_padrao", result.profile.activeBoard)
    }

    @Test
    fun `cosmetico comprado de verdade pode ser equipado`() {
        val owner = base.copy(cosmetics = base.cosmetics + "dorso_eter")
        val result = IntegrityRules.validate(owner, owner.copy(activeCardBack = "dorso_eter"))
        assertTrue(result.clean)
        assertEquals("dorso_eter", result.profile.activeCardBack)
    }

    @Test
    fun `recordes impossiveis sao rejeitados`() {
        val result = IntegrityRules.validate(
            base,
            base.copy(bestChain = 9_000, endlessBest = 99_999, streakDays = 100_000)
        )
        assertFalse(result.clean)
        assertEquals(base.bestChain, result.profile.bestChain)
        assertEquals(base.endlessBest, result.profile.endlessBest)
        assertEquals(base.streakDays, result.profile.streakDays)
    }

    @Test
    fun `o perfil sobrevive a uma ida e volta pelo json`() {
        val profile = base.copy(
            xp = 4_321,
            shards = 987,
            crystals = 12,
            undoCharges = 3,
            powers = mapOf(PowerType.ECO to 2, PowerType.VISAO to 1),
            duelsPlayed = 40,
            duelsWon = 25,
            bestChain = 9,
            achievements = setOf("primeiro_pulso", "corrente_5"),
            cosmetics = setOf("dorso_padrao", "mesa_padrao", "dorso_eter"),
            activeCardBack = "dorso_eter",
            streakDays = 6,
            tutorialDone = true,
            suspicious = true
        )
        val restored = PlayerProfile.fromJson(profile.toJson())
        assertEquals(profile, restored)
    }
}
