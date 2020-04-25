package screen.elements

import fluid.FluidTank
import graphics.Renderer
import graphics.TextureRenderParams
import graphics.text.TextManager
import graphics.text.TextRenderParams
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceType
import screen.mouse.Tooltips

class GUIFluidTankMeter(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, widthPixels: Int = WIDTH, heightPixels: Int = HEIGHT, var tank: FluidTank, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xPixel, yPixel, widthPixels, heightPixels, open, layer) {

    override fun render() {
        Renderer.renderDefaultRectangle(xPixel, yPixel, widthPixels, heightPixels, TextureRenderParams(rotation = 180f, brightness = 0.9f))
        if (tank.currentFluidType != null) {
            localRenderParams.color = tank.currentFluidType!!.color
            Renderer.renderFilledRectangle(xPixel + 1, yPixel + 1, ((widthPixels - 2) * (tank.currentAmount.toDouble() / tank.maxAmount)).toInt(), heightPixels - 2, localRenderParams)
        }
    }

    private fun getText() =
            if (tank.currentFluidType != null)
                "${tank.currentFluidType}\n${tank.currentAmount}/${tank.maxAmount}"
            else
                "Empty"

    companion object {
        const val WIDTH = 90
        const val HEIGHT = 30

        init {
            Tooltips.addScreenTooltipTemplate({
                if(it is GUIFluidTankMeter) {
                    return@addScreenTooltipTemplate it.getText()
                }
                return@addScreenTooltipTemplate null
            }, 1)
        }
    }
}