package screen.gui

import io.*
import main.GameState
import screen.ScreenLayer

object GuiEscapeMenu : Gui(ScreenLayer.MENU_4) {
    init {
        define {
            placement = Placement.Align.Center
            background {
                dimensions = dimensions.pad(8, 8)
                list(Placement.Align.Center) {
                    button("Settings", { GuiEscapeMenu.open = false; GuiSettings.open = true }) {
                        dimensions = Dimensions.Exact(48, 16)
                    }
                    button("Main menu") {
                        dimensions = Dimensions.Exact(48, 16)
                    }
                }
            }
        }
    }
}