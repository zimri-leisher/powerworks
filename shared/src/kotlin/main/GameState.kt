package main

import audio.AudioManager
import player.PlayerManager
import screen.gui.*
import screen.mouse.Mouse
import setting.Settings

class GameState(val activate: () -> Unit, val deactivate: () -> Unit) {
    companion object {

        private var nextState: GameState? = null

        val MAIN_MENU = GameState({
            GuiMainMenu.open = true
        }, {
            GuiMainMenu.open = false
        })

        val INGAME = GameState({
            GuiIngame.initializeFor(PlayerManager.localPlayer.brainRobot)
            GuiLevelLoadingScreen.open = false
            GuiIngame.open = true
            PlayerManager.localPlayer.brainRobot.inventory.listeners.add(Mouse)

            AudioManager.ears = GuiIngame.cameras[0]
            if(Settings.SHOW_TUTORIAL.get()) {
                println("showing tutorial")
                GuiTutorial.open = true
                GuiTutorial.showStage(TutorialStage.OPEN_INVENTORY)
                Settings.SHOW_TUTORIAL.set(false)
            }
        }, {
        })

        var currentState = MAIN_MENU
            private set

        fun setState(s: GameState) {
            if(nextState != s) {
                nextState = s
            }
        }

        fun update() {
            if (nextState != null) {
                currentState.deactivate()
                nextState!!.activate()
                currentState = nextState!!
                nextState = null
            }
        }
    }
}