package screen.gui

import graphics.Animation
import level.LevelManager
import level.RemoteLevel
import main.GameState
import network.ClientNetworkManager
import player.PlayerManager
import screen.ScreenLayer

object GuiLevelLoadingScreen : Gui(ScreenLayer.MENU_0) {

    lateinit var loadingScreen: GuiElement
    lateinit var connectingScreen: GuiElement

    init {
        define {
            background {
                dimensions = Dimensions.Fullscreen
                loadingScreen = animation(Animation.SOLIDIFIER, Placement.Align.Center) {
                    text("Loading level...", Placement.Align(verticalAlign = VerticalAlign.TOP).offset(0, 10))
                }
                connectingScreen = animation(Animation.MINER, Placement.Align.Center) {
                    text("Connecting to server", Placement.Align(verticalAlign = VerticalAlign.TOP).offset(0, 10))
                }
            }
        }
    }

    override fun update() {
        if (!ClientNetworkManager.hasConnected()) {
            connectingScreen.open = true
            loadingScreen.open = false
        } else {
            connectingScreen.open = false
            loadingScreen.open = true
        }
//        println("tick level loading screen")
        if (PlayerManager.isLocalPlayerLoaded()) {
            println("home level loaded: ${PlayerManager.localPlayer.homeLevel.loaded}")
//            println("${LevelManager.allLevels.any { it.loaded && it.data.brainRobots.any { brainRobot -> brainRobot.id == PlayerManager.localPlayer.brainRobotId }}} found brain robot")
            if (((PlayerManager.localPlayer.homeLevel is RemoteLevel && (PlayerManager.localPlayer.homeLevel as RemoteLevel).loaded)
                        || PlayerManager.localPlayer.homeLevel !is RemoteLevel)
                && LevelManager.allLevels.any { it.data.brainRobots.any { brainRobot -> brainRobot.id == PlayerManager.localPlayer.brainRobotId } }
            ) {
                open = false
                GameState.setState(GameState.INGAME)
            }
        }
        super.update()
    }
}