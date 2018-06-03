package routing

import resource.ResourceNode
import resource.ResourceNodeGroup
import resource.ResourceType

class ResourceRoutingNetwork<R : ResourceType> {
    val nodes = ResourceNodeGroup("Node group of resource routing network")

    var behavior = ResourceNetworkRoutingBehavior.DEFAULT

    fun findDestination(resource: R, quantity: Int, xTileFrom: Int, yTileFrom: Int): ResourceNode<R>? {
        val nearest = nodes.getOutputters(resource, quantity).maxBy { Math.abs(it.xTile - xTileFrom) + Math.abs(it.yTile - yTileFrom) }
        return nearest
    }
}