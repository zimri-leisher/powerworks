package screen.elements

import graphics.Image
import graphics.Renderer
import resource.ResourceList
import screen.mouse.Mouse
import screen.mouse.Tooltips

class GUIResourceDisplaySlot(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, var list: ResourceList, var index: Int, var displayQuantity: Boolean = true, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xPixel, yPixel, WIDTH, HEIGHT, open, layer) {

    override fun render() {
        val pair = list[index]
        Renderer.renderTexture(Image.GUI.RESOURCE_DISPLAY_SLOT, xPixel, yPixel)
        if (pair != null) {
            pair.key.icon.render(xPixel, yPixel, WIDTH, HEIGHT, true)
            if(displayQuantity) {
                Renderer.renderText(pair.value, xPixel, yPixel)
            }
        }
    }

    companion object {
        const val WIDTH = 16
        const val HEIGHT = 16

        init {
            Tooltips.addScreenTooltipTemplate({
                if (it is GUIResourceDisplaySlot) {
                    val pair = it.list[it.index]
                    if (pair != null)
                        return@addScreenTooltipTemplate "${pair.key.name} * ${pair.value}"
                }
                return@addScreenTooltipTemplate null
            }, 0)
        }

    }
}