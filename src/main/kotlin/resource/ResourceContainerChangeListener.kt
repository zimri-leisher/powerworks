package resource

interface ResourceContainerChangeListener {
    /**
     * @param quantity the amount of resources added or removed. Negative means removed, positive means added
     */
    fun onContainerChange(container: ResourceContainer<*>, resource: ResourceType, quantity: Int)

    fun onContainerClear(container: ResourceContainer<*>)
}