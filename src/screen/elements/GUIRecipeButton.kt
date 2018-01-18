package screen.elements

import crafting.Recipe
import graphics.Image
import io.PressType

class GUIRecipeButton(parent: RootGUIElement,
                      name: String,
                      xAlignment: () -> Int, yAlignment: () -> Int,
                      val recipe: Recipe) : GUIElement(
        parent, name, xAlignment, yAlignment, { WIDTH }, { HEIGHT }) {

    init {
        GUITexturePane(this, name + " background", 0, 0, Image.GUI.RECIPE_BUTTON_BACKGROUND).run {
            GUITexturePane(this, name + " consume icon", 0, 0, recipe.icon.texture)
        }
    }

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {

    }

    companion object {
        val WIDTH = GUIButton.WIDTH
        val HEIGHT = GUIItemSlot.HEIGHT + 2
    }
}