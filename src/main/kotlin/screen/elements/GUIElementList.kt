package screen.elements

import graphics.Renderer

class GUIElementList(parent: RootGUIElement, name: String, xAlignment: () -> Int, yAlignment: () -> Int, widthAlignment: () -> Int, heightAlignment: () -> Int, initializerList: GUIGroup.() -> Unit = {}, open: Boolean = false, layer: Int = parent.layer + 1) : GUIElement(parent, name, xAlignment, yAlignment, widthAlignment, heightAlignment, open, layer), VerticalScrollable {

    val elements = AutoFormatGUIGroup(this, name + " auto format group", 0, 0, initializerList = initializerList, yPixelSeparation = 2)

    override var viewHeightPixels = heightPixels
    override var maxHeightPixels: Int = elements.heightPixels
        get() = elements.heightPixels

    var scrollBar = GUIVerticalScrollBar(this, name + " scroll bar", widthPixels - GUIVerticalScrollBar.WIDTH, 0, heightPixels, open, layer + 2)

    init {
        elements.autoRender = false
    }

    override fun onMouseScroll(dir: Int) {
        scrollBar.currentPos += dir * SCROLL_SENSITIVITY
    }

    override fun render() {
        Renderer.setClip(xPixel, yPixel, widthPixels, heightPixels)
        elements.children.stream().filter { it.open }.sorted { o1, o2 -> o1.layer.compareTo(o2.layer) }.forEach { it.render() }
        Renderer.resetClip()
    }

    override fun onScroll() {
        elements.yAlignment = { (Math.min(0, heightPixels - elements.heightPixels) * (scrollBar.currentPos.toDouble() / scrollBar.maxPos)).toInt() }
    }

    companion object {
        const val SCROLL_SENSITIVITY = 4
    }
}