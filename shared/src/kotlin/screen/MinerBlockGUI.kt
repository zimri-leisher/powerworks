package screen

import level.block.Block
import level.block.MinerBlock
import screen.elements.*

class MinerBlockGUI(block: MinerBlock) :
        AutoFormatGUIWindow("GUI window for miner block: $block", { 0 }, { 0 }, ScreenManager.Groups.INVENTORY), BlockGUI {
    var block = block
        private set
    var progressBar: GUIProgressBar
    private val outputContainerDisplay: GUIResourceContainerDisplay

    init {
        openAtMouse = true

        GUIText(group, this.name + " name text", 0, 0, "Miner")

        outputContainerDisplay = GUIResourceContainerDisplay(group, this.name + " tank meter", { 0 }, { 0 }, 1, 1, block.output)

        progressBar = GUIProgressBar(group, this.name + " progress bar", { 0 }, { 0 }, { 32 }, { 6 }, block.type.maxWork)

        generateCloseButton()
        generateDragGrip()
    }

    override fun canDisplayBlock(newBlock: Block): Boolean {
        return newBlock is MinerBlock
    }

    override fun displayBlock(newBlock: Block): Boolean {
        if (!canDisplayBlock(newBlock)) {
            return false
        }
        block = newBlock as MinerBlock
        outputContainerDisplay.container = newBlock.output
        progressBar.maxProgress = newBlock.type.maxWork
        progressBar.currentProgress = newBlock.currentWork
        return true
    }

    override fun isDisplayingBlock(block: Block) = block.id == this.block.id

    override fun update() {
        progressBar.currentProgress = block.currentWork
    }
}