package screen

import graphics.Image
import graphics.Renderer
import io.PressType

class GUICloseButton(parent: RootGUIElement,
                     name: String,
                     xAlignment: () -> Int, yAlignment: () -> Int,
                     open: Boolean = false,
                     layer: Int = parent.layer + 1,
                     val actOn: GUIWindow) : GUIElement(parent, name, xAlignment, yAlignment, { WIDTH }, { HEIGHT }, open, layer) {

    override fun render() {
        Renderer.renderTexture(Image.GUI.CLOSE_BUTTON, xPixel, yPixel)
    }

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED)
            actOn.open = false
    }

    companion object {
        const val WIDTH = 4
        const val HEIGHT = 4
    }
}