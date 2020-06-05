package screen

import level.block.Block
import level.block.FurnaceBlock
import main.toColor
import screen.elements.*

class FurnaceBlockGUI(block: FurnaceBlock) : AutoFormatGUIWindow("Furnace block gui", { 0 }, { 0 }, ScreenManager.Groups.INVENTORY), BlockGUI {
    var block = block
        private set
    private val progressArrow: GUIProgressArrow

    private val queueDisplay: GUIResourceContainerDisplay
    private val fluidTankMeter: GUIFluidTankMeter

    init {
        openAtMouse = true

        GUIText(group, this.name + " name text", 0, 0,
                "Furnace")

        queueDisplay = GUIResourceContainerDisplay(group, this.name + " smelting queue display",
                { 0 }, { 0 }, 1, 1,
                block.queue)

        fluidTankMeter = GUIFluidTankMeter(group, this.name + " tank meter", 0, 0, 48, GUIFluidTankMeter.HEIGHT,
                block.tank)

        progressArrow = GUIProgressArrow(this, "Furnace block progress arrow", queueDisplay, 1, fluidTankMeter, 0, maxProgress = block.type.maxWork, backgroundColor = toColor(90, 90, 90))

        generateCloseButton()
        generateDragGrip()
    }

    override fun update() {
        progressArrow.progressColor = block.currentlySmelting?.moltenForm?.color ?: progressArrow.progressColor
        progressArrow.currentProgress = block.currentWork
    }

    override fun canDisplayBlock(newBlock: Block): Boolean {
        return newBlock is FurnaceBlock
    }

    override fun displayBlock(newBlock: Block): Boolean {
        if (!canDisplayBlock(newBlock)) {
            return false
        }
        block = newBlock as FurnaceBlock
        queueDisplay.container = newBlock.queue
        fluidTankMeter.tank = newBlock.tank
        progressArrow.maxProgress = newBlock.type.maxWork
        progressArrow.currentProgress = newBlock.currentWork
        progressArrow.progressColor = newBlock.currentlySmelting?.moltenForm?.color ?: progressArrow.progressColor
        return true
    }

    override fun isDisplayingBlock(block: Block) = block.id == this.block.id
}