package screen

import level.block.Block
import level.block.FluidTankBlock
import level.block.SolidifierBlock
import screen.elements.*

class SolidifierBlockGUI(block: SolidifierBlock) :
        AutoFormatGUIWindow("GUI window for solidifier block: $block", { 0 }, { 0 }, ScreenManager.Groups.INVENTORY), BlockGUI {
    var block = block
        private set
    var progressBar: GUIProgressBar
    private val fluidTankMeter: GUIFluidTankMeter
    private val outputContainerDisplay: GUIResourceContainerDisplay

    init {
        partOfLevel = true
        openAtMouse = true

        val nameText = GUIText(group, this@SolidifierBlockGUI.name + " name text", 0, 0, "Solidifying:")

        fluidTankMeter = GUIFluidTankMeter(group, this@SolidifierBlockGUI.name + " solidifier tank display", 0, 0, 26, 17, block.tank)

        progressBar = GUIProgressBar(group, this@SolidifierBlockGUI.name + " solidifying progress bar", { 0 }, { 0 }, { nameText.widthPixels + 10 }, maxProgress = block.type.maxWork)

        GUIText(group, this@SolidifierBlockGUI.name + " tank text", 0, 0, "Output:")

        outputContainerDisplay = GUIResourceContainerDisplay(group, this@SolidifierBlockGUI.name + " tank meter", { 0 }, { 0 }, 1, 1, block.out)

        generateCloseButton(this.layer + 1)
        generateDragGrip(this.layer + 1)
    }

    override fun canDisplayBlock(newBlock: Block): Boolean {
        return newBlock is SolidifierBlock
    }

    override fun displayBlock(newBlock: Block): Boolean {
        if(!canDisplayBlock(newBlock)) {
            return false
        }
        block = newBlock as SolidifierBlock
        fluidTankMeter.tank = newBlock.tank
        outputContainerDisplay.container = newBlock.out
        progressBar.maxProgress = newBlock.type.maxWork
        progressBar.currentProgress = newBlock.currentWork
        return true
    }

    override fun isDisplayingBlock(block: Block) = block == this.block

    override fun update() {
        progressBar.currentProgress = block.currentWork
    }
}