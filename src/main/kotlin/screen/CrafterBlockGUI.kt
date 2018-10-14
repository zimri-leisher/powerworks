package screen

import level.block.CrafterBlock
import screen.elements.*

/**
 * The GUI opened when a CrafterBlock gets clicked on
 */
class CrafterBlockGUI(val block: CrafterBlock) :
        AutoFormatGUIWindow("Window of crafting block at ${block.xTile}, ${block.yTile}",
                { 0 }, { 0 },
                ScreenManager.Groups.INVENTORY) {

    val progressBar: GUIProgressBar

    init {
        openAtMouse = true
        partOfLevel = true

        GUIText(group, "Recipe text", 0, 0, "Recipe:")

        GUIRecipeButton(group, "Recipe choice button", { 0 }, { 0 }, block.recipe, { block.recipe = it }).apply {
            GUIOutline(this, "Test")
        }

        for (container in block.containers) {
            GUIResourceContainerDisplay(group, this@CrafterBlockGUI.name + " resource list display", { 0 }, { 0 }, block.type.internalStorageSize, 1, container)
        }

        progressBar = GUIProgressBar(group, "Crafting block container progress bar", { 0 }, { 0 }, { this.widthPixels - 4 }, { 6 }, block.type.maxWork)

        generateCloseButton(group.layer + 2)
        generateDragGrip(group.layer + 2)
    }

    override fun update() {
        progressBar.currentProgress = block.currentWork
    }
}