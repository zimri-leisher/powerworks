package screen

import graphics.Image
import main.Game

object TestGUI : GUI("Testing GUI", 0, 0, Game.WIDTH, Game.HEIGHT) {

    init {
        GUITexturePane(this, "Test GUI background", 0, 0, Image.GUI.MAIN_MENU_BACKGROUND, Game.WIDTH, Game.HEIGHT).run {
            GUIButton(this, "Test GUI back button", 1, 1, "Back to Main Menu", {
                this@TestGUI.open = false
                MainMenuGUI.open = true
            }, {})
            GUIButton(this, "Test GUI test button", 1, 1 + GUIButton.DEFAULT_HEIGHT, "Add a component", {
                this@TestGUI.get("Test GUI element list")?.children?.add(GUITexturePane(null, "Test GUI texture pane", 0, 0, Image.ERROR))
            }, {})
            GUIElementList(this, "Test GUI element list", 3 + GUIButton.DEFAULT_WIDTH, 1, Game.WIDTH - (3 + GUIButton.DEFAULT_WIDTH), Game.HEIGHT - 1)
        }
    }
}