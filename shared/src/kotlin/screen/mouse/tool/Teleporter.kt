package screen.mouse.tool

import io.Control
import io.PressType
import level.LevelManager
import level.moving.MovingObject

object Teleporter : Tool(Control.TELEPORT) {

    override fun onUse(control: Control, type: PressType, mouseLevelXPixel: Int, mouseLevelYPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            (LevelManager.levelViewUnderMouse?.camera as? MovingObject)?.setPosition(LevelManager.mouseLevelXPixel, LevelManager.mouseLevelYPixel)
        }
    }

    override fun updateCurrentlyActive() {
        currentlyActive = true
    }

}