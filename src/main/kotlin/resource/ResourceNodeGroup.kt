package resource

/**
 * A group of resource nodes of any type
 *
 * Instead of keeping track of individual nodes, you can just add them to this and depend on this to send resources to
 * and from the correct places
 *
 * For example, every block instance has an instance of this which is where the node templates are put into. In the case of
 * the MinerBlock, instead of the it having an outputNode attribute which is manually set to the correct one, it simply
 * calls nodes.output(resource, quantity) which finds the available node and outputs the resources
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
    fun <R : ResourceType> getAttachedContainers(resourceCategory: ResourceCategory) = nodes.filter { it.resourceCategory == resourceCategory }.map { it.attachedContainer }.distinct() as List<ResourceContainer<R>>

    /**
     * @return a list of all unique attached containers in this node group
     */
    fun getAttachedContainers() = nodes.map { it.attachedContainer }.distinct()

    /**
     * @param mustContainEnough whether or not to check if the containers the nodes are attached to have enough resources.
     * Set to false if you know they do or don't care if they don't
     * @return a list of nodes that are able to output the given resource type with the given quantity
     */
    fun <R : ResourceType> getOutputters(resourceType: R, quantity: Int = 1, mustContainEnough: Boolean = true): List<ResourceNode<R>> {
        val ret = mutableListOf<ResourceNode<R>>()
        nodes.filter { it.canOutput(resourceType, quantity, mustContainEnough) }.forEach { ret.add(it as ResourceNode<R>) }
        return ret
    }

    /**
     * @param mustHaveSpace whether or not to check if the containers the nodes are attached to have enough space.
     * Set to false if you know they do or don't care if they don't
     * @return a list of nodes that are able to input the given resource type with the given quantity
     */
    fun <R : ResourceType> getInputters(resourceType: R, quantity: Int = 1, mustHaveSpace: Boolean = true): List<ResourceNode<R>> {
        val ret = mutableListOf<ResourceNode<R>>()
        nodes.filter { it.canInput(resourceType, quantity, mustHaveSpace) }.forEach { ret.add(it as ResourceNode<R>) }
        return ret
    }

    /**
     * @param onlyTo a predicate for which nodes are considered as options
     * @param mustContainEnough whether or not to check if the container the node is attached to has enough resources.
     * Set to false if you know it does or don't care if it doesn't
     * @return the first node that can output the given resource type with the given quantity while matching the onlyTo predicate
     */
    fun <R : ResourceType> getOutputter(resourceType: R, quantity: Int = 1, onlyTo: (ResourceNode<*>) -> Boolean = { true }, mustContainEnough: Boolean = true) =
            nodes.firstOrNull {
                onlyTo(it) && it.canOutput(resourceType, quantity, mustContainEnough)
            } as ResourceNode<R>?

    /**
     * @param onlyTo a predicate for which nodes are considered as options
     * @param mustHaveSpace whether or not to check if the container the node is attached to has enough space.
     * Set to false if you know it does or don't care if it doesn't
     * @return the first node that can input the given resource type with the given quantity while matching the onlyTo predicate
     */
    fun <R : ResourceType> getInputter(resourceType: R, quantity: Int = 1, onlyTo: (ResourceNode<*>) -> Boolean = { true }, mustHaveSpace: Boolean = true) =
            nodes.firstOrNull {
                onlyTo(it) && it.canInput(resourceType, quantity, mustHaveSpace)
            } as ResourceNode<R>?

    /**
     * Tries to output the resource with the specified quantity
     * @param mustContainEnough whether or not to check if the node this is outputting through has a container which has
     * enough resources. Set to false if you know it does or don't care if it doesn't
     * @param onlyTo a predicate determining which nodes it will output through
     */
    fun output(resourceType: ResourceType, quantity: Int, onlyTo: (ResourceNode<*>) -> Boolean = { true }, mustContainEnough: Boolean = true): Boolean {
        return getOutputter(resourceType, quantity, onlyTo, mustContainEnough)?.output(resourceType, quantity, false) == true
    }

    /**
     * Tries outputting every resource in this list from any available node in this group
     * @param mustContainEnough whether or not to check if the nodes this is outputting through have containers which have
     * enough resources. Set to false if you know it does or don't care if it doesn't
     * @return true if all were successfully outputted
     */
    fun output(list: ResourceList, onlyTo: (ResourceNode<*>) -> Boolean = { true }, mustContainEnough: Boolean = true): Boolean {
        return !list.any { !output(it.key, it.value, onlyTo, mustContainEnough) }
    }

    /**
     * Whether or not this node group is able to output the resources with the given quantity
     * @param onlyTo only consider for outputting nodes that match this predicate
     * @param mustContainEnough whether the attached containers of the nodes must contain enough of the given resources
     */
    fun canOutput(resourceType: ResourceType, quantity: Int, onlyTo: (ResourceNode<*>) -> Boolean = { true }, mustContainEnough: Boolean = true) = getOutputter(resourceType, quantity, onlyTo, mustContainEnough) != null

    /**
     * Whether or not this node group is able to output the resources with the given quantity
     * @param onlyTo only consider for outputting nodes that match this predicate
     * @param mustContainEnough whether the attached containers of the nodes must contain enough of the given resources
     * @return true if every (resource, quantity) pair was outputted successfully
     */
    fun canOutput(list: ResourceList, onlyTo: (ResourceNode<*>) -> Boolean = { true }, mustContainEnough: Boolean = true): Boolean {
        return list.all { (r, q) -> canOutput(r, q, onlyTo, mustContainEnough) }
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

    fun addAll(other: List<ResourceNode<*>>) {
        other.filter { it !in nodes }.forEach { add(it) }
    }

    fun removeAll(l: List<ResourceNode<*>>) {
        l.forEach { remove(it) }
    }

    fun filter(predicate: (ResourceNode<*>) -> Boolean) = nodes.filter(predicate)

    fun forEach(f: (ResourceNode<*>) -> Unit) = nodes.forEach(f)

    operator fun contains(it: ResourceNode<*>) = nodes.contains(it)

    override fun toString() = "Resource node group $name:\n   Nodes:\n   ${nodes.joinToString("\n   ")}"
}