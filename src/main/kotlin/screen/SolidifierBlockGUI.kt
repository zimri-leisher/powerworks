package screen

import level.block.SolidifierBlock
import screen.elements.*

class SolidifierBlockGUI(val block: SolidifierBlock) :
        AutoFormatGUIWindow("GUI window for solidifier block: $block", { 0 }, { 0 }, ScreenManager.Groups.INVENTORY) {

    var progressBar: GUIProgressBar

    init {
        partOfLevel = true
        openAtMouse = true

        val nameText = GUIText(group, this@SolidifierBlockGUI.name + " name text", 0, 0, "Solidifying:")

        GUIFluidTankMeter(group, this@SolidifierBlockGUI.name + " solidifier tank display", 0, 0, 26, 17, block.tank)

        progressBar = GUIProgressBar(group, this@SolidifierBlockGUI.name + " solidifying progress bar", { 0 }, { 0 }, { nameText.widthPixels + 10 }, maxProgress = block.type.maxWork)

        GUIText(group, this@SolidifierBlockGUI.name + " tank text", 0, 0, "Output:")

        GUIResourceContainerDisplay(group, this@SolidifierBlockGUI.name + " tank meter", { 0 }, { 0 }, 1, 1, block.out)

        generateCloseButton(this.layer + 1)
        generateDragGrip(this.layer + 1)
    }

    override fun update() {
        progressBar.currentProgress = block.currentWork
    }
}