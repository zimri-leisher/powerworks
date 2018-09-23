package screen.elements

class AutoFormatGUIGroup(parent: RootGUIElement,
                         name: String,
                         xAlignment: Alignment, yAlignment: Alignment,
                         open: Boolean = false,
                         layer: Int = parent.layer + 1,
                         initializerList: (GUIGroup.() -> Unit)? = null,
                         var yPixelSeparation: Int = 0,
                         var xPixelSeparation: Int = 0,
                         var accountForChildWidth: Boolean = false,
                         var accountForChildHeight: Boolean = false,
                         var flipX: Boolean = false,
                         var flipY: Boolean = false) :
        GUIGroup(parent, name, xAlignment, yAlignment, {}, open, layer) {

    constructor(parent: RootGUIElement,
                name: String,
                relXPixel: Int, relYPixel: Int,
                open: Boolean = false,
                layer: Int = parent.layer + 1,
                initializerList: (GUIGroup.() -> Unit)? = null,
                yPixelSeparation: Int = 0,
                xPixelSeparation: Int = 0,
                accountForChildWidth: Boolean = false,
                accountForChildHeight: Boolean = false,
                flipX: Boolean = false,
                flipY: Boolean = false) :
            this(parent, name, { relXPixel }, { relYPixel }, open, layer, initializerList, yPixelSeparation, xPixelSeparation, accountForChildWidth, accountForChildHeight, flipX, flipY)

    var nextXPixel = 0
    var nextYPixel = 0

    init {
        if (initializerList != null)
            initializerList()
        updateDimensions()
    }

    fun clear() {
        children.clear()
        nextXPixel = 0
        nextXPixel = 0
    }

    override fun onAddChild(child: GUIElement) {
        var x = nextXPixel
        var y = nextYPixel
        val xDiff = xPixelSeparation + if(accountForChildWidth) child.widthPixels else 0
        val yDiff = yPixelSeparation + if(accountForChildHeight) child.heightPixels else 0
        if(flipY) {
            y -= child.heightPixels
            nextYPixel -= yDiff
        } else {
            nextYPixel += yDiff
        }
        if(flipX) {
            x -= child.widthPixels
            nextXPixel -= xDiff
        } else {
            nextXPixel += xDiff
        }
        child.alignments.x = { x }
        child.alignments.y = { y }
        child.layer = layer + 1
        super.onAddChild(child)
    }
}