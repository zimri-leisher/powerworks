package level.block

import com.badlogic.gdx.Input
import io.*
import item.Inventory
import main.Game
import screen.InventoryGUI

class ChestBlock(override val type: ChestBlockType, xTile: Int, yTile: Int, rotation: Int) : Block(type, xTile, yTile, rotation) {

    val inv = containers.first { it is Inventory } as Inventory
    val gui = InventoryGUI("Chest at ${this.xTile}, ${this.yTile}'s inventory gui", type.invName, inv, Game.WIDTH / 2, Game.HEIGHT / 2)

    var typeRuleB = true

    override fun onRemoveFromLevel() {
        super.onRemoveFromLevel()
        gui.open = false
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if(type == PressType.PRESSED) {
            if(button == Input.Buttons.LEFT) {
                if(shift) {
                    val item = inv[0]!!
                    nodes.output(item.type, item.quantity, onlyTo = { it.attachedNodes.isNotEmpty() })
                } else {
                    gui.toggle()
                }
            } else if(shift) {
                typeRuleB = !typeRuleB
                val b = typeRuleB
                inv.typeRule = { !b }
            }
        }

    }
}