package level.node

import level.resource.ResourceType
import main.Game

abstract class TransferNode<R : ResourceType>(val xTile: Int, val yTile: Int, dir: Int, var storageNode: StorageNode<R>?, val resourceTypeID: Int) {
    var dir = dir
        set(value) {
            Game.currentLevel
        }
}

abstract class StorageNode<R : ResourceType>(val resourceTypeID: Int) {
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