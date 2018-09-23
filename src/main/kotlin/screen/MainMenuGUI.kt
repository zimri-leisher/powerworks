package screen

import graphics.Animation
import graphics.Image
import io.PressType
import main.Game
import main.heightPixels
import main.widthPixels
import screen.animations.SlideOpenAnimation
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
                { (Game.WIDTH - Image.GUI.MAIN_MENU_LOGO.widthPixels) / 2 + 1 },
                { (Game.HEIGHT - Image.GUI.MAIN_MENU_LOGO.heightPixels) / 2 + 50 },
                Image.GUI.MAIN_MENU_LOGO,
                layer = 7)

        object : GUIElement(logo, "Main menu play button", { (logo.widthPixels - Animation.MAIN_MENU_PLAY_BUTTON.frames[0].widthPixels) / 2 }, { -Animation.MAIN_MENU_PLAY_BUTTON.frames[0].heightPixels }, { Animation.MAIN_MENU_PLAY_BUTTON.frames[0].widthPixels }, { Animation.MAIN_MENU_PLAY_BUTTON.frames[0].heightPixels }) {

            val texture = GUITexturePane(this, "Main menu play button texture", 0, 0, Animation.MAIN_MENU_PLAY_BUTTON[0]).apply {
                transparentToInteraction = true
            }

            init {
                GUIText(this, "Main menu play button text", widthPixels - 32, heightPixels - 12, "<size=30>Play", allowTags = true).apply {
                    transparentToInteraction = true
                }
            }

            override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
                if (type == PressType.RELEASED) {
                    LevelSelectorGUI.open = true
                    SlideOpenAnimation(LevelSelectorGUI, this@MainMenuGUI).playing = true
                }
            }

            override fun onMouseEnter() {
                Animation.MAIN_MENU_PLAY_BUTTON.play()
            }

            override fun onMouseLeave() {
                with(Animation.MAIN_MENU_PLAY_BUTTON) {
                    if (getCurrentStepID() == "inside_loop") {
                        playBackwardsFrom("outside_loop")
                    } else {
                        playBackwards()
                    }
                }
            }
        }

        /*
        object : GUIElement(this, "Custom play button", 30, 30, 64, 64) {

            var currentTicks = 0
            var lastFrame = 0
            var currentFrame = 0
            val frames = Animation.MAIN_MENU_PLAY_BUTTON.frames
            val frameTimes = arrayOf(5, 8, 14, 17, 15, 15)
            var playing = false
            var reversed = false

            override fun update() {
                if (playing) {
                    currentTicks++
                    if (currentTicks == frameTimes[currentFrame]) {
                        currentTicks = 0
                        if (!reversed) {
                            if (currentFrame == frameTimes.lastIndex) {
                                playing = false
                            } else {
                                lastFrame = currentFrame
                                currentFrame++
                            }
                        } else {
                            if (currentFrame == 0) {
                                playing = false
                            } else {
                                lastFrame = currentFrame
                                currentFrame--
                            }
                        }
                    }
                }
            }

            override fun onMouseEnter() {
                playing = true
                reversed = false
            }

            override fun onMouseLeave() {
                playing = true
                reversed = true
            }

            override fun render() {
                val progress = currentTicks.toFloat() / frameTimes[currentFrame]
                Renderer.renderTexture(frames[lastFrame], xPixel, yPixel)
                Renderer.renderTexture(frames[currentFrame], xPixel, yPixel, TextureRenderParams(alpha = progress))
            }
        }
        */
    }
}