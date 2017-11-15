package main

import audio.AudioManager
import inv.Inventory
import level.SimplexLevel
import player.Camera
import screen.*


class State(val activate: (State) -> (Unit), val deactivate: (State) -> (Unit)) {
    companion object {

        private var NEXT_STATE: State? = null

        val MAIN_MENU = State({
            MainMenuGUI.open = true
        }, {
            MainMenuGUI.open = false
        })

        val INGAME = State({
            Game.currentLevel = SimplexLevel("level1", 256, 256)
            Game.mainInv = Inventory(Game.INVENTORY_WIDTH, Game.INVENTOR_HEIGHT)
            AudioManager.ears = IngameGUI.cameras[0]
            IngameGUI.open = true
            HUD.poke()
        }, {
        })

        var CURRENT_STATE = MAIN_MENU
            private set
        fun setState(s: State) {
            NEXT_STATE = s
        }

        fun update() {
            if (NEXT_STATE != null) {
                CURRENT_STATE.deactivate(NEXT_STATE!!)
                NEXT_STATE!!.activate(CURRENT_STATE)
                CURRENT_STATE = NEXT_STATE!!
                NEXT_STATE = null
            }
        }
    }
}