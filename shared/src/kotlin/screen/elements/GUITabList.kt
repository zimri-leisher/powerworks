package screen.elements

import graphics.Renderer
import graphics.text.TaggedText
import graphics.text.TextManager
import io.ControlEvent
import io.ControlEventType
import screen.mouse.Mouse
import screen.mouse.Tooltips

data class Tab(val id: String, val text: TaggedText, val mouseOverText: String? = null) {
    constructor(id: String, text: String, mouseOverText: String? = null) : this(id, TextManager.parseTags(text), mouseOverText)
    constructor(id: String, mouseOverText: String? = null) : this(id, id, mouseOverText)
}

class GUITabList(parent: RootGUIElement, name: String, xAlignment: Alignment, yAlignment: Alignment,
                 private val tabs: Array<Tab> = arrayOf(),
                 val onSelectTab: (tabID: String) -> Unit = {}, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { 0 }, { TAB_HEIGHT }, open, layer) {

    var selectedTabIndex = 0
    private val tabWidths: Array<Int>
    var selectedTab = tabs[0]

    init {
        val w = arrayOfNulls<Int>(tabs.size)
        for ((index, t) in tabs.withIndex()) {
            w[index] = TextManager.getStringWidth(t.text) + 2 * TAB_PADDING
        }
        tabWidths = w.requireNoNulls()
        alignments.width = { tabWidths.sum() }
    }

    override fun onInteractOn(event: ControlEvent, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (event.type == ControlEventType.PRESS) {
            val selectedIndex = getTabIndexAt(xPixel)
            if (selectedIndex != -1) {
                if (selectedIndex != selectedTabIndex) {
                    selectedTabIndex = selectedIndex
                    selectedTab = tabs[selectedIndex]
                    onSelectTab(selectedTab.id)
                }
            }
        }
    }

    /**
     * @param xPixel the x pixel relative to the screen (not this element)
     * @return -1 if no tab is at mouse
     */
    fun getTabIndexAt(xPixel: Int): Int {
        var relXPixel = xPixel - this.xPixel
        for ((index, width) in tabWidths.withIndex()) {
            relXPixel -= width
            if (relXPixel < 0)
                return index
        }
        return -1
    }

    override fun render() {
        localRenderParams.brightness = 0.8f
        var nextXPixel = 0
        for ((index, t) in tabs.withIndex()) {
            val bounds = TextManager.getStringBounds(t.text)
            if (index != selectedTabIndex) {
                localRenderParams.brightness = 0.9f
            } else {
                localRenderParams.brightness = 1f
            }
            Renderer.renderDefaultRectangle(xPixel + nextXPixel, yPixel, bounds.width + 2 * TAB_PADDING, TAB_HEIGHT, localRenderParams)
            Renderer.renderTaggedText(t.text, xPixel + nextXPixel + TAB_PADDING, yPixel + (TAB_HEIGHT - bounds.height) / 2)
            nextXPixel += bounds.width + 2 * TAB_PADDING
        }
    }

    companion object {
        const val TAB_HEIGHT = 10
        const val TAB_PADDING = 1

        init {
            Tooltips.addScreenTooltipTemplate({ el ->
                if (el is GUITabList) {
                    val index = el.getTabIndexAt(Mouse.xPixel)
                    if (index != -1) {
                        val tab = el.tabs[index]
                        return@addScreenTooltipTemplate tab.mouseOverText
                    }
                }
                return@addScreenTooltipTemplate null
            })
        }
    }
}