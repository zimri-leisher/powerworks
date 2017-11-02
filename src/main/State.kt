package main

import audio.AudioManager
import inv.Inventory
import level.SimplexLevel
import player.Camera
import screen.HUD
import screen.IngameGUI
import screen.MainMenuGUI


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
            Game.camera = Camera(Game.currentLevel.widthPixels / 2, Game.currentLevel.heightPixels / 2)
            AudioManager.ears = Game.camera
            Game.mainInv = Inventory(8, 6)
            Game.currentLevel.add(Game.camera)
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