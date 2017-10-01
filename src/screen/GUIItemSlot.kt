package screen

import graphics.Image
import graphics.Renderer
import inv.Inventory
import io.Mouse
import io.PressType



class GUIItemSlot(parent: RootGUIElement? = RootGUIElementObject, name: String, xPixel: Int, yPixel: Int, var index: Int, var inv: Inventory, var isDisplay: Boolean, layer: Int = (parent?.layer ?: 0) + 1) : GUIElement(parent, name, xPixel, yPixel, DEFAULT_WIDTH, DEFAULT_HEIGHT, layer) {

    private var currentTexture = if(isDisplay) Image.GUI.ITEM_SLOT_DISPLAY else Image.GUI.ITEM_SLOT

    override fun render() {
        Renderer.renderTexture(currentTexture, xPixel, yPixel)
        val i = inv[index]
        if (i != null) {
            Renderer.renderTexture(i.type.texture, xPixel, yPixel, widthPixels, heightPixels)
            Renderer.renderText(i.quantity, xPixel + 1, yPixel + 4)
        }
    }

    override fun onMouseEnter() {
        if(!isDisplay)
            currentTexture = Image.GUI.ITEM_SLOT_HIGHLIGHT
    }

    override fun onMouseLeave() {
        if(!isDisplay)
            currentTexture = Image.GUI.ITEM_SLOT
    }

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int) {
        if (isDisplay)
            return
        when (type) {
            PressType.PRESSED -> {
                currentTexture = Image.GUI.ITEM_SLOT_CLICK
                val i = inv[index]
                val mI = Mouse.heldItem
                if (button == 1) {
                    if (mI != null) {
                        if (i != null) {
                            if (mI.type !== i.type) {
                                Mouse.heldItem = i
                                inv.add(mI)
                            } else {
                                if (i.quantity + mI.quantity >= i.type.maxStack) {
                                    val q = i.quantity
                                    i.quantity = i.type.maxStack
                                    mI.quantity = mI.quantity - (i.type.maxStack - q)
                                } else {
                                    i.quantity = i.quantity + mI.quantity
                                    Mouse.heldItem = null
                                }
                            }
                        } else {
                            inv.add(mI)
                            Mouse.heldItem = null
                        }
                    } else {
                        if (i != null) {
                            inv[index] = null
                            Mouse.heldItem = i
                        }
                    }
                }
            }
            PressType.RELEASED -> currentTexture = Image.GUI.ITEM_SLOT_HIGHLIGHT
            else -> {
            }
        }
    }

    companion object {
        const val DEFAULT_WIDTH = 16
        const val DEFAULT_HEIGHT = 16
    }
}