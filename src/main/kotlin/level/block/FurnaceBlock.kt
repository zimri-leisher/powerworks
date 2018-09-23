package level.block

import fluid.FluidTank
import fluid.FluidType
import io.*
import item.IngotItemType
import item.Inventory
import item.ItemType
import item.OreItemType
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceType
import screen.FurnaceBlockGUI

class FurnaceBlock(type: MachineBlockType<FurnaceBlock>, xTile: Int, yTile: Int, rotation: Int = 0) : MachineBlock(type, xTile, yTile, rotation), ResourceContainerChangeListener {

    // the internal inventory, not the internal tank
    val queue = containers.first { it is Inventory }
    val tank = containers.first { it is FluidTank } as FluidTank
    var currentlySmelting: OreItemType? = null
    private val gui = FurnaceBlockGUI(this)

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

    override fun update() {
        if(tank.totalQuantity > 0) {
            nodes.output(tank.currentFluidType!!, 1)
        }
        on = queue.totalQuantity > 0 && tank.totalQuantity < tank.maxAmount
        super.update()
    }

    override fun onFinishWork() {
        if (tank.canAdd(currentlySmelting!!.moltenForm, 1)) {
            if (queue.remove(currentlySmelting!!, 1)) {
                tank.add(currentlySmelting!!.moltenForm, 1, checkIfAble = false)
                nodes.output(currentlySmelting!!.moltenForm, 1)
            }
        }
        if (queue.totalQuantity > 0) {
            // start smelting another item
            if (queue.getQuantity(currentlySmelting!!) == 0) {
                currentlySmelting = queue.toList()[0]!!.first as OreItemType
            }
            // do nothing, old item still has quantity
        } else {
            on = false
            currentlySmelting = null
        }
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            gui.toggle()
        }
    }
}