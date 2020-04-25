package resource

interface ResourceContainerChangeListener {

    fun onAddToContainer(container: ResourceContainer, resources: ResourceList)

    fun onRemoveFromContainer(container: ResourceContainer, resources: ResourceList)

    fun onContainerClear(container: ResourceContainer)
}