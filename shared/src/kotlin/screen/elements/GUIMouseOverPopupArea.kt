package screen.elements

/**
 * An element that opens all of its children when a mouse enters it and closes them when the mouse leaves it
 */
class GUIMouseOverPopupArea(parent: RootGUIElement, name: String,
                            xAlignment: Alignment = { 0 }, yAlignment: Alignment = { 0 },
                            widthAlignment: Alignment, heightAlignment: Alignment,
                            initializerList: GUIMouseOverPopupArea.() -> Unit = {},
                            var autoClose: Boolean = true,
                            open: Boolean = false,
                            layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {

    private var childrenOpen = false

    init {
        initializerList()
        transparentToInteraction = true
    }

    override fun onAddChild(child: GUIElement) {
        child.matchParentOpening = false
    }

    override fun update() {
        if (mouseOn && !childrenOpen) {
            children.forEach { it.open = true }
            childrenOpen = true
        } else if (!mouseOn && childrenOpen && !children.any { it.mouseOn }) {
            children.forEach { it.open = false }
            childrenOpen = false
        }
    }
}