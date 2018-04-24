package screen.elements

import graphics.Renderer

class GUIOutline(parent: RootGUIElement,
                 name: String,
                 var borderThickness: Int = 1, open: Boolean = false
) : GUIElement(parent, name,
        { -borderThickness }, { -borderThickness },
        { parent.widthPixels + borderThickness * 2 }, { parent.heightPixels + borderThickness * 2 },
        open) {

    init {
        transparentToInteraction = true
    }

    override fun render() {
        Renderer.renderEmptyRectangle(xPixel, yPixel, widthPixels, heightPixels, borderThickness = borderThickness)
    }
}