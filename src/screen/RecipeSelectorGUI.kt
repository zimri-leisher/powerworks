package screen

import crafting.Recipe
import graphics.Image
import graphics.Utils
import io.PressType
import screen.elements.GUIClickableRegion
import screen.elements.GUIText
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

/**
 * A handy little thing that any class can open up and accept a choice from
 * The best way to get the choice is calling getSelected() in your update method
 */
object RecipeSelectorGUI : GUIWindow("Recipe selector", 20, 20, 100, 120, windowGroup = ScreenManager.Groups.PLAYER_UTIL) {

    private const val RECIPIES_PER_ROW = 6
    const val RECIPE_WIDTH = 16
    const val RECIPE_HEIGHT = 16

    private var selected: Recipe? = null

    init {
        val background = GUITexturePane(this.rootChild, "Background", 0, 0, Image(Utils.genRectangle(widthPixels, heightPixels)))
        GUIText(background, "Name text", 0, 0, "Select a recipe:")
        for ((i, recipe) in Recipe.ALL.withIndex()) {
            val icon = GUITexturePane(background, "Recipe $i icon", (i % RECIPIES_PER_ROW) * RECIPE_WIDTH + 1, (i / RECIPIES_PER_ROW) * RECIPE_HEIGHT + 6, recipe.icon.texture, RECIPE_WIDTH, RECIPE_HEIGHT, keepAspect = true)
            GUIClickableRegion(icon, "Recipe $i click region", { 0 }, { 0 }, { RECIPE_WIDTH }, { RECIPE_HEIGHT }, { pressType, _, _, button, shift, ctrl, alt ->
                if (pressType == PressType.PRESSED && button == 1) {
                    selected = recipe
                }
            })
        }
    }

    override fun onOpen() {
        val x = Mouse.xPixel
        val y = Mouse.yPixel
        xAlignment = { x }
        yAlignment = { y }

    }

    /**
     * Gets the last selected recipe
     *
     * *NOTE*: It clears itself after you call this and it returns non-null
     */
    fun getSelected(): Recipe? {
        if (selected != null) {
            val s = selected
            selected = null
            return s
        }
        return null
    }
}