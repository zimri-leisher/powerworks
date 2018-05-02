package screen.elements

import graphics.Font
import graphics.Renderer

class GUIText(parent: RootGUIElement,
              name: String,
              xAlignment: () -> Int, yAlignment: () -> Int,
              text: Any?,
              size: Int = Font.DEFAULT_SIZE,
              var color: Int = 0xffffff,
              open: Boolean = false,
              layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { 0 }, { 0 }, open, layer) {

    constructor(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, text: Any?, size: Int = Font.DEFAULT_SIZE, color: Int = 0xffffff, open: Boolean = false, layer: Int = parent.layer + 1) : this(parent, name, { xPixel }, { yPixel }, text, size, color, open, layer)

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