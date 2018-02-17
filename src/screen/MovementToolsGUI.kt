package screen

import graphics.Font
import graphics.Image
import graphics.Utils
import io.*
import level.moving.MovingObject
import main.Game
import screen.elements.GUIButton
import screen.elements.GUICloseButton
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

object MovementToolsGUI : GUIWindow("Player movement tools", { Game.WIDTH - 80 }, { GUICloseButton.HEIGHT }, { 80 }, { 40 }, windowGroup = ScreenManager.Groups.PLAYER_UTIL), ControlPressHandler {

    var teleporter = false
        set(value) {
            field = value
            if (field)
                Mouse.setSecondaryIcon(Image.Misc.TELEPORT_ICON)
            else
                Mouse.clearIcon()
        }

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.TOGGLE_MOVEMENT_TOOLS)
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_ANY, Control.INTERACT)
        GUITexturePane(this.rootChild, name + " background", 0, 0, Image(Utils.genRectangle(widthPixels, heightPixels))).run {
            val bounds = Font.getStringBounds("Teleporter", GUIButton.TEXT_SIZE)
            GUIButton(this, this@MovementToolsGUI.name + " teleporter button", 2, 2, bounds.width + 2, bounds.height + 2, "Teleporter", {
                teleporter = !teleporter
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