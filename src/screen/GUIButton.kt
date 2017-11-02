package screen

import graphics.Image
import graphics.Renderer
import graphics.Renderer.params
import graphics.Texture
import io.PressType
import main.Game
import java.awt.font.FontRenderContext

class GUIButton(parent: RootGUIElement,
                name: String,
                relXPixel: Int, relYPixel: Int,
                text: String,
                var onPress: () -> (Unit), var onRelease: () -> (Unit), open: Boolean = false,
                layer: Int = parent.layer + 1) :
        GUIElement(parent, name, relXPixel, relYPixel, DEFAULT_WIDTH, DEFAULT_HEIGHT, open, layer) {

    var down = false
    var currentTexture: Texture = Image.GUI.BUTTON
    var text = text
        set(value) {
            field = value
            updateTextPos()
        }
    var textXPixel = 0
    var textYPixel = 0

    init {
        updateTextPos()
    }

    private fun updateTextPos() {
        val r = Game.getFont(28).getStringBounds(text, FontRenderContext(null, false, false))
        textXPixel = ((widthPixels - (r.width / Game.SCALE).toInt()) / 2)
        textYPixel = (heightPixels / 2 + 1)
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

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
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

    override fun onClose() {
        currentTexture = Image.GUI.BUTTON
    }

    override fun render() {
        Renderer.renderTexture(currentTexture, xPixel, yPixel, params)
        Renderer.renderText(text, textXPixel + xPixel, textYPixel + yPixel)
    }

    companion object {
        val DEFAULT_WIDTH = Image.GUI.BUTTON.widthPixels
        val DEFAULT_HEIGHT = Image.GUI.BUTTON.heightPixels
    }
}