package screen.elements

class AutoFormatGUIGroup(parent: RootGUIElement,
                         name: String,
                         xAlignment: () -> Int, yAlignment: () -> Int,
                         open: Boolean = false,
                         layer: Int = parent.layer + 1,
                         initializerList: (GUIGroup.() -> Unit)? = null,
                         val yPixelSeparation: Int = 0,
                         val xPixelSeparation: Int = 0,
                         val accountForChildWidth: Boolean = false,
                         val accountForChildHeight: Boolean = false) :
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
                accountForChildHeight: Boolean = false) :
            this(parent, name, { relXPixel }, { relYPixel }, open, layer, initializerList, yPixelSeparation, xPixelSeparation, accountForChildWidth, accountForChildHeight)

    var nextYPixel = 0
    var nextXPixel = 0

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
        val x = nextXPixel
        val y = nextYPixel
        child.alignments.x = { x }
        child.alignments.y = { y }
        child.layer = layer + 1
        nextXPixel += (if (accountForChildWidth) child.widthPixels else 0) + xPixelSeparation
        nextYPixel += (if (accountForChildHeight) child.heightPixels else 0) + yPixelSeparation
        super.onAddChild(child)
    }
}