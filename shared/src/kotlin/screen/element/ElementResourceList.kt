package screen.element

import graphics.Renderer
import resource.ResourceList
import screen.gui.GuiElement

class ElementResourceList(parent: GuiElement, resources: ResourceList, width: Int, height: Int, val displayQuantity: Boolean = true) :
        ElementIconList(parent,
                width, height,
                renderIcon =
                { x, y, index -> (this as ElementResourceList).renderIcon(x, y, index) }
        ) {
    var resources = resources
        set(value) {
            if (field != value) {
                field = value

            }
        }

    init {
        getToolTip = { index ->
            if (index < this.resources.size) {
                val entry = this.resources[index]
                "${entry.key.name} * ${entry.value}"
            } else {
                null
            }
        }
    }

    private fun renderIcon(x: Int, y: Int, index: Int) {
        val entry = resources[index]
        entry.key.icon.render(x, y, iconSize, iconSize, true)
        if (displayQuantity) {
            Renderer.renderText(entry.value, x, y)
        }
    }
}