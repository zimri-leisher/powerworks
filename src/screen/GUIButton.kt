package screen

import graphics.Images
import graphics.Renderer
import graphics.Texture
import io.PressType
import main.Game
import java.awt.SystemColor.text
import java.awt.font.FontRenderContext

class GUIButton(parent: RootGUIElement? = RootGUIElementObject,
                name: String,
                relXPixel: Int, relYPixel: Int,
                text: String,
                var onPress: () -> (Unit), var onRelease: () -> (Unit),
                layer: Int = (parent?.layer ?: 0) + 1) :
        GUIElement(parent, name, relXPixel, relYPixel, GUI_BUTTON_DEFAULT_WIDTH, GUI_BUTTON_DEFAULT_HEIGHT, layer) {

    val text: GUIText = GUIText(this, name + " text", 0, 0, text)
    var down = false
    var currentTexture: Texture = Images.GUI_BUTTON

    init {
        val r = Game.getFont(28).getStringBounds(text, FontRenderContext(null, false, false))
        this.text.relXPixel = (widthPixels - (r.width / Game.SCALE).toInt()) / 2
        this.text.relYPixel = heightPixels / 2 + 1
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

    override fun render() {
        Renderer.renderTexture(currentTexture, xPixel, yPixel, params)
    }
}