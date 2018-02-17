package level.node

import level.Level
import level.resource.ResourceType
import misc.GeometryHelper

class OutputNode<R : ResourceType>(xTile: Int, yTile: Int, dir: Int, resourceTypeID: Int) :
        ResourceTransferNode<R>(xTile, yTile, dir, resourceTypeID) {

    var attachedInput: InputNode<R>? = null

    fun canOutput(resource: R, quantity: Int): Boolean {
        return (attachedStorageNode != null && attachedStorageNode!!.contains(resource, quantity)) || attachedStorageNode == null
    }

    fun output(resource: R, quantity: Int, checkIfAble: Boolean = true): Boolean {
        if (checkIfAble)
            if (!canOutput(resource, quantity))
                return false
        if (attachedStorageNode != null)
            attachedStorageNode!!.remove(resource, quantity, this)
        if (attachedInput != null) {
            val a = attachedInput!!
            return a.input(resource, quantity)
        }
        // TODO make this output based on the direction
        return Level.add((xTile shl 4) + 8 * GeometryHelper.getXSign(dir), (yTile shl 4) + 8 * GeometryHelper.getYSign(dir), resource, quantity) == quantity
    }

    override fun toString(): String {
        return "Output node at $xTile, $yTile, dir: $dir, resource type: $resourceTypeID, attached input node: ${attachedInput?.xTile}, ${attachedInput?.yTile}, attached storage node: $attachedStorageNode"
    }

    override fun equals(other: Any?): Boolean {
        return other is OutputNode<*> && other.dir == dir && other.xTile == xTile && other.yTile == yTile && other.resourceTypeID == resourceTypeID
    }

    companion object {
        fun <R : ResourceType> createCorrespondingNode(i: InputNode<R>, storageNode: StorageNode<R>?): OutputNode<R> {
            return OutputNode(i.xTile + GeometryHelper.getXSign(i.dir), i.yTile + GeometryHelper.getYSign(i.dir), GeometryHelper.getOppositeAngle(i.dir), i.resourceTypeID)
        }
    }
}