package screen.elements

import graphics.Image
import graphics.Renderer
import graphics.Utils

class GUIDefaultTextureRectangle(parent: RootGUIElement, name: String, xAlignment: () -> Int, yAlignment: () -> Int, widthAlignment: () -> Int = parent.widthAlignment, heightAlignment: () -> Int = parent.heightAlignment, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    constructor(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, widthPixels: Int = if(parent !is GUIElement) parent.parentWindow.widthPixels else parent.widthPixels, heightPixels: Int = if(parent !is GUIElement) parent.parentWindow.heightPixels else parent.heightPixels, open: Boolean = false, layer: Int = parent.layer + 1) : this(
            parent, name, { xPixel }, { yPixel }, { widthPixels }, { heightPixels }, open, layer
    )

    var texture = Image(Utils.genRectangle(widthAlignment(), heightAlignment()))

    override fun onDimensionChange(oldWidth: Int, oldHeight: Int) {
        texture = Image(Utils.genRectangle(widthAlignment(), heightAlignment()))
    }

    override fun render() {
        Renderer.renderTexture(texture, xPixel, yPixel)
    }
}