package screen.gui

import graphics.Animation
import graphics.Image
import graphics.text.TextRenderParams
import main.Game
import network.ClientNetworkManager
import network.packet.RequestLoadGamePacket
import screen.ScreenLayer

object GuiMainMenu : Gui(ScreenLayer.MENU_0, {
    texture(Image.Gui.MAIN_MENU_BACKGROUND) {
        dimensions = Dimensions.Fullscreen
        list(Placement.Align.Center) {

            texture(Image.Gui.MAIN_MENU_LOGO)

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
                            Placement.Align(HorizontalAlign.CENTER, VerticalAlign.CENTER),
                            params = TextRenderParams(size = 40)
                    )
                }
            }
        }
    }
})