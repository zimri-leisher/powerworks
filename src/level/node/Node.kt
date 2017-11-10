package level.node

import level.resource.ResourceType

abstract class TransferNode<R : ResourceType>(var xTile: Int, var yTile: Int, var dir: Int, var storageNode: StorageNode<R>?)

abstract class StorageNode<R : ResourceType> {
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