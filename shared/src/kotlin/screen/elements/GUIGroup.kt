package screen.elements

import com.badlogic.gdx.math.Rectangle

/**
 * A group of GUIElements. When a child is added, removed or has a dimension change, the dimensions of this get updated
 * to be the minimum-sized rectangle that can contain all children
 */
open class GUIGroup(parent: RootGUIElement,
                    name: String,
                    xAlignment: Alignment, yAlignment: Alignment,
                    open: Boolean = false,
                    layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { 0 }, { 0 }, open, layer) {

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
        children.forEach { it.alignments.updateDimension() }
        val r = Rectangle()
        for (child in children) {
            r.merge(Rectangle(child.alignments.x().toFloat(), child.alignments.y().toFloat(), child.widthPixels.toFloat(), child.heightPixels.toFloat()))
        }
        r.merge(0f, 0f)
        val thisWidth = r.width.toInt()
        val thisHeight = r.height.toInt()
        alignments.width = { thisWidth }
        alignments.height = { thisHeight }
    }
}