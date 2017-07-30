package screen

import graphics.Images
import main.Game

object MainMenuGUI : GUI("Main menu", 0, 0, Game.WIDTH, Game.HEIGHT) {
    override fun init() {
        GUITexturePane(this, "Main menu background", 0, 0, Images.MAIN_MENU_BACKGROUND, Game.WIDTH, Game.HEIGHT).run {
            GUITexturePane(this, "Main menu logo", (Game.WIDTH - Images.MAIN_MENU_LOGO.widthPixels) / 2 - 1, (Game.HEIGHT - Images.MAIN_MENU_LOGO.heightPixels) / 10, Images.MAIN_MENU_LOGO, layer = 4)
            GUITexturePane(this, "Main menu button box", (Game.WIDTH - Images.MAIN_MENU_BUTTON_BOX.widthPixels) / 2, (Game.HEIGHT - Images.MAIN_MENU_BUTTON_BOX.heightPixels) / 2, Images.MAIN_MENU_BUTTON_BOX).run {
                GUIGroup(this, "Main menu button box button group", 0, 0).run {
                    GUIButton(this, "Main menu play button", (Images.MAIN_MENU_BUTTON_BOX.widthPixels - GUI_BUTTON_DEFAULT_WIDTH) / 2, (Images.MAIN_MENU_BUTTON_BOX.heightPixels - GUI_BUTTON_DEFAULT_HEIGHT) / 2, "Play", {
                        println("press")
                    }, {
                        println("release")
                    })
                }
            }
        }
    }
}