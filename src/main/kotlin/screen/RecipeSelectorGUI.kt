package screen

import com.badlogic.gdx.Input
import crafting.Recipe
import graphics.text.TextManager
import io.PressType
import screen.elements.*

private class GUIRecipeSelectionButton(parent: GUIElement, name: String, xAlignment: Alignment, yAlignment: Alignment, recipe: Recipe, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { GUIRecipeDisplay.WIDTH }, { GUIRecipeDisplay.HEIGHT }, open, layer) {
    init {
        GUIRecipeDisplay(this, "Recipe (icon: ${recipe.iconType}) display", xAlignment, yAlignment, recipe)
    }

    override fun onMouseEnter() {

    }
}

/**
 * A handy little thing that any class can open up and accept a choice from
 * The best way to get the choice is calling getSelected() in your update method
 */
object RecipeSelectorGUI : GUIWindow("Recipe selector", 20, 20, 100, 120, ScreenManager.Groups.PLAYER_UTIL) {

    private const val RECIPIES_PER_ROW = 6

    private var selected: Recipe? = null

    init {
        openAtMouse = true
        partOfLevel = true

        val background = GUIDefaultTextureRectangle(this, "Background", 0, 0)

        GUIText(background, "Name text", 1, heightPixels - TextManager.getFont().charHeight.toInt() - 1, "Select a recipe:")

        for ((i, recipe) in Recipe.ALL.withIndex()) {

            val display = GUIRecipeDisplay(background, "Recipe $i display", { (i % RECIPIES_PER_ROW) * GUIRecipeDisplay.WIDTH + 1 }, { heightPixels - ((i / RECIPIES_PER_ROW) + 1) * GUIRecipeDisplay.HEIGHT - 6 }, recipe)

            GUIClickableRegion(display, "Recipe $i click region", { 0 }, { 0 }, { GUIRecipeDisplay.WIDTH }, { GUIRecipeDisplay.HEIGHT }, { pressType, _, _, button, _, _, _ ->
                if (pressType == PressType.PRESSED && button == Input.Buttons.LEFT) {
                    selected = recipe
                }
            }, layer = display.layer + 2)

        }
        generateCloseButton(background.layer + 1)
        generateDragGrip(background.layer + 1)
    }

    /**
     * Gets the last selected recipe
     *
     * NOTE - It clears itself after you call this and it returns non-null
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