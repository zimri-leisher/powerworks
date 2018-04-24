package resource

class ResourceNodeGroup(val name: String, nodes: List<ResourceNode<*>> = listOf()) {

    private val nodes = nodes.toMutableList()

    constructor(name: String) : this(name, listOf())

    constructor(name: String, vararg nodes: ResourceNode<*>) : this(name, mutableListOf(*nodes))

    /**
     * @return a list of all unique attached containers in this node group with the given resourceTypeID
     */
    fun <R : ResourceType> getAttachedContainers(resourceTypeID: Int) = nodes.filter { it.resourceTypeID == resourceTypeID }.mapNotNull { it.attachedContainer } as List<ResourceContainer<R>>

    /**
     * @return a list of all unique attached containers in this node group
     */
    fun getAttachedContainers() = nodes.mapNotNull { it.attachedContainer }.distinct()

    fun <R : ResourceType> getPossibleOutputters(resourceType: R, quantity: Int = 1): List<ResourceNode<R>> {
        val ret = mutableListOf<ResourceNode<R>>()
        nodes.filter { it.canOutputFromContainer(resourceType, quantity) }.forEach { ret.add(it as ResourceNode<R>) }
        return ret
    }

    fun <R : ResourceType> getPossibleInputters(resourceType: R, quantity: Int = 1): List<ResourceNode<R>> {
        val ret = mutableListOf<ResourceNode<R>>()
        nodes.filter { it.canInputToContainer(resourceType, quantity) }.forEach { ret.add(it as ResourceNode<R>) }
        return ret
    }

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

    fun removeAll(l: List<ResourceNode<*>>) {
        nodes.removeAll(l)
    }

    fun filter(predicate: (ResourceNode<*>) -> Boolean) = nodes.filter(predicate)

    fun <R : ResourceType> getPossibleOutputter(resourceType: R, quantity: Int = 1, checkIfContains: Boolean = true, onlyTo: (ResourceNode<*>) -> Boolean = { true }): ResourceNode<R>? {
        val r = nodes.firstOrNull {
            onlyTo(it) &&
            if (checkIfContains) {
                it.isValid(resourceType) && it.canOutputFromContainer(resourceType, quantity)
            } else
                it.isValid(resourceType) && it.allowOut
        } as ResourceNode<R>?
        return r
    }

    fun <R : ResourceType> getPossibleInputter(resourceType: R, quantity: Int = 1, checkIfSpaceFor: Boolean = true, onlyTo: (ResourceNode<*>) -> Boolean = { true }) =
            nodes.firstOrNull {
                onlyTo(it) &&
                if (checkIfSpaceFor)
                    it.isValid(resourceType) && it.canInputToContainer(resourceType, quantity)
                else
                    it.isValid(resourceType) && it.allowIn
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

    fun addAll(other: List<ResourceNode<*>>) {
        other.filter { it !in nodes }.forEach { add(it) }
    }

    fun canOutput(resourceType: ResourceType, quantity: Int) = getPossibleOutputter(resourceType, quantity) != null

    fun canInput(resourceType: ResourceType, quantity: Int) = getPossibleInputter(resourceType, quantity) != null

    fun forEach(f: (ResourceNode<*>) -> Unit) = nodes.forEach(f)

    override fun toString() = nodes.joinToString()
    operator fun contains(it: ResourceNode<*>) = nodes.contains(it)
}