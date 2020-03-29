package screen.elements

class GUIMouseOverTextPane(parent: RootGUIElement, name: String,
                           xAlignment: Alignment, yAlignment: Alignment,
                           widthAlignment: Alignment, heightAlignment: Alignment,
                           text: String,
                           open: Boolean = false,
                           layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    private lateinit var guiText: GUIText
    private lateinit var background: GUIDefaultTextureRectangle
    private val mouseOverArea: GUIMouseOverPopupArea
    var text: String = text
        set(value) {
            if (field != value) {
                field = value
                guiText.text = value
                children.remove(background)
                background.alignments.updateDimension()
            }
        }

    init {
        transparentToInteraction = true
        mouseOverArea = GUIMouseOverPopupArea(this, name + " mouse over area", widthAlignment = widthAlignment, heightAlignment = heightAlignment, initializerList = {
            this@GUIMouseOverTextPane.guiText = GUIText(this, name + " text", 0, 0, text, layer = this.layer + 2)
            this@GUIMouseOverTextPane.background = GUIDefaultTextureRectangle(this, name + " background", { 0 }, { 0 }, { guiText.widthPixels }, { guiText.heightPixels })
        })
    }
}