package level.block

import misc.GeometryHelper
import misc.TileCoord
import resource.ResourceContainer
import resource.ResourceNode

class BlockNodesTemplate(val widthTiles: Int, val heightTiles: Int,
                         // lambda so calculations can happen here
                         nodesInitializer: BlockNodesTemplate.() -> List<ResourceNode<*>> = { listOf() }) {
    val nodes = mutableListOf<ResourceNode<*>>()
    val containers = mutableListOf<ResourceContainer<*>>()

    init {
        nodes.addAll(nodesInitializer())
        // get all the containers of the nodes, only one of each. Later, we will use memory equivalence to check what new instantiated node will have which new instantiated container
        containers.addAll(nodes.mapNotNull { if (it.attachedContainer !in containers) it.attachedContainer else null })
    }

    private fun instantiateContainers(): Map<ResourceContainer<*>, ResourceContainer<*>> {
        val map = mutableMapOf<ResourceContainer<*>, ResourceContainer<*>>()
        val newContainers = containers.map { it.copy() }
        for (i in containers.indices) {
            map.put(containers[i], newContainers[i])
        }
        return map
    }

    fun instantiate(xTile: Int, yTile: Int, dir: Int): List<ResourceNode<*>> {
        val ret = mutableListOf<ResourceNode<*>>()
        val containers = instantiateContainers()
        for (node in nodes) {
            val coord = rotate(node.xTile, node.yTile, widthTiles, heightTiles, dir)
            val newContainer = containers.filter { it.key === node.attachedContainer }.entries.first().value
            // we know that the container is the same type as the node because they were originally together
            ret.add(node.copy(coord.xTile + xTile, coord.yTile + yTile, GeometryHelper.addAngles(node.dir, dir), attachedContainer = newContainer))
        }
        return ret
    }

    companion object {
        private fun rotate(xTile: Int, yTile: Int, widthTiles: Int, heightTiles: Int, dir: Int): TileCoord {
            return when (dir % 4) {
                1 -> TileCoord(widthTiles - yTile - 1, xTile)
                2 -> TileCoord(widthTiles - xTile - 1, heightTiles - yTile - 1)
                3 -> TileCoord(yTile, heightTiles - xTile - 1)
                else -> TileCoord(xTile, yTile)
            }
        }

        val NONE = BlockNodesTemplate(1, 1)
    }
}