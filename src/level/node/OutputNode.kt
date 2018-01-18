package level.node

import level.resource.ResourceType
import main.Game
import misc.GeometryHelper

class OutputNode<R : ResourceType>(xTile: Int, yTile: Int, dir: Int, storageNode: StorageNode<R>?, resourceTypeID: Int) :
        TransferNode<R>(xTile, yTile, dir, storageNode, resourceTypeID) {

    var attachedInput: InputNode<R>? = null

    fun canOutput(resource: R, quantity: Int): Boolean {
        return (storageNode != null && storageNode!!.contains(resource, quantity)) || storageNode == null
    }

    fun output(resource: R, quantity: Int, checkIfAble: Boolean = true): Boolean {
        if (checkIfAble)
            if (!canOutput(resource, quantity))
                return false
        if (storageNode != null)
            storageNode!!.remove(resource, quantity)
        if (attachedInput != null) {
            val a = attachedInput!!
            return a.input(resource, quantity)
        }
        // TODO make this output based on the direction
        return Game.currentLevel.add(xTile shl 4, yTile shl 4, resource, quantity) == quantity
    }

    override fun toString(): String {
        return "Output node at $xTile, $yTile, dir: $dir, resource type: $resourceTypeID, attached input node: ${attachedInput?.xTile}, ${attachedInput?.yTile}, attached storage node: $storageNode"
    }

    override fun equals(other: Any?): Boolean {
        return other is OutputNode<*> && other.dir == dir && other.xTile == xTile && other.yTile == yTile && other.resourceTypeID == resourceTypeID
    }

    companion object {
        fun <R : ResourceType> createCorrespondingNode(i: InputNode<R>, storageNode: StorageNode<R>?): OutputNode<R> {
            return OutputNode(i.xTile + GeometryHelper.getXSign(i.dir), i.yTile + GeometryHelper.getYSign(i.dir), GeometryHelper.getOppositeAngle(i.dir), storageNode, i.resourceTypeID)
        }
    }
}