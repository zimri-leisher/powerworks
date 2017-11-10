package level.node

import level.resource.ResourceType
import main.Game

class OutputNode<R : ResourceType>(xTile: Int, yTile: Int, dir: Int, storageNode: StorageNode<R>) :
        TransferNode<R>(xTile, yTile, dir, storageNode) {

    var attachedInput: InputNode<R>? = null

    fun canOutput(resource: R, quantity: Int): Boolean {
        return storageNode != null && storageNode!!.contains(resource, quantity)
    }

    fun output(resource: R, quantity: Int): Boolean {
        if(canOutput(resource, quantity)) {
            if(attachedInput != null) {
                val a = attachedInput!!
                return a.input(resource, quantity)
            }
            storageNode!!.remove(resource, quantity)
            return Game.currentLevel.add(xTile shl 4, yTile shl 4, resource, quantity)
        }
        return false
    }
}