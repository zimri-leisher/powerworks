package screen

import graphics.text.TextManager
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
                ScreenManager.Groups.INVENTORY) {

    lateinit var progressBar: GUIProgressBar

    init {
        openAtMouse = true
        partOfLevel = true
        GUIDefaultTextureRectangle(this, "Crafting block at ${craftingBlock.xTile}, ${craftingBlock.yTile}'s window background", 0, 0).apply {
            GUIText(this, "Recipe text", 3, 3, "Recipe:")
            val recipeButton = GUIRecipeButton(this, "Recipe choice button", { 3 }, { 9 }, craftingBlock.recipe, { craftingBlock.recipe = it })
            val storageGroups = AutoFormatGUIGroup(this, "Crafting block container view group", 1, recipeButton.alignments.y() + recipeButton.heightPixels + 3, initializerList = {
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

/*
interface Colored {
    val color: String
}

interface Named {
    val name: String
}

interface Aged {
    val age: Int
}

fun <T> addName(thing: T, name: String): Named = object : T by thing, Named {
    override val name = name
}

fun <T> printThing(thing: T) where T : Named, T : Colored, T : Aged {
    println(thing.name)
    println(thing.color)
    println(thing.age)
}
        */