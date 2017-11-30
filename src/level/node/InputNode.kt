package level.node

import level.resource.ResourceType
import main.Game
import misc.GeometryHelper

class InputNode<R : ResourceType>(xTile: Int, yTile: Int,
                                  dir: Int, storageNode: StorageNode<R>?, resourceTypeID: Int) :
        TransferNode<R>(xTile, yTile, dir, storageNode, resourceTypeID) {

    init {
        Game.currentLevel.addTransferNode(this)
    }

    fun canInput(resource: R, quantity: Int): Boolean {
        return storageNode != null && storageNode!!.spaceFor(resource, quantity)
    }

    fun input(resource: R, quantity: Int, checkIfAble: Boolean = true) : Boolean {
        if(checkIfAble)
            if(!canInput(resource, quantity))
                return false
        return storageNode!!.add(resource, quantity, false)
    }

    override fun toString(): String {
        return "Input node at $xTile, $yTile, dir: $dir, resource type: $resourceTypeID, attached storage node: $storageNode"
    }

    companion object {
        fun <R : ResourceType >createCorrespondingNode(i: OutputNode<R>, storageNode: StorageNode<R>?): InputNode<R> {
            return InputNode(i.xTile + GeometryHelper.getXSign(i.dir), i.yTile + GeometryHelper.getYSign(i.dir), GeometryHelper.getOppositeAngle(i.dir), storageNode, i.resourceTypeID)
        }
    }
}