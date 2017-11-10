package level.node

import level.resource.ResourceType

class InputNode<R : ResourceType>(xTile: Int, yTile: Int, dir: Int, storageNode: StorageNode<R>?) :
        TransferNode<R>(xTile, yTile, dir, storageNode) {

    fun canInput(resource: R, quantity: Int): Boolean {
        return storageNode != null && storageNode!!.spaceFor(resource, quantity)
    }

    fun input(resource: R, quantity: Int) : Boolean {
        return storageNode != null && storageNode!!.add(resource, quantity)
    }
}