package com.formatfrute.game.data

import com.formatfrute.game.core.Power
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonPassTest {

    @Test
    fun `os trinta degraus entregam recompensa valida`() {
        (1..SeasonPass.TIERS).forEach { level ->
            val tier = SeasonPass.tier(level)
            listOf("grátis" to tier.free, "vídeo" to tier.premium).forEach { (faixa, reward) ->
                assertTrue("degrau $level ($faixa) sem rótulo", reward.label.isNotBlank())
                assertTrue("degrau $level ($faixa) sem emoji", reward.emoji.isNotBlank())
                when (reward.kind) {
                    RewardKind.PODER -> assertNotNull(
                        "degrau $level ($faixa) aponta para um poder que não existe",
                        Power.byId(reward.powerId),
                    )
                    RewardKind.PELE -> assertTrue(
                        "degrau $level ($faixa) aponta para uma pele que não existe",
                        BoardTheme.entries.any { it.id == reward.themeId },
                    )
                    RewardKind.SEMENTES -> assertTrue(
                        "degrau $level ($faixa) dá zero semente",
                        reward.amount > 0,
                    )
                    RewardKind.TITULO -> assertTrue(
                        "degrau $level ($faixa) sem título",
                        reward.title.isNotBlank(),
                    )
                }
            }
        }
    }

    @Test
    fun `a faixa de video entrega as tres peles de temporada`() {
        val themes = (1..SeasonPass.TIERS)
            .map { SeasonPass.tier(it).premium }
            .filter { it.kind == RewardKind.PELE }
            .map { it.themeId }
            .toSet()

        assertEquals(3, themes.size)
        assertEquals(BoardTheme.seasonal.map { it.id }.toSet(), themes)
    }

    /**
     * A regra que separa loja e Passe: se o Passe der de graça o que a loja
     * vende, a vitrine perde a função e o jogador perde o motivo de gastar
     * Semente. Nenhuma pele pode estar nos dois lugares.
     */
    @Test
    fun `o Passe nunca entrega uma pele que a loja vende`() {
        val naLoja = BoardTheme.shop.map { it.id }.toSet()
        val noPasse = (1..SeasonPass.TIERS)
            .map { SeasonPass.tier(it).premium }
            .filter { it.kind == RewardKind.PELE }
            .map { it.themeId }
            .toSet()

        assertTrue(
            "peles vendidas E dadas: ${naLoja intersect noPasse}",
            (naLoja intersect noPasse).isEmpty(),
        )
        assertTrue("a loja ficou sem pele para vender", BoardTheme.shop.any { it.price > 0 })
        assertEquals(BoardTheme.entries.size, BoardTheme.shop.size + BoardTheme.seasonal.size)
    }

    @Test
    fun `pele de temporada nao tem preco`() {
        BoardTheme.seasonal.forEach {
            assertEquals("${it.id} está com preço", 0, it.price)
            assertTrue(it.exclusive)
        }
    }

    @Test
    fun `a conta de fichas fecha`() {
        assertEquals(0, SeasonPass.tierOf(0))
        assertEquals(0, SeasonPass.tierOf(SeasonPass.POINTS_PER_TIER - 1))
        assertEquals(1, SeasonPass.tierOf(SeasonPass.POINTS_PER_TIER))
        assertEquals(SeasonPass.TIERS, SeasonPass.tierOf(999_999))

        val (into, need) = SeasonPass.progressInTier(250)
        assertEquals(50, into)
        assertEquals(SeasonPass.POINTS_PER_TIER, need)
    }

    @Test
    fun `partida boa rende mais ficha que partida fraca`() {
        val boa = SeasonPass.pointsForGame(score = 6000, merges = 90, won = true)
        val fraca = SeasonPass.pointsForGame(score = 300, merges = 8, won = false)

        assertTrue(boa > fraca)
        assertTrue(fraca > 0)
    }

    @Test
    fun `a temporada tem nome e prazo`() {
        assertEquals("Temporada de Agosto", SeasonPass.seasonName("2026-08"))
        assertEquals("Temporada de Janeiro", SeasonPass.seasonName("2026-01"))
        assertTrue(SeasonPass.daysLeft() in 1..31)
    }
}
