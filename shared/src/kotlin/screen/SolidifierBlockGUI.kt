package screen

import level.block.Block
import level.block.SolidifierBlock
import main.toColor
import screen.elements.*

class SolidifierBlockGUI(block: SolidifierBlock) :
        AutoFormatGUIWindow("GUI window for solidifier block: $block", { 0 }, { 0 }, ScreenManager.Groups.INVENTORY), BlockGUI {
    var block = block
        private set
    var progressArrow: GUIProgressArrow
    private val fluidTankMeter: GUIFluidTankMeter
    private val outputContainerDisplay: GUIResourceContainerDisplay

    init {
        openAtMouse = true

        GUIText(group, this@SolidifierBlockGUI.name + " name text", 0, 0, "Solidifier")

        fluidTankMeter = GUIFluidTankMeter(group, this@SolidifierBlockGUI.name + " solidifier tank display", 0, 0, 52, GUIFluidTankMeter.HEIGHT, block.tank)

        outputContainerDisplay = GUIResourceContainerDisplay(group, this@SolidifierBlockGUI.name + " tank meter", { 0 }, { 0 }, 1, 1, block.out)

        progressArrow = GUIProgressArrow(group, name + " progress arrow", fluidTankMeter, 2, outputContainerDisplay, 1, block.type.maxWork, toColor(90, 90, 90))

        generateCloseButton()
        generateDragGrip()
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
        progressArrow.maxProgress = newBlock.type.maxWork
        progressArrow.currentProgress = newBlock.currentWork
        progressArrow.progressColor = newBlock.currentlySolidifying?.color ?: progressArrow.progressColor
        return true
    }

    override fun isDisplayingBlock(block: Block) = block == this.block

    override fun update() {
        progressArrow.progressColor = block.currentlySolidifying?.color ?: progressArrow.progressColor
        progressArrow.currentProgress = block.currentWork
    }
}