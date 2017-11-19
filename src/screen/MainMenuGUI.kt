package screen

import graphics.Image
import main.Game
import main.State

object MainMenuGUI : GUIWindow("Main menu", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, true, 0, ScreenManager.Groups.BACKGROUND) {

    init {
        adjustDimensions = true
        //GUITexturePane(rootChild, "Main menu background filler", { 0 }, { 0 }, Image.GUI.MAIN_MENU_BACKGROUND_FILLER, { Game.WIDTH }, { Game.HEIGHT})
        GUITexturePane(rootChild, "Main menu background",
                { 0 }, { 0 },
                Image.GUI.MAIN_MENU_BACKGROUND,
                { Math.max(Game.WIDTH, Image.GUI.MAIN_MENU_BACKGROUND.widthPixels)}, { Math.max(Game.HEIGHT, Image.GUI.MAIN_MENU_BACKGROUND.heightPixels) })
        GUITexturePane(this.rootChild, "Main menu logo",
                { (Game.WIDTH - Image.GUI.MAIN_MENU_LOGO.widthPixels) / 2 - 1 }, { (Game.HEIGHT - Image.GUI.MAIN_MENU_LOGO.heightPixels) / 2 - 50 },
                Image.GUI.MAIN_MENU_LOGO,
                layer = 7).run {
            GUITexturePane(this, "Main menu button box",
                    { (Image.GUI.MAIN_MENU_LOGO.widthPixels - Image.GUI.MAIN_MENU_BUTTON_BOX.widthPixels) / 2 }, { Image.GUI.MAIN_MENU_LOGO.heightPixels },
                    Image.GUI.MAIN_MENU_BUTTON_BOX).run {
                AutoFormatGUIGroup(this, "Main menu button box button group",
                        { (Image.GUI.MAIN_MENU_BUTTON_BOX.widthPixels - GUIButton.DEFAULT_WIDTH) / 2 }, { (Image.GUI.MAIN_MENU_BUTTON_BOX.heightPixels - GUIButton.DEFAULT_HEIGHT) / 2 - GUIButton.DEFAULT_HEIGHT },
                        yPixelSeparation = 2,
                        accountForChildHeight = true).run {
                    GUIButton(this, "Main menu play button", 0, 0, "Play", {
                        State.setState(State.INGAME)
                    }, {
                    }, layer = this.layer + 2)
                    GUIButton(this, "Main menu test button", 0, 0, "Test", {
                        this@MainMenuGUI.open = false
                        TestGUI.open = true
                    }, {
                    }, layer = this.layer + 2)
                }
            }
        }
        open = true
    }
}