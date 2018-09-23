package screen.elements

import graphics.Image
import graphics.Renderer
import io.PressType

/**
 * If a window has this as a child, it will be able to be closed by Control.ESCAPE as well as clicking this
 */
class GUICloseButton(parent: RootGUIElement,
                     name: String,
                     xAlignment: Alignment, yAlignment: Alignment,
                     open: Boolean = false,
                     layer: Int = parent.layer + 1,
                     val actOn: GUIWindow) : GUIElement(parent, name, xAlignment, yAlignment, { WIDTH }, { HEIGHT }, open, layer) {

    override fun render() {
        Renderer.renderTexture(Image.GUI.CLOSE_BUTTON, xPixel, yPixel, localRenderParams)
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED)
            actOn.open = false
    }

    companion object {
        const val WIDTH = 4
        const val HEIGHT = 4
    }
}