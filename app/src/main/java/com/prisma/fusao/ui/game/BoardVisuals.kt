package com.prisma.fusao.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.prisma.fusao.core.Cell
import com.prisma.fusao.core.GemColor
import com.prisma.fusao.core.GemKind
import com.prisma.fusao.core.Pos
import com.prisma.fusao.core.ResolveStep
import com.prisma.fusao.ui.theme.highlight
import com.prisma.fusao.ui.theme.primary
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * Uma peça na tela. As coordenadas são em *unidades de casa* (0..cols), não em pixels,
 * então o mesmo estado serve para qualquer tamanho de tabuleiro.
 */
class Sprite(
    val id: Long,
    var kind: GemKind,
    var color: GemColor?,
    var x: Float,
    var y: Float,
    var hp: Int = 1,
) {
    var targetX: Float = x
    var targetY: Float = y
    var scale: Float = 1f
    var targetScale: Float = 1f
    var alpha: Float = 1f
    var targetAlpha: Float = 1f
    var spin: Float = 0f
    var glow: Float = 0f
    var dying: Boolean = false

    /** Velocidade de aproximação: peças caindo puxam mais forte que peças nascendo. */
    var speed: Float = 14f
}

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val color: Color,
    val radius: Float,
)

/** Rastro luminoso de uma detonação. */
class Beam(
    val origin: Pos,
    val cells: List<Pos>,
    val color: Color,
    var life: Float,
    val maxLife: Float,
)

/** Número que sobe na tela ao pontuar. */
class FloatingScore(
    var x: Float,
    var y: Float,
    val text: String,
    var life: Float,
    val maxLife: Float,
    val color: Color,
)

/**
 * Estado visual do tabuleiro.
 *
 * O motor entrega a partida já resolvida em [ResolveStep]s; esta classe reproduz
 * esses quadros ao longo do tempo. Ela é deliberadamente tolerante: se por qualquer
 * motivo a tela sair de sincronia com o tabuleiro real, [reconcile] conserta sem
 * que o jogador perceba.
 */
class BoardVisuals(val rows: Int, val cols: Int) {

    val sprites = LinkedHashMap<Long, Sprite>()
    val particles = mutableListOf<Particle>()
    val beams = mutableListOf<Beam>()
    val floatingScores = mutableListOf<FloatingScore>()
    var ice = Array(rows) { IntArray(cols) }
    var playable = Array(rows) { BooleanArray(cols) { true } }

    /** Incrementado a cada quadro: é o que faz o Canvas se redesenhar. */
    var frame by mutableIntStateOf(0)
        private set

    // Destaque e seleção são estado observável: quando o tabuleiro está parado o
    // contador de quadros para de subir, então eles precisam invalidar o desenho
    // por conta própria.
    /** Casas destacadas pelo tutorial ou pela dica. */
    var hints: Set<Pos> by mutableStateOf(emptySet())

    /** Casa atualmente selecionada pelo toque. */
    var selected: Pos? by mutableStateOf(null)

    /** Sacudida da tela em detonações grandes. */
    var shake: Float = 0f

    private val rng = Random(1)

    // ------------------------------------------------------------ sincronismo

    fun syncFrom(cells: List<List<Cell>>) {
        sprites.clear()
        particles.clear()
        beams.clear()
        floatingScores.clear()
        ice = Array(rows) { r -> IntArray(cols) { c -> cells[r][c].ice } }
        playable = Array(rows) { r -> BooleanArray(cols) { c -> cells[r][c].playable } }
        for (r in 0 until rows) for (c in 0 until cols) {
            val gem = cells[r][c].gem ?: continue
            sprites[gem.id] = Sprite(gem.id, gem.kind, gem.color, c.toFloat(), r.toFloat(), gem.hp)
        }
        frame++
    }

    /**
     * Alinha a tela com o tabuleiro real. Chamado quando a animação de uma jogada
     * termina, para que nenhum erro de reprodução se acumule entre jogadas.
     */
    fun reconcile(cells: List<List<Cell>>) {
        val live = mutableSetOf<Long>()
        for (r in 0 until rows) for (c in 0 until cols) {
            ice[r][c] = cells[r][c].ice
            val gem = cells[r][c].gem ?: continue
            live += gem.id
            val sprite = sprites[gem.id]
            if (sprite == null) {
                sprites[gem.id] = Sprite(gem.id, gem.kind, gem.color, c.toFloat(), r.toFloat(), gem.hp)
                    .also { it.scale = 0f; it.targetScale = 1f }
            } else {
                sprite.kind = gem.kind
                sprite.color = gem.color
                sprite.hp = gem.hp
                sprite.targetX = c.toFloat()
                sprite.targetY = r.toFloat()
                sprite.targetAlpha = 1f
                sprite.targetScale = 1f
                sprite.dying = false
            }
        }
        sprites.entries.removeAll { (id, sprite) -> id !in live && !sprite.dying }
        frame++
    }

