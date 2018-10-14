package screen.elements

import graphics.Renderer

/**
 * A list of GUIElements that is scrollable. To add elements, either create them inside of the initializerList closure
 * or use the add(GUIElement) method. Note - adding elements to this element's children won't add them to the list
 */
class GUIElementList(parent: RootGUIElement, name: String,
                     xAlignment: Alignment, yAlignment: Alignment,
                     widthAlignment: Alignment, heightAlignment: Alignment,
                     initializerList: GUIGroup.() -> Unit = {},
                     open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer), VerticalScrollable {

    private val elements = AutoFormatGUIGroup(this, name + " auto format group", 0, heightPixels, initializerList = initializerList, accountForChildHeight = true, yPixelSeparation = 2, flipY = true)

    private var scrollBar = GUIVerticalScrollBar(this, name + " scroll bar", { widthPixels - GUIVerticalScrollBar.WIDTH }, { 0 }, { heightPixels }, open, layer + 2)

    override val viewHeightPixels
        get() = heightPixels

    override val maxHeightPixels: Int
        get() = elements.heightPixels

    init {
        elements.autoRender = false
        scrollBar.updateScrollBarHeight()
        elements.alignments.y = { heightPixels - (Math.min(0, heightPixels - elements.heightPixels) * (scrollBar.currentPos.toDouble() / scrollBar.maxPos)).toInt() }
    }

    /**
     * Adds an element to the list, at the bottom
     */
    fun add(el: GUIElement) {
        elements.children.add(el)
    }

    override fun onChildDimensionChange(child: GUIElement) {
        if (child == elements) {
            scrollBar.updateScrollBarHeight()
        }
    }

    override fun onScroll(dir: Int) {
        scrollBar.currentPos += dir * SCROLL_SENSITIVITY
    }

    override fun render() {
        Renderer.setClip(xPixel, yPixel, widthPixels, heightPixels)

        fun recursivelyRender(e: GUIElement) {
            e.render()
            e.children.stream().filter { it.open }.sorted { o1, o2 -> o1.layer.compareTo(o2.layer) }.forEach { recursivelyRender(it) }
        }

        elements.children.stream().filter { it.open }.sorted { o1, o2 -> o1.layer.compareTo(o2.layer) }.forEach { recursivelyRender(it) }
        Renderer.resetClip()
    }

    override fun onScroll() {
        elements.alignments.updatePosition()
    }

    companion object {
        const val SCROLL_SENSITIVITY = 4
    }
}