package screen.elements

import graphics.Image
import graphics.Renderer
import io.PressType
import item.Inventory
import resource.ResourceType
import screen.Mouse

class GUIResourceTypeDisplaySlot(parent: RootGUIElement, name: String, xAlignment: () -> Int, yAlignment: () -> Int, var type: ResourceType?, var inv: Inventory? = null, var interactable: Boolean = false, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xAlignment, yAlignment, { WIDTH }, { HEIGHT }, open, layer) {

    constructor(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, type: ResourceType?, inv: Inventory? = null, interactable: Boolean = false, open: Boolean = false, layer: Int = parent.layer + 1) : this(parent, name, { xPixel }, { yPixel }, type, inv, interactable, open, layer)

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (interactable) {
            if (type == PressType.PRESSED) {
                if (!shift && !ctrl && !alt) {
                    if (button == 3) {
                        this.type = null
                    } else if (button == 1) {
                        if (Mouse.heldItemType != null) {
                            this.type = Mouse.heldItemType
                        }
                    }
                }
            }
        }
    }

    override fun render() {
        Renderer.renderTexture(Image.GUI.RESOURCE_DISPLAY_SLOT, xPixel, yPixel)
        if (type != null) {
            Renderer.renderTextureKeepAspect(type!!.texture, xPixel, yPixel, widthPixels, heightPixels)
            if(inv != null)
                Renderer.renderText(inv!!.getQuantity(type!!), xPixel, yPixel)
        }
    }

    companion object {
        const val WIDTH = 16
        const val HEIGHT = 16
    }
}