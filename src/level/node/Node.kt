package level.node

import level.resource.ResourceType

abstract class Node<R : ResourceType>(var xTile: Int, var yTile: Int)

abstract class TransferNode<R : ResourceType>(xTile: Int, yTile: Int, var dir: Int, val storageNode: StorageNode<R>?) :
        Node<R>(xTile, yTile) {
}

abstract class StorageNode<R : ResourceType>(xTile: Int, yTile: Int) :
        Node<R>(xTile, yTile) {
    /**
     * @return true on successful addition
     */
    abstract fun add(resource: R, quantity: Int): Boolean

    abstract fun spaceFor(resource: R, quantity: Int): Boolean
    /**
     * @return true on successful removal
     */
    abstract fun remove(resource: R, quantity: Int): Boolean

    abstract fun contains(resource: R, quantity: Int): Boolean
}