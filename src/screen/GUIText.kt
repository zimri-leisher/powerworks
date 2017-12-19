package screen

import graphics.Renderer
import main.Game

class GUIText(parent: RootGUIElement,
              name: String,
              relXPixel: Int, relYPixel: Int,
              var text: String,
              var size: Int = 20,
              var color: Int = 0xffffff,
              open: Boolean = false,
              layer: Int = parent.layer + 1) :
        GUIElement(parent, name, relXPixel, relYPixel, 0, 0, open, layer) {

    init {
        val r = Game.getStringBounds(text, size)
        widthAlignment = { r.width }
        heightAlignment = { r.height }
    }

    override fun render() {
        Renderer.renderText(text, xPixel, yPixel, size, color)
    }
}