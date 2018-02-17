package screen

import graphics.Image
import main.Game
import screen.elements.GUIButton
import screen.elements.GUIElementList
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

object TestGUI : GUIWindow("Testing GUI", 0, 0, Game.WIDTH, Game.HEIGHT, windowGroup = ScreenManager.Groups.BACKGROUND) {

    init {
        GUITexturePane(rootChild, "Test GUI background", 0, 0, Image.GUI.MAIN_MENU_BACKGROUND, Game.WIDTH, Game.HEIGHT).run {
            GUIButton(this, "Test GUI back button", 1, 1, "Back to Main Menu", {
                this@TestGUI.open = false
                MainMenuGUI.open = true
            }, {})
            GUIButton(this, "Test GUI test button", 1, 1 + GUIButton.HEIGHT, "Add a component", {
                this@TestGUI.getChild("Test GUI element list")?.children?.add(
                        GUITexturePane(this, "Test GUI texture pane", 0, 0, Image.Misc.ERROR)
                )
            }, {})
            GUIElementList(this, "Test GUI element list", 3 + GUIButton.WIDTH, 1, Game.WIDTH - (3 + GUIButton.WIDTH), Game.HEIGHT - 1)
        }
    }
}