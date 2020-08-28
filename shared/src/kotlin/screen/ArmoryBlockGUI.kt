package screen

import level.block.ArmoryBlock
import level.block.Block
import screen.elements.AutoFormatGUIWindow
import screen.elements.BlockGUI
import screen.elements.GUIDefaultTextureRectangle
import screen.elements.GUIProgressBar

class ArmoryBlockGUI(block: ArmoryBlock) : AutoFormatGUIWindow("Armory block gui window",
        { 0 }, { 0 }), BlockGUI {
    var block = block
        private set

    private val progressBar: GUIProgressBar

    init {
        openAtMouse = true
        progressBar = GUIProgressBar(group, "Armory block progress bar", { 0 }, { 0 }, { 32 }, { 6 }, block.type.maxWork)

        generateCloseButton()
        generateDragGrip()
    }

    override fun canDisplayBlock(newBlock: Block): Boolean {
        return newBlock is ArmoryBlock
    }

    override fun displayBlock(newBlock: Block): Boolean {
        if (!canDisplayBlock(newBlock)) {
            return false
        }
        block = newBlock as ArmoryBlock
        progressBar.maxProgress = newBlock.type.maxWork
        progressBar.currentProgress = newBlock.currentWork
        return true
    }

    override fun update() {
        progressBar.currentProgress = block.currentWork
    }

    override fun isDisplayingBlock(block: Block) = block.id == this.block.id
}