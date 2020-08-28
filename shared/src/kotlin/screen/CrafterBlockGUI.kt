package screen

import level.block.Block
import level.block.CrafterBlock
import network.BlockReference
import player.PlayerManager
import player.SelectCrafterRecipeAction
import screen.elements.*

/**
 * The GUI opened when a CrafterBlock gets clicked on
 */
class CrafterBlockGUI(block: CrafterBlock) :
        AutoFormatGUIWindow("Crafting block gui window",
                { 0 }, { 0 }), BlockGUI {
    var block = block
        private set

    private val progressBar: GUIProgressBar
    private val recipeButton: GUIRecipeButton
    private val containers: List<GUIResourceContainerDisplay>

    init {
        openAtMouse = true

        GUIText(group, "Recipe text", 0, 0, "Recipe:")

        recipeButton = GUIRecipeButton(group, "Recipe choice button", { 0 }, { 0 }, block.recipe,
                {
                    PlayerManager.takeAction(SelectCrafterRecipeAction(PlayerManager.localPlayer, BlockReference(this.block), it))
                },
                { it.validCrafterTypes != null && it.validCrafterTypes.contains(this.block.crafterType) && it.consume.size <= this.block.type.internalStorageSize })

        GUIText(group, "Container text", 0, 0, "Ingredients:")

        containers = mutableListOf()

        for (container in block.containers) {
            containers.add(GUIResourceContainerDisplay(group, this@CrafterBlockGUI.name + " resource list display", { 0 }, { 0 }, block.type.internalStorageSize, 1, container))
        }

        progressBar = GUIProgressBar(group, "Crafting block container progress bar", { 0 }, { 0 }, { this.widthPixels - 4 }, { 6 }, block.type.maxWork)

        generateCloseButton()
        generateDragGrip()
    }

    override fun canDisplayBlock(newBlock: Block): Boolean {
        if (newBlock.containers.size != block.containers.size)
            return false
        return newBlock is CrafterBlock
    }

    override fun onClose() {
        if (recipeButton.waitingForRecipeSelection) {
            RecipeSelectorGUI.open = false
        }
    }

    override fun displayBlock(newBlock: Block): Boolean {
        if (!canDisplayBlock(newBlock))
            return false
        block = newBlock as CrafterBlock
        recipeButton.recipe = newBlock.recipe
        containers.forEachIndexed { index, it ->
            it.columns = newBlock.type.internalStorageSize
            it.container = newBlock.containers[index]
        }
        progressBar.maxProgress = newBlock.type.maxWork
        progressBar.currentProgress = newBlock.currentWork
        return true
    }

    override fun update() {
        progressBar.currentProgress = block.currentWork
    }

    override fun isDisplayingBlock(block: Block) = block.id == this.block.id
}