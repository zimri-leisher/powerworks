package screen

import level.block.SolidifierBlock
import screen.elements.*

class SolidifierBlockGUI(val block: SolidifierBlock) : GUIWindow("GUI window for solidifier block: $block", 0, 0, WIDTH, HEIGHT, ScreenManager.Groups.INVENTORY) {

    lateinit var progressBar: GUIProgressBar

    init {
        partOfLevel = true
        openAtMouse = true
        GUIDefaultTextureRectangle(this, this.name + " background", 0, 0).run {
            generateDragGrip(this.layer + 1)
            generateCloseButton(this.layer + 1)
            val text = GUIText(this, this@SolidifierBlockGUI.name + " name text", 1, 1, "Solidifying:")
            val queue = GUIFluidTankMeter(this, this@SolidifierBlockGUI.name + " solidifier tank display", 2, text.heightPixels + 3, WIDTH - 4, 17, block.tank)
            progressBar = GUIProgressBar(this, this@SolidifierBlockGUI.name + " solidifying progress bar", { 2 }, { queue.alignments.y() + queue.heightPixels + 2 }, { this@SolidifierBlockGUI.widthPixels - 4 }, maxProgress = block.type.maxWork)
            val text2 = GUIText(this, this@SolidifierBlockGUI.name + " tank text", 1, progressBar.alignments.y() + progressBar.heightPixels, "Output:")
            GUIResourceContainerDisplay(this, this@SolidifierBlockGUI.name + " tank meter", block.out, { 1 }, { text2.alignments.y() + text2.heightPixels + 1 }, 1, 1)
        }
    }

    companion object {
        val WIDTH = CrafterBlockGUI.WIDTH
        val HEIGHT = 60
    }
}