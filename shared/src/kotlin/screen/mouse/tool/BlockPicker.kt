package screen.mouse.tool

import io.Control
import io.PressType
import level.LevelManager
import level.block.Block
import player.PlayerManager
import screen.HUD
import screen.mouse.Mouse

object BlockPicker : Tool(Control.PICK_BLOCK) {

    init {
        activationPredicate = {
            LevelManager.levelObjectUnderMouse is Block
        }
    }

    override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int): Boolean {
        if (type == PressType.PRESSED) {
            val selectedBlock = LevelManager.levelObjectUnderMouse!!
            if (selectedBlock.type.itemForm != null && PlayerManager.localPlayer.brainRobot.inventory.contains(selectedBlock.type.itemForm!!)) {
                Mouse.heldItemType = selectedBlock.type.itemForm
                HUD.Hotbar.selected = HUD.Hotbar.items.items.indexOf(Mouse.heldItemType)
            }
        }
        return true
    }
}