package screen

import graphics.Image
import graphics.Utils
import level.block.CrafterBlock
import screen.elements.GUIRecipeButton
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

class CraftingBlockGUI(val craftingBlock: CrafterBlock) :
        GUIWindow("Window of crafting block at ${craftingBlock.xTile}, ${craftingBlock.yTile}", 50, 50, WIDTH, HEIGHT, windowGroup = ScreenManager.Groups.PLAYER_UTIL) {

    init {
        GUITexturePane(this.rootChild, "Crafting block at ${craftingBlock.xTile}, ${craftingBlock.yTile}'s window background", 0, 0, Image(Utils.genRectangle(WIDTH, HEIGHT))).run {
            GUIRecipeButton
        }
    }

    companion object {
        const val WIDTH = 160
        const val HEIGHT = 300
    }
}