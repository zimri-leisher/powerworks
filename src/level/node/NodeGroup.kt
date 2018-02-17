package level.node

import level.resource.ResourceType


class NodeGroup(val name: String) {
    private val nodes = mutableListOf<ResourceNode<*>>()

    fun <R : ResourceType> getPossibleOutputters(resourceType: R, quantity: Int = 1): List<OutputNode<R>> {
        val ret = mutableListOf<OutputNode<R>>()
        for (n in nodes) {
            if (n is OutputNode<*>) {
                if (n.resourceTypeID == resourceType.typeID) {
                    ret.add(n as OutputNode<R>)
                }
            }
        }
        return ret
    }

    fun add(n: ResourceNode<*>) {
        nodes.add(n)
    }

    fun remove(n: ResourceNode<*>) {
        nodes.remove(n)
    }

    fun removeAll(predicate: (ResourceNode<*>) -> Boolean) {

    }

    fun <R : ResourceType> getPossibleOutputter(resourceType: R, quantity: Int = 1): OutputNode<R>? {
        return nodes.firstOrNull { it is OutputNode<*> && it.resourceTypeID == resourceType.typeID && (it as OutputNode<R>).canOutput(resourceType, quantity) } as OutputNode<R>
    }

    fun addAll(other: NodeGroup) {
        other.nodes.forEach { if (it !in nodes) nodes.add(it) }
    }

    fun forEach(f: (ResourceNode<*>) -> Unit) = nodes.forEach(f)

    fun firstOrNull(f: (ResourceNode<*>) -> Boolean) = nodes.firstOrNull(f)
}