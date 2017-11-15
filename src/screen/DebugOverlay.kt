package screen

import io.*

object DebugOverlay : GUIWindow("Debug overlay", 0, 0, 0, 0, windowGroup = ScreenManager.Groups.DEBUG_OVERLAY), ControlPressHandler {

    val group: AutoFormatGUIGroup

    init {
        transparentToInteraction = true
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.DEBUG)
        group = AutoFormatGUIGroup(rootChild, "Debug overlay text auto format group", 0, 0, yPixelSeparation = 4, accountForChildHeight = true)
        group.nextYPixel = 4
        GUIText(group, "Debug overlay name text", 0, 0, "Debug information:", color = 0x45f442)
    }

    fun setInfo(key: String, value: String) {
        val c = group.getChild(key + " text")
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