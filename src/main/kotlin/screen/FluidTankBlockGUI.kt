package screen

import level.block.Block
import level.block.FluidTankBlock
import screen.elements.BlockGUI

class FluidTankBlockGUI(block: FluidTankBlock) : FluidTankGUI(block.tank), BlockGUI {
    var block = block
        private set

    override fun canDisplayBlock(newBlock: Block): Boolean {
        return newBlock is FluidTankBlock
    }

    override fun displayBlock(newBlock: Block): Boolean {
        if (!canDisplayBlock(newBlock)) {
            return false
        }
        block = newBlock as FluidTankBlock
        tank = newBlock.tank
        fluidTankMeter.tank = tank
        return true
    }

    override fun isDisplayingBlock(block: Block) = block == this.block
}