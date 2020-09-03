package screen.attribute

import screen.gui.GuiElement
import screen.gui.GuiOpenListener
import screen.gui.Placement
import screen.mouse.Mouse

class AttributeOpenAtMouse(element: GuiElement) : Attribute(element) {
    init {
        element.eventListeners.add(GuiOpenListener {
            gui.parentElement.placement = Placement.Exact(Mouse.xPixel, Mouse.yPixel - heightPixels)
            gui.layout.recalculateExactPlacement(gui.parentElement)
        })
    }
}