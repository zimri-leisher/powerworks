package screen.elements

import crafting.Recipe

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
                consumeList.columns = value.consume.size
                consumeList.iconCount = value.consume.size
                produceList.currentResources = value.produce
                produceList.columns = value.produce.size
                produceList.iconCount = value.produce.size
                mouseOverAreaOpenGroup.children.remove(popupBackground)
                mouseOverAreaOpenGroup.updateDimensions()
                popupBackground.alignments.updateDimension()
            } else if (field != value) {
                value!! // must not be null because null == null
                icon.renderable = value.iconType.icon
                consumeList.currentResources = value.consume
                consumeList.columns = value.consume.size
                consumeList.iconCount = value.consume.size
                produceList.currentResources = value.produce
                produceList.columns = value.produce.size
                produceList.iconCount = value.produce.size
                mouseOverAreaOpenGroup.children.remove(popupBackground)
                mouseOverAreaOpenGroup.updateDimensions()
                popupBackground.alignments.updateDimension()
            }
            field = value
        }

    private var icon: GUITexturePane
    private var consumeList: GUIResourceListDisplay
    private var produceList: GUIResourceListDisplay
    private var mouseOverAreaOpenGroup: GUIGroup
    private var mouseOverArea: GUIMouseOverPopupArea
    private var popupBackground: GUIDefaultTextureRectangle
    val background: GUIDefaultTextureRectangle

    init {
        val fakeRecipe = recipe ?: Recipe.ERROR

        background = GUIDefaultTextureRectangle(this, name + " background", 0, 0, GUIRecipeButton.WIDTH, GUIRecipeButton.HEIGHT, open).apply {

            transparentToInteraction = true

            icon = GUITexturePane(this, name + " icon", 0, heightPixels - 16, fakeRecipe.iconType.icon, 16, 16, keepAspect = true, open = open).apply {

                updateDimensionAlignmentOnTextureChange = false
                transparentToInteraction = true
                matchParentOpening = false

                mouseOverArea = GUIMouseOverPopupArea(this, name + " mouse over info", { 0 }, { 0 }, this.alignments.width, this.alignments.height, open = open).apply {

                    val mouseOver = this

                    mouseOverAreaOpenGroup = GUIGroup(this, "Open group", { this@apply.widthPixels + 3 }, { -1 }).apply {
                        produceList = GUIResourceListDisplay(this, "produce list icons",
                                fakeRecipe.produce,
                                { 0 }, { 1 },
                                fakeRecipe.produce.size, 1)

                        val produceText = GUIText(this, "produce text",
                                0, produceList.alignments.y() + produceList.heightPixels + 1,
                                "Produce:",
                                layer = this.layer + 2)

                        consumeList = GUIResourceListDisplay(this, "consume list display",
                                fakeRecipe.consume,
                                { 0 }, { produceText.alignments.y() + produceText.heightPixels + 1 },
                                fakeRecipe.consume.size, 1)

                        GUIText(this, "consume text",
                                0, consumeList.alignments.y() + consumeList.heightPixels + 1,
                                "Consume:",
                                layer = this.layer + 2)
                    }

                    popupBackground = GUIDefaultTextureRectangle(mouseOver,
                            this@GUIRecipeDisplay.name + " background",
                            { this@apply.widthPixels + 1 }, { -2 },
                            { mouseOverAreaOpenGroup.alignments.width() + 4 }, { mouseOverAreaOpenGroup.alignments.height() + 4 })

                }
            }
        }
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