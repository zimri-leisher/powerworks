package screen

class AutoFormatGUIGroup(parent: RootGUIElement,
                         name: String,
                         xAlignment: () -> Int, yAlignment: () -> Int,
                         open: Boolean = false,
                         layer: Int = parent.layer + 1,
                         val yPixelSeparation: Int = 0,
                         val xPixelSeparation: Int = 0,
                         val accountForChildWidth: Boolean = false,
                         val accountForChildHeight: Boolean = false) :
        GUIGroup(parent, name, xAlignment, yAlignment, open, layer) {

    constructor(parent: RootGUIElement,
                name: String,
                relXPixel: Int, relYPixel: Int,
                open: Boolean = false,
                layer: Int = parent.layer + 1,
                yPixelSeparation: Int = 0,
                xPixelSeparation: Int = 0,
                accountForChildWidth: Boolean = false,
                accountForChildHeight: Boolean = false) :
            this(parent, name, { relXPixel }, { relYPixel }, open, layer, yPixelSeparation, xPixelSeparation, accountForChildWidth, accountForChildHeight)

    var nextYPixel = 0
    var nextXPixel = 0

    override fun onAddChild(child: GUIElement) {
        child.updatePosition = false
        val y = nextYPixel
        val x = nextXPixel
        child.yAlignment = { y }
        child.xAlignment = { x }
        child.layer = layer + 1
        nextYPixel += (if(accountForChildHeight) child.heightPixels else 0) + yPixelSeparation
        nextXPixel += (if(accountForChildWidth) child.widthPixels else 0) + xPixelSeparation
        super.onAddChild(child)
    }
}