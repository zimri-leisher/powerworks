package screen.element

import io.ControlEventType
import item.Inventory
import item.ItemType
import screen.ScreenLayer
import screen.gui.*
import screen.mouse.Mouse

open class ElementInventory(parent: GuiElement, inventory: Inventory) : ElementResourceContainer(parent, inventory.width, inventory.height, inventory) {

    init {
        allowSelection = true
        onSelectIcon = { index, interaction ->
            if(index < currentResources.size) {
                val (type, quantity) = currentResources[index]
                if (interaction.event.type == ControlEventType.PRESS) {
                    if (interaction.shift) {
                        val other = getSecondaryInventory()
                        if (other != null) {
                            other.add(type, quantity)
                            inventory.remove(type, quantity)
                        } else {
                            GuiIngame.Hotbar.addItemType(type as ItemType)
                        }
                    } else {
                        Mouse.heldItemType = type as ItemType?
                    }
                }
            }
        }
    }

    private fun getSecondaryInventory(): Inventory? {
        for(gui in ScreenLayer.WINDOWS.guis.elements) {
            if(gui.open && gui != this.gui) {
                if(gui is GuiChestBlock) {
                    return gui.block.inventory
                } else if(gui is GuiBrainRobot) {
                    return gui.brainRobot.inventory
                }
            }
        }
        return null
    }
}