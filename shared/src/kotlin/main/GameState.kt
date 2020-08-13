package main

import audio.AudioManager
import player.PlayerManager
import screen.*
import screen.mouse.Mouse

class GameState(val activate: () -> Unit, val deactivate: () -> Unit) {
    companion object {

        private var NEXT_STATE: GameState? = null

        val MAIN_MENU = GameState({
            MainMenuGUI.open = true
        }, {
            MainMenuGUI.open = false
        })

        val INGAME = GameState({
            IngameGUI.open = true
            TestGUI
            PlayerManager.localPlayer.brainRobot.inventory.listeners.add(Mouse)

            AudioManager.ears = IngameGUI.cameras[0]
            RecipeSelectorGUI
            HUD
            MovementToolsGUI
        }, {
        })

        var CURRENT_STATE = MAIN_MENU
            private set

        fun setState(s: GameState) {
            NEXT_STATE = s
        }

        fun update() {
            if (NEXT_STATE != null) {
                CURRENT_STATE.deactivate()
                NEXT_STATE!!.activate()
                CURRENT_STATE = NEXT_STATE!!
                NEXT_STATE = null
            }
        }
    }
}