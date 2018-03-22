package resource

class ResourceNodeGroup(val name: String, nodes: List<ResourceNode<*>> = listOf()) {

    private val nodes = nodes.toMutableList()

    constructor(name: String) : this(name, listOf())

    constructor(name: String, vararg nodes: ResourceNode<*>) : this(name, mutableListOf(*nodes))

    fun <R : ResourceType> getAttachedContainers(resourceTypeID: Int) = nodes.filter { it.resourceTypeID == resourceTypeID }.mapNotNull { it.attachedContainer } as List<ResourceContainer<R>>

    fun getAttachedContainers() = nodes.mapNotNull { it.attachedContainer }

    fun <R : ResourceType> getPossibleOutputters(resourceType: R, quantity: Int = 1): List<ResourceNode<R>> {
        val ret = mutableListOf<ResourceNode<R>>()
        nodes.filter { it.canOutputFromContainer(resourceType, quantity) }.forEach { ret.add(it as ResourceNode<R>) }
        return ret
    }

    fun add(n: ResourceNode<*>) {
        nodes.add(n)
    }

    fun remove(n: ResourceNode<*>) {
        nodes.remove(n)
    }

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

    fun <R : ResourceType> getPossibleOutputter(resourceType: R, quantity: Int = 1, checkIfContains: Boolean = true): ResourceNode<R>? {
        val r = nodes.firstOrNull {
            if (checkIfContains) {
                it.canOutputFromContainer(resourceType, quantity) && it.isAcceptableResource(resourceType)
            } else
                it.isAcceptableResource(resourceType) && it.allowOut
        } as ResourceNode<R>?
        return r
    }

    fun <R : ResourceType> getPossibleInputter(resourceType: R, quantity: Int = 1, checkIfSpaceFor: Boolean = true) =
            nodes.firstOrNull {
                if (checkIfSpaceFor)
                    it.canInputToContainer(resourceType, quantity) && it.isAcceptableResource(resourceType)
                else
                    it.isAcceptableResource(resourceType) && it.allowIn
            } as ResourceNode<R>?

    fun output(resourceType: ResourceType, quantity: Int, checkIfContains: Boolean = true): Boolean {
        return getPossibleOutputter(resourceType, quantity, checkIfContains)?.output(resourceType, quantity, false) == true
    }

    /**
     * @return true if all were successfully outputted
     */
    fun output(list: ResourceList, checkIfContains: Boolean = true): Boolean {
        return list.any { !output(it.key, it.value, checkIfContains) }
    }

    fun input(resourceType: ResourceType, quantity: Int, checkIfSpaceFor: Boolean = true): Boolean {
        return getPossibleInputter(resourceType, quantity, checkIfSpaceFor)?.input(resourceType, quantity, false) == true
    }

    fun addAll(other: ResourceNodeGroup) {
        other.nodes.filter { it !in nodes }.forEach { add(it) }
    }

    fun addAll(other: List<ResourceNode<*>>) {
        other.filter { it !in nodes }.forEach { add(it) }
    }

    fun addAll(vararg other: ResourceNode<*>) {
        other.filter { it !in nodes }.forEach { add(it) }
    }

    fun forEach(f: (ResourceNode<*>) -> Unit) = nodes.forEach(f)

    fun firstOrNull(f: (ResourceNode<*>) -> Boolean) = nodes.firstOrNull(f)

    override fun toString() = nodes.joinToString()
    operator fun contains(it: ResourceNode<*>) = nodes.contains(it)
}