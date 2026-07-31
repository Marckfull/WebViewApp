package com.prisma.fusao.core

/**
 * Modelo do tabuleiro do PRISMA.
 *
 * O jogo é um match-3 na superfície, mas a mecânica central — a **Fusão de Essências** —
 * muda a forma de pensar cada jogada:
 *
 *  1. Um match de 3 gemas limpa normalmente (como qualquer jogo do gênero).
 *  2. Um match de 4 ou em L/T **não** vira um doce listrado: ele condensa uma **ESSÊNCIA**,
 *     uma gema-catalisadora que fica parada no tabuleiro.
 *  3. Essências podem ser **movidas livremente** (troca com qualquer vizinha, sem precisar
 *     formar match). Elas não explodem ao serem movidas — são um recurso que o jogador
 *     transporta pelo tabuleiro.
 *  4. Quando duas essências ficam **encostadas**, elas se fundem sozinhas:
 *     - cores iguais    -> SUPERNOVA daquela cor (limpa toda a cor + área 5x5)
 *     - cores diferentes-> PRISMA (limpa linha + coluna + braços diagonais)
 *
 * O resultado é um jogo em que a jogada boa não é "onde eu limpo agora", e sim
 * "onde eu deposito essa essência para fundir daqui a duas jogadas".
 */

/** As seis cores jogáveis, nomeadas como gemas. */
enum class GemColor {
    RUBI,
    AMBAR,
    TOPAZIO,
    ESMERALDA,
    SAFIRA,
    AMETISTA,
}

/** Tipo de peça ocupando uma casa. */
enum class GemKind {
    /** Gema comum e colorida: a única que forma match por cor. */
    NORMAL,

    /** Catalisadora criada por match de 4+ ou L/T. Move-se livre e funde por adjacência. */
    ESSENCE,

    /** Fusão de duas essências de cores diferentes. Incolor. */
    PRISM,

    /** Fusão de duas essências da mesma cor. Guarda a cor de origem. */
    SUPERNOVA,

    /**
     * Terceiro nível: fusão de duas peças de nível 2 encostadas.
     *
     * É a jogada mais difícil do jogo por um motivo estrutural: prismas e supernovas
     * **detonam ao serem trocados**, então não dá para caminhar um até o outro como
     * se faz com uma essência. A única forma de encostá-los é fundir duas essências
     * exatamente na casa ao lado de um nível 2 que já estava no tabuleiro.
     */
    NOVA,

    /** Coletável que precisa descer até a base do tabuleiro. Cai, mas não é trocável. */
    PRISMOID,

    /** Bloco de pedra: não cai, não troca, some ao levar dois estouros vizinhos. */
    STONE,
}

/**
 * Uma peça no tabuleiro. O [id] é único e estável enquanto a peça existir — é ele que
 * permite à camada visual animar a mesma gema caindo em vez de recriar sprites.
 */
data class Gem(
    val id: Long,
    val kind: GemKind,
    val color: GemColor? = null,
    /** Vida restante de peças que exigem mais de um estouro (STONE). */
    val hp: Int = 1,
) {
    val isNormal: Boolean get() = kind == GemKind.NORMAL

    /** Só gemas normais entram na detecção de match por cor. */
    val matchable: Boolean get() = kind == GemKind.NORMAL

    /** Peças que a gravidade pode puxar. */
    val fallable: Boolean get() = kind != GemKind.STONE

    /** Peças que o jogador pode arrastar. */
    val swappable: Boolean get() = kind != GemKind.STONE && kind != GemKind.PRISMOID

    /** Essência, prisma, supernova e nova formam a família "especial". */
    val isSpecial: Boolean
        get() = kind == GemKind.ESSENCE || kind == GemKind.PRISM ||
            kind == GemKind.SUPERNOVA || kind == GemKind.NOVA

    /**
     * Nível na escada de fusão. Duas peças **do mesmo nível** encostadas se fundem
     * e sobem um degrau. Zero significa que a peça não participa de fusão.
     */
    val fusionTier: Int
        get() = when (kind) {
            GemKind.ESSENCE -> 1
            GemKind.PRISM, GemKind.SUPERNOVA -> 2
            else -> 0 // a Nova é o topo: não funde com mais nada
        }
}

/** Coordenada no tabuleiro: linha (de cima para baixo) e coluna. */
data class Pos(val r: Int, val c: Int) {
    fun isNeighbor(other: Pos): Boolean {
        val dr = kotlin.math.abs(r - other.r)
        val dc = kotlin.math.abs(c - other.c)
        return dr + dc == 1
    }
}

/**
 * Uma casa do tabuleiro.
 *
 * @param playable false para buracos: recortes que dão formato ao tabuleiro.
 * @param ice camadas de cristal sobre a casa; cada estouro em cima remove uma.
 * @param spawner true quando novas gemas entram por esta casa (topo da coluna).
 */
data class Cell(
    val gem: Gem? = null,
    val playable: Boolean = true,
    val ice: Int = 0,
    val spawner: Boolean = false,
) {
    val isEmpty: Boolean get() = playable && gem == null
}

/** Grade mutável de casas. */
class Board(val rows: Int, val cols: Int) {
    private val cells = Array(rows) { Array(cols) { Cell() } }

    operator fun get(r: Int, c: Int): Cell = cells[r][c]

    operator fun get(p: Pos): Cell = cells[p.r][p.c]

    operator fun set(r: Int, c: Int, cell: Cell) {
        cells[r][c] = cell
    }

    operator fun set(p: Pos, cell: Cell) {
        cells[p.r][p.c] = cell
    }

    fun inBounds(r: Int, c: Int): Boolean = r in 0 until rows && c in 0 until cols

    fun inBounds(p: Pos): Boolean = inBounds(p.r, p.c)

    fun gemAt(r: Int, c: Int): Gem? = if (inBounds(r, c)) cells[r][c].gem else null

    fun gemAt(p: Pos): Gem? = gemAt(p.r, p.c)

    fun setGem(p: Pos, gem: Gem?) {
        cells[p.r][p.c] = cells[p.r][p.c].copy(gem = gem)
    }

    fun positions(): Sequence<Pos> = sequence {
        for (r in 0 until rows) for (c in 0 until cols) yield(Pos(r, c))
    }

    fun playablePositions(): Sequence<Pos> = positions().filter { this[it].playable }

    /** Última linha jogável de uma coluna — onde os prismoides são entregues. */
    fun bottomPlayableRow(c: Int): Int {
        for (r in rows - 1 downTo 0) if (cells[r][c].playable) return r
        return -1
    }

    fun copy(): Board {
        val b = Board(rows, cols)
        for (r in 0 until rows) for (c in 0 until cols) b[r, c] = cells[r][c]
        return b
    }
}
