package screen

import graphics.Image
import graphics.Utils

class GUIMouseOverTextPane(parent: RootGUIElement, name: String,
                           xAlignment: () -> Int, yAlignment: () -> Int,
                           widthAlignment: () -> Int, heightAlignment: () -> Int,
                           text: String,
                           open: Boolean = false,
                           layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    private lateinit var guiText: GUIText
    private lateinit var background: GUITexturePane
    private val mouseOverArea: GUIMouseOverArea
    var text: String = text
        set(value) {
            if (field != value) {
                field = value
                guiText.text = value
                children.remove(background)
                background = genBackground()
            }
        }

    init {
        transparentToInteraction = true
        mouseOverArea = GUIMouseOverArea(this, name + " mouse over area", widthAlignment = widthAlignment, heightAlignment = heightAlignment, initializerList = {
            this@GUIMouseOverTextPane.guiText = GUIText(this, name + " text", 0, 0, text, layer = this.layer + 2)
            this@GUIMouseOverTextPane.background = GUITexturePane(this, name + " background", 0, 0, Image(Utils.genRectangle(guiText.widthPixels, guiText.heightPixels)))
        })
    }

    fun genBackground(): GUITexturePane {
        return GUITexturePane(mouseOverArea, name + " background", 0, 0, Image(Utils.genRectangle(guiText.widthPixels, guiText.heightPixels)))
    }
}