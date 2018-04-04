package resource

abstract class ResourceContainer<R : ResourceType>(val resourceTypeID: Int, var rule: (ResourceType) -> Boolean = { true }) {

    /**
     * Mutator methods should send appropriate calls to these
     */
    val listeners = mutableListOf<ResourceContainerChangeListener>()

    /**
     * Adds the specified resource with the specified quantity to this node, and notifies listeners of this event
     * @param checkIfAble whether or not to check with spaceFor and isValid. Set to false if you already know there is space and the resource is valid
     * @param from the node that is adding to this, null if none
     * @return true on successful addition
     */
    abstract fun add(resource: ResourceType, quantity: Int = 1, from: ResourceNode<*>? = null, checkIfAble: Boolean = true): Boolean

    /**
     * If this is able to accept the specified resource in the specified quantity
     *
     * *NOTE - this assumes resource is of the correct type so as to avoid checking the type twice when calling add. Should usually be used in conjuction with isValid
     */
    abstract fun spaceFor(resource: ResourceType, quantity: Int = 1): Boolean

    /**
     * Removes the specified resource with the specified quantity from this node, and notifies listeners of this event
     * @param checkIfAble whether or not to check with contains and isValid. Set to false if you already know there are sufficient amounts and the resource is valid
     * @param to the node that is removing from this, null if none
     * @return true on successful removal
     */
    abstract fun remove(resource: ResourceType, quantity: Int = 1, to: ResourceNode<*>? = null, checkIfAble: Boolean = true): Boolean

    /**
     * If this has the specified resource in the specified quantity
     *
     * *NOTE* - this assumes resource is of the correct type so as to avoid checking the type twice when calling remove. Should usually be used in conjuction with isValid
     */
    abstract fun contains(resource: ResourceType, quantity: Int = 1): Boolean

    /**
     * Removes all resources from this node, and notifies listeners of this event
     */
    abstract fun clear()

    fun isValid(resource: ResourceType) = resource.typeID == resourceTypeID && rule(resource)

    /**
     * Creates an identical copy of this container, including internal stored resources
     */
    abstract fun copy(): ResourceContainer<R>

    abstract fun getQuantity(resource: ResourceType): Int

    abstract fun toList(): ResourceList
}