    // ------------------------------------------------------------- reprodução

    /** Primeira metade de um quadro: o que some, o que nasce, o que funde. */
    fun playClearPhase(step: ResolveStep) {
        for (blast in step.blasts) {
            val color = blast.color?.primary() ?: Color(0xFFEFF6FF)
            beams += Beam(blast.origin, blast.cells, color, life = BEAM_LIFE, maxLife = BEAM_LIFE)
            shake = maxOf(shake, if (blast.kind == GemKind.SUPERNOVA) 1f else 0.6f)
        }

        for (cleared in step.cleared) {
            val sprite = sprites[cleared.gem.id]
            val color = cleared.gem.color?.primary() ?: Color.White
            burst(cleared.pos, color, count = if (cleared.gem.kind == GemKind.NORMAL) 6 else 14)
            if (sprite != null) {
                sprite.dying = true
                sprite.targetScale = 0f
                sprite.targetAlpha = 0f
                sprite.speed = 22f
            }
        }

        for (broken in step.iceBroken) {
            ice[broken.pos.r][broken.pos.c] = broken.remaining
            burst(broken.pos, Color(0xFFCFEFFF), count = 8)
        }

        for (fusion in step.fusions) {
            // As duas essências correm uma para a outra antes de virar a peça nova.
            listOf(fusion.a, fusion.b).forEach { pos ->
                spriteAt(pos)?.let { sprite ->
                    sprite.targetX = fusion.at.c.toFloat()
                    sprite.targetY = fusion.at.r.toFloat()
                    sprite.targetScale = 0f
                    sprite.targetAlpha = 0f
                    sprite.dying = true
                    sprite.speed = 26f
                }
            }
            val color = fusion.result.color?.primary() ?: Color.White
            burst(fusion.at, color, count = 26, spread = 3.2f)
            beams += Beam(fusion.at, listOf(fusion.at), color, FUSION_FLASH, FUSION_FLASH)
            shake = maxOf(shake, 0.85f)
            sprites[fusion.result.id] = Sprite(
                fusion.result.id,
                fusion.result.kind,
                fusion.result.color,
                fusion.at.c.toFloat(),
                fusion.at.r.toFloat(),
            ).also {
                it.scale = 0f
                it.targetScale = 1f
                it.glow = 1f
                it.speed = 9f
            }
        }

        for (created in step.created) {
            sprites[created.gem.id] = Sprite(
                created.gem.id,
                created.gem.kind,
                created.gem.color,
                created.pos.c.toFloat(),
                created.pos.r.toFloat(),
            ).also {
                it.scale = 0f
                it.targetScale = 1f
                it.glow = 1f
                it.speed = 11f
            }
            burst(created.pos, created.gem.color?.highlight() ?: Color.White, count = 16)
        }

        for (pos in step.prismoidsDelivered) {
            spriteAt(pos)?.let {
                it.dying = true
                it.targetY = rows + 1.5f
                it.targetAlpha = 0f
                it.speed = 8f
            }
            burst(pos, Color(0xFFFFE49A), count = 22, spread = 2.6f)
        }

        if (step.scoreGained > 0) {
            val anchor = step.cleared.firstOrNull()?.pos
                ?: step.fusions.firstOrNull()?.at
                ?: Pos(rows / 2, cols / 2)
            floatingScores += FloatingScore(
                x = anchor.c.toFloat(),
                y = anchor.r.toFloat(),
                text = "+${step.scoreGained}",
                life = SCORE_LIFE,
                maxLife = SCORE_LIFE,
                color = if (step.cascade > 0) Color(0xFFFFD166) else Color.White,
            )
        }
        frame++
    }

    /** Segunda metade: a gravidade puxa tudo para baixo e novas peças entram. */
    fun playFallPhase(step: ResolveStep) {
        for (move in step.moves) {
            sprites[move.id]?.let { sprite ->
                sprite.targetX = move.to.c.toFloat()
                sprite.targetY = move.to.r.toFloat()
                sprite.speed = 15f
            }
        }
        for (spawn in step.spawns) {
            sprites[spawn.gem.id] = Sprite(
                spawn.gem.id,
                spawn.gem.kind,
                spawn.gem.color,
                spawn.from.c.toFloat(),
                spawn.from.r.toFloat(),
                spawn.gem.hp,
            ).also {
                it.targetX = spawn.to.c.toFloat()
                it.targetY = spawn.to.r.toFloat()
                it.speed = 15f
            }
        }
        frame++
    }

