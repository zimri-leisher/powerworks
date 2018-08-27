package screen

import graphics.Image
import graphics.text.TextManager
import io.*
import main.Game
import screen.elements.*
import screen.mouse.Mouse

/**
 * Some tools for moving where you want
 */
object MovementToolsGUI : GUIWindow("Player movement tools", { Game.WIDTH - 80 }, { GUICloseButton.HEIGHT }, { 80 }, { 40 }, ScreenManager.Groups.PLAYER_UTIL), ControlPressHandler {

    var teleporter = false
        set(value) {
            field = value
            if (field)
                Mouse.setSecondaryIcon(Image.Misc.TELEPORT_ICON)
            else
                Mouse.clearSecondaryIcon()
        }

    init {
        partOfLevel = true
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.TOGGLE_MOVEMENT_TOOLS)
        GUIDefaultTextureRectangle(this, name + " background", 0, 0).run {
            val runSpeedPrompt = GUIText(this, this@MovementToolsGUI.name + " run speed prompt", 2, 4, "Movement speed:")
            GUITextInputField(this, this@MovementToolsGUI.name + " run speed input", { runSpeedPrompt.widthPixels + 4 }, { 4 }, 3, 1,
                    "1", charRule = { char -> char.isDigit() }, onPressEnter = { text ->
                this.selected = false
                IngameGUI.views.forEach { it.CAMERA_SPEED = text.toInt() }
            })
        }
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.pressType == PressType.PRESSED) {
            if (p.control == Control.TOGGLE_MOVEMENT_TOOLS) {
                open = !open
            }
        }
    }
}