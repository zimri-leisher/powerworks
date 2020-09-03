package screen.element

import graphics.Renderer
import graphics.TextureRenderParams
import main.toColor
import screen.gui.GuiElement

class ElementProgressBar(parent: GuiElement, var maxProgress: Int = 0, var getProgress: () -> Int = {0}) : GuiElement(parent) {
    var currentProgress = getProgress()

    override fun update() {
        currentProgress = getProgress()
        super.update()
    }

    override fun render(params: TextureRenderParams?) {
        val actualParams = params ?: TextureRenderParams.DEFAULT
        Renderer.renderDefaultRectangle(absoluteX, absoluteY, width, height, actualParams)
        if(currentProgress == 0) {
            return
        }
        val color = actualParams.color
        actualParams.color = toColor(0x00BC06)
        Renderer.renderFilledRectangle(absoluteX + 1, absoluteY + 1, ((width - 2) * (currentProgress.toDouble() / maxProgress)).toInt(), height - 2, params = actualParams)
        actualParams.color = color
        super.render(params)
    }
}