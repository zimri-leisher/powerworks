package screen

import level.block.Block
import level.block.FluidTankBlock
import level.block.FurnaceBlock
import screen.elements.*

class FurnaceBlockGUI(block: FurnaceBlock) : AutoFormatGUIWindow("Furnace block gui", { 0 }, { 0 }, ScreenManager.Groups.INVENTORY), BlockGUI {
    var block = block
        private set
    private val progressBar: GUIProgressBar

    private val queueDisplay: GUIResourceContainerDisplay
    private val fluidTankMeter: GUIFluidTankMeter

    init {
        partOfLevel = true
        openAtMouse = true

        GUIText(group, this.name + " name text", 0, 0,
                "Melting:")

        queueDisplay = GUIResourceContainerDisplay(group, this.name + " smelting queue display",
                { 0 }, { 0 }, 1, 1,
                block.queue)

        progressBar = GUIProgressBar(group, this.name + " smelting progress bar",
                { 0 }, { 0 }, { group.widthPixels }, { GUIProgressBar.HEIGHT }, maxProgress = block.type.maxWork)

        GUIText(group, this.name + " tank text", 0, 0,
                "Output:")

        fluidTankMeter = GUIFluidTankMeter(group, this.name + " tank meter", 0, 0, group.widthPixels, GUIFluidTankMeter.HEIGHT,
                block.tank)

        generateCloseButton(this.layer + 1)
        generateDragGrip(this.layer + 1)
    }

    override fun update() {
        progressBar.currentProgress = block.currentWork
    }

    override fun canDisplayBlock(newBlock: Block): Boolean {
        return newBlock is FurnaceBlock
    }

    override fun displayBlock(newBlock: Block): Boolean {
        if(!canDisplayBlock(newBlock)) {
            return false
        }
        block = newBlock as FurnaceBlock
        queueDisplay.container = newBlock.queue
        fluidTankMeter.tank = newBlock.tank
        progressBar.maxProgress = newBlock.type.maxWork
        progressBar.currentProgress = newBlock.currentWork
        return true
    }

    override fun isDisplayingBlock(block: Block) = block == this.block
}