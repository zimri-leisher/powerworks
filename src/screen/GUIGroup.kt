package screen

import java.awt.Rectangle

open class GUIGroup(parent: RootGUIElement,
                    name: String,
                    xAlignment: () -> Int, yAlignment: () -> Int,
                    open: Boolean = false,
                    layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { 0 }, { 0 }, open, layer) {

    override fun onAddChild(child: GUIElement) {
        updateDimensions()
    }

    init {
        updateDimensions()
    }

    private fun updateDimensions() {
        val r = Rectangle()
        children.forEach { r.add(Rectangle(it.xAlignment(), it.yAlignment(), it.widthPixels, it.heightPixels)) }
        widthPixels = r.width
        heightPixels = r.height
    }
}