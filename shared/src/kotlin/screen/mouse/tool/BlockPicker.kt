package screen.mouse.tool

import io.Control
import io.ControlEvent
import io.ControlEventType
import level.LevelManager
import level.block.Block
import player.PlayerManager
import screen.gui.GuiIngame
import screen.mouse.Mouse

object BlockPicker : Tool(Control.PICK_BLOCK) {

    init {
        activationPredicate = {
            LevelManager.levelObjectUnderMouse is Block
        }
    }

    override fun onUse(event: ControlEvent, mouseLevelX: Int, mouseLevelY: Int): Boolean {
        if (event.type == ControlEventType.PRESS) {
            val selectedBlock = LevelManager.levelObjectUnderMouse!! as? Block
            if (selectedBlock?.type?.itemForm != null && PlayerManager.localPlayer.brainRobot.inventory.canRemove(
                    selectedBlock.type.itemForm
                )
            ) {
                Mouse.heldItemType = selectedBlock.type.itemForm
                GuiIngame.Hotbar.selectedSlotIndex = GuiIngame.Hotbar.slots.indexOf(Mouse.heldItemType)
            }
        }
        return true
    }
}