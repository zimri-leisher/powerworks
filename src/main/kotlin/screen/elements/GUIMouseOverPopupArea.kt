package screen.elements

/**
 * An element that opens all of its children when a mouse enters it and closes them when the mouse leaves it
 */
class GUIMouseOverPopupArea(parent: RootGUIElement, name: String,
                            xAlignment: Alignment = { 0 }, yAlignment: Alignment = { 0 },
                            widthAlignment: Alignment, heightAlignment: Alignment,
                            initializerList: GUIMouseOverPopupArea.() -> Unit = {},
                            open: Boolean = false,
                            layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    init {
        transparentToInteraction = true
        initializerList()
    }

    override fun onAddChild(child: GUIElement) {
        child.matchParentOpening = false
        child.transparentToInteraction = true
    }

    override fun onMouseEnter() {
        children.forEach { it.open = true }
    }

    override fun onMouseLeave() {
        children.forEach { it.open = false }
    }
}