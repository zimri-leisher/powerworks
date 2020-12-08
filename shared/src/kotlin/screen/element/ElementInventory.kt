package screen.element

import io.Control
import io.ControlEventType
import item.Inventory
import item.ItemType
import player.ActionTransferResourcesBetweenLevelObjects
import player.PlayerManager
import resource.resourceListOf
import screen.ScreenManager
import screen.gui.GuiElement
import screen.gui.GuiIngame
import screen.mouse.Mouse

open class ElementInventory(parent: GuiElement, inventory: Inventory) : ElementResourceContainer(parent, inventory.width, inventory.height, inventory) {

    init {
        allowModification = true
        allowSelection = true
        onSelectIcon = { index, interaction ->
            if (index < currentResources.size) {
                val (type, quantity) = currentResources[index]
                if (interaction.event.type == ControlEventType.PRESS) {
                    if(interaction.event.control == Control.INTERACT) {
                        val other = ScreenManager.getSecondaryResourceContainer(container)
                        if (other?.attachedLevelObject != null && container.attachedLevelObject != null) {
                            PlayerManager.takeAction(ActionTransferResourcesBetweenLevelObjects(PlayerManager.localPlayer,
                                    container.attachedLevelObject!!.toReference(),
                                    container.id, other.attachedLevelObject!!.toReference(),
                                    other.id, resourceListOf(type to quantity)))
                        }
                    } else if(interaction.event.control == Control.SECONDARY_INTERACT) {
                        GuiIngame.Hotbar.addItemType(type as ItemType)
                    }
                }
            } else if (Mouse.heldItemType != null) {
                val quantity = PlayerManager.localPlayer.brainRobot.inventory.getQuantity(Mouse.heldItemType!!)
                if (quantity > 0) {
                    PlayerManager.takeAction(ActionTransferResourcesBetweenLevelObjects(PlayerManager.localPlayer,
                            PlayerManager.localPlayer.brainRobot.toReference(), PlayerManager.localPlayer.brainRobot.inventory.id,
                            container.attachedLevelObject!!.toReference(),
                            container.id, resourceListOf(Mouse.heldItemType!! to quantity)))
                }
            }
        }
    }
}