package resource

import level.LevelObject
import level.LevelObjectType
import network.ResourceContainerReference
import serialization.Id
import serialization.Referencable
import java.util.*
import kotlin.math.absoluteValue

abstract class ResourceContainer : LevelObject(LevelObjectType.RESOURCE_CONTAINER),
    ResourceConduit {

    // todo we want a "constrain" function
    // which takes a resource order and constrains it to what this container can perform
    // todo interface that has a constrain function? ResourceHandler??

    /**
     * Mutator methods should send appropriate calls to these
     */
    @Id(3)
    val listeners = mutableListOf<ResourceContainerChangeListener>()

    @Id(-4)
    val nodes = mutableListOf<ResourceNode>()

    @Id(-56)
    val orders = mutableListOf<ResourceOrder>()

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

    fun add(stack: ResourceStack) = add(resourceListOf(stack))

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

    fun canAdd(stack: ResourceStack) = canAdd(resourceListOf(stack))

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

    fun canRemove(stack: ResourceStack) = canRemove(resourceListOf(stack))

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

    fun remove(stack: ResourceStack) = remove(resourceListOf(stack))

    /**
     * Removes the [resources] from this container, and notifies listeners of this event
     * @param checkIfAble whether or not to check with contains, isRightType and removalRule. Set to false if you already know there are sufficient amounts,
     * the resource is valid and it matches the removal rule. This function is unsafe when this parameter is false, meaning there are no guarantees as to how it will work
     * given unexpected parameters
     * @param to the node that is removing from this, null if none
     * @return true if resources were removed
     */
    abstract fun remove(resources: ResourceList): Boolean

    override fun maxFlow(flow: ResourceFlow): ResourceFlow {
        if (flow.direction == ResourceFlowDirection.IN) {
            val addable = mostPossibleToAdd(resourceListOf(flow.stack))
            return ResourceFlow(addable[0], ResourceFlowDirection.IN)
        } else {
            val removable = mostPossibleToRemove(resourceListOf(flow.stack))
            return ResourceFlow(removable[0], ResourceFlowDirection.OUT)
        }
    }

    fun getFlowInProgress(): List<ResourceFlow> {
        return nodes.flatMap { node ->
            node.networks.flatMap { network ->
                network.getFlowInProgress(this)
            }
        }
    }

    fun getFlowInProgressForType(type: ResourceType): Int {
        return getFlowInProgress().sumOf { flow ->
            if (flow.stack.type == type) {
                if (flow.direction == ResourceFlowDirection.IN)
                    flow.stack.quantity
                else
                // out
                    -flow.stack.quantity
            } else
            // it is not of the right type
                0
        }
    }

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

    abstract fun getQuantity(type: ResourceType): Int

    abstract fun toResourceList(): ResourceList

    fun toMutableResourceList() = MutableResourceList(toResourceList())

    /**
     * @return a set of [ResourceType]s present with quantity greater than 0
     */
    fun toTypeList(): Set<ResourceType> = toResourceList().keys

    override fun toReference() = ResourceContainerReference(this)

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