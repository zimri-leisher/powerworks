package screen.elements

import graphics.Renderer
import graphics.text.TaggedText
import graphics.text.TextManager
import io.PressType

data class Tab(val id: String, val text: TaggedText) {
    constructor(id: String, text: String) : this(id, TextManager.parseTags(text))
    constructor(id: String) : this(id, id)
}

class GUITabList(parent: RootGUIElement, name: String, xAlignment: Alignment, yAlignment: Alignment,
                 private val tabs: Array<Tab> = arrayOf(),
                 val onSelectTab: (tabID: String) -> Unit = {}, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { 0 }, { TAB_HEIGHT }, open, layer) {

    private var selectedTabIndex = 0
    private val tabWidths: Array<Int>
    var selectedTab = tabs[0]

    init {
        val w = arrayOfNulls<Int>(tabs.size)
        for ((index, t) in tabs.withIndex()) {
            w[index] = TextManager.getStringWidth(t.text)
        }
        tabWidths = w.requireNoNulls()
        alignments.width = { tabWidths.sum() }
    }

    override fun onInteractOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (type == PressType.PRESSED) {
            var selectedIndex = -1
            var relXPixel = xPixel - this.xPixel
            for ((index, width) in tabWidths.withIndex()) {
                if (relXPixel + (if (index == selectedTabIndex - 1) 2 else 0) - width < 0) {
                    selectedIndex = index
                    break
                } else {
                    relXPixel -= width
                }
            }
            if (selectedIndex != -1) {
                if (selectedIndex != selectedTabIndex) {
                    selectedTabIndex = selectedIndex
                    selectedTab = tabs[selectedIndex]
                    onSelectTab(selectedTab.id)
                }
            }
        }
    }

    override fun render() {
        localRenderParams.brightness = 0.8f
        var nextXPixel = 0
        for ((index, t) in tabs.withIndex()) {
            val bounds = TextManager.getStringBounds(t.text)
            if (index != selectedTabIndex) {
                Renderer.renderDefaultRectangle(xPixel + nextXPixel, yPixel, bounds.width + 2, TAB_HEIGHT, localRenderParams)
                Renderer.renderTaggedText(t.text, xPixel + nextXPixel + 1, yPixel + (TAB_HEIGHT - bounds.height) / 2)
            }
            nextXPixel += bounds.width - 2
        }
        localRenderParams.brightness = 1f
        val selectedTabXPixel = tabWidths.slice(0 until selectedTabIndex).sumBy { it - 2 }
        val bounds = TextManager.getStringBounds(selectedTab.text)
        Renderer.renderDefaultRectangle(xPixel + selectedTabXPixel, yPixel, bounds.width + 2, TAB_HEIGHT)
        Renderer.renderTaggedText(selectedTab.text, xPixel + selectedTabXPixel + 1, yPixel + (TAB_HEIGHT - bounds.height) / 2)
    }

    companion object {
        const val TAB_HEIGHT = 10
    }
}