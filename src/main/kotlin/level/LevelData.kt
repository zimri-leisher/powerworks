package level

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import data.ConcurrentlyModifiableMutableList
import item.weapon.Projectile
import level.particle.Particle
import player.Player
import level.entity.robot.BrainRobot

data class LevelData(
        @Tag(1)
        val particles: ConcurrentlyModifiableMutableList<Particle>,
        @Tag(2)
        val projectiles: MutableList<Projectile>,
        @Tag(3)
        val chunks: Array<Chunk>) {

    private constructor() : this(ConcurrentlyModifiableMutableList(), mutableListOf(), arrayOf())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LevelData

        if (particles != other.particles) return false
        if (projectiles != other.projectiles) return false
        if (!chunks.contentEquals(other.chunks)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = particles.hashCode()
        result = 31 * result + projectiles.hashCode()
        result = 31 * result + chunks.contentHashCode()
        return result
    }
}