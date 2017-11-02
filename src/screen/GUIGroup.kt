package screen

import java.awt.Rectangle

open class GUIGroup(parent: RootGUIElement,
                    name: String,
                    relXPixel: Int, relYPixel: Int,
                    open: Boolean = false,
                    layer: Int = parent.layer + 1) :
        GUIElement(parent, name, relXPixel, relYPixel, 0, 0, open, layer) {

    override fun onAddChild(child: GUIElement) {
        updateDimensions()
    }

    init {
        updateDimensions()
    }

    private fun updateDimensions() {
        val r = Rectangle()
        children.forEach { r.add(Rectangle(it.relXPixel, it.relYPixel, it.widthPixels, it.heightPixels)) }
        widthPixels = r.width
        heightPixels = r.height
    }
}