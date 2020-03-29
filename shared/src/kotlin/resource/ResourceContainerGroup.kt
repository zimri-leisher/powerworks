package resource

typealias ResourceContainerGroup = MutableList<ResourceContainer>

/**
 * Same as the add method except gets called for each individual resource-quantity pairs
 * @return true if all were successfully added. Even if one was unable to be added, it will still try to add all the rest
 */
fun ResourceContainerGroup.give(list: ResourceList, from: ResourceNode? = null, checkIfAble: Boolean = true): Boolean {
    var ret = true
    for ((r, q) in list) {
        if (!give(r, q, from, checkIfAble)) ret = false
    }
    return ret
}

/**
 * Adds the specified type and quantity to a single container in this list, if possible
 * @param from the resource node this is from, null if N/A
 * @param checkIfAble whether or not to check if the receiving container is able to hold the resources before beginning the adding process
 * @return true if a container was able to accept this
 */
fun ResourceContainerGroup.give(resource: ResourceType, quantity: Int, from: ResourceNode? = null, checkIfAble: Boolean = true): Boolean {
    for (container in this) {
        if (container.add(resource, quantity, from, checkIfAble))
            return true
    }
    return false
}

/**
 * Removes all resources in the list (only once per type) from containers in this group, if possible
 * @return false if any resource/quantity pair was unable to be removed from any container in this group
 */
fun ResourceContainerGroup.take(list: ResourceList, to: ResourceNode? = null, checkIfAble: Boolean = true): Boolean {
    var ret = true
    for ((r, q) in list) {
        if (!take(r, q, to, checkIfAble))
            ret = false
    }
    return ret
}

/**
 * Removes the specified type and quantity from a single container in this list, if possible
 * @param to the resource node that this quantity is going to after it is removed from this
 * @param checkIfAble whether or not to check if the container contains the specified resources
 * @return true if a container had this amount and type removed from it
 */
fun ResourceContainerGroup.take(resource: ResourceType, quantity: Int, to: ResourceNode? = null, checkIfAble: Boolean = true): Boolean {
    for (container in this) {
        if (container.remove(resource, quantity, to, checkIfAble))
            return true
    }
    return false
}

/**
 * Returns a list of all the resources in this group
 */
fun ResourceContainerGroup.toResourceList(): ResourceList {
    val list = ResourceList()
    forEach { list.addAll(it.resourceList()) }
    return list
}

fun ResourceContainerGroup.contains(resource: ResourceType, quantity: Int) = getQuantityOf(resource) >= quantity

fun ResourceContainerGroup.getQuantityOf(resource: ResourceType): Int {
    var q = 0
    for (container in this) {
        q += container.getQuantity(resource)
    }
    return q
}