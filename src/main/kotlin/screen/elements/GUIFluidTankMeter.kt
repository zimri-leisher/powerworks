package screen.elements

import fluid.FluidTank
import graphics.Renderer
import graphics.text.TextManager
import graphics.text.TextRenderParams
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceType

class GUIFluidTankMeter(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, widthPixels: Int = WIDTH, heightPixels: Int = HEIGHT, val tank: FluidTank, open: Boolean = false, layer: Int = parent.layer + 2) :
        GUIElement(parent, name, xPixel, yPixel, widthPixels, heightPixels, open, layer + 1), ResourceContainerChangeListener {

    var infoText: GUIText = GUIText(this, this.name + " tank info text", (widthPixels - TextManager.getStringWidth(getText())) / 2, heightPixels - TextManager.getFont().charHeight.toInt(),
            getText(), TextRenderParams(size = 15)).apply {
        transparentToInteraction = true
    }
    val background = GUIDefaultTextureRectangle(this, name + " background", 0, 0, heightPixels = this.heightPixels - infoText.heightPixels - 1, layer = this.layer - 2).apply {
        localRenderParams.rotation = 180f
    }

    init {
        transparentToInteraction = true
        tank.listeners.add(this)
    }

    override fun render() {
        if (tank.currentFluidType != null) {
            localRenderParams.color = tank.currentFluidType!!.color
            Renderer.renderFilledRectangle(xPixel + 1, yPixel + 1, ((background.widthPixels - 2) * (tank.currentAmount.toDouble() / tank.maxAmount)).toInt(), background.heightPixels - 2, localRenderParams)
        }
    }

    override fun onContainerChange(container: ResourceContainer, resource: ResourceType, quantity: Int) {
        infoText.text = getText()
    }

    override fun onContainerClear(container: ResourceContainer) {
        infoText.text = "0/${tank.maxAmount}"
    }

    private fun getText() =
            if (tank.currentFluidType != null)
                "${tank.currentFluidType} * ${tank.currentAmount}/${tank.maxAmount}"
            else
                "0/${tank.maxAmount}"

    companion object {
        const val WIDTH = 90
        const val HEIGHT = 30
    }
}