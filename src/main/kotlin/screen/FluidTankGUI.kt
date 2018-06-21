package screen

import fluid.FluidTank
import screen.elements.*

class FluidTankGUI(val tank: FluidTank) : GUIWindow("Fluid tank GUI of $tank", 0, 0, WIDTH, HEIGHT, ScreenManager.Groups.INVENTORY) {

    lateinit var infoText: GUIText

    init {
        partOfLevel = true
        openAtMouse = true
        generateDragGrip(this.layer + 2)
        generateCloseButton(this.layer + 2)
        GUIDefaultTextureRectangle(this, this.name + " background", 0, 0).run {
            GUIFluidTankMeter(this, this@FluidTankGUI.name + " meter", 1, 1, WIDTH - 2, HEIGHT - 2, tank)
        }
    }

    companion object {
        val WIDTH = GUIFluidTankMeter.WIDTH + 4
        val HEIGHT = GUIFluidTankMeter.HEIGHT + 2
    }
}