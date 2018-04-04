package screen

import graphics.Image
import graphics.Utils
import item.Inventory
import screen.elements.GUIItemSlot
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

/**
 * A GUIWindow that you can instantiate for any inventory
 * It will display the current contents in GUIItemSlots, and it has a drag grip and a close button
 */
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

    // TODO make this use init functions for x and y so that u can have it start in the center

    private val itemSlots: Array<GUIItemSlot>
    private val background = GUITexturePane(this.rootChild, name + " background", 0, 0, texture = Image(Utils.genRectangle(widthPixels, heightPixels)))

    init {
        generateCloseButton(background.layer + 1)
        generateDragGrip(background.layer + 1)
        val arr = arrayOfNulls<GUIItemSlot>(inv.width * inv.height)
        for (y in 0 until inv.height) {
            for (x in 0 until inv.width) {
                arr[x + y * inv.width] = GUIItemSlot(background, name + " item slot ${x + y * inv.width}", ITEM_SLOT_PADDING + x * (GUIItemSlot.WIDTH + ITEM_SLOT_PADDING), ITEM_SLOT_PADDING + 5 + y * (GUIItemSlot.HEIGHT + ITEM_SLOT_PADDING), x + y * inv.width, inv)
            }
        }
        itemSlots = arr as Array<GUIItemSlot>
        // You want to be able to move and edit inventories at the same time
        partOfLevel = true
    }

    companion object {
        const val ITEM_SLOT_PADDING = 0
    }

}