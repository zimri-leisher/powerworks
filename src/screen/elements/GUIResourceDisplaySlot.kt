package screen.elements

import graphics.Image
import graphics.Renderer
import resource.ResourceList
import screen.Mouse

class GUIResourceDisplaySlot(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, var list: ResourceList, var index: Int, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xPixel, yPixel, WIDTH, HEIGHT, open, layer) {

    override fun render() {
        val pair = list[index]
        Renderer.renderTexture(Image.GUI.RESOURCE_DISPLAY_SLOT, xPixel, yPixel)
        if (pair != null) {
            Renderer.renderTextureKeepAspect(pair.first.texture, xPixel, yPixel, WIDTH, HEIGHT)
            Renderer.renderText(pair.second, xPixel, yPixel)
        }
    }

    companion object {
        val WIDTH = 16
        val HEIGHT = 16

        init {
            Mouse.addScreenTooltipTemplate({
                if (it is GUIResourceDisplaySlot) {
                    val pair = it.list[it.index]
                    if (pair != null)
                        return@addScreenTooltipTemplate "${pair.first.name} * ${pair.second}"
                }
                return@addScreenTooltipTemplate null
            }, 0)
        }

    }
}