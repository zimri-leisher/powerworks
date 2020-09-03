package screen.element

import graphics.Renderer
import graphics.TextureRenderParams
import graphics.text.TaggedText
import io.Control
import io.ControlEventType
import screen.gui.Dimensions
import screen.gui.GuiElement
import screen.Interaction
import screen.mouse.Mouse

class ElementTabs(parent: GuiElement, val padding: Int = 0) : GuiElement(parent) {

    data class Tab(val text: TaggedText, val width: Int, val height: Int, val onClick: (index: Int) -> Unit)

    var tabs = listOf<Tab>()
        set(value) {
            if(field != value) {
                field = value
                gui.layout.recalculateExactDimensions(this)
            }
        }
    var selectedTabIndex = -1
        set(value) {
            if (field != value) {
                field = value
                if (value != -1) {
                    tabs[value].onClick(value)
                }
            }
        }
    var highlightedTabIndex = -1

    init {
        dimensions = Dimensions.Dynamic({
            tabs.sumBy { it.width + padding * 2 }
        }, {
            (tabs.maxBy { it.height }?.height ?: 0) + padding * 2
        })
    }

    override fun onInteractOn(interaction: Interaction) {
        if (interaction.event.type == ControlEventType.PRESS && interaction.event.control == Control.INTERACT) {
            selectedTabIndex = highlightedTabIndex
        }
        super.onInteractOn(interaction)
    }

    override fun update() {
        highlightedTabIndex = if(!mouseOn) -1 else getTabIndexAt(Mouse.x - absoluteX)
        super.update()
    }

    fun getTabIndexAt(x: Int): Int {
        if(x < 0)
            return -1
        var currentX = 0
        for ((index, tab) in tabs.withIndex()) {
            currentX += tab.width + padding * 2
            if (x <= currentX) {
                return index
            }
        }
        return -1
    }

    override fun render(params: TextureRenderParams?) {
        val actualParams = params ?: TextureRenderParams.DEFAULT
        var currentX = 0
        for ((index, tab) in tabs.withIndex()) {
            if (index == selectedTabIndex) {
                Renderer.renderDefaultRectangle(absoluteX + currentX, absoluteY, tab.width + padding * 2, height, actualParams.combine(TextureRenderParams(brightness = 0.8f, rotation=180f)))
            } else if(index == highlightedTabIndex) {
                Renderer.renderDefaultRectangle(absoluteX + currentX, absoluteY, tab.width + padding * 2, height, actualParams.combine(TextureRenderParams(brightness = 1.1f)))
            } else {
                Renderer.renderDefaultRectangle(absoluteX + currentX, absoluteY, tab.width + padding * 2, height, actualParams)
            }
            Renderer.renderTaggedText(tab.text, absoluteX + currentX + padding, absoluteY + padding)
            currentX += tab.width + padding * 2
        }
        super.render(params)
    }
}