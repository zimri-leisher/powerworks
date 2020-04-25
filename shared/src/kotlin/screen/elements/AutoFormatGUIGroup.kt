package screen.elements

import com.badlogic.gdx.math.Rectangle
import misc.PixelCoord

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
                          * The amount to separate each element
                          */
                         var padding: Int = 2,
                         var dir: Int = 0) :
        GUIGroup(parent, name, xAlignment, yAlignment, open, layer) {

    private val originalXAlignment = xAlignment
    private val originalYAlignment = yAlignment

    /**
     * A group of GUIElements. The dimensions of this are automatically adjusted to be the smallest square surrounding
     * all child elements
     */
    constructor(parent: RootGUIElement,
                name: String,
                xPixel: Int, yPixel: Int,
                open: Boolean = false,
                layer: Int = parent.layer + 1,
                spacing: Int = 2,
                dir: Int = 0) :
            this(parent, name, { xPixel }, { yPixel }, open, layer, spacing, dir)

    private var nextXPixel = 0
    private var nextYPixel = 0

    fun reformat() {
        nextXPixel = 0
        nextYPixel = 0
        val positions = mutableListOf<Pair<GUIElement, PixelCoord>>()
        for (child in children) {
            val childX = getChildX(child)
            val childY = getChildY(child)
            positions.add(child to PixelCoord(childX, childY))
        }
        val r = Rectangle()
        for ((child, position) in positions) {
            r.merge(Rectangle(position.xPixel.toFloat(), position.yPixel.toFloat(), child.widthPixels.toFloat(), child.heightPixels.toFloat()))
        }
        val thisX = r.x.toInt()
        val thisY = r.y.toInt()
        val thisWidth = r.width.toInt()
        val thisHeight = r.height.toInt()
        alignments.x = { originalXAlignment() + thisX }
        alignments.y = { originalYAlignment() + thisY }
        alignments.width = { thisWidth }
        alignments.height = { thisHeight }
        for ((child, position) in positions) {
            val childX = position.xPixel
            val childY = position.yPixel
            child.alignments.x = { childX + if (dir == 3) thisWidth else 0 }
            child.alignments.y = { childY + if (dir == 2) thisHeight else 0 }
        }
    }

    private fun getChildX(child: GUIElement): Int {
        var x = nextXPixel
        if (dir == 1) {
            // elements should be added on the right of this element
            nextXPixel += padding + child.widthPixels
        } else if (dir == 3) {
            // elements should be added on the left of this element
            x -= child.widthPixels
            nextXPixel -= padding + child.widthPixels
        }
        return x
    }

    private fun getChildY(child: GUIElement): Int {
        var y = nextYPixel
        if (dir == 0) {
            // elements should be added on the top of this element
            nextYPixel += padding + child.heightPixels
        } else if (dir == 2) {
            // elements should be added on the bottom of this element
            y -= child.heightPixels
            nextYPixel -= padding + child.heightPixels
        }
        return y
    }

    override fun onRemoveChild(child: GUIElement) {
        reformat()
    }

    override fun onAddChild(child: GUIElement) {
        reformat()
    }

    override fun onChildDimensionChange(child: GUIElement) {
        reformat()
    }
}