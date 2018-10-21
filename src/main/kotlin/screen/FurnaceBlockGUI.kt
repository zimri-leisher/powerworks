package screen

import level.block.FurnaceBlock
import screen.elements.*

class FurnaceBlockGUI(val block: FurnaceBlock) : AutoFormatGUIWindow("Furnace block gui for $block", { 0 }, { 0 }, ScreenManager.Groups.INVENTORY) {

    private val progressBar: GUIProgressBar

    init {
        partOfLevel = true
        openAtMouse = true

        GUIText(group, this.name + " name text", 0, 0,
                "Melting:")

        GUIResourceContainerDisplay(group, this.name + " smelting queue display",
                { 0 }, { 0 }, 1, 1,
                block.queue)

        progressBar = GUIProgressBar(group, this.name + " smelting progress bar",
                { 0 }, { 0 }, { group.widthPixels }, { GUIProgressBar.HEIGHT }, maxProgress = block.type.maxWork)

        GUIText(group, this.name + " tank text", 0, 0,
                "Output:")

        GUIFluidTankMeter(group, this.name + " tank meter", 0, 0, group.widthPixels, GUIFluidTankMeter.HEIGHT,
                block.tank)

        generateCloseButton(this.layer + 1)
        generateDragGrip(this.layer + 1)
    }

    override fun update() {
        progressBar.currentProgress = block.currentWork
    }
}