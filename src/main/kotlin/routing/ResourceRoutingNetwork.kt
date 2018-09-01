package routing

import resource.ResourceNode
import resource.ResourceNodeGroup
import resource.ResourceType

class ResourceRoutingNetwork<R : ResourceType> {
    val nodes = ResourceNodeGroup("Resource routing network node group")

    var behavior = ResourceNetworkRoutingBehavior.DEFAULT

    fun findDestination(resource: R, quantity: Int, xTileFrom: Int, yTileFrom: Int): ResourceNode<R>? {
        val nearest = nodes.getOutputters(resource, quantity).maxBy { Math.abs(it.xTile - xTileFrom) + Math.abs(it.yTile - yTileFrom) }
        return nearest
    }

    fun contains(resource: ResourceType, quantity: Int) = nodes.getAttachedContainers().sumBy { it.getQuantity(resource) } >= quantity

    fun getQuantity(resource: ResourceType) = nodes.getAttachedContainers().sumBy { it.getQuantity(resource) }
}