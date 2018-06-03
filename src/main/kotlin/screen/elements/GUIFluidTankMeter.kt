package screen.elements

import fluid.FluidTank
import graphics.Renderer
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceType

class GUIFluidTankMeter(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, widthPixels: Int = WIDTH, heightPixels: Int = HEIGHT, val tank: FluidTank, open: Boolean = false, layer: Int = parent.layer + 2) :
        GUIElement(parent, name, xPixel, yPixel, widthPixels, heightPixels, open, layer + 1), ResourceContainerChangeListener {

    var infoText: GUIText = GUIText(this, this.name + " tank info text", 1, 0,
            if (tank.currentFluidType != null) "${tank.currentFluidType} * ${tank.currentAmount}/${tank.maxAmount}" else "Empty").apply { transparentToInteraction = true }
    val background = GUIDefaultTextureRectangle(this, name + " background", 0, infoText.heightPixels + 1, heightPixels = this.heightPixels - infoText.heightPixels - 1, layer = this.layer - 1)

    init {
        transparentToInteraction = true
        tank.listeners.add(this)
    }

    override fun render() {
        if (tank.currentFluidType != null) {
            Renderer.renderFilledRectangle(xPixel + 1, infoText.yPixel + infoText.heightPixels + 2, ((widthPixels - 4) * (tank.currentAmount.toDouble() / tank.maxAmount)).toInt(), heightPixels - infoText.heightPixels - 3, tank.currentFluidType!!.color)
        }
    }

    override fun onContainerChange(container: ResourceContainer<*>, resource: ResourceType, quantity: Int) {
        if (tank.currentFluidType != null)
            infoText.text = "${tank.currentFluidType} * ${tank.currentAmount}/${tank.maxAmount}"
        else
            infoText.text = "0/${tank.maxAmount}"
    }

    override fun onContainerClear(container: ResourceContainer<*>) {
        infoText.text = "0/${tank.maxAmount}"
    }

    companion object {
        const val WIDTH = 90
        const val HEIGHT = 30
    }

}