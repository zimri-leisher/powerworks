package screen

import fluid.FluidTank
import level.block.ChestBlock
import level.block.FluidTankBlock
import screen.elements.*

open class FluidTankGUI(tank: FluidTank) : GUIWindow("Fluid tank GUI", 0, 0, WIDTH, HEIGHT) {
    var tank = tank
        protected set

    protected val fluidTankMeter: GUIFluidTankMeter

    init {
        openAtMouse = true
        generateDragGrip(this.layer + 2)
        generateCloseButton(this.layer + 2)
        GUIDefaultTextureRectangle(this, this.name + " background", 0, 0).run {
            fluidTankMeter = GUIFluidTankMeter(this, this@FluidTankGUI.name + " meter", 1, 1, WIDTH - 2, HEIGHT - 2, tank)
        }
    }

    companion object {
        val WIDTH = GUIFluidTankMeter.WIDTH + 4
        val HEIGHT = GUIFluidTankMeter.HEIGHT + 2
    }
}