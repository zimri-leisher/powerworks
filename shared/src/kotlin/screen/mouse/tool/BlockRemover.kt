package screen.mouse.tool

import io.Control
import io.PressType
import level.LevelManager
import level.block.Block
import network.BlockReference
import player.PlayerManager
import player.RemoveLevelObjectAction

object BlockRemover : Tool(Control.REMOVE_BLOCK) {
    override fun updateCurrentlyActive() {
        currentlyActive = LevelManager.levelObjectUnderMouse != null && LevelManager.levelObjectUnderMouse is Block
    }

    override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            if (control == Control.REMOVE_BLOCK) {
                val toRemove = if (Selector.currentSelected.isNotEmpty())
                    Selector.currentSelected.filterIsInstance<Block>()
                else
                    listOf(LevelManager.levelObjectUnderMouse!!)
                if (toRemove.isNotEmpty()) {
                    PlayerManager.takeAction(RemoveLevelObjectAction(PlayerManager.localPlayer, toRemove.map { BlockReference(it as Block) }))
                }
            }
        }
    }
}