package screen.gui

import io.*
import main.GameState
import screen.ScreenLayer

object GuiEscapeMenu : Gui(ScreenLayer.MENU_4), ControlEventHandler {
    init {
        InputManager.register(this, Control.TOGGLE_ESCAPE_MENU)
        define {
            placement = Placement.Align.Center
            background {
                dimensions = dimensions.pad(8, 8)
                list(Placement.Align.Center) {
                    button("Settings", { GuiEscapeMenu.open = false; GuiSettings.open = true }) {
                        dimensions = Dimensions.Exact(48, 16)
                    }
                    button("Quit to main menu") {
                        dimensions = Dimensions.Exact(48, 16)
                    }
                }
            }
        }
    }

    override fun handleControlEvent(event: ControlEvent) {
        if (event.control == Control.TOGGLE_ESCAPE_MENU && GameState.currentState == GameState.INGAME) {
            if (event.type == ControlEventType.PRESS) {
                open = !open
            }
        }
    }
}