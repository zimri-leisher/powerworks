package screen

import fluid.FluidTank
import graphics.Renderer
import graphics.TextManager
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceType
import screen.elements.*

class FluidTankGUI(val tank: FluidTank) : GUIWindow("Fluid tank GUI of $tank", 0, 0, WIDTH, HEIGHT, windowGroup = ScreenManager.Groups.INVENTORY), ResourceContainerChangeListener {
    private class GUIFluidTankMeter(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, val tank: FluidTank, open: Boolean = false, layer: Int = parent.layer + 1) :
            GUIElement(parent, name, xPixel, yPixel, widthPixels, heightPixels, open, layer + 1) {

        val background = GUIDefaultTextureRectangle(this, name + " background", 0, 0, layer = this.layer - 1)

        override fun render() {
            if (tank.currentFluidType != null) {
                Renderer.renderFilledRectangle(xPixel + 2, yPixel + 2, ((widthPixels - 4) * (tank.currentAmount.toDouble() / tank.maxAmount)).toInt(), heightPixels - 4, tank.currentFluidType!!.color)
            }
        }

    }

    lateinit var infoText: GUIText

    init {
        partOfLevel = true
        openAtMouse = true
        tank.listeners.add(this)
        GUIDefaultTextureRectangle(this.rootChild, this.name + " background", 0, 0).run {
            generateDragGrip(this.layer + 1)
            generateCloseButton(this.layer + 1)
            infoText = GUIText(this, this@FluidTankGUI.name + " tank info text", 2, 0,
                    if (tank.currentFluidType != null) "${tank.currentFluidType} x ${tank.currentAmount}/${tank.maxAmount}" else "Empty")
            GUIFluidTankMeter(this, this@FluidTankGUI.name + " meter", 1, TextManager.getFont().charHeight + 2, WIDTH - 2, HEIGHT - (TextManager.getFont().charHeight + 4), tank)
        }
    }

    override fun onContainerChange(container: ResourceContainer<*>, resource: ResourceType, quantity: Int) {
        if (tank.currentFluidType != null)
            infoText.text = "${tank.currentFluidType} x ${tank.currentAmount}/${tank.maxAmount}"
        else
            infoText.text = "Empty"
    }

    override fun onContainerClear(container: ResourceContainer<*>) {
        infoText.text = "Empty"
    }

    companion object {
        val WIDTH = 80
        val HEIGHT = 30
    }
}