package resource

import java.io.DataInputStream
import java.io.DataOutputStream

class ResourceContainerGroup(val containers: List<ResourceContainer<*>>) {
    /**
     * Same as the add method except gets called for each individual resource-quantity pairs
     * @return true if all were successfully added. Even if one was unable to be added, it will still try to add all the rest
     */
    fun add(list: ResourceList, from: ResourceNode<*>? = null, checkIfAble: Boolean = true): Boolean {
        var ret = true
        for ((r, q) in list) {
            if(!add(r, q, from, checkIfAble)) ret = false
        }
        return ret
    }

    /**
     * Adds the specified type and quantity to a single container in this list, if possible
     * @param from the resource node this is from, null if N/A
     * @param checkIfAble whether or not to check if the receiving container is able to hold the resources before beginning the adding process
     * @return true if a container was able to accept this
     */
    fun add(resource: ResourceType, quantity: Int, from: ResourceNode<*>? = null, checkIfAble: Boolean = true): Boolean {
        for (container in containers) {
            if (container.add(resource, quantity, from, checkIfAble))
                return true
        }
        return false
    }

    /**
     * Removes all resources in the list (only once per type) from containers in this group, if possible
     * @return false if any resource/quantity pair was unable to be removed from any container in this group
     */
    fun remove(list: ResourceList, to: ResourceNode<*>? = null, checkIfAble: Boolean = true): Boolean {
        var ret = true
        for((r, q) in list) {
            if(!remove(r, q, to, checkIfAble))
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
    fun remove(resource: ResourceType, quantity: Int, to: ResourceNode<*>? = null, checkIfAble: Boolean = true): Boolean {
        for(container in containers) {
            if(container.remove(resource, quantity, to, checkIfAble))
                return true
        }
        return false
    }

    /**
     * Returns a list of all the resources in this group
     */
    fun toList(): ResourceList {
        val list = ResourceList()
        containers.forEach { list.addAll(it.toList()) }
        return list
    }

    fun forEach(f: (ResourceContainer<*>) -> Unit) = containers.forEach(f)

    operator fun iterator() = containers.iterator()
}