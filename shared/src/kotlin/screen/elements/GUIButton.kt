package screen.elements

import graphics.Renderer
import graphics.text.TextManager
import io.PressType
import screen.mouse.Tooltips

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
                private var onPress: () -> (Unit) = {}, private var onRelease: () -> (Unit) = {}, available: Boolean = true,
                /**
                 * If this button has available set to false, this message will show when the user mouses over it
                 */
                var notAvailableMessage: String? = null,
                open: Boolean = false,
                layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    constructor(parent: RootGUIElement,
                name: String,
                xPixel: Int, yPixel: Int,
                text: String,
                allowTags: Boolean = false,
                widthPixels: Int = if (TextManager.getStringWidth(text) > WIDTH - 4) TextManager.getStringWidth(text) + 4 else WIDTH, heightPixels: Int = HEIGHT,
                onPress: () -> Unit = {}, onRelease: () -> Unit = {}, available: Boolean = true,
                notAvailableMessage: String? = null,
                open: Boolean = false,
                layer: Int = parent.layer + 1) :
            this(parent, name, { xPixel }, { yPixel }, text, allowTags, { widthPixels }, { heightPixels }, onPress, onRelease, available, notAvailableMessage, open, layer)

    /**
     * Whether the user should be able to click this button. If this is false, the button will be greyed out
     * and the notAvailableMessage will be displayed if not null
     */
    var available = available
        set(value) {
            if (field != value) {
                if (!value) {
                    localRenderParams.rotation = 0f
                    localRenderParams.brightness = 0.9f
                } else {
                    localRenderParams.brightness = 1f
                }
                field = value
            }
        }

    private var down = false

    private var text = GUIText(this, name + " text", 0, 0, text, allowTags = allowTags, open = open).apply {
        transparentToInteraction = true
    }

    init {
        this.text.transparentToInteraction = true
        this.text.alignments.x = { (this.widthPixels - this.text.widthPixels) / 2 }
        this.text.alignments.y = { (this.heightPixels - this.text.heightPixels) / 2 }
        if (!available) {
            localRenderParams.brightness = 0.9f
        }
    }

    override fun onMouseEnter() {
        if (available) {
            localRenderParams.brightness = 1.1f
        }
    }

    override fun onMouseLeave() {
        if (available) {
            localRenderParams.rotation = 0f
            localRenderParams.brightness = 1f
            down = false
        }
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (available) {
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
    }

    override fun render() {
        Renderer.renderDefaultRectangle(xPixel, yPixel, widthPixels, heightPixels, localRenderParams)
    }

    companion object {
        const val WIDTH = 64
        const val HEIGHT = 16

        init {
            Tooltips.addScreenTooltipTemplate({ el ->
                if (el is GUIButton) {
                    if (!el.available) {
                        return@addScreenTooltipTemplate el.notAvailableMessage
                    }
                }
                return@addScreenTooltipTemplate null
            })
        }
    }
}