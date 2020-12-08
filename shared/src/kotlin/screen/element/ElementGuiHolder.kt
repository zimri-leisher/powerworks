package screen.element

import screen.gui.*

class ElementGuiHolder(parent: GuiElement, val guiToHold: Gui) : GuiElement(parent) {
    init {
        dimensions = Dimensions.Dynamic({ guiToHold.dimensions.width }, { guiToHold.dimensions.height })
        guiToHold.parentElement.eventListeners.add(GuiChangeDimensionListener {
            gui.layout.recalculateExactDimensions(this@ElementGuiHolder)
        })
    }

    override fun onChangePlacement() {
        guiToHold.parentElement.placement = Placement.Exact(absoluteX, absoluteY)
        guiToHold.layout.recalculateExactPlacement(guiToHold.parentElement)
        super.onChangePlacement()
    }
}