package screen.element

import crafting.Recipe
import graphics.Renderer
import graphics.TextureRenderParams
import screen.Interaction
import screen.gui.*
import screen.mouse.Tooltips

class ElementRecipeButton(parent: GuiElement, recipe: Recipe? = null, var onRecipeChange: (Recipe?) -> Unit = {}, var recipePredicate: (Recipe) -> Boolean = { true }) : GuiElement(parent) {

    var recipe = recipe
        set(value) {
            if (field != value) {
                field = value
                onRecipeChange(value)
            }
        }

    var currentlySelecting = false

    init {
        GuiRecipeSelector.parentElement.eventListeners.add(GuiCloseListener {
            if (this@ElementRecipeButton.currentlySelecting) {
                this@ElementRecipeButton.currentlySelecting = false
            }
        })
    }

    override fun onInteractOn(interaction: Interaction) {
        currentlySelecting = true
        GuiRecipeSelector.show(Recipe.ALL.filter(recipePredicate), Placement.Exact(absoluteX, absoluteY - GuiRecipeSelector.parentElement.height / 2),
                { this.recipe = it; this.currentlySelecting = false })
        super.onInteractOn(interaction)
    }

    override fun render(params: TextureRenderParams?) {
        val actualParams = params ?: TextureRenderParams.DEFAULT
        Renderer.renderDefaultRectangle(absoluteX, absoluteY, width, height, actualParams.combine(TextureRenderParams(brightness = 1.2f + if (mouseOn) 0.1f else 0.0f, rotation = 180f)))
        recipe?.iconType?.icon?.render(absoluteX, absoluteY, width, height, true, actualParams)
        super.render(params)
    }

    companion object {
        init {
            Tooltips.addScreenTooltipTemplate({
                if(it is ElementRecipeButton) {
                     it.recipe?.iconType?.name
                } else {
                    null
                }
            }, 0)
        }
    }
}