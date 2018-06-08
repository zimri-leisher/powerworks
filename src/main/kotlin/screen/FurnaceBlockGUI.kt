package screen

import level.block.FurnaceBlock
import screen.elements.*

class FurnaceBlockGUI(val block: FurnaceBlock) : GUIWindow("Furnace block gui for $block", 0, 0, WIDTH, HEIGHT, windowGroup = ScreenManager.Groups.INVENTORY) {

    private lateinit var progressBar: GUIProgressBar

    init {
        partOfLevel = true
        openAtMouse = true
        GUIDefaultTextureRectangle(this.rootChild, name + " background", 0, 0).run {
            generateDragGrip(this.layer + 1)
            generateCloseButton(this.layer + 1)
            val text = GUIText(this, this@FurnaceBlockGUI.name + " name text", 1, 1, "Furnace queue:")
            val queue = GUIResourceContainerDisplay(this, this@FurnaceBlockGUI.name + " smelting queue display", block.queue, { 1 }, { text.heightPixels + 2 }, 1, 1)
            progressBar = GUIProgressBar(this, this@FurnaceBlockGUI.name + " smelting progress bar", { 2 }, { queue.yAlignment() + queue.heightPixels }, {this@FurnaceBlockGUI.widthPixels - 4}, maxProgress = block.type.maxWork)
            val text2 = GUIText(this, this@FurnaceBlockGUI.name + " tank text", 1, progressBar.yAlignment() + progressBar.heightPixels, "Furnace tank:")
            GUIFluidTankMeter(this, this@FurnaceBlockGUI.name + " tank meter", 2, text2.yAlignment() + text2.heightPixels + 1, WIDTH - 4, HEIGHT - (text2.yAlignment() + text2.heightPixels + 5), block.tank)
        }
    }

    override fun update() {
        progressBar.currentProgress = block.currentWork
    }

    companion object {
        val WIDTH = CraftingBlockGUI.WIDTH
        val HEIGHT = 60
    }
}