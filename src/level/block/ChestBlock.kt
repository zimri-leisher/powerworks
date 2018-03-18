package level.block

import item.Inventory
import item.ItemType
import io.*
import resource.ResourceType
import main.Game
import screen.InventoryGUI

class ChestBlock(override val type: ChestBlockTemplate, xTile: Int, yTile: Int, rotation: Int) : Block(type, xTile, yTile, rotation), ControlPressHandler {

    val inv = nodes.getAttachedContainers<ItemType>(ResourceType.ITEM).first() as Inventory
    val invGUI = InventoryGUI("Chest at ${this.xTile}, ${this.yTile}'s inventory gui", type.invName, inv, Game.WIDTH / 2, Game.HEIGHT / 2)

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_THIS, Control.INTERACT)
        invGUI.xPixel = (Game.WIDTH - invGUI.widthPixels) / 2
        invGUI.yPixel = (Game.HEIGHT - invGUI.heightPixels) / 2
    }

    override fun onAddToLevel() {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_THIS, Control.INTERACT)
        super.onAddToLevel()
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.control == Control.INTERACT && p.pressType == PressType.PRESSED) {
            invGUI.toggle()
        }
    }
}