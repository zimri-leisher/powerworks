package screen.element

import crafting.Recipe
import screen.gui.GuiElement
import screen.gui.GuiRecipeDisplay
import screen.gui.Placement

class ElementRecipeList(parent: GuiElement, recipes: List<Recipe>, width: Int, height: Int, onSelectRecipe: (Recipe) -> Unit = {}) : ElementIconList(parent, width, height, { _, _, _ -> }, recipes.size) {

    var recipes = recipes
        set(value) {
            if (field != value) {
                field = value
                iconCount = value.size
            }
        }

    init {
        allowSelection = true
        onSelectIcon = { index, interaction ->
            val recipe = this.recipes[index]
            onSelectRecipe(recipe)
        }
        onMouseEnterIcon = { index ->
            val coord = getIconPosition(index)
            GuiRecipeDisplay.show(this@ElementRecipeList.recipes[index], Placement.Exact(coord.x + absoluteX, coord.y + absoluteY - GuiRecipeDisplay.parentElement.height), {
                onSelectRecipe(this@ElementRecipeList.recipes[index])
            })
        }
        renderIcon = { x, y, index ->
            this@ElementRecipeList.recipes[index].iconType.icon.render(x, y, iconSize, iconSize, true)
        }
        getToolTip = { index ->
            if(index > recipes.lastIndex) {
                null
            } else {
                val recipe = recipes[index]
                recipe.iconType.name
            }
        }
    }
}