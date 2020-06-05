package screen

import level.block.Block
import level.block.ChestBlock
import screen.elements.BlockGUI

class ChestBlockGUI(block: ChestBlock) : InventoryGUI("Chest inventory gui", block.type.invName, block.inv, 0, 0), BlockGUI {
    var block = block
        private set

    override fun canDisplayBlock(newBlock: Block): Boolean {
        if(newBlock !is ChestBlock)
            return false
        if (inv.width != newBlock.inv.width)
            return false
        if (inv.height != newBlock.inv.height)
            return false
        return true
    }

    override fun displayBlock(newBlock: Block): Boolean {
        if (!canDisplayBlock(newBlock)) {
            return false
        }
        block = newBlock as ChestBlock
        itemSlots.forEach { it.inv = newBlock.inv }
        return true
    }

    override fun isDisplayingBlock(block: Block) = block.id == this.block.id
}