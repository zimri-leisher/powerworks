package resource

import level.*
import network.ResourceNodeReference

// handles transactions for a resource container
// should be the way that they have physicality
// should be the way that they filter what resources can be allowed in
// expectations and shit are handled by the network
class ResourceNode(
    val container: ResourceContainer,
    xTile: Int,
    yTile: Int
) : PhysicalLevelObject(PhysicalLevelObjectType.RESOURCE_NODE, xTile * 16, yTile * 16), PotentialPipeNetworkVertex,
    ResourceOrderer {

    private constructor() : this(SourceContainer(), 0, 0)

    private var networks = mutableListOf<ResourceNetwork<*>>()
    override val validFarVertex get() = true

    override var vertex: PipeNetworkVertex? = null

    override fun getNecessaryFlow(order: ResourceOrder): ResourceOrder {
        // no constraints
        return order
    }

    override fun getNetwork(type: ResourceNetworkType) = networks.firstOrNull { it.networkType == type }

    override fun onAddToNetwork(network: ResourceNetwork<*>) {
        networks.add(network)
    }

    override fun onRemoveFromNetwork(network: ResourceNetwork<*>) {
        if (!networks.remove(network)) {
            throw Exception("Cannot remove $this from $network because this is not in that network")
        }
    }

    override fun afterAddToLevel(oldLevel: Level) {
        val network = PipeNetwork(level)
        network.add(this)
        super.afterAddToLevel(oldLevel)
    }

    override fun afterRemoveFromLevel(oldLevel: Level) {
        networks.forEach { it.remove(this) }
        super.afterRemoveFromLevel(oldLevel)
    }

    fun copy(xTile: Int, yTile: Int, rotation: Int, attachedContainer: ResourceContainer): ResourceNode {
        return ResourceNode(attachedContainer, xTile, yTile).apply {
            this.rotation = rotation; this.level = this@ResourceNode.level
        }
    }

    override fun toReference(): ResourceNodeReference {
        return ResourceNodeReference(this)
    }
}