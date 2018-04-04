package screen.elements

import crafting.Recipe
import graphics.Image
import graphics.Utils
import io.PressType
import misc.Numbers
import screen.RecipeSelectorGUI

class GUIRecipeButton(parent: RootGUIElement,
                      name: String,
                      xAlignment: () -> Int, yAlignment: () -> Int,
                      recipe: Recipe?, val onRecipeChange: (Recipe?) -> Unit = {}) : GUIElement(
        parent, name, xAlignment, yAlignment, { WIDTH }, { HEIGHT }) {

    var recipe = recipe
        set(value) {
            if (field != null && value == null) {
                button.open = false
                onRecipeChange(value)
            } else if (field == null && value != null) {
                consumeGroup.clear()
                produceGroup.clear()
                generateConsumeGroup(value, consumeGroup)
                generateProduceGroup(value, produceGroup)
                button.open = true
                button.texture = value.icon.texture
                onRecipeChange(value)
            } else if (field != value) {
                value!! // must not be null because null == null
                consumeGroup.clear()
                produceGroup.clear()
                generateConsumeGroup(value, consumeGroup)
                generateProduceGroup(value, produceGroup)
                button.texture = value.icon.texture
                onRecipeChange(value)
            }
            field = value
        }

    var waitingForRecipeSelection = false

    private lateinit var button: GUITexturePane
    private lateinit var consumeGroup: AutoFormatGUIGroup
    private lateinit var produceGroup: AutoFormatGUIGroup

    init {
        val fakeRecipe = recipe ?: Recipe.ERROR
        GUITexturePane(this, name + " background", 0, 0, Image.GUI.RECIPE_BUTTON_BACKGROUND).run {
            transparentToInteraction = true
            button = GUITexturePane(this, name + " icon", 1, 1, fakeRecipe.icon.texture, 16, 16, keepAspect = true).apply {
                updateDimensionAlignmentOnTextureChange = false
                transparentToInteraction = true
                matchParentOpening = false
                GUIMouseOverArea(this, name + " mouse over info", { 0 }, { 0 }, this.widthAlignment, this.heightAlignment, {
                    transparentToInteraction = true
                    val consumeText = GUIText(this, "consume text", this@apply.widthPixels + 3, 1, "Consume:", layer = this.layer + 2)
                    consumeGroup = AutoFormatGUIGroup(this, "consume icons", consumeText.xAlignment(), consumeText.yAlignment() + consumeText.heightPixels + 1, initializerList = {
                        transparentToInteraction = true
                        generateConsumeGroup(fakeRecipe, this)
                    }, accountForChildWidth = true, xPixelSeparation = 1, layer = this.layer + 2)
                    val produceText = GUIText(this, "produce text", consumeText.xAlignment(), consumeGroup.yAlignment() + consumeGroup.heightPixels + 1, "Produce:", layer = this.layer + 2)
                    produceGroup = AutoFormatGUIGroup(this, "produce icons", consumeText.xAlignment(), produceText.yAlignment() + produceText.heightPixels + 1, initializerList = {
                        transparentToInteraction = true
                        generateProduceGroup(fakeRecipe, this)
                    }, accountForChildWidth = true, xPixelSeparation = 1, layer = this.layer + 2)
                    GUITexturePane(this, this.name + " background", this@apply.widthPixels + 1, -1,
                            Image(Utils.genRectangle(
                                    Numbers.max(consumeText.widthPixels, produceText.widthPixels, consumeGroup.widthPixels, produceGroup.widthPixels) + 3,
                                    consumeGroup.heightPixels + produceGroup.heightPixels + produceText.heightPixels + consumeText.heightPixels + 6)))
                })
            }
        }
    }

    override fun onOpen() {
        if(recipe != null)
            button.open = true
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

    private fun generateConsumeGroup(recipe: Recipe, parent: GUIElement) =
            recipe.consume.forEach { resourceType, q ->
                GUIGroup(parent, "consume stack", { 0 }, { 0 }, {
                    GUITexturePane(this, "consume icon", 0, 0, resourceType.texture, 16, 16, keepAspect = true)
                    GUIText(this, "consume quantity", 0, 0, q)
                })
            }

    private fun generateProduceGroup(recipe: Recipe, parent: GUIElement) =
            recipe.produce.forEach { resourceType, q ->
                GUIGroup(parent, "produce stack", { 0 }, { 0 }, {
                    GUITexturePane(this, "produce icon", 0, 0, resourceType.texture, 16, 16, keepAspect = true)
                    GUIText(this, "produce quantity", 0, 0, q)
                })
            }

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED && button == 1) {
            RecipeSelectorGUI.open = true
            RecipeSelectorGUI.windowGroup.bringToTop(RecipeSelectorGUI)
            waitingForRecipeSelection = true
        }
    }

    companion object {
        val WIDTH = 18
        val HEIGHT = 18
    }
}