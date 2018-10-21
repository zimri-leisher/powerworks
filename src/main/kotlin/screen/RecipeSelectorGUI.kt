package screen

import com.badlogic.gdx.Input
import crafting.Recipe
import crafting.RecipeCategory
import data.ResourceManager
import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import graphics.text.TaggedText
import graphics.text.TextManager
import io.PressType
import main.toColor
import screen.elements.*

/**
 * A handy little thing that any class can open up and accept a choice from
 * The best way to get the choice is calling getSelected() in your update method
 */
object RecipeSelectorGUI : GUIWindow("Recipe selector", 20, 20, 100, 120, ScreenManager.Groups.PLAYER_UTIL) {

    private class GUIRecipeSelectionButton(parent: RootGUIElement, name: String, xAlignment: Alignment, yAlignment: Alignment, val recipe: Recipe, open: Boolean = false, layer: Int = parent.layer + 1) :
            GUIElement(parent, name, xAlignment, yAlignment, { GUIRecipeDisplay.WIDTH }, { GUIRecipeDisplay.HEIGHT }, open, layer + 2) {

        private val recipeDisplay = GUIRecipeDisplay(this, "Recipe (icon: ${recipe.iconType}) display", { 0 }, { 0 }, recipe, layer = layer - 1, open = open).apply { transparentToInteraction = true }

        override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
            if (isAvailable()) {
                if (type == PressType.PRESSED && button == Input.Buttons.LEFT) {
                    RecipeSelectorGUI.selected = recipeDisplay.recipe
                    recipeDisplay.background.localRenderParams.brightness = 0.9f
                    recipeDisplay.background.localRenderParams.rotation = 180f
                } else if (type == PressType.RELEASED) {
                    recipeDisplay.background.localRenderParams.brightness = 1.1f
                }
            }
        }

        fun isAvailable() = RecipeSelectorGUI.available(recipe)

        override fun render() {
            if (!isAvailable()) {
                Renderer.renderTexture(Image.GUI.WHITE_FILLER, xPixel, yPixel, widthPixels, heightPixels, TextureRenderParams(color = toColor(r = 1f, g = 0f, b = 0f, a = 0.3f)))
            }
        }

        override fun onMouseEnter() {
            if (isAvailable()) {
                recipeDisplay.background.localRenderParams.brightness = 1.1f
            }
        }

        override fun onMouseLeave() {
            if (isAvailable()) {
                recipeDisplay.background.localRenderParams.brightness = 1f
                recipeDisplay.background.localRenderParams.rotation = 0f
            }
        }
    }

    private const val RECIPIES_PER_ROW = 6

    private val tabs: Array<GUIDefaultTextureRectangle>

    var available: (Recipe) -> Boolean = { true }

    var selected: Recipe? = null
        private set
        /**
         * Gets the last selected recipe
         *
         * NOTE - It clears itself after you call this and it returns non-null
         */
        get() {
            if (field != null) {
                val s = field
                field = null
                return s
            }
            return null
        }

    init {
        openAtMouse = true
        partOfLevel = true

        val tagTexts = mutableListOf<TaggedText>()
        var i = 0
        val tabList = mutableListOf<GUIDefaultTextureRectangle>()
        for (category in RecipeCategory) {
            tagTexts.add(TextManager.parseTags("<size=40><img=${ResourceManager.getIdentifier(category.iconType.icon)}>"))

            GUIDefaultTextureRectangle(this, "recipe category background $i", 0, 0, heightPixels = heightPixels - GUITabList.TAB_HEIGHT, open = i == 0).apply {

                matchParentOpening = false

                for ((recipeIndex, recipe) in category.iterator().withIndex()) {

                    GUIRecipeSelectionButton(this, "Recipe $recipeIndex display", { (recipeIndex % RECIPIES_PER_ROW) * GUIRecipeDisplay.WIDTH + 2 }, { heightPixels - ((recipeIndex / RECIPIES_PER_ROW) + 1) * GUIRecipeDisplay.HEIGHT - 2 }, recipe, open)

                }
                tabList.add(this)
            }
            i++
        }
        tabs = tabList.toTypedArray()
        GUITabList(this, "tab list of recipe categories", { 0 }, { heightPixels - GUITabList.TAB_HEIGHT },
                tagTexts.mapIndexed { index, taggedText -> Tab("$index", taggedText, RecipeCategory.values()[index].categoryName) }.toTypedArray(), { id ->
            val index = id.toInt()
            tabs.forEachIndexed { i, tab -> tab.open = i == index }
        })
        generateCloseButton()
        generateDragGrip()
    }

    override fun onOpen() {
        tabs[0].open = true
    }
}