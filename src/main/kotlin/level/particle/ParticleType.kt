package level.particle

import graphics.Image
import graphics.Texture

class ParticleType(initializer: ParticleType.() -> Unit) {
    var texture: Texture = Image.Misc.ERROR
    /**
     * The minimum amount of ticks this particle can last until being removed from the level
     */
    var minTicksToDisappear = 60
    /**
     * The maximum amount of ticks this particle can last before being removed from the level
     */
    var maxTicksToDisappear = 60
    /**
     * The number of ticks to wait before rotating the particle. Will happen continuously, providing a spinning effect.
     *
     * If the value is -1, it will not rotate at all
     */
    var ticksToRotate = -1

    init {
        initializer()
    }

    companion object {
        val BLOCK_PLACE = ParticleType {
            texture = Image.Particle.BLOCK_PLACE
            maxTicksToDisappear = 120
        }
    }
}