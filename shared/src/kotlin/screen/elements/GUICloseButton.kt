package screen.elements

import graphics.Image
import graphics.Renderer
import io.*
import main.heightPixels
import main.widthPixels

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
        Renderer.renderTexture(if (mouseOn) Image.GUI.CLOSE_BUTTON_HIGHLIGHT else Image.GUI.CLOSE_BUTTON, xPixel, yPixel, localRenderParams)
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.RELEASED)
            actOn.open = false
    }

    companion object {
        val WIDTH = Image.GUI.CLOSE_BUTTON.widthPixels
        val HEIGHT = Image.GUI.CLOSE_BUTTON.heightPixels

    }
}