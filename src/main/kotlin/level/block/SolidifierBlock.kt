package level.block

import fluid.FluidTank
import fluid.MoltenOreFluidType
import io.PressType
import item.Inventory
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceType
import screen.SolidifierBlockGUI

class SolidifierBlock(xTile: Int, yTile: Int, rotation: Int) : MachineBlock(MachineBlockType.SOLIDIFIER, xTile, yTile, rotation), ResourceContainerChangeListener {
    val tank = containers.first { it is FluidTank } as FluidTank

    val out = containers.first { it is Inventory } as Inventory

    var currentlySolidifying: MoltenOreFluidType? = null

    init {
        containers.forEach { it.listeners.add(this) }
    }

    override fun onContainerChange(container: ResourceContainer<*>, resource: ResourceType, quantity: Int) {
        if (container == tank) {
            if (tank.currentAmount > 0) {
                currentlySolidifying = tank.currentFluidType!! as MoltenOreFluidType
                on = true
            } else {
                on = false
                currentWork = 0
            }
        }
    }

    override fun onContainerClear(container: ResourceContainer<*>) {
        if (container == tank) {
            currentlySolidifying = null
            on = false
            currentWork = 0
        }
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.RELEASED) {
            this.type.guiPool!!.toggle(this)
        }
    }

    override fun onFinishWork() {
        if (out.add(currentlySolidifying!!.ingot)) {
            if (tank.remove(currentlySolidifying!!)) {
                nodes.output(currentlySolidifying!!.ingot, 1)
            }
        }
        if (tank.currentAmount == 0) {
            currentlySolidifying = null
        }
    }

}
