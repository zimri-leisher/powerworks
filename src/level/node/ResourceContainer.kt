package level.node

import level.resource.ResourceType

abstract class ResourceContainer<R : ResourceType>(val resourceTypeID: Int) {
    /**
     * Adds the specified resource with the specified quantity to this node
     * @param checkForSpace whether or not to call spaceFor before executing. Set to false if you already know there is space
     * @param from the node that is adding to this, null if none
     * @return true on successful addition
     */
    abstract fun add(resource: R, quantity: Int, from: ResourceNode<R>? = null, checkForSpace: Boolean = true): Boolean

    /**
     * If this is able to accept the specified resource in the specified quantity
     */
    abstract fun spaceFor(resource: R, quantity: Int): Boolean

    /**
     * Removes the specified resource with the specified quantity from this node
     * @param checkIfContains whether or not to call contains before executing. Set to false if you already know that it contains sufficient amounts
     * @param to the node that is removing from this, null if none
     * @return true on successful removal
     */
    abstract fun remove(resource: R, quantity: Int, to: ResourceNode<R>? = null, checkIfContains: Boolean = true): Boolean

    /**
     * If this has the specified resource in the specified quantity
     */
    abstract fun contains(resource: R, quantity: Int): Boolean

    /**
     * Removes all reources from this node
     */
    abstract fun clear()

    /**
     * Creates an identical copy of this container, including internal stored resources
     */
    abstract fun copy(): ResourceContainer<R>
}