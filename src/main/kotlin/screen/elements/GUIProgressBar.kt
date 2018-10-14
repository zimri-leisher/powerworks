package screen.elements

import graphics.Renderer
import main.toColor

class GUIProgressBar(parent: RootGUIElement, name: String, xAlignment: Alignment, yAlignment: Alignment, widthAlignment: Alignment, heightAlignment: Alignment = { HEIGHT }, var maxProgress: Int, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {
    var currentProgress = 0

    override fun render() {
        Renderer.renderDefaultRectangle(xPixel, yPixel, widthPixels, heightPixels, localRenderParams)
        val color = localRenderParams.color
        localRenderParams.color = toColor(0x00BC06)
        Renderer.renderFilledRectangle(xPixel + 1, yPixel + 1, ((widthPixels - 2) * (currentProgress.toDouble() / maxProgress)).toInt(), heightPixels - 2, params = localRenderParams)
        localRenderParams.color = color
    }

    companion object {
        const val HEIGHT = 6
    }
}