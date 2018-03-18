package resource

import item.Inventory

interface ResourceContainerChangeListener {
    fun onContainerAdd(container: ResourceContainer<*>, resource: ResourceType, quantity: Int)
    fun onContainerRemove(inv: Inventory, resource: ResourceType, quantity: Int)
    fun onContainerClear(container: ResourceContainer<*>)
    fun onContainerChange(container: ResourceContainer<*>)
}