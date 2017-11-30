package level.node

import level.resource.ResourceType
import main.Game

abstract class TransferNode<R : ResourceType>(val xTile: Int, val yTile: Int, dir: Int, var storageNode: StorageNode<R>?, val resourceTypeID: Int) {
    var dir = dir
        set(value) {
            Game.currentLevel
        }

    init {
        Game.currentLevel.addTransferNode(this)
    }
}

abstract class StorageNode<R : ResourceType>(val resourceTypeID: Int) {
    /**
     * @param checkForSpace whether or not to call spaceFor before executing. Set to false if you already know there is space
     * @return true on successful addition
     */
    abstract fun add(resource: R, quantity: Int, checkForSpace: Boolean = true): Boolean

    abstract fun spaceFor(resource: R, quantity: Int): Boolean
    /**
     * @param checkIfContains whether or not to call contains before executing. Set to false if you already know that it contains sufficient amounts
     * @return true on successful removal
     */
    abstract fun remove(resource: R, quantity: Int, checkIfContains: Boolean = true): Boolean

    abstract fun contains(resource: R, quantity: Int): Boolean
}