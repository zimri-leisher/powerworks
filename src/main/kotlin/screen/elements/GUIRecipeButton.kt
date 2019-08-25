package screen.elements

import com.badlogic.gdx.Input
import crafting.Recipe
import graphics.Renderer
import io.PressType
import screen.RecipeSelectorGUI

class GUIRecipeButton(parent: RootGUIElement,
                      name: String,
                      xAlignment: Alignment, yAlignment: Alignment,
                      recipe: Recipe?,
                      /**
                       * Called when the selected recipe is switched. The parameter is
                       * the new recipe
                       */
                      val onRecipeChange: (Recipe?) -> Unit = {},
                      private val available: (Recipe) -> Boolean = { true }) :
        GUIElement(parent, name, xAlignment, yAlignment, { WIDTH }, { HEIGHT }) {

    var recipe = recipe
        set(value) {
            if (field != value) {
                display.recipe = value
                field = value
                onRecipeChange(value)
            }
        }

    val display: GUIRecipeDisplay = GUIRecipeDisplay(this, "Recipe button's recipe display", { 0 }, { 0 }, recipe).apply { transparentToInteraction = true }
    var waitingForRecipeSelection = false

    override fun onOpen() {
        if (RecipeSelectorGUI.open) {
            RecipeSelectorGUI.open = false
        }
    }

    override fun update() {
        if (waitingForRecipeSelection) {
            val selected = RecipeSelectorGUI.selected
            if (selected != null) {
                waitingForRecipeSelection = false
                recipe = selected
                RecipeSelectorGUI.open = false
            }
        }
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED && button == Input.Buttons.LEFT) {
            RecipeSelectorGUI.open = true
            RecipeSelectorGUI.available = available
            RecipeSelectorGUI.windowGroup.bringToTop(RecipeSelectorGUI)
            waitingForRecipeSelection = true
        }
    }

    override fun render() {
        display.background.localRenderParams.brightness = if(mouseOn) 1.1f else 1f
    }

    companion object {
        val WIDTH = GUIRecipeDisplay.WIDTH
        val HEIGHT = GUIRecipeDisplay.HEIGHT
    }
}