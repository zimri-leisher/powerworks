package resource

interface ResourceContainerChangeListener {
    /**
     * @param quantity can be negative, meaning removal of resources
     */
    fun onContainerChange(container: ResourceContainer<*>, resource: ResourceType, quantity: Int)

    fun onContainerClear(container: ResourceContainer<*>)
}