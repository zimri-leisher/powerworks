package screen.attribute

import screen.gui.GuiCloseListener
import screen.gui.GuiElement
import screen.gui.GuiIngame
import screen.gui.GuiOpenListener

class AttributeOpenWithBrainInventory(element: GuiElement) : Attribute(element) {
    init {
        element.eventListeners.add(GuiOpenListener {
            GuiIngame.brainRobotGui.open = true
        })
        element.eventListeners.add(GuiCloseListener {
            if (gui.layer.guis.filter { it.open && it != GuiIngame.brainRobotGui && it != this.gui }.isEmpty()) {
                GuiIngame.brainRobotGui.open = false
            }
        })
    }
}