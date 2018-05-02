package screen

import io.*
import screen.elements.AutoFormatGUIGroup
import screen.elements.GUIText
import screen.elements.GUIWindow

/**
 * Contains debugging information.
 * Easily allows for addition of new information - see the setInfo method
 */
object DebugOverlay : GUIWindow("Debug overlay", 0, 0, 0, 0, windowGroup = ScreenManager.Groups.DEBUG_OVERLAY), ControlPressHandler {

    private val group: AutoFormatGUIGroup

    init {
        transparentToInteraction = true
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.DEBUG)
        group = AutoFormatGUIGroup(rootChild, "Debug overlay text auto format group", 0, 0, initializerList = {
            GUIText(this, "Debug overlay name text", 0, 0, "Debug information:", color = 0x45f442)
        }, yPixelSeparation = 4, accountForChildHeight = true)
    }

    /**
     * If this has not already been set before, it will create a new section for it. Otherwise, it updates the previous section
     * @param the name of the info
     * @param value the value to be displayed
     */
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