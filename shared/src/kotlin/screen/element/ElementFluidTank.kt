package screen.element

import fluid.FluidTank
import graphics.Renderer
import graphics.TextureRenderParams
import screen.gui.GuiElement

class ElementFluidTank(parent: GuiElement, var tank: FluidTank) : GuiElement(parent) {
    override fun render(params: TextureRenderParams?) {
        val actualParams = params ?: TextureRenderParams.DEFAULT
        Renderer.renderDefaultRectangle(absoluteXPixel, absoluteYPixel, widthPixels, heightPixels, TextureRenderParams(rotation = 180f, brightness = 0.9f))
        if (tank.currentFluidType != null) {
            Renderer.renderFilledRectangle(absoluteXPixel + 1, absoluteYPixel + 1, ((widthPixels - 2) * (tank.currentAmount.toDouble() / tank.maxAmount)).toInt(), heightPixels - 2, actualParams.combine(TextureRenderParams(color = tank.currentFluidType!!.color)))
        }
        super.render(params)
    }

    private fun getText() =
            if (tank.currentFluidType != null)
                "${tank.currentFluidType}\n${tank.currentAmount}/${tank.maxAmount}"
            else
                "Empty"
}