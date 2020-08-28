package screen.mouse.tool

import io.Control
import io.ControlEvent
import io.ControlEventType
import level.LevelManager
import level.moving.MovingObject

object Teleporter : Tool(Control.TELEPORT) {

    override fun onUse(event: ControlEvent, mouseLevelXPixel: Int, mouseLevelYPixel: Int): Boolean {
        if (event.type == ControlEventType.PRESS) {
            (LevelManager.levelViewUnderMouse?.camera as? MovingObject)?.setPosition(LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel)
            return true
        }
        return false
    }
}