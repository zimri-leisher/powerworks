package screen.elements

import graphics.Renderer
import graphics.TextureRenderParams
import graphics.text.TextManager
import graphics.text.TextRenderParams
import io.*
import main.toColor
import misc.Numbers
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor


class GUIAutocompleteMenu(parent: GUITextInputField) : GUIElement(parent, parent.name + " autocomplete menu", 0, Numbers.ceil(TextManager.getFont().charHeight), 1, 1, false) {
    var options = mutableListOf<String>()

    var currentlySelectedOption: String? = null
    /**
     * @return an ordered list of options that begin with the current word, and then options that contain the current word
     */
    fun getPossibleOptions(currentWord: String) = (options.filter { it.startsWith(currentWord) } + options.filter { it.contains(currentWord) }).distinct()
}

class GUITextInputField(parent: RootGUIElement, name: String,
                        xAlignment: Alignment, yAlignment: Alignment,
                        val widthChars: Int, val heightChars: Int,
                        var prompt: String = "",
                        var defaultValue: String = "",
                        var onPressEnter: GUITextInputField.(currentText: String) -> Unit = {},
                        var onEnterText: GUITextInputField.(currentText: String, newText: String) -> Unit = { _, _ -> },
                        var charRule: GUITextInputField.(Char) -> Boolean = { true },
                        open: Boolean = false,
                        layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { (widthChars * TextManager.getFont().charWidth).toInt() }, { Numbers.ceil(heightChars * (TextManager.getFont().charHeight + 1)) + 1 }, open, layer), TextHandler, ControlPressHandler {

    val maxChars = widthChars * heightChars

    val text = StringBuilder(defaultValue)

    var lines: List<String> = listOf(text.toString())

    /**
     * Selected doesn't necessarily mean showing a cursor, it could mean highlighted
     * If this was the last thing clicked on and escape has not been pressed, this should be selected
     */
    var selected = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    InputManager.textHandler = this
                    InputManager.map = ControlMap.TEXT_EDITOR
                } else {
                    InputManager.textHandler = null
                    InputManager.map = ControlMap.DEFAULT
                    currentIndex = -1
                    cursorFlash = false
                    cursorFlashTicks = -1
                }
            }
        }

    /**
     * Just a way to tell if the outline is flashing the OK sign (lighter color) or the ERROR sign (darker color)
     */
    private var positiveFlash = false

    private var outlineFlashTicks = -1
    private var cursorFlashTicks = -1

    private var cursorFlash = false
    var currentIndex = 0

    var autocompleteMenu = GUIAutocompleteMenu(this)

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.SCREEN_THIS, ControlMap.TEXT_EDITOR)
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            selected = true
            currentIndex = Math.min(text.lastIndex + 1, getIndex(xPixel, yPixel))
        }
    }

    override fun onInteractOff(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        selected = false
    }

    override fun handleChar(c: Char) {
        if (!insert(c.toString())) {
            negativeFlashOutline()
        } else {
            val currentWord = text.split(' ').lastOrNull()?.trim()
            if (currentWord != null && currentWord.length > CHARS_BEFORE_AUTOCOMPLETE) {
                val options = autocompleteMenu.getPossibleOptions(currentWord)
                println(options)
            }
        }
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.pressType == PressType.PRESSED) {
            when (p.control) {
                Control.PASTE_FROM_CLIPBOARD -> {
                    val data = Toolkit.getDefaultToolkit()
                            .systemClipboard.getData(DataFlavor.stringFlavor).toString()
                    insert(data)
                }
                Control.ESCAPE -> {
                    selected = false
                }
                Control.SUBMIT -> {
                    onPressEnter(text.toString())
                    positiveFlashOutline()
                }
                Control.DELETE -> {
                    delete(1)
                }
                Control.CURSOR_LEFT -> {
                    currentIndex = Math.max(0, currentIndex - 1)
                }
                Control.CURSOR_RIGHT -> {
                    currentIndex = Math.min(text.length, currentIndex + 1)
                }
                Control.CURSOR_DOWN -> {
                    currentIndex = Math.min(text.length, currentIndex + widthChars)
                }
                Control.CURSOR_UP -> {
                    currentIndex = Math.max(0, currentIndex - widthChars)
                }
            }
        }
    }

    private fun getIndex(xPixel: Int, yPixel: Int): Int {
        val relX = xPixel - this.xPixel - 2
        val relY = yPixel - this.yPixel - 2
        val charWidth = TextManager.getFont().charWidth
        val charHeight = TextManager.getFont().charHeight
        return Math.max(0, Numbers.ceil((relY / charHeight) * widthChars + relX / charWidth))
    }

    fun insert(s: String): Boolean {
        var cutString = s
        // trim to fit
        if (text.length == maxChars) {
            return false
        } else if (text.length + s.length > maxChars) {
            cutString = s.substring(0, maxChars - text.length)
        }
        // filter to rule
        cutString = cutString.filter { charRule(it) }
        // add
        text.insert(currentIndex, cutString)
        currentIndex += cutString.length
        lines = text.chunked(widthChars)
        onEnterText(text.toString(), cutString)
        return true
    }

    fun delete(number: Int): Boolean {
        var cutNumber = number
        if (text.isEmpty() || currentIndex == 0) {
            return false
        } else if (currentIndex - number < 0) {
            cutNumber = currentIndex + 1
        }
        currentIndex -= cutNumber
        text.delete(currentIndex, currentIndex + cutNumber)
        lines = text.chunked(widthChars)
        return true
    }

    private fun negativeFlashOutline() {
        positiveFlash = false
        outlineFlashTicks = OUTLINE_FLASH_TICKS
    }

    private fun positiveFlashOutline() {
        positiveFlash = true
        outlineFlashTicks = OUTLINE_FLASH_TICKS
    }

    override fun update() {
        if (selected) {
            if (cursorFlashTicks > -1) {
                cursorFlashTicks--
            } else {
                cursorFlashTicks = CURSOR_FLASH_TICKS
                cursorFlash = !cursorFlash
            }
        }
        if (outlineFlashTicks > -1) {
            outlineFlashTicks--
        }
    }

    override fun render() {
        val charWidth = TextManager.getFont().charWidth
        val charHeight = TextManager.getFont().charHeight
        val oldColor = localRenderParams.color
        localRenderParams.color.mul(toColor(if (outlineFlashTicks == -1) OUTLINE_COLOR else if (positiveFlash) OUTLINE_POS_FLASH_COLOR else OUTLINE_NEG_FLASH_COLOR))
        Renderer.renderEmptyRectangle(xPixel - 1, yPixel - 1, widthPixels + 2, heightPixels + 2, params = localRenderParams)
        localRenderParams.color = oldColor.cpy().mul(toColor(BOX_COLOR))
        Renderer.renderFilledRectangle(xPixel, yPixel, widthPixels, heightPixels, localRenderParams)
        // asssume that this will be ok
        if (text.isEmpty()) {
            Renderer.renderText(prompt, xPixel + 1, yPixel + 1, TextRenderParams(color = oldColor.cpy().mul(toColor(PROMPT_COLOR))), true)
        } else {
            for ((index, line) in lines.withIndex()) {
                Renderer.renderText(line, xPixel + 1, yPixel + 1 + Numbers.ceil((charHeight + LINE_SPACING) * index), TextRenderParams(color = oldColor.cpy().mul(toColor(TEXT_COLOR))), true)
            }
        }
        if (selected && cursorFlash) {
            Renderer.renderFilledRectangle(xPixel + Numbers.ceil(charWidth * (currentIndex % widthChars)), yPixel + Numbers.ceil((currentIndex / widthChars) * (charHeight + 1)), 1, Numbers.ceil(charHeight) + 2, TextureRenderParams(color = oldColor.cpy().mul(toColor(TEXT_COLOR))))
        }
    }

    companion object {
        const val LINE_SPACING = 1
        const val CURSOR_FLASH_TICKS = 30
        const val OUTLINE_FLASH_TICKS = 30
        const val TEXT_COLOR = 0x1D1D1D
        const val PROMPT_COLOR = 0xC0C0C0
        const val BOX_COLOR = 0xFCFAEF
        const val OUTLINE_COLOR = 0x9E9E9E
        const val OUTLINE_NEG_FLASH_COLOR = 0x2E2E2E
        const val OUTLINE_POS_FLASH_COLOR = 0xBBBBBB
        const val CHARS_BEFORE_AUTOCOMPLETE = 3
    }

}