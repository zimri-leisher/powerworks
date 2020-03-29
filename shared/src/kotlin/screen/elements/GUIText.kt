package screen.elements

import graphics.Renderer
import graphics.text.TaggedText
import graphics.text.TextManager
import graphics.text.TextRenderParams

class GUIText(parent: RootGUIElement,
              name: String,
              xAlignment: Alignment, yAlignment: Alignment,
              text: Any?,
              val textRenderParams: TextRenderParams = TextRenderParams(),
              allowTags: Boolean = false,
              open: Boolean = false,
              layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, {
            if (allowTags) {
                TextManager.getStringWidth(TextManager.parseTags(text.toString()), textRenderParams.size, textRenderParams.style)
            } else {
                TextManager.getStringWidth(text.toString(), textRenderParams.size, textRenderParams.style)
            }
        }, {
            if (allowTags) {
                TextManager.getStringHeight(TextManager.parseTags(text.toString()), textRenderParams.size, textRenderParams.style)
            } else {
                TextManager.getStringHeight(text.toString(), textRenderParams.size, textRenderParams.style)
            }
        }, open, layer) {

    constructor(parent: RootGUIElement,
                name: String,
                xPixel: Int, yPixel: Int,
                text: Any?,
                renderParams: TextRenderParams = TextRenderParams(),
                allowTags: Boolean = false,
                open: Boolean = false, layer: Int = parent.layer + 1) : this(parent, name, { xPixel }, { yPixel }, text, renderParams, allowTags, open, layer)

    var allowTags = allowTags
        set(value) {
            if (value != field) {
                field = value
                if (value) {
                    tags = TextManager.parseTags(text.toString())
                    updateText()
                }
            }
        }

    var text = text
        set(value) {
            if (value != field) {
                field = value
                if (allowTags) {
                    tags = TextManager.parseTags(value.toString())
                }
                updateText()
            }
        }

    private var tags: TaggedText = TextManager.parseTags(if (allowTags) text.toString() else "")

    init {
        updateText()
    }


    fun updateText() {
        val r = if (allowTags) {
            TextManager.getStringBounds(tags, textRenderParams.size, textRenderParams.style)
        } else {
            TextManager.getStringBounds(text.toString(), textRenderParams.size, textRenderParams.style)
        }
        alignments.width = { r.width }
        alignments.height = { r.height }
    }

    override fun render() {
        if (!allowTags)
            Renderer.renderText(text, xPixel, yPixel, textRenderParams)
        else
            Renderer.renderTaggedText(tags, xPixel, yPixel, textRenderParams)
    }
}