package level.block

import com.badlogic.gdx.Input
import fluid.FluidTank
import io.*
import item.Inventory
import item.OreItemType
import resource.*
import serialization.Id

class FurnaceBlock(type: MachineBlockType<FurnaceBlock>, xTile: Int, yTile: Int, rotation: Int = 0) : MachineBlock(type, xTile, yTile, rotation), ResourceContainerChangeListener {

    // the internal inventory, not the internal tank
    @Id(23)
    val queue = containers.first { it is Inventory } as Inventory
    @Id(24)
    val tank = containers.first { it is FluidTank } as FluidTank
    @Id(25)
    var currentlySmelting: OreItemType? = null

    init {
        containers.forEach { it.listeners.add(this) }
    }

    override fun onContainerClear(container: ResourceContainer) {
        if (container.id == queue.id) {
            currentlySmelting = null
            on = false
        }
    }

    override fun onAddToContainer(container: ResourceContainer, resources: ResourceList) {
        if (container.id == queue.id) {
            if (currentlySmelting == null) {
                currentlySmelting = resources[0]!!.key as OreItemType
                on = true
            }
        }
    }

    override fun onRemoveFromContainer(container: ResourceContainer, resources: ResourceList) {
        if (container.id == queue.id) {
            if (currentlySmelting == null) {
                currentlySmelting = resources[0]!!.key as OreItemType
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
        if(currentlySmelting == null) {
            // some desync
            return
        }
        if (tank.canAdd(ResourceList(currentlySmelting!!.moltenForm to 1))) {
            if (queue.remove(currentlySmelting!!, 1)) {
                tank.add(currentlySmelting!!.moltenForm, 1, checkIfAble = false)
            }
        }
        if (queue.totalQuantity > 0) {
            // start smelting another item
            if (queue.getQuantity(currentlySmelting!!) == 0) {
                currentlySmelting = queue.toResourceList()[0]!!.key as OreItemType
            }
            // do nothing, old item still has quantity
        } else {
            on = false
            currentlySmelting = null
        }
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.RELEASED && !shift && !ctrl && !alt && button == Input.Buttons.LEFT) {
            this.type.guiPool!!.toggle(this)
        }
    }
}