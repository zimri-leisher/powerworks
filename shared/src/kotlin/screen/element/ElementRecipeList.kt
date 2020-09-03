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
            println("coord is: $coord")
            println("absolute screen coord: ${Placement.Exact(coord.xPixel + absoluteXPixel, coord.yPixel + absoluteYPixel - GuiRecipeDisplay.parentElement.heightPixels)}")
            GuiRecipeDisplay.show(this@ElementRecipeList.recipes[index], Placement.Exact(coord.xPixel + absoluteXPixel, coord.yPixel + absoluteYPixel - GuiRecipeDisplay.parentElement.heightPixels), {
                onSelectRecipe(this@ElementRecipeList.recipes[index])
            })
        }
        renderIcon = { xPixel, yPixel, index ->
            this@ElementRecipeList.recipes[index].iconType.icon.render(xPixel, yPixel, iconSize, iconSize, true)
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