package screen.elements

class GUIMouseOverRegion(parent: RootGUIElement, name: String,
                         xAlignment: Alignment, yAlignment: Alignment, widthAlignment: Alignment, heightAlignment: Alignment,
                         val onEnter: () -> Unit = {},
                         val onLeave: () -> Unit = {},
                         open: Boolean = false,
                         layer: Int = parent.layer + 1) : GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer) {
    override fun onMouseEnter() {
        onEnter()
    }
    override fun onMouseLeave() {
        onLeave()
    }
}