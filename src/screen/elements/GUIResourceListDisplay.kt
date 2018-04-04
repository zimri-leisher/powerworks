package screen.elements

import resource.ResourceList

class GUIResourceListDisplay(parent: RootGUIElement, name: String, var list: ResourceList,
                             xAlignment: () -> Int, yAlignment: () -> Int, val width: Int, val height: Int,
                             open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, {width * 16 + 4}, {height * 16 + 4}, open, layer) {
    init {
        for(x in 0 until width) {
            for(y in 0 until height) {
            }
        }
    }
}