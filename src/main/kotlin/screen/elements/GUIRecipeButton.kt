package screen.elements

import crafting.Recipe
import io.PressType
import screen.RecipeSelectorGUI

class GUIRecipeButton(parent: RootGUIElement,
                      name: String,
                      xAlignment: Alignment, yAlignment: Alignment,
                      recipe: Recipe?, val onRecipeChange: (Recipe?) -> Unit = {}) : GUIElement(
        parent, name, xAlignment, yAlignment, { WIDTH }, { HEIGHT }) {

    var recipe = recipe
        set(value) {
            if (field != value) {
                display.recipe = value
                field = value
                onRecipeChange(value)
            }
        }

    val display: GUIRecipeDisplay
    var waitingForRecipeSelection = false

    init {
        display = GUIRecipeDisplay(this, "Recipe button's recipe display", { 0 }, { 0 }, recipe).apply { transparentToInteraction = true }
    }

    override fun update() {
        if (waitingForRecipeSelection) {
            val selected = RecipeSelectorGUI.getSelected()
            if (selected != null) {
                waitingForRecipeSelection = false
                recipe = selected
                RecipeSelectorGUI.open = false
            }
        }
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED && button == 1) {
            RecipeSelectorGUI.open = true
            RecipeSelectorGUI.windowGroup.bringToTop(RecipeSelectorGUI)
            waitingForRecipeSelection = true
        }
    }

    companion object {
        val WIDTH = GUIRecipeDisplay.WIDTH
        val HEIGHT = GUIRecipeDisplay.HEIGHT
    }
}