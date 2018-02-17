package level.node

import level.resource.ResourceType
import misc.GeometryHelper

class InputNode<R : ResourceType>(xTile: Int, yTile: Int,
                                  dir: Int, resourceTypeID: Int) :
        ResourceTransferNode<R>(xTile, yTile, dir, resourceTypeID) {

    fun canInput(resource: R, quantity: Int): Boolean {
        return attachedStorageNode != null && attachedStorageNode!!.spaceFor(resource, quantity)
    }

    fun input(resource: R, quantity: Int, checkIfAble: Boolean = true) : Boolean {
        if(checkIfAble)
            if(!canInput(resource, quantity))
                return false
        return attachedStorageNode!!.add(resource, quantity, this, false)
    }

    override fun toString(): String {
        return "Input node at $xTile, $yTile, dir: $dir, resource type: $resourceTypeID, attached storage node: $attachedStorageNode"
    }

    override fun equals(other: Any?): Boolean {
        return other is InputNode<*> && other.dir == dir && other.xTile == xTile && other.yTile == yTile && other.resourceTypeID == resourceTypeID
    }

    companion object {
        fun <R : ResourceType >createCorrespondingNode(i: OutputNode<R>, storageNode: StorageNode<R>?): InputNode<R> {
            return InputNode(i.xTile + GeometryHelper.getXSign(i.dir), i.yTile + GeometryHelper.getYSign(i.dir), GeometryHelper.getOppositeAngle(i.dir), i.resourceTypeID)
        }
    }
}