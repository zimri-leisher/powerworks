package main

import audio.AudioManager
import item.Inventory
import item.ItemType
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
            // the level should be set before this
            Game.mainInv = Inventory(Game.INVENTORY_WIDTH, Game.INVENTOR_HEIGHT)
            // the mouse listens to changes so that if there are no more items of the selected type in the main inventory, then it will switch the type to null
            Game.mainInv.listeners.add(Mouse)
            for(i in ItemType.ALL) {
                Game.mainInv.add(i, i.maxStack)
            }
            AudioManager.ears = IngameGUI.cameras[0]
            IngameGUI.open = true
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
                CURRENT_STATE.deactivate(NEXT_STATE!!)
                NEXT_STATE!!.activate(CURRENT_STATE)
                CURRENT_STATE = NEXT_STATE!!
                NEXT_STATE = null
            }
        }
    }
}