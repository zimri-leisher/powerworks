package screen

import graphics.Images
import main.Game
import main.State

object MainMenuGUI : GUI("Main menu", 0, 0, Game.WIDTH, Game.HEIGHT) {

    init {
        GUITexturePane(this, "Main menu background", 0, 0, Images.MAIN_MENU_BACKGROUND, Game.WIDTH, Game.HEIGHT).run {
            GUITexturePane(this, "Main menu logo", (Game.WIDTH - Images.MAIN_MENU_LOGO.widthPixels) / 2 - 1, (Game.HEIGHT - Images.MAIN_MENU_LOGO.heightPixels) / 10, Images.MAIN_MENU_LOGO, layer = 6)
            GUITexturePane(this, "Main menu button box", (Game.WIDTH - Images.MAIN_MENU_BUTTON_BOX.widthPixels) / 2, (Game.HEIGHT - Images.MAIN_MENU_BUTTON_BOX.heightPixels) / 2, Images.MAIN_MENU_BUTTON_BOX).run {
                AutoFormatGUIGroup(this, "Main menu button box button group", (Images.MAIN_MENU_BUTTON_BOX.widthPixels - GUIButton.DEFAULT_WIDTH) / 2, (Images.MAIN_MENU_BUTTON_BOX.heightPixels - GUIButton.DEFAULT_HEIGHT) / 2 - GUIButton.DEFAULT_HEIGHT, yPixelSeparation = 2).run {
                    GUIButton(this, "Main menu play button", 0, 0, "Play", {
                        State.setState(State.INGAME)
                    }, {

                    }, this.layer + 2)
                    GUIButton(this, "Main menu test button", 0, 0, "Test", {
                        this@MainMenuGUI.open = false
                        TestGUI.open = true
                    }, {
                    }, this.layer + 2)
                }
                GUIDragGrip(this, "Main menu button box drag grip", Images.MAIN_MENU_BUTTON_BOX.widthPixels - GUIDragGrip.WIDTH - 1, Images.MAIN_MENU_BUTTON_BOX.heightPixels - GUIDragGrip.HEIGHT - 1)
            }
        }
    }
}