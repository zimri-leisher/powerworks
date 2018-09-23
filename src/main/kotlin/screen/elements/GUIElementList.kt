package screen.elements

import graphics.Renderer

class GUIElementList(parent: RootGUIElement, name: String,
                     xAlignment: Alignment, yAlignment: Alignment,
                     widthAlignment: Alignment, heightAlignment: Alignment,
                     initializerList: GUIElementList.() -> Unit = {},
                     open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer), VerticalScrollable {

    private val elements = AutoFormatGUIGroup(this, name + " auto format group", 0, heightPixels, accountForChildHeight = true, yPixelSeparation = 2, flipY = true)

    override val viewHeightPixels
        get() = heightPixels
    override val maxHeightPixels: Int
        get() = elements.heightPixels

    private var scrollBar = GUIVerticalScrollBar(this, name + " scroll bar", { widthPixels - GUIVerticalScrollBar.WIDTH }, { 0 }, { heightPixels }, open, layer + 2)

    init {
        initializerList()
        elements.autoRender = false
        elements.alignments.y = { heightPixels - (Math.min(0, heightPixels - elements.heightPixels) * (scrollBar.currentPos.toDouble() / scrollBar.maxPos)).toInt() }
    }

    override fun onScroll(dir: Int) {
        scrollBar.currentPos += dir * SCROLL_SENSITIVITY
    }

    override fun onAddChild(child: GUIElement) {
        // change this from name to id later
        if (child.name != name + " auto format group" && child.name != name + " scroll bar") {
            elements.children.add(child)
            scrollBar.updateScrollBarHeight()
        }
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