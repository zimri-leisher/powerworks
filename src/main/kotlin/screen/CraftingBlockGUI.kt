package screen

import graphics.Image
import graphics.TextManager
import graphics.Utils
import level.block.CrafterBlock
import screen.elements.*

/**
 * The GUI opened when a CrafterBlock gets clicked on
 */
class CraftingBlockGUI(val craftingBlock: CrafterBlock) :
        GUIWindow("Window of crafting block at ${craftingBlock.xTile}, ${craftingBlock.yTile}",
                50, 30,
                WIDTH,
                TextManager.getFont().charHeight + GUIRecipeButton.HEIGHT + (craftingBlock.containers.size * GUIResourceDisplaySlot.HEIGHT + 14 + GUIProgressBar.HEIGHT),
                windowGroup = ScreenManager.Groups.INVENTORY) {

    lateinit var progressBar: GUIProgressBar

    init {
        openAtMouse = true
        partOfLevel = true
        GUIDefaultTextureRectangle(this.rootChild, "Crafting block at ${craftingBlock.xTile}, ${craftingBlock.yTile}'s window background", 0, 0).apply {
            GUIText(this, "Recipe text", 3, 3, "Recipe:")
            val recipeButton = GUIRecipeButton(this, "Recipe choice button", { 3 }, { 9 }, craftingBlock.recipe, { craftingBlock.recipe = it })
            val storageGroups = AutoFormatGUIGroup(this, "Crafting block container view group", 1, recipeButton.yAlignment() + recipeButton.heightPixels + 3, initializerList = {
                for (container in craftingBlock.containers) {
                    GUIResourceContainerDisplay(this, this@CraftingBlockGUI.name + " resource list display", container, { 0 }, { 0 }, craftingBlock.type.internalStorageSize, 1)
                }
            }, accountForChildHeight = true, yPixelSeparation = 1)
            progressBar = GUIProgressBar(this, "Crafting block container progress bar", { 2 }, { this.heightPixels - 8 }, { this.widthPixels - 4 }, { 6 }, craftingBlock.type.maxWork)
            generateDragGrip(this.layer + 2)
            generateCloseButton(this.layer + 2)
        }
    }

    override fun update() {
        progressBar.currentProgress = craftingBlock.currentWork
    }

    companion object {
        const val WIDTH = 80
    }
}