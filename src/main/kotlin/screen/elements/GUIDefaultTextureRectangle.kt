package screen.elements

import graphics.Renderer

class GUIDefaultTextureRectangle(parent: RootGUIElement, name: String,
                                 xAlignment: Alignment, yAlignment: Alignment,
                                 widthAlignment: Alignment = parent.alignments.width, heightAlignment: Alignment = parent.alignments.height,
                                 open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    constructor(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, widthPixels: Int = if (parent !is GUIElement) parent.parentWindow.widthPixels else parent.widthPixels, heightPixels: Int = if (parent !is GUIElement) parent.parentWindow.heightPixels else parent.heightPixels, open: Boolean = false, layer: Int = parent.layer + 1) : this(
            parent, name, { xPixel }, { yPixel }, { widthPixels }, { heightPixels }, open, layer
    )

    override fun render() {
        Renderer.renderDefaultRectangle(xPixel, yPixel, widthPixels, heightPixels, localRenderParams)
    }
}