package screen

import graphics.Image
import graphics.Utils
import inv.Inventory

class InventoryGUI(name: String,
                   displayName: String,
                   val inv: Inventory,
                   xPixel: Int, yPixel: Int,
                   open: Boolean = false,
                   layer: Int = 1) :
        GUIWindow(name, xPixel, yPixel,
                inv.width * (GUIItemSlot.WIDTH + ITEM_SLOT_PADDING) + ITEM_SLOT_PADDING,
                inv.height * (GUIItemSlot.HEIGHT + ITEM_SLOT_PADDING) + ITEM_SLOT_PADDING + 5,
                open,
                layer, ScreenManager.Groups.INVENTORY) {

    val itemSlots: Array<GUIItemSlot>
    val background = GUITexturePane(this.rootChild, name + " background", 0, 0, Image(Utils.genRectangle(widthPixels, heightPixels)))
    val nameText = GUIText(background, name + " name text", 2, 5, displayName)

    init {
        generateCloseButton(this.rootChild, background.layer + 1)
        generateDragGrip(this.rootChild, background.layer + 1)
        val arr = arrayOfNulls<GUIItemSlot>(inv.width * inv.height)
        for (y in 0 until inv.height) {
            for (x in 0 until inv.width) {
                arr[x + y * inv.width] = GUIItemSlot(background, name + " item slot ${x + y * inv.width}", ITEM_SLOT_PADDING + x * (GUIItemSlot.WIDTH + ITEM_SLOT_PADDING), ITEM_SLOT_PADDING + 5 + y * (GUIItemSlot.HEIGHT + ITEM_SLOT_PADDING), x + y * inv.width, inv)
            }
        }
        itemSlots = arr.requireNoNulls()
    }

    companion object {
        const val ITEM_SLOT_PADDING = 1
    }

}