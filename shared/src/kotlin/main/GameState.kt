package main

import audio.AudioManager
import player.PlayerManager
import screen.gui.GuiIngame
import screen.gui.GuiMainMenu
import screen.mouse.Mouse

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
            GuiIngame.open = true
            PlayerManager.localPlayer.brainRobot.inventory.listeners.add(Mouse)

            AudioManager.ears = GuiIngame.cameras[0]
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