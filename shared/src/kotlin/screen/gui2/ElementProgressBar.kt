package screen.gui2

import graphics.Renderer
import graphics.TextureRenderParams
import main.toColor

class ElementProgressBar(parent: GuiElement, var maxProgress: Int = 0, var getProgress: () -> Int = {0}) : GuiElement(parent) {
    var currentProgress = getProgress()

    override fun update() {
        currentProgress = getProgress()
        super.update()
    }

    override fun render(params: TextureRenderParams?) {
        val actualParams = params ?: TextureRenderParams.DEFAULT
        Renderer.renderDefaultRectangle(absoluteXPixel, absoluteYPixel, widthPixels, heightPixels, actualParams)
        if(currentProgress == 0) {
            return
        }
        val color = actualParams.color
        actualParams.color = toColor(0x00BC06)
        Renderer.renderFilledRectangle(absoluteXPixel + 1, absoluteYPixel + 1, ((widthPixels - 2) * (currentProgress.toDouble() / maxProgress)).toInt(), heightPixels - 2, params = actualParams)
        actualParams.color = color
        super.render(params)
    }
}