package level.block

import fluid.FluidTank
import fluid.FluidType
import item.IngotItemType
import item.Inventory
import item.ItemType
import item.OreItemType
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceType

class FurnaceBlock(type: MachineBlockType<FurnaceBlock>, xTile: Int, yTile: Int, rotation: Int = 0) : MachineBlock(type, xTile, yTile, rotation), ResourceContainerChangeListener {

    // the internal inventory, not the internal tank
    val queue = containers.first { it is Inventory }
    val tank = containers.first { it is FluidTank }
    var currentlySmelting: OreItemType? = null

    init {
        containers.forEach { it.listeners.add(this) }
    }

    override fun onContainerClear(container: ResourceContainer<*>) {
        if (container == queue) {
            currentlySmelting = null
            on = false
        }
    }

    override fun onContainerChange(container: ResourceContainer<*>, resource: ResourceType, quantity: Int) {
        if (container == queue) {
            resource as OreItemType
            if (currentlySmelting == null) {
                currentlySmelting = resource
                on = true
            }
        }
    }

    override fun onFinishWork() {
        if(tank.canAdd(currentlySmelting!!.moltenForm, 1)) {
            if(queue.remove(currentlySmelting!!, 1)) {
                tank.add(currentlySmelting!!.moltenForm, 1, checkIfAble = false)
                nodes.output(currentlySmelting!!.moltenForm, 1)
            }
        }
        // if there is no more of this resource to be smelted, move on to the next one
        if (queue.getQuantity(currentlySmelting!!) == 0) {
            if (queue.totalQuantity != 0) {
                // keep on smelting
                currentlySmelting = queue.toList()[0]!!.first as OreItemType
                on = true
            } else {
                currentlySmelting = null
            }
        }
    }
}