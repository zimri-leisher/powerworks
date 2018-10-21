package screen.elements

import graphics.Renderer
import screen.WindowGroup

/**
 * A handy window that automatically puts elements in the order (y-down) that they are created, and adjusts the dimensions
 * accordingly. This is great for quickly creating GUIs that you want to have a 'top-down' flow.
 *
 * For example, the class
 * FurnaceBlockGUI extends this one. Inside of its initializer, it creates multiple elements in sequential order. To add
 * an element to this so that it will be formatted, you must either create the element with 'group' as its parent, which
 * gives it directly to the auto format group that underlies this, or you can use the add(GUIElement) method this class
 * provides, which has the same effect
 */
open class AutoFormatGUIWindow(name: String,
                               xAlignment: Alignment, yAlignment: Alignment,
                               windowGroup: WindowGroup,
                               open: Boolean = false, layer: Int = 0) :
        GUIWindow(name, xAlignment, yAlignment, { 0 }, { 0 }, windowGroup, open, layer) {

    val group = AutoFormatGUIGroup(this, name + " element group", { WIDTH_PADDING }, { 0 }, open, flipY = true, yPixelSeparation = 2, accountForChildHeight = true)

    init {
        group.alignments.y = { group.alignments.height() + HEIGHT_PADDING }
        alignments.width = { group.alignments.width() + 2 * WIDTH_PADDING }
        alignments.height = { group.alignments.height() + 2 * HEIGHT_PADDING }
    }

    /**
     * Adds an element to the bottom of this window. Dimensions (and thus background size) will be adjusted accordingly
     */
    fun add(el: GUIElement) {
        group.children.add(el)
    }

    override fun onChildDimensionChange(child: GUIElement) {
        alignments.updateDimension()
    }

    override fun render() {
        Renderer.renderDefaultRectangle(xPixel, yPixel, widthPixels, heightPixels)
    }

    companion object {
        const val WIDTH_PADDING = 2
        const val HEIGHT_PADDING = 2
    }
}