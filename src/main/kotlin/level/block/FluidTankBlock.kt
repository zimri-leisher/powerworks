package level.block

import fluid.FluidTank
import io.*
import screen.FluidTankGUI

class FluidTankBlock(type: FluidTankBlockType, xTile: Int, yTile: Int, rotation: Int) : Block(type, xTile, yTile, rotation) {
    val tank = containers.first { it is FluidTank } as FluidTank

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if(type == PressType.RELEASED) {
            this.type.guiPool!!.toggle(this)
        }
    }
}