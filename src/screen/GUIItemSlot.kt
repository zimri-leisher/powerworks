package screen

import graphics.Image
import graphics.Renderer
import inv.Inventory
import inv.Item
import io.PressType


class GUIItemSlot(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, var index: Int, var inv: Inventory,
                  var isDisplay: Boolean = false, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xPixel, yPixel, WIDTH, HEIGHT, open, layer) {

    private var currentTexture = if (isDisplay) Image.GUI.ITEM_SLOT_DISPLAY else Image.GUI.ITEM_SLOT

    var currentItem: Item? = null

    override fun update() {
        currentItem = inv[index]
    }

    override fun render() {
        Renderer.renderTexture(currentTexture, xPixel, yPixel)
        if (currentItem != null) {
            val i = currentItem!!
            Renderer.renderTextureKeepAspect(i.type.texture, xPixel, yPixel, WIDTH, HEIGHT)
            Renderer.renderText(i.quantity, xPixel, yPixel)
        }
    }

    override fun onMouseEnter() {
        if (!isDisplay)
            currentTexture = Image.GUI.ITEM_SLOT_HIGHLIGHT
    }

    override fun onMouseLeave() {
        if (!isDisplay)
            currentTexture = Image.GUI.ITEM_SLOT
    }

    override fun onClose() {
        currentTexture = Image.GUI.ITEM_SLOT
    }

    override fun onMouseActionOn(type: PressType, xPixel: Int, yPixel: Int, button: Int, shift: Boolean, ctrl: Boolean, alt: Boolean) {
        if (isDisplay)
            return
        if (type == PressType.PRESSED) {
            if (shift) {
                if (currentItem != null) {
                    val i = currentItem!!
                    val invGUIs = ScreenManager.Groups.INVENTORY.windows
                    if (invGUIs.isNotEmpty()) {
                        val highestOtherWindow = invGUIs.filter { it.open && it != parentWindow }.maxBy { it.layer }
                        if (highestOtherWindow != null && highestOtherWindow is InventoryGUI) {
                            if (highestOtherWindow.inv.add(i))
                                inv.remove(i)
                        } else {
                            if (parentWindow != HUD.Hotbar) {
                                if (HUD.Hotbar.items.add(i))
                                    inv.remove(i)
                            }
                        }
                    }
                }
            } else {
                currentTexture = Image.GUI.ITEM_SLOT_CLICK
                val mI = Mouse.heldItem
                if (button == 1) {
                    if (mI != null) {
                        if (currentItem != null) {
                            Mouse.removeHeldItem(mI.quantity)
                            Mouse.setHeldItem(inv, index)
                            println("adding $mI")
                            inv.add(mI)
                        } else {
                            inv.add(mI)
                            Mouse.removeHeldItem(mI.quantity)
                        }
                    } else {
                        if (currentItem != null) {
                            Mouse.setHeldItem(inv, index)
                        }
                    }
                }
            }
        } else if (type == PressType.RELEASED) {
            currentTexture = Image.GUI.ITEM_SLOT
        }
    }

    companion object {
        const val WIDTH = 16
        const val HEIGHT = 16

        init {
            Mouse.addScreenTooltipTemplate({
                if (it is GUIItemSlot && it.currentItem != null) {
                    return@addScreenTooltipTemplate "${it.currentItem!!.type.name} * ${it.currentItem!!.quantity}"
                }
                return@addScreenTooltipTemplate null
            }, 0)
        }
    }
}