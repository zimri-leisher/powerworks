package screen.gui2

import graphics.Animation
import graphics.Image
import graphics.text.TextRenderParams
import main.Game
import network.ClientNetworkManager
import network.packet.RequestLoadGamePacket

object GuiMainMenu : Gui(ScreenLayer.MENU, {
    texture(Image.GUI.MAIN_MENU_BACKGROUND) {
        dimensions = Dimensions.Fullscreen
        list(Placement.Align.Center) {

            texture(Image.GUI.MAIN_MENU_LOGO)

            button(onRelease = {
                GuiMainMenu.open = false
                ClientNetworkManager.sendToServer(RequestLoadGamePacket(Game.USER))
                GuiLevelLoadingScreen.open = true
            }) {

                animation(Animation.MAIN_MENU_PLAY_BUTTON) {
                    onMouseEnter {
                        Animation.MAIN_MENU_PLAY_BUTTON.play()
                    }
                    onMouseLeave {
                        with(Animation.MAIN_MENU_PLAY_BUTTON) {
                            if (currentStepID == "inside_loop") {
                                playBackwardsFrom("outside_loop")
                            } else {
                                playBackwards()
                            }
                        }
                    }
                    text("PLAY",
                            Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP),
                            params = TextRenderParams(size = 30)
                    )
                }
            }
        }
    }
})