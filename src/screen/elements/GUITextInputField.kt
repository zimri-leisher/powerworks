package screen.elements

import graphics.Font
import graphics.Renderer
import io.InputManager
import io.PressType
import io.SpecialChar
import io.TextHandler

class GUITextInputField(parent: RootGUIElement, name: String,
                        xAlignment: () -> Int, yAlignment: () -> Int,
                        widthAlignment: () -> Int, heightAlignment: () -> Int,
                        var prompt: String = "",
                        val inputRule: (Char) -> Boolean = { true },
                        val onPressEnter: GUITextInputField.(String) -> Unit = {},
                        var limitTextLength: Boolean = true,
                        open: Boolean = false,
                        layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer),
        TextHandler {

    // -1 = no cursor
    var cursorTicks = 0
    var showCursor = false
    var cursorIndex = -1
    var selected = false
        set(value) {
            if (field != value) {
                field = value
                if (field)
                    InputManager.textHandler = this
                else
                    InputManager.textHandler = null
            }
        }
    var text = StringBuilder()
    var highlightStart = -1
    var highlightEnd = -1

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            cursorIndex = getIndexOfTextAt(xPixel)
            println(cursorIndex)
            highlightStart = cursorIndex
            highlightEnd = cursorIndex
            selected = true
        }
    }

    override fun onMouseActionOff(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        selected = false
    }

    private fun getIndexOfTextAt(xPixel: Int): Int {
        val relXPixel = xPixel - this.xPixel
        var index = 0
        while (index * Font.getFont().charWidth <= relXPixel) {
            index++
        }
        return Math.min(index, text.length)
    }

    override fun handleChar(c: Char) {
        if (inputRule(c)) {
            if((limitTextLength && widthPixels / Font.getFont().charWidth > cursorIndex) || !limitTextLength) {
                text.insert(cursorIndex, c)
                cursorIndex++
            }
        }
    }

    override fun handleSpecialKey(s: SpecialChar) {
        if(s == SpecialChar.BACKSPACE) {
            if (text.isNotEmpty()) {
                if (cursorIndex > 0) {
                    text.deleteCharAt(cursorIndex - 1)
                    cursorIndex--
                }
            }
        } else if(s == SpecialChar.ESCAPE) {
            selected = false
            cursorIndex = -1
        } else if(s == SpecialChar.ENTER) {
            selected = false
            onPressEnter(text.toString())
            cursorIndex = -1
        }
    }

    override fun update() {
        if (cursorIndex != -1) {
            cursorTicks++
            if (cursorTicks >= CURSOR_BLINK_LENGTH) {
                cursorTicks = 0
                showCursor = !showCursor
            }
        }
    }

    override fun render() {
        Renderer.renderFilledRectangle(xPixel, yPixel, widthPixels, heightPixels, BOX_COLOR)
        Renderer.setClip(xPixel, yPixel, widthPixels, heightPixels)
        if (text.isNotEmpty()) {
            Renderer.renderText(text, xPixel, yPixel, color = 0x1D1D1D)
            if (showCursor)
                Renderer.renderFilledRectangle(xPixel + Font.getFont().charWidth * cursorIndex, yPixel + 1, 1, Font.getFont().charHeight - 2, color = 0x1D1D1D)
        } else {
            Renderer.renderText(prompt, xPixel, yPixel, color = 0xC8C6BB)
        }
        Renderer.resetClip()
    }

    companion object {
        const val BOX_COLOR = 0xFCFAEF
        const val CURSOR_BLINK_LENGTH = 30
    }
}