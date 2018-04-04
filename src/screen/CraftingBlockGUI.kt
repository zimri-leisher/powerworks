package screen

import graphics.Image
import graphics.Utils
import level.block.CrafterBlock
import screen.elements.*

/**
 * The GUI opened when a CrafterBlock gets clicked on
 */
class CraftingBlockGUI(val craftingBlock: CrafterBlock) :
        GUIWindow("Window of crafting block at ${craftingBlock.xTile}, ${craftingBlock.yTile}", 50, 30, WIDTH, HEIGHT, windowGroup = ScreenManager.Groups.INVENTORY) {

    init {
        GUITexturePane(this.rootChild, "Crafting block at ${craftingBlock.xTile}, ${craftingBlock.yTile}'s window background", 0, 0, Image(Utils.genRectangle(WIDTH, HEIGHT))).run {
            GUIDragGrip(this, this.name + " drag grip", { widthPixels - 5 }, { 1 }, actOn = this@CraftingBlockGUI)
            GUIText(this, "Recipe text", 3, 3, "Recipe:")
            GUIRecipeButton(this, "Recipe choice button", { 3 }, { 9 }, craftingBlock.recipe, { craftingBlock.recipe = it })
        }
    }

    companion object {
        const val WIDTH = 80
        const val HEIGHT = 100
    }
}