package com.formatfrute.game.data

import com.formatfrute.game.core.GameMode
import com.formatfrute.game.core.GameState
import com.formatfrute.game.core.Tile
import com.formatfrute.game.core.TileKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {

    private val zerado = Profile()

    private val completo = Profile(
        totalMerges = 99_999,
        totalGames = 999,
        totalHarvests = 99,
        highestFruit = 11,
        recipeStars = (1..60).associate { it.toString() to 3 },
        powersUsed = 99,
        streak = 7,
        xp = 500_000,
        passPoints = 99_999,
        unlockedThemes = BoardTheme.entries.map { it.id }.toSet(),
    )

    @Test
    fun `catalogo de conquistas e coerente`() {
        assertEquals(
            "há conquistas com id repetido",
            Achievements.all.size,
            Achievements.all.map { it.id }.toSet().size,
        )
        Achievements.all.forEach {
            assertTrue("${it.id} sem meta", it.target > 0)
            assertTrue("${it.id} sem prêmio", it.reward > 0)
            assertTrue("${it.id} sem título", it.title != 0)
            assertTrue("${it.id} sem descrição", it.desc != 0)
            assertTrue("${it.id} sem emoji", it.emoji.isNotBlank())
        }

        // Cada conquista precisa de textos próprios: título repetido é sinal
        // de copiar-e-colar errado no catálogo.
        assertEquals(
            Achievements.all.size,
            Achievements.all.map { it.title }.toSet().size,
        )
    }

    @Test
    fun `jogador novo comeca do zero`() {
        assertEquals(0, Achievements.doneCount(zerado))
        assertTrue(Achievements.pending(zerado).isEmpty())
    }

    @Test
    fun `jogador completo fecha todas`() {
        assertEquals(Achievements.all.size, Achievements.doneCount(completo))
    }

    @Test
    fun `pendente e o que esta pronto e nao foi retirado`() {
        assertEquals(Achievements.all.size, Achievements.pending(completo).size)

        val retirouTudo = completo.copy(
            achievementsClaimed = Achievements.all.map { it.id }.toSet(),
        )
        assertTrue(Achievements.pending(retirouTudo).isEmpty())
    }

    @Test
    fun `progresso nunca passa da meta`() {
        Achievements.all.forEach {
            assertTrue("${it.id} passou da meta", it.progressOf(completo) <= it.target)
        }
    }
}

class VaultTest {

    @Test
    fun `o que entra e o que sai`() {
        listOf(
            "250",
            "coins:250|xp:1000",
            "",
            "acentuação çãé 🍓",
            "a".repeat(2_000),
        ).forEach {
            assertEquals("falhou com '${it.take(20)}'", it, Vault.decode(Vault.encode(it)))
        }
    }

    @Test
    fun `save adulterado e recusado`() {
        val original = Vault.encode("250")
        val adulterado = original.dropLast(2) + "ff"

        assertNull(Vault.decode(adulterado))
        assertNull(Vault.decode("v1:deadbeef:aabb"))
        assertNull(Vault.decode("v1:semdoispontos"))
    }

    @Test
    fun `save antigo em texto puro continua valendo`() {
        // Quem já tinha o jogo instalado não pode perder progresso na atualização.
        assertEquals("250", Vault.decode("250"))
        assertNull(Vault.decode(null))
        assertNull(Vault.decode(""))
    }

    @Test
    fun `o valor nao fica legivel no arquivo`() {
        val guardado = Vault.encode("999999")

        assertTrue(!guardado.contains("999999"))
    }
}

class SavedGameTest {

    private fun partida() = SavedGame(
        mode = GameMode.BATALHA,
        recipeNumber = -1,
        recipeDay = "2026-08-05",
        state = GameState(
            size = 4,
            tiles = listOf(
                Tile(id = 1, level = 3, row = 0, col = 0),
                Tile(id = 2, level = 0, row = 2, col = 3, kind = TileKind.ROTTEN),
                Tile(id = 3, level = 5, row = 1, col = 1, ice = 1),
            ),
            score = 4_200,
            moves = 37,
            nextId = 88,
        ),
        timeLeft = 42,
        movesLeft = 12,
        movesTotal = 30,
        bossHp = 900,
        movesSinceAttack = 2,
        produced = mapOf(3 to 2, 5 to 1),
        merges = 40,
        harvests = 1,
    )

    @Test
    fun `a partida volta exatamente como parou`() {
        val original = partida()
        val voltou = SavedGame.decode(original.encode())

        assertNotNull(voltou)
        assertEquals(original, voltou)
    }

    @Test
    fun `as pecas especiais sobrevivem ao salvamento`() {
        val voltou = SavedGame.decode(partida().encode())!!

        assertEquals(TileKind.ROTTEN, voltou.state.tiles.first { it.id == 2L }.kind)
        assertEquals(1, voltou.state.tiles.first { it.id == 3L }.ice)
        assertEquals(mapOf(3 to 2, 5 to 1), voltou.produced)
    }

    @Test
    fun `save corrompido nao derruba o jogo`() {
        assertNull(SavedGame.decode(null))
        assertNull(SavedGame.decode(""))
        assertNull(SavedGame.decode("lixo"))
        assertNull(SavedGame.decode("pomar|1|x"))
    }
}
