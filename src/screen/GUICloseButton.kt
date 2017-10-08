package screen

import graphics.Image
import graphics.Renderer
import io.PressType

class GUICloseButton(parent: GUIElement, name: String, xPixel: Int, yPixel: Int, layer: Int = parent.layer + 1) : GUIElement(parent, name, xPixel, yPixel, WIDTH, HEIGHT, layer) {

    override fun render() {
        Renderer.renderTexture(Image.GUI.CLOSE_BUTTON, xPixel, yPixel)
    }

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
        if (type == PressType.PRESSED)
            parent.open = false
    }

    companion object {
        const val WIDTH = 8
        const val HEIGHT = 8
    }
}