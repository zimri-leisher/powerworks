package screen.gui

import crafting.Recipe
import crafting.RecipeCategory
import data.GameResourceManager
import graphics.Animation
import graphics.Image
import graphics.Texture
import graphics.TextureRenderParams
import screen.ScreenLayer
import screen.element.ElementRecipeList

object GuiRecipeSelector : Gui(ScreenLayer.MENU_2) {

    private var onSelect: (recipe: Recipe) -> Unit = {}

    var recipes: List<Recipe> = listOf()
        set(value) {
            if (field != value) {
                field = value
                categoryDisplays.forEach { category, display ->
                    display.recipes = value.filter { it.category == category }
                }
            }
        }

    lateinit var categoryDisplays: Map<RecipeCategory, ElementRecipeList>

    init {
        define {
            keepInsideScreen()
            open = false
            onClose {
                GuiRecipeDisplay.open = false
            }
            background {
                makeDraggable()
                list(horizontalAlign = HorizontalAlign.LEFT) {
                    tabs(2) {
                        for (category in RecipeCategory) {
                            val iconRenderable = category.iconType.icon
                            val textureRegion = if (iconRenderable is Texture) {
                                iconRenderable.region
                            } else if (iconRenderable is Animation) {
                                iconRenderable.currentFrame
                            } else {
                                Image.Misc.ERROR
                            }
                            tab("<size=40><img=${GameResourceManager.getIdentifier(textureRegion)}>") { index ->
                                GuiRecipeDisplay.open = false
                                categoryDisplays.entries.forEachIndexed { displayIndex, (_, display) ->
                                    display.parent.open = displayIndex == index
                                }
                            }
                        }
                    }
                    background(TextureRenderParams(brightness = 0.8f, rotation = 180f)) {
                        onMouseLeave {
                            GuiRecipeDisplay.open = false
                        }
                        dimensions = Dimensions.FitChildren.pad(4, 4)
                        val categories = mutableMapOf<RecipeCategory, ElementRecipeList>()
                        RecipeCategory.values().forEach { category ->
                            group(Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(2, -2)) {
                                open = category == RecipeCategory.values().first()
                                categories.put(category, recipeListDisplay(recipes.filter { it.category == category }, 6, 6, {
                                    this@GuiRecipeSelector.onSelect(it)
                                    this@GuiRecipeSelector.open = false
                                    GuiRecipeDisplay.open = false
                                }))
                            }
                        }
                        categoryDisplays = categories
                    }
                }
            }
        }
    }

    fun show(recipes: List<Recipe>, at: Placement.Exact, onSelect: (recipe: Recipe) -> Unit) {
        layer.bringToTop(this)
        GuiRecipeDisplay.open = false
        this.onSelect = onSelect
        parentElement.placement = at
        layout.recalculateExactPlacement(parentElement)
        this.recipes = recipes
        open = true
    }
}