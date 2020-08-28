package screen.gui2

import graphics.Renderer
import resource.ResourceList

class ElementResourceList(parent: GuiElement, var resources: ResourceList, width: Int, height: Int, val displayQuantity: Boolean = false) :
        ElementIconList(parent,
                width, height,
                renderIcon =
                { xPixel, yPixel, index -> (this as ElementResourceList).renderIcon(xPixel, yPixel, index) }
        ) {
    init {
        getToolTip = { index ->
            if (index != -1 && resources[index] != null)
                "${resources[index]!!.key.name} * ${resources[index]!!.value}"
            else null
        }
    }

    private fun renderIcon(xPixel: Int, yPixel: Int, index: Int) {
        val entry = resources[index]
        if (entry != null) {
            entry.key.icon.render(xPixel, yPixel, iconSize, iconSize, true)
            if (displayQuantity) {
                Renderer.renderText(entry.value, xPixel, yPixel)
            }
        }
    }
}