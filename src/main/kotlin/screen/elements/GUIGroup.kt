package screen.elements

import java.awt.Rectangle

open class GUIGroup(parent: RootGUIElement,
                    name: String,
                    xAlignment: Alignment, yAlignment: Alignment,
                    initializerList: GUIGroup.() -> Unit = {},
                    open: Boolean = false,
                    layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { 0 }, { 0 }, open, layer) {

    var print = true

    init {
        initializerList()
    }

    override fun onAddChild(child: GUIElement) {
        updateDimensions()
    }

    override fun onRemoveChild(child: GUIElement) {
        updateDimensions()
    }

    override fun onChildDimensionChange(child: GUIElement) {
        updateDimensions()
    }

    fun updateDimensions() {
        val r = Rectangle()
        children.forEach { r.add(Rectangle(it.alignments.x(), it.alignments.y(), it.alignments.width(), it.alignments.height())) }
        val width = r.width
        val height = r.height
        alignments.width = { width }
        alignments.height = { height }
    }
}