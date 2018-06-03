package level.block

import fluid.FluidTank
import fluid.MoltenOreFluidType
import io.*
import item.Inventory
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceType
import screen.SolidifierBlockGUI

class SolidifierBlock(xTile: Int, yTile: Int, rotation: Int) : MachineBlock(MachineBlockType.SOLIDIFIER, xTile, yTile, rotation), ResourceContainerChangeListener, ControlPressHandler {
    val tank = containers.first { it is FluidTank } as FluidTank

    val out = containers.first { it is Inventory } as Inventory
    private val gui = SolidifierBlockGUI(this)

    var currentlySolidifying: MoltenOreFluidType? = null

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_THIS, Control.INTERACT)
        containers.forEach { it.listeners.add(this) }
    }

    override fun onContainerChange(container: ResourceContainer<*>, resource: ResourceType, quantity: Int) {
        if(container == tank) {
            if(tank.currentAmount > 0) {
                currentlySolidifying = tank.currentFluidType!! as MoltenOreFluidType
                on = true
            } else {
                on = false
                currentWork = 0
            }
        }
    }

    override fun onContainerClear(container: ResourceContainer<*>) {
        if(container == tank)
            currentlySolidifying = null; on = false; currentWork = 0;
    }

    override fun handleControlPress(p: ControlPress) {
        if(p.pressType == PressType.PRESSED && p.control == Control.INTERACT) {
            gui.toggle()
        }
    }

    override fun onFinishWork() {
        if(out.add(currentlySolidifying!!.ingot)) {
            if(tank.remove(currentlySolidifying!!)) {
                nodes.output(currentlySolidifying!!.ingot, 1)
            }
        }
        if(tank.currentAmount == 0) {
            currentlySolidifying = null
        }
    }

}
