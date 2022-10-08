package level.particle

import level.PhysicalLevelObject
import level.block.Block

/**
 * A type of particle effect, usually comprised of multiple particles spawning simultaneously
 *
 * For example, placing a block causes ParticleEffect.BLOCK_PLACE
 *
 * @param L the type of the level object this acts on/around
 */
class ParticleEffect<L : PhysicalLevelObject>(var instantiate: (L) -> Unit = {}) {

    companion object {
        val BLOCK_PLACE = ParticleEffect<Block> {

        }
    }
}