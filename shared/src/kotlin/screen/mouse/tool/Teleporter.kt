package screen.mouse.tool

import io.Control
import io.ControlEvent
import io.ControlEventType
import level.LevelManager
import level.moving.MovingObject

object Teleporter : Tool(Control.TELEPORT) {

    override fun onUse(event: ControlEvent, mouseLevelX: Int, mouseLevelY: Int): Boolean {
        if (event.type == ControlEventType.PRESS) {
            (LevelManager.levelViewUnderMouse?.camera as? MovingObject)?.setPosition(LevelManager.mouseLevelX, LevelManager.mouseLevelY)
            return true
        }
        return false
    }
}