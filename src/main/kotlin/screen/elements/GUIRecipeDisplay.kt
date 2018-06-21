package screen.elements

import crafting.Recipe
import graphics.Image

class GUIRecipeDisplay(parent: RootGUIElement, name: String, xAlignment: () -> Int, yAlignment: () -> Int, recipe: Recipe? = null, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { WIDTH }, { HEIGHT }, open, layer) {

    var recipe = recipe
        set(value) {
            if (field != null && value == null) {
                icon.open = false
            } else if (field == null && value != null) {
                icon.open = true
                icon.texture = value.icon.icon
                consumeList.currentResources = value.consume
                consumeList.width = value.consume.size
                produceList.currentResources = value.produce
                produceList.width = value.produce.size
                mouseOverAreaOpenGroup.children.remove(background)
                background.alignments.updateDimension()
            } else if (field != value) {
                value!! // must not be null because null == null
                icon.texture = value.icon.icon
                consumeList.currentResources = value.consume
                consumeList.width = value.consume.size
                produceList.currentResources = value.produce
                produceList.width = value.produce.size
                mouseOverAreaOpenGroup.children.remove(background)
                background.alignments.updateDimension()
            }
            field = value
        }

    private lateinit var icon: GUITexturePane
    private lateinit var consumeList: GUIResourceListDisplay
    private lateinit var produceList: GUIResourceListDisplay
    private lateinit var mouseOverAreaOpenGroup: GUIGroup
    private lateinit var mouseOverArea: GUIMouseOverArea
    private lateinit var background: GUIDefaultTextureRectangle

    init {
        val fakeRecipe = recipe ?: Recipe.ERROR
        GUITexturePane(this, name + " background", 0, 0, Image.GUI.RECIPE_BUTTON_BACKGROUND).run {
            transparentToInteraction = true
            icon = GUITexturePane(this, name + " icon", 0, 0, fakeRecipe.icon.icon, 16, 16, keepAspect = true).apply {
                updateDimensionAlignmentOnTextureChange = false
                transparentToInteraction = true
                matchParentOpening = false
                mouseOverArea = GUIMouseOverArea(this, name + " mouse over info", { 0 }, { 0 }, this.alignments.width, this.alignments.height, {
                    transparentToInteraction = true
                    val mouseOver = this
                    mouseOverAreaOpenGroup = GUIGroup(this, "Open group", { 0 }, { 0 }, {
                        val consumeText = GUIText(this, "consume text", this@apply.widthPixels + 3, 1, "Consume:", layer = this.layer + 2)
                        consumeList = GUIResourceListDisplay(this, "consume list display", fakeRecipe.consume, { consumeText.alignments.x() }, { consumeText.alignments.y() + consumeText.heightPixels + 1 }, fakeRecipe.consume.size, 1)
                        val produceText = GUIText(this, "produce text", consumeText.alignments.x(), consumeList.alignments.y() + consumeList.heightPixels + 1, "Produce:", layer = this.layer + 2)
                        produceList = GUIResourceListDisplay(this, "produce list icons", fakeRecipe.produce, { consumeText.alignments.x() }, { produceText.alignments.y() + produceText.heightPixels + 1 }, fakeRecipe.produce.size, 1)
                    })
                    background = GUIDefaultTextureRectangle(mouseOver, this@GUIRecipeDisplay.name + " background", { this@apply.widthPixels + 1 }, { -1 }, { mouseOverAreaOpenGroup.widthPixels }, { mouseOverAreaOpenGroup.heightPixels })
                }).apply { transparentToInteraction = true }
            }
        }
    }

    override fun onOpen() {
        if (recipe != null)
            icon.open = true
    }

    companion object {
        val WIDTH = 16
        val HEIGHT = 16
    }
}