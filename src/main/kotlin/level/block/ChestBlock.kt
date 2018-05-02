package level.block

import io.*
import item.Inventory
import item.ItemType
import main.Game
import resource.ResourceType
import screen.InventoryGUI

class ChestBlock(override val type: ChestBlockType, xTile: Int, yTile: Int, rotation: Int) : Block(type, xTile, yTile, rotation), ControlPressHandler {

    val inv = nodes.getAttachedContainers<ItemType>(ResourceType.ITEM).first() as Inventory
    val invGUI = InventoryGUI("Chest at ${this.xTile}, ${this.yTile}'s inventory gui", type.invName, inv, Game.WIDTH / 2, Game.HEIGHT / 2)

    init {
        invGUI.xPixel = (Game.WIDTH - invGUI.widthPixels) / 2
        invGUI.yPixel = (Game.HEIGHT - invGUI.heightPixels) / 2
    }

    override fun onAddToLevel() {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.LEVEL_THIS, Control.INTERACT, Control.SHIFT_INTERACT, Control.SHIFT_SECONDARY_INTERACT)
        super.onAddToLevel()
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.pressType == PressType.PRESSED) {
            if (p.control == Control.INTERACT) {
                invGUI.toggle()
            } else if (p.control == Control.SHIFT_INTERACT) {
                val item = inv[0]!!
                nodes.output(item.type, item.quantity, onlyTo = { it.attachedNode != null })
            } else if (p.control == Control.SHIFT_SECONDARY_INTERACT) {
                inv.typeRule = { false }
            }
        }
    }
}