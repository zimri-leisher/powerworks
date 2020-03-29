package main

import audio.AudioManager
import player.PlayerManager
import screen.*
import screen.mouse.Mouse

class State(val activate: () -> Unit, val deactivate: () -> Unit) {
    companion object {

        private var NEXT_STATE: State? = null

        val MAIN_MENU = State({
            MainMenuGUI.open = true
        }, {
            MainMenuGUI.open = false
        })

        val INGAME = State({
            IngameGUI.open = true
            // the mouse listens to changes so that if there are no more items of the selected type in the main inventory, then it will switch the type to null
            PlayerManager.localPlayer.brainRobot.inventory.listeners.add(Mouse)

            AudioManager.ears = IngameGUI.cameras[0]
            RecipeSelectorGUI
            HUD
            MovementToolsGUI
        }, {
        })

        var CURRENT_STATE = MAIN_MENU
            private set

        fun setState(s: State) {
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