package level.block

import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceType

class FurnaceBlock(type: MachineBlockType<FurnaceBlock>, xTile: Int, yTile: Int, rotation: Int = 0) : MachineBlock(type, xTile, yTile, rotation), ResourceContainerChangeListener {

    init {
        containers.forEach { it.listeners.add(this) }
    }

    override fun onContainerClear(container: ResourceContainer<*>) {
    }

    override fun onContainerChange(container: ResourceContainer<*>, resource: ResourceType, quantity: Int) {
    }
}