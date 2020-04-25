package screen

import graphics.Renderer
import graphics.text.TextManager
import item.Inventory
import screen.elements.*
import level.block.ChestBlock

/**
 * A GUIWindow that you can instantiate for any inventory
 * It will display the current contents in GUIItemSlots, and it has a drag grip and a close button
 */
open class InventoryGUI(name: String,
                   displayName: String,
                   val inv: Inventory,
                   xPixel: Int, yPixel: Int,
                   open: Boolean = false,
                   layer: Int = 1) :
        GUIWindow(name, xPixel, yPixel,
                inv.width * (GUIItemSlot.WIDTH + ITEM_SLOT_PADDING) + ITEM_SLOT_PADDING + 2,
                inv.height * (GUIItemSlot.HEIGHT + ITEM_SLOT_PADDING) + ITEM_SLOT_PADDING + 8,
                ScreenManager.Groups.INVENTORY,
                open,
                layer) {

    protected val itemSlots: Array<GUIItemSlot>
    private val background = GUIDefaultTextureRectangle(this, name + " background", 0, 0)

    init {
        openAtMouse = true
        generateCloseButton(background.layer + 1)
        generateDragGrip(background.layer + 1)
        val arr = arrayOfNulls<GUIItemSlot>(inv.width * inv.height)
        for (y in 0 until inv.height) {
            for (x in 0 until inv.width) {
                arr[x + y * inv.width] = GUIItemSlot(background, name + " item slot ${x + y * inv.width}", ITEM_SLOT_PADDING + x * (GUIItemSlot.WIDTH + ITEM_SLOT_PADDING) + 1, ITEM_SLOT_PADDING + (inv.height - y - 1) * (GUIItemSlot.HEIGHT + ITEM_SLOT_PADDING) + 1, x + y * inv.width, inv)
            }
        }
        GUIText(background, "Inventory GUI name text", 1, heightPixels - TextManager.getStringHeight(displayName) - 2, displayName, allowTags = true)
        itemSlots = arr as Array<GUIItemSlot>
        // You want to be able to move and edit inventories at the same time
    }

    override fun render() {
        var index = 0
    }

    companion object {
        const val ITEM_SLOT_PADDING = 0
    }

}