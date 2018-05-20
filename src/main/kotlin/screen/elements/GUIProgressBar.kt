package screen.elements

import graphics.Image
import graphics.Renderer
import graphics.Utils

class GUIProgressBar(parent: RootGUIElement, name: String, xAlignment: () -> Int, yAlignment: () -> Int, widthAlignment: () -> Int, heightAlignment: () -> Int = { HEIGHT }, var maxProgress: Int, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {
    var currentProgress = 0
    private var background = Image(Utils.genRectangle(widthPixels, heightPixels))

    override fun onDimensionChange(oldWidth: Int, oldHeight: Int) {
        background = Image(Utils.genRectangle(widthPixels, heightPixels))
    }

    override fun render() {
        Renderer.renderTexture(background, xPixel, yPixel)
        Renderer.renderFilledRectangle(xPixel + 1, yPixel + 1, ((widthPixels - 2) * (currentProgress.toDouble() / maxProgress)).toInt(), heightPixels - 2, color = 0x00BC06)
    }

    companion object {
        val HEIGHT = 6
    }
}