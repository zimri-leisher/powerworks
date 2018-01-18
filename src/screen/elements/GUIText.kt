package screen.elements

import graphics.Font
import graphics.Renderer

class GUIText(parent: RootGUIElement,
              name: String,
              relXPixel: Int, relYPixel: Int,
              text: Any?,
              size: Int = Font.DEFAULT_SIZE,
              var color: Int = 0xffffff,
              open: Boolean = false,
              layer: Int = parent.layer + 1) :
        GUIElement(parent, name, relXPixel, relYPixel, 0, 0, open, layer) {

    var size = size
        set(value) {
            if (value != field) {
                field = value
                updateText()
            }
        }
    var text = text
        set(value) {
            if (value != field) {
                field = value
                updateText()
            }
        }

    init {
        updateText()
    }

    fun updateText() {
        val r = Font.getStringBounds(text.toString(), size)
        widthAlignment = { r.width }
        heightAlignment = { r.height }
    }

    override fun render() {
        Renderer.renderText(text, xPixel, yPixel, size, color)
    }
}