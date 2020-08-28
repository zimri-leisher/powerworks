package screen

import graphics.Animation
import graphics.Image
import main.Game
import main.heightPixels
import main.widthPixels
import screen.elements.GUIElement
import screen.elements.GUIText
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

object MainMenuGUI : GUIWindow("Main menu", { 0 }, { 0 }, { Game.WIDTH }, { Game.HEIGHT }, true, 0) {

    internal var logo: GUITexturePane

    init {

        GUITexturePane(this, "Main menu background",
                { 0 }, { 0 },
                Image.GUI.MAIN_MENU_BACKGROUND,
                { Math.max(Game.WIDTH, Image.GUI.MAIN_MENU_BACKGROUND.widthPixels) }, { Math.max(Game.HEIGHT, Image.GUI.MAIN_MENU_BACKGROUND.heightPixels) }).run {
        }

        logo = GUITexturePane(this, "Main menu logo",
                { (Game.WIDTH - Image.GUI.MAIN_MENU_LOGO.widthPixels) / 2 + 1 },
                { (Game.HEIGHT - Image.GUI.MAIN_MENU_LOGO.heightPixels) / 2 + 50 },
                Image.GUI.MAIN_MENU_LOGO,
                layer = 7)

        object : GUIElement(logo, "Main menu play button", { (logo.widthPixels - Animation.MAIN_MENU_PLAY_BUTTON.frames[0].widthPixels) / 2 }, { -Animation.MAIN_MENU_PLAY_BUTTON.frames[0].heightPixels }, { Animation.MAIN_MENU_PLAY_BUTTON.frames[0].widthPixels }, { Animation.MAIN_MENU_PLAY_BUTTON.frames[0].heightPixels }) {

            init {
                GUITexturePane(this, "Main menu play button animation", 0, 0, Animation.MAIN_MENU_PLAY_BUTTON).apply {
                    transparentToInteraction = true
                }
                GUIText(this, "Main menu play button text", widthPixels - 32, heightPixels - 12, "<size=30>Play", allowTags = true).apply {
                    transparentToInteraction = true
                }
            }

            override fun onMouseEnter() {
                Animation.TEST_ANIM.play()
                Animation.MAIN_MENU_PLAY_BUTTON.play()
            }

            override fun onMouseLeave() {
                Animation.TEST_ANIM.playBackwards()
                with(Animation.MAIN_MENU_PLAY_BUTTON) {
                    if (currentStepID == "inside_loop") {
                        playBackwardsFrom("outside_loop")
                    } else {
                        playBackwards()
                    }
                }
            }
        }
    }
}