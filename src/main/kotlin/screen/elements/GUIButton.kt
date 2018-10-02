package screen.elements

import graphics.Renderer
import graphics.text.TextManager
import io.PressType

class GUIButton(parent: RootGUIElement,
                name: String,
                xAlignment: Alignment, yAlignment: Alignment,
                text: String,
                allowTags: Boolean = false,
                widthAlignment: Alignment = {
                    val width = TextManager.getStringWidth(text)
                    if (width > WIDTH - 4)
                        width + 4
                    else
                        WIDTH
                }, heightAlignment: Alignment = { HEIGHT },
                private var onPress: () -> (Unit) = {}, private var onRelease: () -> (Unit) = {}, open: Boolean = false,
                layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    constructor(parent: RootGUIElement,
                name: String,
                xPixel: Int, yPixel: Int,
                text: String,
                allowTags: Boolean = false,
                widthPixels: Int = if (TextManager.getStringWidth(text) > WIDTH - 4) TextManager.getStringWidth(text) + 4 else WIDTH, heightPixels: Int = HEIGHT,
                onPress: () -> Unit = {}, onRelease: () -> Unit = {}, open: Boolean = false,
                layer: Int = parent.layer + 1) :
            this(parent, name, { xPixel }, { yPixel }, text, allowTags, { widthPixels }, { heightPixels }, onPress, onRelease, open, layer)

    var down = false

    var text = GUIText(this, name + " text", 0, 0, text, allowTags = allowTags, open = open).apply {
        transparentToInteraction = true
    }

    init {
        this.text.transparentToInteraction = true
        this.text.alignments.x = { (this.widthPixels - this.text.widthPixels) / 2 }
        this.text.alignments.y = { (this.heightPixels - this.text.heightPixels) / 2 }
        // if it is the default specs
    }

    override fun onMouseEnter() {
        localRenderParams.brightness = 1.1f
    }

    override fun onMouseLeave() {
        localRenderParams.rotation = 0f
        localRenderParams.brightness = 1f
        down = false
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            onPress.invoke()
            localRenderParams.rotation = 180f
            localRenderParams.brightness = 0.9f
            down = true
        } else if (type == PressType.RELEASED) {
            if (down) {
                onRelease.invoke()
                localRenderParams.rotation = 0f
                localRenderParams.brightness = 1.1f
                down = false
            }
        }
    }

    override fun render() {
        Renderer.renderDefaultRectangle(xPixel, yPixel, widthPixels, heightPixels, localRenderParams)
    }

    companion object {
        const val WIDTH = 64
        const val HEIGHT = 16
    }
}