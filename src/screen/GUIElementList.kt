package screen

import graphics.Renderer

class GUIElementList(parent: RootGUIElement? = RootGUIElementObject, name: String, xPixel: Int, yPixel: Int, widthPixels: Int, heightPixels: Int, layer: Int = (parent?.layer ?: 0) + 1) : GUIElement(parent, name, xPixel, yPixel, widthPixels, heightPixels, layer), VerticalScrollable {

    val elements = AutoFormatGUIGroup(this, name + " auto format group", 0, 0, yPixelSeparation = 2)

    override var viewHeightPixels = heightPixels
    override var maxHeightPixels: Int = elements.heightPixels
        get() = elements.heightPixels

    var scrollBar = GUIVerticalScrollBar(this, name + " scroll bar", widthPixels - GUIVerticalScrollBar.DEFAULT_WIDTH, 0, heightPixels, layer + 2)

    init {
        elements.autoRender = false
    }

    override fun onAddChild(child: GUIElement) {
        if(child.name != name + " auto format group" && child.name != name + " scroll bar") {
            children.remove(child)
            elements.children.add(child)
            scrollBar.updateScrollBarHeight()
        }
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
        elements.relYPixel = (Math.min(0, heightPixels - elements.heightPixels) * (scrollBar.currentPos.toDouble() / scrollBar.maxPos)).toInt()
    }

    companion object {
        const val SCROLL_SENSITIVITY = 4
    }
}