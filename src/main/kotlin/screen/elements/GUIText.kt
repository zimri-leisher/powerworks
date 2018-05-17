package screen.elements

import graphics.TextManager
import graphics.Renderer

class GUIText(parent: RootGUIElement,
              name: String,
              xAlignment: () -> Int, yAlignment: () -> Int,
              text: Any?,
              open: Boolean = false,
              layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { 0 }, { 0 }, open, layer) {

    constructor(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, text: Any?, open: Boolean = false, layer: Int = parent.layer + 1) : this(parent, name, { xPixel }, { yPixel }, text, open, layer)

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
        val r = TextManager.getStringBounds(text.toString())
        widthAlignment = { r.width }
        heightAlignment = { r.height }
    }

    override fun render() {
        Renderer.renderText(text, xPixel, yPixel)
    }
}