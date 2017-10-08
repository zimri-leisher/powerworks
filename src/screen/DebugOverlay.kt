package screen

import io.*
import main.Game

object DebugOverlay : GUI("Debug overlay", 0, 0, Game.WIDTH, Game.HEIGHT, Integer.MAX_VALUE), ControlPressHandler {

    val group: AutoFormatGUIGroup

    init {
        InputManager.registerControlPressHandler(this, Control.DEBUG)
        group = AutoFormatGUIGroup(this, "Debug overlay text auto format group", 0, 0, yPixelSeparation = 4)
        group.nextYPixel = 4
            GUIText(group, "Debug overlay name text", 0, 0, "Debug information:", color = 0x45f442)
    }

    fun setInfo(key: String, value: String) {
        val c = group.get(key + " text")
        if (c == null) {
            GUIText(group, key + " text", 0, 0, key + ": " + value)
        } else {
            c as GUIText
            c.text = key + ": " + value
        }
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.control == Control.DEBUG && p.pressType == PressType.PRESSED) {
            open = !open
        }
    }
}