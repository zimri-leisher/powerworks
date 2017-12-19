package screen

import graphics.Image
import graphics.Renderer
import graphics.Renderer.params
import graphics.Texture
import io.PressType
import main.Game

class GUIButton(parent: RootGUIElement,
                name: String,
                relXPixel: Int, relYPixel: Int,
                text: String,
                private var onPress: () -> (Unit), private var onRelease: () -> (Unit), open: Boolean = false,
                layer: Int = parent.layer + 1) :
        GUIElement(parent, name, relXPixel, relYPixel, WIDTH, HEIGHT, open, layer) {

    var down = false
    var currentTexture: Texture = Image.GUI.BUTTON
    var text = text
        set(value) {
            field = value
            updateTextPos()
        }
    var textXPixel = 0
    var textYPixel = 0
    var textWidthPixels = 0
    var textHeightPixels = 0

    init {
        updateTextPos()
    }

    private fun updateTextPos() {
        val r = Game.getStringBounds(text, 20)
        textWidthPixels = r.width
        textHeightPixels = r.height
        textXPixel = (widthPixels - textWidthPixels) / 2
        textYPixel = (heightPixels - textHeightPixels) / 2
    }

    override fun onMouseEnter() {
        currentTexture = Image.GUI.BUTTON_HIGHLIGHT
    }

    override fun onMouseLeave() {
        if (down) {
            down = false
            onRelease.invoke()
        }
        currentTexture = Image.GUI.BUTTON
    }

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            currentTexture = Image.GUI.BUTTON_CLICK
            onPress.invoke()
            down = true
        } else if (type == PressType.RELEASED) {
            if (down) {
                currentTexture = Image.GUI.BUTTON_HIGHLIGHT
                onRelease.invoke()
                down = false
            }
        }
    }

    override fun onOpen() {
        if (mouseOn)
            currentTexture = Image.GUI.BUTTON_HIGHLIGHT
        else currentTexture = Image.GUI.BUTTON
    }

    override fun onClose() {
        currentTexture = Image.GUI.BUTTON
    }

    override fun render() {
        Renderer.renderTexture(currentTexture, xPixel, yPixel, params)
        Renderer.renderText(text, textXPixel + xPixel, textYPixel + yPixel)
        Renderer.renderEmptyRectangle(textXPixel + xPixel, textYPixel + yPixel, textWidthPixels, textHeightPixels)
    }

    companion object {
        val WIDTH = Image.GUI.BUTTON.widthPixels
        val HEIGHT = Image.GUI.BUTTON.heightPixels
    }
}