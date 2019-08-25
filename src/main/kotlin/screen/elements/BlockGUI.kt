package screen.elements

import level.block.Block

// I do wish this were somehow a real part of the GUI hierarchy but i think this works well for now
/**
 * A GUI that can display a [Block]
 */
interface BlockGUI {
    fun canDisplayBlock(newBlock: Block): Boolean
    fun displayBlock(newBlock: Block): Boolean
    fun isDisplayingBlock(block: Block): Boolean
}