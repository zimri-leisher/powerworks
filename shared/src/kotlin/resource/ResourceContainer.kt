package resource

import serialization.Id
import java.util.*

abstract class ResourceContainer {

    // TODO make resource containers just resource lists with a capacity total max

    @Id(2)
    var id: UUID = UUID.randomUUID()

    /**
     * Mutator methods should send appropriate calls to these
     */
    @Id(3)
    val listeners = mutableListOf<ResourceContainerChangeListener>()

    @Id(-4)
    val nodes = mutableListOf<ResourceNode2>()

    abstract val totalQuantity: Int

    /**
     * Adds the specified resource with the specified quantity to this container, and notifies listeners of this event
     * @param checkIfAble whether or not to check with spaceFor, isRightType and additionRule. Set to false if you already know there is space,
     * the resource is valid and it matches the addition rule. This function is unsafe when this parameter is false, meaning there are no guarantees as to how it will work
     * when you aren't sure of the previous things. Use it only when certain and only for performance
     * @param from the node that is adding to this, null if none
     * @return true if resources were added
     */
    fun add(resource: ResourceType, quantity: Int = 1) = add(resourceListOf(resource to quantity))

    /**
     * Adds all the resources in the [resources] list to this container, and notifies listeners of this event
     * @param checkIfAble whether or not to check with spaceFor, isRightType and additionRule. Set to false if you already know there is space,
     * the resources are valid and they match the addition rule. This function is unsafe when this parameter is false, meaning there are no guarantees as to how it will work
     * when you aren't sure of the previous things. Use it only when certain and only for performance
     * @param from the node that is adding to this, null if none
     * @return true if resources were added
     */
    abstract fun add(resources: ResourceList): Boolean

    /**
     * If this is able to accept the specified resource in the specified quantity. Doesn't take into account expected resources because
     * they aren't yet present
     */
    fun canAdd(resource: ResourceType, quantity: Int = 1) = canAdd(resourceListOf(resource to quantity))

    /**
     * If this is able to accept the specified resources. Doesn't take into account expected resources
     */
    fun canAdd(list: ResourceList) = mostPossibleToAdd(list) == list

    abstract fun mostPossibleToAdd(list: ResourceList): ResourceList

    /**
     * If this has the specified resource in the specified quantity. Doesn't take into account expected resources because
     * they aren't yet present
     */
    fun canRemove(resource: ResourceType, quantity: Int = 1) = canRemove(resourceListOf(resource to quantity))

    /**
     * If this has the specified resource in the specified quantity. Doesn't take into account expected resources because
     * they aren't yet present
     */
    fun canRemove(list: ResourceList) = mostPossibleToRemove(list) == list

    abstract fun mostPossibleToRemove(list: ResourceList): ResourceList

    /**
     * Removes the specified resource with the specified quantity from this container, and notifies listeners of this event
     * @param checkIfAble whether or not to check with contains, isRightType and removalRule. Set to false if you already know there are sufficient amounts,
     * the resource is valid and it matches the removal rule. This function is unsafe when this parameter is false, meaning there are no guarantees as to how it will work
     * given unexpected parameters
     * @param to the node that is removing from this, null if none
     * @return true if resources were removed
     */
    fun remove(resource: ResourceType, quantity: Int = 1) = remove(resourceListOf(resource to quantity))

    /**
     * Removes the [list] from this container, and notifies listeners of this event
     * @param checkIfAble whether or not to check with contains, isRightType and removalRule. Set to false if you already know there are sufficient amounts,
     * the resource is valid and it matches the removal rule. This function is unsafe when this parameter is false, meaning there are no guarantees as to how it will work
     * given unexpected parameters
     * @param to the node that is removing from this, null if none
     * @return true if resources were removed
     */
    abstract fun remove(list: ResourceList): Boolean

    /**
     * Removes all resources from this container
     */
    abstract fun clear()

    /**
     * Creates an identical copy of this container, including internal stored resources
     *
     * *NOTE* - does not copy listeners for changes
     */
    abstract fun copy(): ResourceContainer

    abstract fun getQuantity(resource: ResourceType): Int

    abstract fun toResourceList(): ResourceList

    fun toMutableResourceList() = toResourceList().toMutableResourceList()

    /**
     * @return a set of [ResourceType]s present with quantity greater than 0
     */
    fun toTypeList(): Set<ResourceType> = toResourceList().keys

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResourceContainer

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}