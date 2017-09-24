package screen

import graphics.Images
import main.Game

object TestGUI : GUI("Testing GUI", 0, 0, Game.WIDTH, Game.HEIGHT) {

    init {
        GUITexturePane(this, "Test GUI background", 0, 0, Images.MAIN_MENU_BACKGROUND, Game.WIDTH, Game.HEIGHT).run {
            GUIButton(this, "Test GUI back button", 1, 1, "Back to Main Menu", {
                this@TestGUI.open = false
                MainMenuGUI.open = true
            }, {})
            GUIElementList(this, "Test GUI element list", 0, 0, Game.WIDTH, Game.HEIGHT).run {
            }
        }
    }
}