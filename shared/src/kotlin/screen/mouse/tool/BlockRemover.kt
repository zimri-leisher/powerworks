package screen.mouse.tool

import io.Control
import io.ControlEvent
import io.ControlEventType
import level.LevelManager
import level.block.Block
import network.BlockReference
import player.PlayerManager
import player.ActionLevelObjectRemove

object BlockRemover : Tool(Control.REMOVE_BLOCK) {

    init {
        activationPredicate = {
            LevelManager.levelObjectUnderMouse != null && LevelManager.levelObjectUnderMouse is Block
        }
    }

    override fun onUse(event: ControlEvent, mouseLevelXPixel: Int, mouseLevelYPixel: Int): Boolean {
        if (event.type == ControlEventType.PRESS) {
            if (event.control == Control.REMOVE_BLOCK) {
                val toRemove = if (Selector.currentSelected.isNotEmpty())
                    Selector.currentSelected.filterIsInstance<Block>()
                else if (LevelManager.levelObjectUnderMouse is Block)
                    listOf(LevelManager.levelObjectUnderMouse as Block)
                else
                    listOf()
                if (toRemove.isNotEmpty()) {
                    toRemove.forEach { Selector.currentSelected.remove(it) }
                    PlayerManager.takeAction(ActionLevelObjectRemove(PlayerManager.localPlayer, toRemove.map { BlockReference(it) }))
                }
                return true
            }
        }
        return false
    }
}