    /** Balança as duas peças quando a troca não é válida. */
    fun rejectSwap(a: Pos, b: Pos) {
        val dx = (b.c - a.c) * 0.22f
        val dy = (b.r - a.r) * 0.22f
        spriteAt(a)?.let { it.x += dx; it.y += dy }
        spriteAt(b)?.let { it.x -= dx; it.y -= dy }
        frame++
    }

    fun spriteAt(pos: Pos): Sprite? = sprites.values.firstOrNull {
        !it.dying && it.targetX.toInt() == pos.c && it.targetY.toInt() == pos.r
    }

    private fun burst(pos: Pos, color: Color, count: Int, spread: Float = 2.2f) {
        repeat(count) {
            val angle = rng.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (0.4f + rng.nextFloat()) * spread
            particles += Particle(
                x = pos.c + 0.5f,
                y = pos.r + 0.5f,
                vx = kotlin.math.cos(angle) * speed,
                vy = kotlin.math.sin(angle) * speed - 0.6f,
                life = PARTICLE_LIFE,
                maxLife = PARTICLE_LIFE,
                color = color,
                radius = 0.04f + rng.nextFloat() * 0.07f,
            )
        }
    }

    // ------------------------------------------------------------------ tempo

    /** Avança a simulação. [dt] em segundos. */
    fun update(dt: Float) {
        val step = dt.coerceAtMost(0.05f)
        var changed = false

        val iterator = sprites.entries.iterator()
        while (iterator.hasNext()) {
            val sprite = iterator.next().value
            // Peças especiais giram sem parar; as comuns só contam como "animando"
            // enquanto estiverem longe do alvo. Sem essa distinção o tabuleiro
            // parado redesenharia a 60 fps sem nenhum motivo.
            val moving = abs(sprite.targetX - sprite.x) > 0.001f ||
                abs(sprite.targetY - sprite.y) > 0.001f ||
                abs(sprite.targetScale - sprite.scale) > 0.001f ||
                abs(sprite.targetAlpha - sprite.alpha) > 0.001f ||
                sprite.glow > 0f ||
                sprite.kind != GemKind.NORMAL

            val k = min(1f, step * sprite.speed)
            sprite.x += (sprite.targetX - sprite.x) * k
            sprite.y += (sprite.targetY - sprite.y) * k
            sprite.scale += (sprite.targetScale - sprite.scale) * min(1f, step * 16f)
            sprite.alpha += (sprite.targetAlpha - sprite.alpha) * min(1f, step * 16f)
            if (sprite.kind != GemKind.NORMAL) sprite.spin += step * 1.4f
            if (sprite.glow > 0f) sprite.glow = (sprite.glow - step * 1.6f).coerceAtLeast(0f)
            if (sprite.dying && sprite.scale < 0.02f && sprite.alpha < 0.02f) iterator.remove()
            if (moving) changed = true
        }

        particles.removeAll { particle ->
            particle.life -= step
            particle.x += particle.vx * step
            particle.y += particle.vy * step
            particle.vy += 5.5f * step // gravidade das faíscas
            particle.vx *= 0.98f
            particle.life <= 0f
        }

        beams.removeAll { beam ->
            beam.life -= step
            beam.life <= 0f
        }

        floatingScores.removeAll { score ->
            score.life -= step
            score.y -= step * 1.3f
            score.life <= 0f
        }

        if (shake > 0f) shake = (shake - step * 3.2f).coerceAtLeast(0f)

        if (changed || particles.isNotEmpty() || beams.isNotEmpty() ||
            floatingScores.isNotEmpty() || shake > 0f
        ) {
            frame++
        }
    }

    /** True enquanto alguma peça ainda está longe do lugar — usado para esperar. */
    fun isSettled(): Boolean = sprites.values.none {
        abs(it.x - it.targetX) > 0.02f || abs(it.y - it.targetY) > 0.02f || it.dying
    }

    companion object {
        const val PARTICLE_LIFE = 0.55f
        const val BEAM_LIFE = 0.32f
        const val FUSION_FLASH = 0.45f
        const val SCORE_LIFE = 0.9f
    }
}
