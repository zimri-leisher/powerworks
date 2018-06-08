package screen.elements

import graphics.text.TextManager
import graphics.Renderer
import graphics.text.TextRenderParams

class GUIText(parent: RootGUIElement,
              name: String,
              xAlignment: () -> Int, yAlignment: () -> Int,
              text: Any?,
              val renderParams: TextRenderParams = TextRenderParams(),
              open: Boolean = false,
              layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { 0 }, { 0 }, open, layer) {

    constructor(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, text: Any?, renderParams: TextRenderParams = TextRenderParams(), open: Boolean = false, layer: Int = parent.layer + 1) : this(parent, name, { xPixel }, { yPixel }, text, renderParams, open, layer)

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
        Renderer.renderText(text, xPixel, yPixel, renderParams)
    }
}