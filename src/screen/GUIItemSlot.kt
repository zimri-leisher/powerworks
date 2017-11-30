package screen

import graphics.Image
import graphics.Renderer
import inv.Inventory
import io.*


class GUIItemSlot(parent: RootGUIElement, name: String, xPixel: Int, yPixel: Int, var index: Int, var inv: Inventory,
                  var isDisplay: Boolean = false, open: Boolean = false, layer: Int = parent.layer + 1) :
        GUIElement(parent, name, xPixel, yPixel, WIDTH, HEIGHT, open, layer),
        ControlPressHandler {

    private var currentTexture = if (isDisplay) Image.GUI.ITEM_SLOT_DISPLAY else Image.GUI.ITEM_SLOT

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.SCREEN, Control.SHIFT_INTERACT)
    }

    override fun render() {
        Renderer.renderTexture(currentTexture, xPixel, yPixel)
        val i = inv[index]
        if (i != null) {
            var w = widthPixels
            var h = heightPixels
            val t = i.type.texture
            if (t.widthPixels > t.heightPixels) {
                if (t.widthPixels > widthPixels) {
                    w = widthPixels
                    val ratio = widthPixels.toDouble() / t.widthPixels
                    h = (t.heightPixels * ratio).toInt()
                }
            }
            if (t.heightPixels > t.widthPixels) {
                if (t.heightPixels > heightPixels) {
                    h = heightPixels
                    val ratio = heightPixels.toDouble() / t.heightPixels
                    w = (t.widthPixels * ratio).toInt()
                }
            }
            Renderer.renderTexture(t, xPixel + (widthPixels - w) / 2, yPixel + (heightPixels - h) / 2, w, h)
            Renderer.renderText(i.quantity, xPixel + 1, yPixel + 5)
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
                            inv.remove(i)
                            inv.add(mI)
                            Mouse.heldItem = i
                        } else {
                            inv.add(mI)
                            Mouse.heldItem = null
                        }
                    } else {
                        if (i != null) {
                            inv.remove(i)
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

    override fun handleControlPress(p: ControlPress) {
        if (p.control == Control.SHIFT_INTERACT && p.pressType == PressType.PRESSED) {
            val i = inv[index]
            if (i != null) {
                val invGUIs = ScreenManager.Groups.INVENTORY.windows
                if (invGUIs.isNotEmpty()) {
                    val highestOtherWindow = invGUIs.filter { it.open && it != parentWindow }.maxBy { it.layer }
                    if (highestOtherWindow != null && highestOtherWindow is InventoryGUI) {
                        inv.remove(i)
                        highestOtherWindow.inv.add(i)
                    }
                }
            }
        }
    }

    companion object {
        const val WIDTH = 16
        const val HEIGHT = 16
    }
}