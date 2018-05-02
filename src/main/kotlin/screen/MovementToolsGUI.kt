package screen

import graphics.Font
import graphics.Image
import graphics.Utils
import io.*
import level.moving.MovingObject
import main.Game
import screen.elements.*

/**
 * A bunch of tools for moving where you want
 */
object MovementToolsGUI : GUIWindow("Player movement tools", { Game.WIDTH - 80 }, { GUICloseButton.HEIGHT }, { 80 }, { 40 }, windowGroup = ScreenManager.Groups.PLAYER_UTIL), ControlPressHandler {

    var teleporter = false
        set(value) {
            field = value
            if (field)
                Mouse.setSecondaryIcon(Image.Misc.TELEPORT_ICON)
            else
                Mouse.clearSecondaryIcon()
        }

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.TOGGLE_MOVEMENT_TOOLS)
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_ANY, Control.INTERACT)
        GUITexturePane(this.rootChild, name + " background", 0, 0, Image(Utils.genRectangle(widthPixels, heightPixels))).run {
            val teleporterBounds = Font.getStringBounds("Teleporter", GUIButton.TEXT_SIZE)
            GUIButton(this, this@MovementToolsGUI.name + " teleporter button", 2, 2, "Teleporter", teleporterBounds.width + 2, teleporterBounds.height + 2, {
                teleporter = !teleporter
            })
            val runSpeedPrompt = GUIText(this, this@MovementToolsGUI.name + " run speed prompt", 2, teleporterBounds.height + 4, "Movement speed:")
            val inputBounds = Font.getStringBounds("1 ")
            GUITextInputField(this, this@MovementToolsGUI.name + " run speed input", { runSpeedPrompt.widthPixels + 4 }, { teleporterBounds.height + 4 }, { inputBounds.width + 2 }, { inputBounds.height + 1 },
                    "1", { char -> char.isDigit()}, onPressEnter = {
                text -> selected = false; cursorIndex = -1; IngameGUI.views.forEach { it.CAMERA_SPEED = text.toInt() }
            })
        }
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.pressType == PressType.PRESSED) {
            if (p.control == Control.TOGGLE_MOVEMENT_TOOLS) {
                open = !open
            } else if (p.control == Control.INTERACT && Game.currentLevel.selectedLevelObject == null && teleporter) {
                (Game.currentLevel.viewBeingInteractedWith?.camera as? MovingObject)?.setPosition(Game.currentLevel.mouseLevelXPixel, Game.currentLevel.mouseLevelYPixel)
            }
        }
    }
}