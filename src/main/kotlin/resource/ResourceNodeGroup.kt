package resource

/**
 * A group of ResourceNodes of any type
 *
 * @param name the name of the group, useful for debugging purposes
 */
class ResourceNodeGroup(val name: String, nodes: List<ResourceNode<*>> = listOf()) {

    private val nodes = nodes.toMutableList()

    constructor(name: String) : this(name, listOf())

    constructor(name: String, vararg nodes: ResourceNode<*>) : this(name, mutableListOf(*nodes))

    /**
     * @return a list of all unique attached containers in this node group with the given resourceCategory
     */
    fun <R : ResourceType> getAttachedContainers(resourceCategory: ResourceCategory) = nodes.filter { it.resourceCategory == resourceCategory }.mapNotNull { it.attachedContainer }.distinct() as List<ResourceContainer<R>>

    /**
     * @return a list of all unique attached containers in this node group
     */
    fun getAttachedContainers() = nodes.mapNotNull { it.attachedContainer }.distinct()

    /**
     * @return a list of nodes that are able to output the given resource type with the given quantity
     */
    fun <R : ResourceType> getPossibleOutputters(resourceType: R, quantity: Int = 1): List<ResourceNode<R>> {
        val ret = mutableListOf<ResourceNode<R>>()
        nodes.filter { it.canOutputFromContainer(resourceType, quantity) }.forEach { ret.add(it as ResourceNode<R>) }
        return ret
    }

    /**
     * @return a list of nodes that are able to input the given resource type with the given quantity
     */
    fun <R : ResourceType> getPossibleInputters(resourceType: R, quantity: Int = 1): List<ResourceNode<R>> {
        val ret = mutableListOf<ResourceNode<R>>()
        nodes.filter { it.canInputToContainer(resourceType, quantity) }.forEach { ret.add(it as ResourceNode<R>) }
        return ret
    }

    /**
     * @param onlyTo a predicate for which nodes are considered as options
     * @param checkIfAble whether or not to check if a possible node is able to output the amount and type. Set to false if you know that the first node to be checked will be able to output successfully
     * @return the first node that can output the given resource type with the given quantity while matching the onlyTo predicate
     */
    fun <R : ResourceType> getPossibleOutputter(resourceType: R, quantity: Int = 1, checkIfAble: Boolean = true, onlyTo: (ResourceNode<*>) -> Boolean = { true }): ResourceNode<R>? {
        val r = nodes.firstOrNull {
            onlyTo(it) &&
            if (checkIfAble) {
                it.isRightType(resourceType) && it.canOutputFromContainer(resourceType, quantity)
            } else
                it.isRightType(resourceType) && it.allowOut
        } as ResourceNode<R>?
        return r
    }

    /**
     * @param onlyTo a predicate for which nodes are considered as options
     * @param checkIfAble whether or not to check if a possible node is able to input the amount and type. Set to false if you know that the first node to be checked will be able to input successfully
     * @return the first node that can input the given resource type with the given quantity while matching the onlyTo predicate
     */
    fun <R : ResourceType> getPossibleInputter(resourceType: R, quantity: Int = 1, checkIfAble: Boolean = true, onlyTo: (ResourceNode<*>) -> Boolean = { true }) =
            nodes.firstOrNull {
                onlyTo(it) &&
                if (checkIfAble)
                    it.isRightType(resourceType) && it.canInputToContainer(resourceType, quantity)
                else
                    it.isRightType(resourceType) && it.allowIn
            } as ResourceNode<R>?

    /**
     * Tries to output the resource with the specified quantity
     * @param checkIfContains whether or not to check if there is a container attached to a node in the group that contains enough resources
     * @param onlyTo a predicate determining which nodes it will output through
     */
    fun output(resourceType: ResourceType, quantity: Int, checkIfContains: Boolean = true, onlyTo: (ResourceNode<*>) -> Boolean = { true }): Boolean {
        return getPossibleOutputter(resourceType, quantity, checkIfContains, onlyTo)?.output(resourceType, quantity, false) == true
    }

    /**
     * Tries outputting every resource in this list from any available node in this group
     * @return true if all were successfully outputted
     */
    fun output(list: ResourceList, checkIfContains: Boolean = true): Boolean {
        return !list.any { !output(it.key, it.value, checkIfContains) }
    }

    /**
     * Tries to input resources into any available node in this group
     */
    fun input(resourceType: ResourceType, quantity: Int, checkIfSpaceFor: Boolean = true): Boolean {
        return getPossibleInputter(resourceType, quantity, checkIfSpaceFor)?.input(resourceType, quantity, false) == true
    }

    fun canOutput(resourceType: ResourceType, quantity: Int) = getPossibleOutputter(resourceType, quantity) != null

    fun canInput(resourceType: ResourceType, quantity: Int) = getPossibleInputter(resourceType, quantity) != null

    fun add(n: ResourceNode<*>) {
        nodes.add(n)
    }

    fun remove(n: ResourceNode<*>) {
        nodes.remove(n)
    }

    /**
     * Removes all nodes that match this predicate
     */
    fun removeAll(predicate: (ResourceNode<*>) -> Boolean) {
        val i = nodes.iterator()
        for (n in i) {
            if (predicate(n)) i.remove()
        }
    }

    fun addAll(other: List<ResourceNode<*>>) {
        other.filter { it !in nodes }.forEach { add(it) }
    }

    fun removeAll(l: List<ResourceNode<*>>) {
        nodes.removeAll(l)
    }

    fun filter(predicate: (ResourceNode<*>) -> Boolean) = nodes.filter(predicate)

    fun forEach(f: (ResourceNode<*>) -> Unit) = nodes.forEach(f)

    operator fun contains(it: ResourceNode<*>) = nodes.contains(it)

    override fun toString() = nodes.joinToString()
}