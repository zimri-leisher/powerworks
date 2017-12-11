package screen

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
            GUIGroup(this, name + " main group", { 0 }, { 1 }).run {
                var i = 0
                recipe.consume.forEach { k, v ->
                    GUITexturePane(this, name + " consumer texture", (16 * i++), 0, k.texture, 16, 16, keepAspect = true).run {
                        GUIText(this, name + " consumer quantity", 2, 4, v.toString())
                    }
                }
                GUITexturePane(this, name + " arrow", i * 16 + 1, 0, Image.GUI.CRAFTING_ARROW)
                recipe.produce.forEach { k, v ->
                    GUITexturePane(this, name + " producer texture", (16 * i++) + Image.GUI.CRAFTING_ARROW.widthPixels + 2, 0, k.texture, 16, 16, keepAspect = true).run {
                        GUIText(this, name + " producer quantity", 2, 4, v.toString())
                    }
                }
                this.xAlignment = { (WIDTH - this.widthPixels) / 2 }
            }
        }
    }

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {

    }

    override fun render() {

    }

    companion object {
        val WIDTH = GUIButton.WIDTH
        val HEIGHT = GUIItemSlot.HEIGHT + 2
    }
}