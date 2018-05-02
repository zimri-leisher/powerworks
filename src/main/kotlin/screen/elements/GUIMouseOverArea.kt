package screen.elements

class GUIMouseOverArea(parent: RootGUIElement, name: String,
                       xAlignment: () -> Int = { 0 }, yAlignment: () -> Int = { 0 },
                       widthAlignment: () -> Int = parent.widthAlignment, heightAlignment: () -> Int = parent.heightAlignment,
                       initializerList: GUIMouseOverArea.() -> Unit = {},
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