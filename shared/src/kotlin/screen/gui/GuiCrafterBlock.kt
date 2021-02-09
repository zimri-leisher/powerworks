package screen.gui

import level.block.CrafterBlock
import network.BlockReference
import player.PlayerManager
import player.ActionSelectCrafterRecipe
import screen.ScreenLayer
import screen.attribute.AttributeResourceContainerLink
import screen.element.ElementProgressBar
import screen.element.ElementRecipeButton
import screen.element.ElementResourceContainer

class GuiCrafterBlock(block: CrafterBlock) : Gui(ScreenLayer.MENU_1), PoolableGui {
    var block = block
        set(value) {
            if (field != value) {
                field = value
                recipe.recipe = value.recipe
                input.container = value.inputContainer
                input.columns = value.type.internalStorageSize
                output.container = value.outputContainer
                progressBar.maxProgress = value.type.maxWork
                containerLink.container = field.inputContainer
                layout.set()
            }
        }

    lateinit var recipe: ElementRecipeButton
    lateinit var input: ElementResourceContainer
    lateinit var progressBar: ElementProgressBar
    lateinit var output: ElementResourceContainer
    lateinit var containerLink: AttributeResourceContainerLink

    init {
        define {
            onClose {
                if(recipe.currentlySelecting) {
                    GuiRecipeSelector.open = false
                }
            }
            openAtCenter(0)
            openWithBrainInventory()
            keepInsideScreen()
            containerLink = linkToContainer(block.inputContainer)
            background {
                makeDraggable()
                dimensions = Dimensions.FitChildren.pad(4, 9)
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
                text("Crafter", Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(1, -1)) { makeDraggable() }
                list(Placement.Align(HorizontalAlign.CENTER, VerticalAlign.BOTTOM).offset(0, 2), padding = 1) {
                    makeDraggable()
                    recipe = recipeSelectButton(block.recipe,
                            {
                                it.validCrafterTypes == null || this@GuiCrafterBlock.block.crafterType in it.validCrafterTypes
                            },
                            {
                                PlayerManager.takeAction(ActionSelectCrafterRecipe(PlayerManager.localPlayer, this@GuiCrafterBlock.block.toReference() as BlockReference, it))
                            })
                    input = resourceContainerView(block.inputContainer, block.type.internalStorageSize, 1, allowSelection = true, allowModification = true)
                    progressBar = progressBar(block.type.maxWork, { this@GuiCrafterBlock.block.currentWork }, Dimensions.Exact(32, 6))
                    output = resourceContainerView(block.outputContainer, 1, 1)
                }
            }
        }
    }

    override fun canDisplay(obj: Any?) = obj is CrafterBlock

    override fun display(obj: Any?) {
        block = obj as CrafterBlock
    }

    override fun isDisplaying(obj: Any?) = obj == block
}