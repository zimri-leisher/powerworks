package level.block

import fluid.FluidTank
import io.*
import screen.FluidTankGUI

class FluidTankBlock(type: FluidTankBlockType, xTile: Int, yTile: Int, rotation: Int) : Block(type, xTile, yTile, rotation), ControlPressHandler {
    val tank = containers.first { it is FluidTank } as FluidTank
    private val gui = FluidTankGUI(tank)

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_THIS, Control.INTERACT)
    }

    override fun handleControlPress(p: ControlPress) {
        if(p.control == Control.INTERACT && p.pressType == PressType.PRESSED)
            gui.toggle()
    }
}