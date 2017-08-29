package screen

import java.awt.Rectangle

open class GUIGroup(parent: RootGUIElement? = RootGUIElementObject,
               name: String,
               relXPixel: Int, relYPixel: Int,
               layer: Int = (parent?.layer ?: 0) + 1) :
        GUIElement(parent, name, relXPixel, relYPixel, 0, 0, layer) {

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