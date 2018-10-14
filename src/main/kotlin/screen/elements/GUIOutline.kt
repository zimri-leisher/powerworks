package screen.elements

import graphics.Renderer

/**
 * Draws a white empty rectangle around the parent given (just outside the edge of its width and height pixels)
 */
class GUIOutline(parent: RootGUIElement,
                 name: String, xAlignment: Alignment, yAlignment: Alignment, widthAlignment: Alignment, heightAlignment: Alignment,
                 var borderThickness: Int = 1, open: Boolean = false, layer: Int = parent.layer + 1
) : GUIElement(parent, name,
        xAlignment, yAlignment, widthAlignment, heightAlignment,
        open, layer) {

    constructor(parent: RootGUIElement, name: String, borderThickness: Int = 1, open: Boolean = false, layer: Int = parent.layer + 1) : this(parent, name,
            { -borderThickness }, { -borderThickness },
            { parent.widthPixels + borderThickness * 2 }, { parent.heightPixels + borderThickness * 2 },
            borderThickness, open, layer)

    init {
        transparentToInteraction = true
    }

    override fun render() {
        Renderer.renderEmptyRectangle(xPixel, yPixel, widthPixels, heightPixels, borderThickness = borderThickness.toFloat())
    }
}