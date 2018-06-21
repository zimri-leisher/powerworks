package screen

import graphics.Image
import main.Game
import screen.animations.VelocityAnimation
import screen.elements.*

object MainMenuGUI : GUIWindow("Main menu", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, ScreenManager.Groups.BACKGROUND, true, 0) {

    internal var logo: GUITexturePane

    init {
        GUITexturePane(this, "Main menu background",
                { 0 }, { 0 },
                Image.GUI.MAIN_MENU_BACKGROUND,
                { Math.max(Game.WIDTH, Image.GUI.MAIN_MENU_BACKGROUND.widthPixels) }, { Math.max(Game.HEIGHT, Image.GUI.MAIN_MENU_BACKGROUND.heightPixels) }).run {
        }

        logo = GUITexturePane(this, "Main menu logo",
                { (Game.WIDTH - Image.GUI.MAIN_MENU_LOGO.widthPixels) / 2 - 1 }, { (Game.HEIGHT - Image.GUI.MAIN_MENU_LOGO.heightPixels) / 2 - 50 },
                Image.GUI.MAIN_MENU_LOGO,
                layer = 7).apply {

            GUITexturePane(this, "Main menu button box",
                    { (Image.GUI.MAIN_MENU_LOGO.widthPixels - Image.GUI.MAIN_MENU_BUTTON_BOX.widthPixels) / 2 }, { Image.GUI.MAIN_MENU_LOGO.heightPixels },
                    Image.GUI.MAIN_MENU_BUTTON_BOX).run {

                AutoFormatGUIGroup(this, "Main menu button box button group",
                        { (Image.GUI.MAIN_MENU_BUTTON_BOX.widthPixels - GUIButton.WIDTH) / 2 }, { (Image.GUI.MAIN_MENU_BUTTON_BOX.heightPixels - GUIButton.HEIGHT) / 2 - GUIButton.HEIGHT },
                        initializerList = {

                            GUIButton(this, "Main menu play button", 0, 0, "Play", onRelease = {
                                this@MainMenuGUI.open = false
                                LevelSelectorGUI.open = true
                            }, layer = this.layer + 2)

                            GUIButton(this, "Main menu test button", 0, 0, "Test", onRelease = {
                                this@MainMenuGUI.open = false
                                TestGUI.open = true
                            }, layer = this.layer + 2)
                        },
                        yPixelSeparation = 2,
                        accountForChildHeight = true)
            }
        }
        open = true
    }
}