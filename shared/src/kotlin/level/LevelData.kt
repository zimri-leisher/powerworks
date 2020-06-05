package level

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import data.ConcurrentlyModifiableMutableList
import item.weapon.Projectile
import level.entity.robot.BrainRobot
import level.particle.Particle
import serialization.Id

data class LevelData(
        @Id(1)
        val particles: ConcurrentlyModifiableMutableList<Particle>,
        @Id(2)
        val projectiles: MutableList<Projectile>,
        @Id(3)
        val chunks: Array<Chunk>,
        @Id(4)
        val brainRobots: MutableList<BrainRobot>) {

    // transient, client side only
    val ghostObjects = mutableListOf<GhostLevelObject>()

    private constructor() : this(ConcurrentlyModifiableMutableList(), mutableListOf(), arrayOf(), mutableListOf())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LevelData

        if (particles != other.particles) return false
        if (projectiles != other.projectiles) return false
        if (!chunks.contentEquals(other.chunks)) return false
        if (brainRobots != other.brainRobots) return false

        return true
    }

    override fun hashCode(): Int {
        var result = particles.hashCode()
        result = 31 * result + projectiles.hashCode()
        result = 31 * result + chunks.contentHashCode()
        return result
    }
}