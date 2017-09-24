package screen

import graphics.Images
import graphics.Renderer
import graphics.Texture
import io.PressType
import main.Game
import java.awt.font.FontRenderContext

class GUIButton(parent: RootGUIElement? = RootGUIElementObject,
                name: String,
                relXPixel: Int, relYPixel: Int,
                text: String,
                var onPress: () -> (Unit), var onRelease: () -> (Unit),
                layer: Int = (parent?.layer ?: 0) + 1) :
        GUIElement(parent, name, relXPixel, relYPixel, DEFAULT_WIDTH, DEFAULT_HEIGHT, layer) {

    var down = false
    var currentTexture: Texture = Images.GUI_BUTTON
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
        currentTexture = Images.GUI_BUTTON_HIGHLIGHT
    }

    override fun onMouseLeave() {
        if (down) {
            down = false
            onRelease.invoke()
        }
        currentTexture = Images.GUI_BUTTON
    }

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int) {
        if (type == PressType.PRESSED) {
            currentTexture = Images.GUI_BUTTON_CLICK
            onPress.invoke()
            down = true
        } else if (type == PressType.RELEASED) {
            if (down) {
                currentTexture = Images.GUI_BUTTON_HIGHLIGHT
                onRelease.invoke()
                down = false
            }
        }
    }

    override fun onClose() {
        currentTexture = Images.GUI_BUTTON
    }

    override fun render() {
        Renderer.renderTexture(currentTexture, xPixel, yPixel, params)
        Renderer.renderText(text, textXPixel + xPixel, textYPixel + yPixel)
    }

    companion object {
        val DEFAULT_WIDTH = Images.GUI_BUTTON.widthPixels
        val DEFAULT_HEIGHT = Images.GUI_BUTTON.heightPixels
    }
}