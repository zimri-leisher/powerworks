package screen.elements

import graphics.Renderer

class GUIElementList(parent: RootGUIElement, name: String, xAlignment: () -> Int, yAlignment: () -> Int, widthAlignment: () -> Int, heightAlignment: () -> Int, initializerList: GUIElementList.() -> Unit = {}, open: Boolean = false, layer: Int = parent.layer + 1) : GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer), VerticalScrollable {

    private val elements = AutoFormatGUIGroup(this, name + " auto format group", 0, 0, accountForChildHeight = true, yPixelSeparation = 2)

    override val viewHeightPixels
        get() = heightPixels
    override val maxHeightPixels: Int
        get() = elements.heightPixels

    private var scrollBar = GUIVerticalScrollBar(this, name + " scroll bar", { widthPixels - GUIVerticalScrollBar.WIDTH }, { 0 }, { heightPixels }, open, layer + 2)

    init {
        elements.autoRender = false
        elements.yAlignment = { (Math.min(0, heightPixels - elements.heightPixels) * (scrollBar.currentPos.toDouble() / scrollBar.maxPos)).toInt() }
        initializerList()
    }

    override fun onMouseScroll(dir: Int) {
        scrollBar.currentPos += dir * SCROLL_SENSITIVITY
    }

    override fun onAddChild(child: GUIElement) {
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
        elements.updateAlignment()
    }

    companion object {
        const val SCROLL_SENSITIVITY = 4
    }
}