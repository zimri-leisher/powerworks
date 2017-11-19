package level.block

import inv.Inventory
import io.*
import main.Game
import screen.InventoryGUI

class ChestBlock(xTile: Int, yTile: Int, override val type: ChestBlockType) : Block(xTile, yTile, type), ControlPressHandler {

    val inv = Inventory(type.invWidth, type.invHeight)
    val invGUI = InventoryGUI("Chest at ${this.xTile}, ${this.yTile}'s inventory gui", "Small Chest", inv, Game.WIDTH / 2, Game.HEIGHT / 2)

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL, Control.INTERACT)
        invGUI.xPixel = (Game.WIDTH - invGUI.widthPixels) / 2
        invGUI.yPixel = (Game.HEIGHT - invGUI.heightPixels) / 2
    }

    override fun handleControlPress(p: ControlPress) {
        if(p.control == Control.INTERACT && p.pressType == PressType.PRESSED) {
            invGUI.toggle()
        }
    }
}