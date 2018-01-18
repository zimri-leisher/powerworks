package screen

import graphics.Image
import graphics.Utils
import io.*
import main.Game
import screen.elements.GUICloseButton
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

object MovementToolsGUI : GUIWindow("Player movement tools", { Game.WIDTH - 40 }, { GUICloseButton.HEIGHT }, {40}, {20}, windowGroup = ScreenManager.Groups.PLAYER_UTIL), ControlPressHandler {
    val background = GUITexturePane(this.rootChild, name + " background", 0, 0, Image(Utils.genRectangle(widthPixels, heightPixels)))

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.TOGGLE_MOVEMENT_TOOLS)
    }

    override fun handleControlPress(p: ControlPress) {
        if(p.pressType == PressType.PRESSED && p.control == Control.TOGGLE_MOVEMENT_TOOLS) {
            open = !open
        }
    }
}