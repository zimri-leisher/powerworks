package level

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import data.ConcurrentlyModifiableMutableList
import item.weapon.Projectile
import level.entity.robot.BrainRobot
import level.particle.Particle
import resource.ResourceContainer
import resource.ResourceNetwork
import serialization.Id

data class LevelData(
    @Id(1)
    val particles: ConcurrentlyModifiableMutableList<Particle>,
    @Id(2)
    val projectiles: ConcurrentlyModifiableMutableList<Projectile>,
    @Id(3)
    val chunks: Array<Chunk>,
    @Id(5)
    val resourceNetworks: MutableList<ResourceNetwork<*>>,
    @Id(6)
    val resourceContainers: MutableList<ResourceContainer>,
    @Id(4)
    val brainRobots: MutableList<BrainRobot>
) {

    // transient, client side only
    val ghostObjects = mutableListOf<GhostLevelObject>()

    private constructor() : this(
        ConcurrentlyModifiableMutableList(),
        ConcurrentlyModifiableMutableList(),
        arrayOf(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LevelData

        if (particles != other.particles) return false
        if (projectiles != other.projectiles) return false
        if (!chunks.contentEquals(other.chunks)) return false
        if (brainRobots != other.brainRobots) return false
        if (resourceNetworks != other.resourceNetworks) return false
        if (resourceContainers != other.resourceContainers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = particles.hashCode()
        result = 31 * result + projectiles.hashCode()
        result = 31 * result + chunks.contentHashCode()
        result = 31 * result + brainRobots.hashCode()
        result = 31 * result + resourceNetworks.hashCode()
        result = 31 * result + resourceContainers.hashCode()
        return result
    }
}