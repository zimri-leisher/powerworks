package screen.elements

import graphics.Renderer
import graphics.TextureRenderParams
import graphics.text.TextManager
import graphics.text.TextRenderParams
import main.toColor
import resource.ResourceContainer
import resource.ResourceContainerChangeListener
import resource.ResourceList
import kotlin.math.ceil
import kotlin.math.exp

class GUIResourceContainerDisplay(parent: RootGUIElement, name: String,
                                  xAlignment: Alignment, yAlignment: Alignment, width: Int, height: Int,
                                  container: ResourceContainer,
                                  open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIIconList(parent, name, xAlignment, yAlignment, width, height, renderIcon = { _, _, _ -> }, open = open, layer = layer), ResourceContainerChangeListener {

    var container = container
        set(value) {
            if (field.id != value.id) {
                field.listeners.remove(this)
                field = value
                value.listeners.add(this)
            }
        }

    var currentResources = container.toResourceList()
        private set

    init {
        renderIcon = { xPixel, yPixel, index -> renderIconAt(xPixel, yPixel, index) }
        getToolTip = {index ->
            val entry = currentResources[index]
            if(entry != null) {
                "${entry.key.name} * ${entry.value}"
            } else {
                val expected = container.expected[index - currentResources.size]
                if(expected != null) {
                    "${expected.key.name} * ${expected.value}"
                } else {
                    null
                }
            }
        }
        container.listeners.add(this)
    }

    fun renderIconAt(xPixel: Int, yPixel: Int, index: Int) {
        val entry = currentResources[index]
        if (entry != null) {
            entry.key.icon.render(xPixel, yPixel, iconSize, iconSize, true)
            Renderer.renderText(entry.value, xPixel, yPixel)
            val width = TextManager.getStringWidth(entry.value.toString())
            val expectedOfType = container.expected[entry.key]
            if(expectedOfType != 0) {
                Renderer.renderText("(+$expectedOfType)", xPixel + width, yPixel, TextRenderParams(size = 15))
            }
        } else {
            if(currentResources.isEmpty()) {
                val expected = container.expected[index]
                if(expected != null) {
                    expected.key.icon.render(xPixel, yPixel, iconSize, iconSize, true, TextureRenderParams(color = toColor(a = 0.6f)))
                    Renderer.renderText(expected.value, xPixel, yPixel)
                }
            } else {
                val displayed = currentResources.keys
                val toDisplay = container.expected.filterNot { it.key in displayed }
                for((type, quantity) in toDisplay) {
                    type.icon.render(xPixel, yPixel, iconSize, iconSize, true, TextureRenderParams(color = toColor(a = 0.6f)))
                    Renderer.renderText(quantity, xPixel, yPixel)
                }
            }
        }
    }

    override fun onContainerClear(container: ResourceContainer) {
        currentResources.clear()
    }

    override fun onAddToContainer(container: ResourceContainer, resources: ResourceList) {
        currentResources.addAll(resources)
    }

    override fun onRemoveFromContainer(container: ResourceContainer, resources: ResourceList) {
        currentResources.takeAll(resources)
    }
}