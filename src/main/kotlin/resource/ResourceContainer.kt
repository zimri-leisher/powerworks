package resource

abstract class ResourceContainer<R : ResourceType>(val resourceCategory: ResourceCategory, var typeRule: (ResourceType) -> Boolean = { true }) {

    // TODO worried about forgetting checkIfAble and thus skipping add/remove rule checks, how2fix??

    /**
     * Should be checked in the addition method of all resource containers. If false, and the checkIfAble arg of the add method is true, no addition operation will be done
     * This is not checked in the spaceFor method
     */
    var additionRule: (ResourceType, Int) -> Boolean = { _, _ -> true }
    /**
     * Should be checked in the removal method of all resource containers. If false, and the checkIfAble arg of the remove method is true, no removal operation will be done
     * This is not checked in the contains method
     */
    var removalRule: (ResourceType, Int) -> Boolean = { _, _ -> true }

    /**
     * Mutator methods should send appropriate calls to these
     */
    val listeners = mutableListOf<ResourceContainerChangeListener>()

    abstract val totalQuantity: Int

    /**
     * Adds the specified resource with the specified quantity to this node, and notifies listeners of this event
     * @param checkIfAble whether or not to check with spaceFor, isRightType and additionRule. Set to false if you already know there is space,
     * the resource is valid and it matches the addition rule. This function is unsafe when this parameter is false, meaning there are no guarantees as to how it will work
     * given unexpected parameters. Use it only when certain and only for performance
     * @param from the node that is adding to this, null if none
     * @return true on successful addition
     */
    abstract fun add(resource: ResourceType, quantity: Int = 1, from: ResourceNode<*>? = null, checkIfAble: Boolean = true): Boolean

    /**
     * If this is able to accept the specified resource in the specified quantity
     */
    abstract fun spaceFor(resource: R, quantity: Int = 1): Boolean

    /**
     * Removes the specified resource with the specified quantity from this node, and notifies listeners of this event
     * @param checkIfAble whether or not to check with contains, isRightType and removalRule. Set to false if you already know there are sufficient amounts,
     * the resource is valid and it matches the removal rule. This function is unsafe when this parameter is false, meaning there are no guarantees as to how it will work
     * given unexpected parameters
     * @param to the node that is removing from this, null if none
     * @return true on successful removal
     */
    abstract fun remove(resource: ResourceType, quantity: Int = 1, to: ResourceNode<*>? = null, checkIfAble: Boolean = true): Boolean

    /**
     * If this has the specified resource in the specified quantity
     */
    abstract fun contains(resource: R, quantity: Int = 1): Boolean

    /**
     * Removes all resources from this node, and notifies listeners of this event
     */
    abstract fun clear()

    fun canAdd(resource: ResourceType, quantity: Int = 1) = isRightType(resource) && additionRule(resource, quantity) && spaceFor(resource as R, quantity)

    fun canRemove(resource: ResourceType, quantity: Int = 1) = isRightType(resource) && removalRule(resource, quantity) && contains(resource as R, quantity)

    fun isRightType(resource: ResourceType) = resource.category == resourceCategory && typeRule(resource)

    /**
     * Creates an identical copy of this container, including internal stored resources
     *
     * *NOTE* - does not copy listeners for changes
     */
    abstract fun copy(): ResourceContainer<R>

    abstract fun getQuantity(resource: ResourceType): Int

    abstract fun toList(): ResourceList
}