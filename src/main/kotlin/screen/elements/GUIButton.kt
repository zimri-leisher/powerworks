package screen.elements

import graphics.*
import graphics.Renderer.params
import graphics.text.TextManager
import io.PressType

class GUIButton(parent: RootGUIElement,
                name: String,
                xAlignment: () -> Int, yAlignment: () -> Int,
                text: String,
                allowTags: Boolean = false,
                widthAlignment: () -> Int = { if (TextManager.getStringBounds(text).width > WIDTH - 4) TextManager.getStringBounds(text).width + 4 else WIDTH }, heightAlignment: () -> Int = { HEIGHT },
                private var onPress: () -> (Unit) = {}, private var onRelease: () -> (Unit) = {}, open: Boolean = false,
                layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    constructor(parent: RootGUIElement,
                name: String,
                xPixel: Int, yPixel: Int,
                text: String,
                allowTags: Boolean = false,
                widthPixels: Int = if (TextManager.getStringBounds(text).width > WIDTH - 4) TextManager.getStringBounds(text).width + 4 else WIDTH, heightPixels: Int = HEIGHT,
                onPress: () -> Unit = {}, onRelease: () -> Unit = {}, open: Boolean = false,
                layer: Int = parent.layer + 1) :
            this(parent, name, {xPixel}, {yPixel}, text, allowTags, {widthPixels}, {heightPixels}, onPress, onRelease, open, layer)

    var down = false

    // 0: unhighlighted, 1: highlighted, 2: clicked
    private val textures = arrayOf<Texture>(
            Image(Utils.genRectangle(widthPixels, heightPixels)),
            Image(Utils.modify(Utils.genRectangle(widthPixels, heightPixels), ImageParams(brightnessMultiplier = 1.2))),
            Image(Utils.modify(Utils.genRectangle(widthPixels, heightPixels), ImageParams(rotation = 2))))

    var currentTexture: Texture = textures[0]
    var text = GUIText(this, name + " text", 0, 0, text, allowTags = allowTags, open = open).apply { transparentToInteraction = true }

    init {
        this.text.transparentToInteraction = true
        this.text.alignments.x = { (this.widthPixels - this.text.widthPixels) / 2 }
        this.text.alignments.y = { (this.heightPixels - this.text.heightPixels) / 2 }
        // if it is the default specs
    }

    override fun onMouseEnter() {
        currentTexture = textures[1]
    }

    override fun onMouseLeave() {
        down = false
        currentTexture = textures[0]
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            currentTexture = textures[2]
            onPress.invoke()
            down = true
        } else if (type == PressType.RELEASED) {
            if (down) {
                currentTexture = textures[1]
                onRelease.invoke()
                down = false
            }
        }
    }

    override fun onOpen() {
        if (mouseOn)
            currentTexture = textures[1]
        else currentTexture = textures[0]
    }

    override fun onClose() {
        currentTexture = textures[0]
    }

    override fun render() {
        Renderer.renderTexture(currentTexture, xPixel, yPixel, params)
    }

    companion object {
        val WIDTH = 64
        val HEIGHT = 16
    }
}