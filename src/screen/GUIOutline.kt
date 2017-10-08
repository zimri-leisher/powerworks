package screen

import graphics.Renderer

class GUIOutline(parent: GUIElement,
                 name: String,
                 var borderThickness: Int = 1
) : GUIElement(parent, name,
        parent.xPixel - borderThickness, parent.yPixel - borderThickness, parent.widthPixels + borderThickness * 2, parent.heightPixels + borderThickness * 2) {

    init {
        transparentToInteraction = true
    }

    override fun render() {
        Renderer.renderEmptyRectangle(xPixel, yPixel, widthPixels, heightPixels, borderThickness = borderThickness)
    }
}