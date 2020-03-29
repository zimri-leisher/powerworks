package screen.elements

/**
 * A group of GUIElements. The dimensions of this are automatically adjusted to be the smallest square surrounding
 * all child elements. When a child is added, it is given a position determined by the separation, width, height and
 * flipX/Y parameters (see their documentations for details)
 */
class AutoFormatGUIGroup(parent: RootGUIElement,
                         name: String,
                         xAlignment: Alignment, yAlignment: Alignment,
                         open: Boolean = false,
                         layer: Int = parent.layer + 1,
                         /**
                          * Run inside the closure of this AutoFormatGUIGroup. This is handy to create elements at
                          * instantiation-time, meaning that all dimensions will be correct from the start. There is no
                          * difference between creating an element in here with this as its parent and creating an
                          * element outside of here but adding that element to this's child list (or setting that
                          * element's parent to this)
                          */
                         initializerList: (GUIGroup.() -> Unit)? = null,
                         /**
                          * The amount to separate each element on the x axis - note, this doesn't include width or height
                          */
                         var xPixelSeparation: Int = 0,
                         /**
                          * The amount to separate each element on the y axis - note, this doesn't include width or height
                          */
                         var yPixelSeparation: Int = 0,
                         /**
                          * Whether or not to separate elements by width (in addition to the xPixelSeparation)
                          */
                         var accountForChildWidth: Boolean = false,
                         /**
                          * Whether or not to separate elements by height (in addition to the yPixelSeparation)
                          */
                         var accountForChildHeight: Boolean = false,
                         /**
                          * Whether to reverse the x axis. Elements will be added starting from the right and going in
                          * the negative to the left
                          */
                         var flipX: Boolean = false,
                         /**
                          * Whether to reverse the y axis. Elements will be added starting from the top and going in
                          * the negative to the bottom
                          */
                         var flipY: Boolean = false) :
        GUIGroup(parent, name, xAlignment, yAlignment, {}, open, layer) {

    constructor(parent: RootGUIElement,
                name: String,
                relXPixel: Int, relYPixel: Int,
                open: Boolean = false,
                layer: Int = parent.layer + 1,
                initializerList: (GUIGroup.() -> Unit)? = null,
                xPixelSeparation: Int = 0,
                yPixelSeparation: Int = 0,
                accountForChildWidth: Boolean = false,
                accountForChildHeight: Boolean = false,
                flipX: Boolean = false,
                flipY: Boolean = false) :
            this(parent, name, { relXPixel }, { relYPixel }, open, layer, initializerList, xPixelSeparation, yPixelSeparation, accountForChildWidth, accountForChildHeight, flipX, flipY)

    private var nextXPixel = 0
    private var nextYPixel = 0

    init {
        if (initializerList != null)
            initializerList()
        updateDimensions()
    }

    /**
     * Removes all children. The next child will go to the default position
     */
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