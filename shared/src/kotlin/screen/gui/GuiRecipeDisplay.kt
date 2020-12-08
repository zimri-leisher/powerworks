package screen.gui

import crafting.Recipe
import graphics.Image
import graphics.TextureRenderParams
import screen.Interaction
import screen.ScreenLayer
import screen.element.ElementResourceList

object GuiRecipeDisplay : Gui(ScreenLayer.MENU_3) {

    private var recipe = Recipe.ERROR
        set(value) {
            if (field != value) {
                field = value
                consume.resources = value.consume
                consume.columns = value.consume.size
                consume.iconCount = value.consume.size
                produce.resources = value.produce
                produce.columns = value.produce.size
                produce.iconCount = value.produce.size
                layout.set()
            }
        }
    private lateinit var consume: ElementResourceList
    private lateinit var produce: ElementResourceList

    init {
        open = false
        define {
            onMouseLeave {
                open = false
            }
            background {
                dimensions = Dimensions.FitChildren.pad(4, 4)
                list(Placement.Align.Center) {
                    consume = resourceList(recipe.consume)
                    texture(Image.Misc.THIN_ARROW, params = TextureRenderParams(rotation = 180f))
                    produce = resourceList(recipe.produce)
                }
            }
        }
    }

    fun show(recipe: Recipe, at: Placement.Exact, onInteractOn: GuiElement.(interaction: Interaction) -> Unit) {
        parentElement.eventListeners.removeIf { it is GuiInteractOnListener }
        parentElement.eventListeners.add(GuiInteractOnListener(onInteractOn))
        layer.bringToTop(this)
        parentElement.placement = at
        this.recipe = recipe
        open = true
    }
}