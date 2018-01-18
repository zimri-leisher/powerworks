package level.block

import inv.Inventory
import io.*
import level.node.InputNode
import level.resource.ResourceType
import main.Game
import screen.InventoryGUI

class ChestBlock(xTile: Int, yTile: Int, override val type: ChestBlockType) : Block(type, yTile, xTile), ControlPressHandler {

    val inv = Inventory(type.invWidth, type.invHeight)
    val invGUI = InventoryGUI("Chest at ${this.xTile}, ${this.yTile}'s inventory gui", "Small Chest", inv, Game.WIDTH / 2, Game.HEIGHT / 2)
    val inputs = arrayOf(InputNode(xTile, yTile, 0, inv, ResourceType.ITEM),
            InputNode(xTile, yTile, 1, inv, ResourceType.ITEM),
            InputNode(xTile, yTile, 2, inv, ResourceType.ITEM),
            InputNode(xTile, yTile, 3, inv, ResourceType.ITEM))

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL, Control.INTERACT)
        invGUI.xPixel = (Game.WIDTH - invGUI.widthPixels) / 2
        invGUI.yPixel = (Game.HEIGHT - invGUI.heightPixels) / 2
    }

    override fun onAddToLevel() {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL, Control.INTERACT)
        super.onAddToLevel()
    }

    override fun onRemoveFromLevel() {
        Game.currentLevel.removeAllTransferNodes(xTile, yTile)
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.control == Control.INTERACT && p.pressType == PressType.PRESSED) {
            invGUI.toggle()
        }
    }
}