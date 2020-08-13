package screen.mouse.tool

import io.Control
import io.PressType
import level.LevelManager
import level.moving.MovingObject

object Teleporter : Tool(Control.TELEPORT) {

    override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int): Boolean {
        if (type == PressType.PRESSED) {
            (LevelManager.levelViewUnderMouse?.camera as? MovingObject)?.setPosition(LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel)
            return true
        }
        return false
    }
}