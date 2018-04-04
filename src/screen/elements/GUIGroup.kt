package screen.elements

import java.awt.Rectangle

open class GUIGroup(parent: RootGUIElement,
                    name: String,
                    xAlignment: () -> Int, yAlignment: () -> Int,
                    initializerList: GUIGroup.() -> Unit = {},
                    open: Boolean = false,
                    layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { 0 }, { 0 }, open, layer) {

    var print = false

    init {
        initializerList()
    }

    override fun onAddChild(child: GUIElement) {
        updateDimensions()
    }

    override fun onRemoveChild(child: GUIElement) {
        updateDimensions()
    }

    fun updateDimensions() {
        val r = Rectangle()
        children.forEach { r.add(Rectangle(it.xAlignment(), it.yAlignment(), it.widthPixels, it.heightPixels)) }
        widthAlignment = { r.width }
        heightAlignment = { r.height }
    }
}