package screen.elements

import crafting.Recipe
import graphics.Renderer
import graphics.Texture

class GUIRecipeDisplay(parent: RootGUIElement, name: String, xAlignment: Alignment, yAlignment: Alignment, recipe: Recipe? = null, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { WIDTH }, { HEIGHT }, open, layer) {

    var recipe = recipe
        set(value) {
            if (field != null && value == null) {
                icon.open = false
            } else if (field == null && value != null) {
                icon.open = true
                icon.renderable = value.iconType.icon
                consumeList.currentResources = value.consume
                consumeList.width = value.consume.size
                produceList.currentResources = value.produce
                produceList.width = value.produce.size
                mouseOverAreaOpenGroup.children.remove(popupBackground)
                popupBackground.alignments.updateDimension()
            } else if (field != value) {
                value!! // must not be null because null == null
                icon.renderable = value.iconType.icon
                consumeList.currentResources = value.consume
                consumeList.width = value.consume.size
                produceList.currentResources = value.produce
                produceList.width = value.produce.size
                mouseOverAreaOpenGroup.children.remove(popupBackground)
                popupBackground.alignments.updateDimension()
            }
            field = value
        }

    private var icon: GUITexturePane
    private lateinit var consumeList: GUIResourceListDisplay
    private lateinit var produceList: GUIResourceListDisplay
    private lateinit var mouseOverAreaOpenGroup: GUIGroup
    private var mouseOverArea: GUIMouseOverPopupArea
    private lateinit var popupBackground: GUIDefaultTextureRectangle
    val background: GUIDefaultTextureRectangle

    init {
        val fakeRecipe = recipe ?: Recipe.ERROR

        background = GUIDefaultTextureRectangle(this, name + " background", 0, 0, GUIRecipeButton.WIDTH, GUIRecipeButton.HEIGHT, open).apply {

            transparentToInteraction = true

            icon = GUITexturePane(this, name + " icon", 0, heightPixels - 16, fakeRecipe.iconType.icon, 16, 16, keepAspect = true, open = open).apply {

                updateDimensionAlignmentOnTextureChange = false
                transparentToInteraction = true
                matchParentOpening = false

                mouseOverArea = GUIMouseOverPopupArea(this, name + " mouse over info", { 0 }, { 0 }, this.alignments.width, this.alignments.height, {

                    transparentToInteraction = true
                    val mouseOver = this

                    mouseOverAreaOpenGroup = GUIGroup(this, "Open group", { 0 }, { 0 }, {

                        produceList = GUIResourceListDisplay(this, "produce list icons", fakeRecipe.produce, { this@apply.widthPixels + 3 }, { 1 }, fakeRecipe.produce.size, 1)

                        val produceText = GUIText(this, "produce text", produceList.alignments.x(), produceList.alignments.y() + produceList.heightPixels + 2, "Produce:", layer = this.layer + 2)

                        consumeList = GUIResourceListDisplay(this, "consume list display", fakeRecipe.consume, { produceList.alignments.x() }, { produceText.alignments.y() + produceText.heightPixels + 1 }, fakeRecipe.consume.size, 1)

                        GUIText(this, "consume text", produceList.alignments.x(), consumeList.alignments.y() + consumeList.heightPixels + 2, "Consume:", layer = this.layer + 2)

                    })

                    popupBackground = GUIDefaultTextureRectangle(mouseOver, this@GUIRecipeDisplay.name + " background", { this@apply.widthPixels + 1 }, { -1 }, { mouseOverAreaOpenGroup.widthPixels }, { mouseOverAreaOpenGroup.heightPixels })

                }, open).apply { transparentToInteraction = true }
            }
        }
    }

    override fun render() {
        Renderer.renderText("test", xPixel, yPixel)
    }

    override fun onOpen() {
        if (recipe != null)
            icon.open = true
    }

    companion object {
        const val WIDTH = 16
        const val HEIGHT = 16
    }
}