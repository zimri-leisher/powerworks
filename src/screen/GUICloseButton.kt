package screen

import graphics.Image
import graphics.Renderer
import io.PressType

class GUICloseButton(parent: RootGUIElement,
                     name: String,
                     xPixel: Int, yPixel: Int,
                     open: Boolean = false,
                     layer: Int = parent.layer + 1) : GUIElement(parent, name, xPixel, yPixel, WIDTH, HEIGHT, open, layer) {

    override fun render() {
        Renderer.renderTexture(Image.GUI.CLOSE_BUTTON, xPixel, yPixel)
    }

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
        if (type == PressType.PRESSED)
            parentWindow.open = false
    }

    companion object {
        const val WIDTH = 4
        const val HEIGHT = 4
    }
}