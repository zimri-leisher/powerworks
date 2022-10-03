package level.particle

import com.badlogic.gdx.graphics.g2d.TextureRegion
import graphics.Image
import serialization.ObjectIdentifier
import serialization.ObjectList

private var nextId = 0

class ParticleType(initializer: ParticleType.() -> Unit) {

    @ObjectIdentifier
    val id = nextId++

    var texture: TextureRegion = Image.Misc.ERROR

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
        ALL.add(this)
        initializer()
    }

    companion object {
        @ObjectList
        val ALL = mutableListOf<ParticleType>()

        val BLOCK_PLACE = ParticleType {
            texture = Image.Particle.BLOCK_PLACE
            maxTicksToDisappear = 120
        }
    }
}