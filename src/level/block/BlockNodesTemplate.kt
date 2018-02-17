package level.block

import inv.Inventory
import inv.ItemType
import level.node.InputNode
import level.node.OutputNode
import level.node.StorageNode
import level.node.TransferNode
import level.resource.ResourceType
import misc.GeometryHelper

sealed class TransferNodeTemplate<R : ResourceType, T : TransferNode<R>>(val xTileOffset: Int, val yTileOffset: Int, val dir: Int, val storageNodeTemplate: StorageNodeTemplate<R>? = null, val resourceTypeID: Int) {
    abstract fun instantiate(xTile: Int, yTile: Int, dir: Int): T
}

class InputNodeTemplate<R : ResourceType>(xTileOffset: Int, yTileOffset: Int, dir: Int, storageNodeTemplate: StorageNodeTemplate<R>? = null, resourceTypeID: Int) : TransferNodeTemplate<R, InputNode<R>>(xTileOffset, yTileOffset, dir, storageNodeTemplate, resourceTypeID) {
    override fun instantiate(xTile: Int, yTile: Int, dir: Int) = InputNode(xTile + xTileOffset, yTile + yTileOffset, GeometryHelper.addAngles(dir, this.dir), storageNodeTemplate?.getInstance(), resourceTypeID)
}

class OutputNodeTemplate<R : ResourceType>(xTileOffset: Int, yTileOffset: Int, dir: Int, storageNodeTemplate: StorageNodeTemplate<R>? = null, resourceTypeID: Int) : TransferNodeTemplate<R, OutputNode<R>>(xTileOffset, yTileOffset, dir, storageNodeTemplate, resourceTypeID) {
    override fun instantiate(xTile: Int, yTile: Int, dir: Int) = OutputNode(xTile + xTileOffset, yTile + yTileOffset, GeometryHelper.addAngles(dir, this.dir), storageNodeTemplate?.getInstance(), resourceTypeID)
}

sealed class StorageNodeTemplate<R : ResourceType>(val resourceTypeID: Int) {
    var instantiated: StorageNode<R>? = null
    abstract fun getInstance(): StorageNode<R>
}

class InventoryTemplate(val width: Int, val height: Int) : StorageNodeTemplate<ItemType>(ResourceType.ITEM) {
    override fun getInstance() = if(instantiated != null) instantiated!! else Inventory(width, height).apply { instantiated = this }
}

class BlockNodesTemplate(init: BlockNodesTemplate.() -> Unit = {}, val blockTemplate: BlockTemplate) {
    val nodeTemplates = mutableListOf<TransferNodeTemplate<*, *>>()
    val storageNodeTemplates = mutableListOf<StorageNodeTemplate<*>>()

    init {
        init()
    }



    companion object {
        val NONE = BlockNodesTemplate(blockTemplate = BlockTemplate.ERROR)
    }
}