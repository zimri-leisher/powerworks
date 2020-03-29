package screen.elements

import graphics.Renderer
import graphics.TextureRenderParams
import graphics.text.TextManager
import io.PressType
import main.toColor

class GUIListSelector(parent: RootGUIElement, name: String,
                      xAlignment: Alignment, yAlignment: Alignment,
                      val options: Array<String> = arrayOf(),
                      defaultOption: String = "",
                      var onSelectOption: GUIListSelector.(option: String) -> Unit = {}, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment,
                { TextManager.getStringWidth(options.maxBy { TextManager.getStringWidth(it) } ?: "") + 2 * TEXT_WIDTH_PADDING },
                { TextManager.getStringHeight(options.maxBy { TextManager.getStringHeight(it) } ?: "") + 2 * TEXT_HEIGHT_PADDING },
                open, layer) {

    var selectionsOpen = false
        set(value) {
            field = value
            alignments.update()
        }
    var selectedOption = defaultOption

    init {
        with(alignments) {
            width = {
                TextManager.getStringWidth(options.maxBy { TextManager.getStringWidth(it) } ?: "") + 2 * TEXT_WIDTH_PADDING
            }
            height = {
                if (selectionsOpen)
                    TextManager.getStringHeight(options.maxBy { TextManager.getStringHeight(it) }
                            ?: "") + 2 * TEXT_HEIGHT_PADDING + options.sumBy { TextManager.getStringHeight(it) + 2 * TEXT_HEIGHT_PADDING }
                else
                    TextManager.getStringHeight(options.maxBy { TextManager.getStringHeight(it) }
                            ?: "") + 2 * TEXT_HEIGHT_PADDING
            }
            y = { yAlignment() - if (selectionsOpen) options.sumBy { TextManager.getStringHeight(it) + 2 * TEXT_HEIGHT_PADDING } else 0 }
        }
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.RELEASED) {
            if (selectionsOpen) {
                val index = getSelectionIndex(yPixel)
                if (index != -1) {
                    val option = options[index]
                    selectedOption = option
                    selectionsOpen = false
                }
            } else {
                selectionsOpen = true
            }
        }
    }

    private fun getSelectionIndex(yPixel: Int): Int {
        var relYPixel = yPixel - this.yPixel
        if(relYPixel <= TextManager.getStringHeight(selectedOption) + 2 * TEXT_HEIGHT_PADDING) {
            return -1
        }
        for((index, option) in options.withIndex()) {
            relYPixel -= TextManager.getStringHeight(option) + 2 * TEXT_HEIGHT_PADDING
            if(relYPixel <= 0) {
                return index
            }
        }
        return -1
    }

    override fun update() {

    }

    override fun render() {
        Renderer.renderFilledRectangle(xPixel, yPixel, widthPixels, heightPixels, TextureRenderParams(color = toColor(0x878787)))
        if(selectionsOpen) {
            var yOffset = 0
            for((index, option) in options.withIndex()) {
                Renderer.renderText(option, xPixel + TEXT_WIDTH_PADDING, yPixel)
            }
        } else {
            Renderer.renderText(selectedOption, xPixel + TEXT_WIDTH_PADDING, yPixel + TEXT_HEIGHT_PADDING)
        }
    }

    companion object {
        val TEXT_HEIGHT_PADDING = 1
        val TEXT_WIDTH_PADDING = 1
    }
}