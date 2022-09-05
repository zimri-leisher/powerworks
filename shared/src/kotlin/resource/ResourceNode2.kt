package resource

import item.Inventory
import level.*
import level.pipe.PipeBlock
import network.LevelObjectReference
import network.ResourceNode2Reference
import player.team.Team
import serialization.Id

// handles transactions for a resource container
// should be the way that they have physicality
// expectations and shit are handled by the network
class ResourceNode2(
    val container: ResourceContainer,
    xTile: Int,
    yTile: Int
) : LevelObject(LevelObjectType.RESOURCE_NODE, xTile * 16, yTile * 16), PipeNetworkVertex {

    private constructor() : this(Inventory(0, 0), 0, 0)

    override var network: PipeNetwork? = null

    override val farEdges = arrayOfNulls<PipeNetworkVertex>(4)

    override val nearEdges = arrayOfNulls<PipeNetworkVertex>(4)
    override val validFarVertex get() = true

    override fun afterAddToLevel(oldLevel: Level) {
        network = PipeNetwork(level)
        network!!.add(this)
        super.afterAddToLevel(oldLevel)
    }

    override fun afterRemoveFromLevel(oldLevel: Level) {
        network?.remove(this)
        network = null
        super.afterRemoveFromLevel(oldLevel)
    }

    fun canInput(resources: ResourceList): Boolean {
        return container.canAddAll(resources)
    }

    fun canOutput(resources: ResourceList): Boolean {
        return container.canRemoveAll(resources)
    }

    fun input(resources: ResourceList) {
        container.add(resources)
    }

    fun output(resources: ResourceList) {
        container.remove(resources)
    }

    fun copy(xTile: Int, yTile: Int, rotation: Int, attachedContainer: ResourceContainer): ResourceNode2 {
        return ResourceNode2(attachedContainer, xTile, yTile).apply { this.rotation = rotation; this.level = this@ResourceNode2.level }
    }

    override fun toReference(): LevelObjectReference {
        return ResourceNode2Reference(this)
    }
}