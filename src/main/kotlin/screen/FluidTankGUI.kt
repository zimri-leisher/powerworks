package screen

import fluid.FluidTank
import graphics.Renderer
import graphics.TextManager
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceType
import screen.elements.*

class FluidTankGUI(val tank: FluidTank) : GUIWindow("Fluid tank GUI of $tank", 0, 0, WIDTH, HEIGHT, windowGroup = ScreenManager.Groups.INVENTORY) {

    lateinit var infoText: GUIText

    init {
        partOfLevel = true
        openAtMouse = true
        generateDragGrip(this.layer + 2)
        generateCloseButton(this.layer + 2)
        GUIDefaultTextureRectangle(this.rootChild, this.name + " background", 0, 0).run {
            GUIFluidTankMeter(this, this@FluidTankGUI.name + " meter", 1, 1, WIDTH - 2, HEIGHT - 2, tank)
        }
    }

    companion object {
        val WIDTH = GUIFluidTankMeter.WIDTH + 4
        val HEIGHT = GUIFluidTankMeter.HEIGHT + 2
    }
}