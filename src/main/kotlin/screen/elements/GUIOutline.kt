package screen.elements

import graphics.Renderer

/**
 * Draws a white empty rectangle around the parent given (just outside the edge of its width and height pixels)
 */
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
        Renderer.renderEmptyRectangle(xPixel, yPixel, widthPixels, heightPixels, borderThickness = borderThickness.toFloat())
    }
}