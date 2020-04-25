package screen.elements

import graphics.Renderer
import resource.ResourceList

class GUIResourceListDisplay(parent: RootGUIElement, name: String, var currentResources: ResourceList,
                             xAlignment: Alignment, yAlignment: Alignment, width: Int, height: Int,
                             val displayQuantity: Boolean = true,
                             open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIIconList(parent, name, xAlignment, yAlignment, width, height,
                renderIcon = { xPixel, yPixel, index -> (this as GUIResourceListDisplay).renderIcon(xPixel, yPixel, index) }, open = open, layer = layer) {

    init {
        getToolTip = { index ->
            if (index != -1 && currentResources[index] != null)
                "${currentResources[index]!!.key.name} * ${currentResources[index]!!.value}"
            else null
        }
    }

    private fun renderIcon(xPixel: Int, yPixel: Int, index: Int) {
        val entry = currentResources[index]
        if (entry != null) {
            entry.key.icon.render(xPixel, yPixel, iconSize, iconSize, true)
            if (displayQuantity) {
                Renderer.renderText(entry.value, xPixel, yPixel)
            }
        }
    }
}