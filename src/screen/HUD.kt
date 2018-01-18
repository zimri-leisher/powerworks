package screen

import graphics.Image
import inv.Inventory
import inv.InventoryChangeListener
import inv.Item
import inv.ItemType
import io.*
import main.State

object HUD {
    const val HOTBAR_SIZE = 8

    init {
        Hotbar
    }

    object Hotbar : GUIWindow("In game default hotbar",
            { (IngameGUI.widthPixels - (HOTBAR_SIZE * GUIItemSlot.WIDTH)) / 2 },
            { (IngameGUI.heightPixels - GUIItemSlot.HEIGHT) },
            { HOTBAR_SIZE * GUIItemSlot.WIDTH },
            { GUIItemSlot.HEIGHT },
            true,
            IngameGUI.layer + 2,
            // Above the background, view group and inventory group
            ScreenManager.Groups.HOTBAR),
            ControlPressHandler,
            InventoryChangeListener {

        val items = Inventory(8, 1).apply { listeners.add(this@Hotbar) }
        var selected = 0
            set(value) {
                field = value
                selectOverlay.xAlignment = { value * GUIItemSlot.WIDTH }
            }

        val selectOverlay = GUITexturePane(rootChild, "Hotbar slot selected overlay", 0, 0, texture = Image.GUI.HOTBAR_SELECTED_SLOT, open = true, layer = layer + 2)

        init {
            partOfLevel = true
            InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.SLOT_1, Control.SLOT_2, Control.SLOT_3, Control.SLOT_4, Control.SLOT_5, Control.SLOT_6, Control.SLOT_7, Control.SLOT_8, Control.GIVE_TEST_ITEM)
            for (i in 0 until HOTBAR_SIZE) {
                GUIItemSlot(rootChild, "Hotbar slot $i", i * GUIItemSlot.WIDTH, 0, i, items, open = true)
            }
            selectOverlay.transparentToInteraction = true
        }

        override fun handleControlPress(p: ControlPress) {
            if (p.pressType != PressType.PRESSED)
                return
            if (State.CURRENT_STATE == State.INGAME) {
                when (p.control) {
                    Control.SLOT_1 -> setSlot(0)
                    Control.SLOT_2 -> setSlot(1)
                    Control.SLOT_3 -> setSlot(2)
                    Control.SLOT_4 -> setSlot(3)
                    Control.SLOT_5 -> setSlot(4)
                    Control.SLOT_6 -> setSlot(5)
                    Control.SLOT_7 -> setSlot(6)
                    Control.SLOT_8 -> setSlot(7)
                    Control.GIVE_TEST_ITEM -> items.add(Item(ItemType.TUBE))
                }
            }
        }

        override fun onInventoryChange(inv: Inventory) {
            if(inv == items) {
                Mouse.setHeldItem(items, selected)
            }
        }

        private fun setSlot(slot: Int) {
            selected = slot
            Mouse.setHeldItem(items, slot)
        }
    }
}