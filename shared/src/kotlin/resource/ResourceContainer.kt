package resource

import level.LevelObject
import serialization.Id
import java.util.*

abstract class ResourceContainer(
        @Id(1)
        val resourceCategory: ResourceCategory) {

    private constructor() : this(ResourceCategory.ITEM)

    @Id(2)
    var id: UUID = UUID.randomUUID()

    /**
     * Mutator methods should send appropriate calls to these
     */
    @Id(3)
    val listeners = mutableListOf<ResourceContainerChangeListener>()

    /**
     * A [LevelObject] associated with this container.
     */
    @Id(1001)
    var attachedLevelObject: LevelObject? = null

    abstract val expected: ResourceList

    abstract val totalQuantity: Int

    /**
     * Adds the specified resource with the specified quantity to this container, and notifies listeners of this event
     * @param checkIfAble whether or not to check with spaceFor, isRightType and additionRule. Set to false if you already know there is space,
     * the resource is valid and it matches the addition rule. This function is unsafe when this parameter is false, meaning there are no guarantees as to how it will work
     * when you aren't sure of the previous things. Use it only when certain and only for performance
     * @param from the node that is adding to this, null if none
     * @return true if resources were added
     */
    fun add(resource: ResourceType, quantity: Int = 1, from: ResourceNode? = null, checkIfAble: Boolean = true) = add(resourceListOf(resource to quantity), from, checkIfAble)

    /**
     * Adds all the resources in the [resources] list to this container, and notifies listeners of this event
     * @param checkIfAble whether or not to check with spaceFor, isRightType and additionRule. Set to false if you already know there is space,
     * the resources are valid and they match the addition rule. This function is unsafe when this parameter is false, meaning there are no guarantees as to how it will work
     * when you aren't sure of the previous things. Use it only when certain and only for performance
     * @param from the node that is adding to this, null if none
     * @return true if resources were added
     */
    abstract fun add(resources: ResourceList, from: ResourceNode? = null, checkIfAble: Boolean = true): Boolean

    /**
     * If this is able to accept the specified resource in the specified quantity. Doesn't take into account expected resources because
     * they aren't yet present
     */
    fun spaceFor(resource: ResourceType, quantity: Int = 1) = spaceFor(resourceListOf(resource to quantity))

    /**
     * If this is able to accept the specified resources. Doesn't take into account expected resources
     */
    fun spaceFor(list: ResourceList) = mostPossibleToAdd(list) == list

    abstract fun mostPossibleToAdd(list: ResourceList): ResourceList

    /**
     * If this has the specified resource in the specified quantity. Doesn't take into account expected resources because
     * they aren't yet present
     */
    fun contains(resource: ResourceType, quantity: Int = 1) = contains(resourceListOf(resource to quantity))

    /**
     * If this has the specified resource in the specified quantity. Doesn't take into account expected resources because
     * they aren't yet present
     */
    fun contains(list: ResourceList) = mostPossibleToRemove(list) == list

    abstract fun mostPossibleToRemove(list: ResourceList): ResourceList

    /**
     * Removes the specified resource with the specified quantity from this container, and notifies listeners of this event
     * @param checkIfAble whether or not to check with contains, isRightType and removalRule. Set to false if you already know there are sufficient amounts,
     * the resource is valid and it matches the removal rule. This function is unsafe when this parameter is false, meaning there are no guarantees as to how it will work
     * given unexpected parameters
     * @param to the node that is removing from this, null if none
     * @return true if resources were removed
     */
    fun remove(resource: ResourceType, quantity: Int = 1, to: ResourceNode? = null, checkIfAble: Boolean = true) = remove(resourceListOf(resource to quantity), to, checkIfAble)

    /**
     * Removes the [resources] from this container, and notifies listeners of this event
     * @param checkIfAble whether or not to check with contains, isRightType and removalRule. Set to false if you already know there are sufficient amounts,
     * the resource is valid and it matches the removal rule. This function is unsafe when this parameter is false, meaning there are no guarantees as to how it will work
     * given unexpected parameters
     * @param to the node that is removing from this, null if none
     * @return true if resources were removed
     */
    abstract fun remove(resources: ResourceList, to: ResourceNode? = null, checkIfAble: Boolean = true): Boolean

    /**
     * Tells the container to expect some resources to be added later. These are not able to be used by the player.
     * When resources that are also in the expectations are [add]ed, a corresponding quantity will be removed
     * from expectations.
     *
     * Expected resources are used when resources are routed for a container but have not arrived yet. Because they are included
     * in space checks, they will prevent other resources from being sent to the same container.
     *
     * @return true if the container will be able to fit these resources (in addition to other expected resources)
     */
    fun expect(resource: ResourceType, quantity: Int = 1) = expect(resourceListOf(resource to quantity))

    /**
     * Tells the container to expect some resources to be added later. These are not able to be used by the player.
     * When resources that are also in the expectations are [add]ed, a corresponding quantity will be removed
     * from expectations.
     *
     * Expected resources are used when resources are routed for a container but have not arrived yet. Because they are included
     * in space checks when routing resources around resource networks, they will prevent other resources from being sent to the same container.
     *
     * @return true if the container will be able to fit these resources (in addition to other expected resources)
     */
    abstract fun expect(resources: ResourceList): Boolean

    /**
     * Removes the specified [quantity] of [type] expected resources (which are added with [expect]). Expected resources are not able
     * to be used by the player. When the next resource with the same type is [add]ed, a corresponding quantity will
     * be removed from expectations.
     * @return true if the resources were expected (and are no longer)
     */
    fun cancelExpectation(resource: ResourceType, quantity: Int = 1) = cancelExpectation(resourceListOf(resource to quantity))

    /**
     * Removes the [resources] from the expected resources (which are added with [expect]). Expected resources are not able
     * to be used by the player. When the next resource with the same type is [add]ed, a corresponding quantity will
     * be removed from expectations.
     * @return true if the resources were expected (and are no longer)
     */
    abstract fun cancelExpectation(resources: ResourceList): Boolean

    /**
     * Removes all resources from this container
     */
    abstract fun clear()

    fun canAddAll(resources: ResourceList) = resources.keys.all { isRightType(it) } && spaceFor(resources)

    fun canAddSome(resources: ResourceList) = mostPossibleToAdd(resources.filterKeys { isRightType(it) }.toResourceList())

    fun canRemoveAll(resources: ResourceList) = resources.keys.all { isRightType(it) } && contains(resources)

    fun canRemoveSome(resources: ResourceList) = mostPossibleToRemove(resources.filterKeys { isRightType(it) }.toResourceList())

    fun isRightType(resource: ResourceType) = resource.category == resourceCategory

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