package screen

import io.Control
import io.ControlPress
import io.ControlPressHandler
import io.InputManager

object ScreenManager : ControlPressHandler {

    init {
        InputManager.registerControlPressHandler(this, Control.INTERACT)
    }

    val guiElements = mutableListOf<GUIElement>()

    fun update() {
        guiElements.forEach { it.update() }
    }

    fun render() {
        RootGUIElementObject.render()
    }

    override fun handleControlPress(p: ControlPress) {
        guiElements.stream().filter { it.open }
    }
}