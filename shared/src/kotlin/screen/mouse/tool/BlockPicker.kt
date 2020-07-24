package screen.mouse.tool

import io.Control
import io.PressType
import level.LevelManager
import level.block.Block
import player.PlayerManager
import screen.mouse.Mouse

object BlockPicker : Tool(Control.PICK_BLOCK) {

    override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (currentlyActive && type == PressType.PRESSED) {
            val selectedBlock = LevelManager.levelObjectUnderMouse!!
            if (selectedBlock.type.itemForm != null && PlayerManager.localPlayer.brainRobot.inventory.contains(selectedBlock.type.itemForm!!)) {
                Mouse.heldItemType = selectedBlock.type.itemForm
            }
        }
    }

    override fun updateCurrentlyActive() {
        currentlyActive = LevelManager.levelObjectUnderMouse is Block
    }
}