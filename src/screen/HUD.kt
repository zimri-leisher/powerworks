package screen

import graphics.Image
import io.*
import item.Inventory
import item.Item
import item.ItemType
import main.Game
import main.State
import resource.*
import screen.elements.GUIItemSlot
import screen.elements.GUITexturePane
import screen.elements.GUIWindow

object HUD {
    const val HOTBAR_SIZE = 8

    init {
        Hotbar
    }

    object Hotbar : GUIWindow("In game default hotbar",
            { (IngameGUI.widthPixels - (HOTBAR_SIZE * GUIItemSlot.WIDTH)) / 2 },
            { IngameGUI.heightPixels - GUIItemSlot.HEIGHT },
            { HOTBAR_SIZE * GUIItemSlot.WIDTH },
            { GUIItemSlot.HEIGHT },
            true,
            IngameGUI.layer + 2,
            // Above the background, view group and inventory group
            ScreenManager.Groups.HOTBAR),
            ControlPressHandler,
            ResourceContainerChangeListener {
        const val WIDTH = 8

        val items = HotbarInventory()
        var selected = -1
            set(value) {
                field = value
                if (value == -1) {
                    selectOverlay.open = false
                } else {
                    selectOverlay.open = true
                    selectOverlay.updateAlignment()
                }
            }
        class HotbarInventory : ResourceContainer<ItemType>(ResourceType.ITEM) {

            val items = arrayOfNulls<ItemType>(HOTBAR_SIZE)

            override fun add(resource: ResourceType, quantity: Int, from: ResourceNode<ItemType>?, checkIfAble: Boolean): Boolean {
                if (!isValid(resource))
                    return false
                if (checkIfAble)
                    if (!spaceFor(resource, quantity))
                        return false
                resource as ItemType
                for (i in items.indices) {
                    if (items[i] == null) {
                        items[i] = resource
                        return true
                    }
                }
                throw Exception("Please use checkIfAble when calling this and not knowing if it has adequate space left")
            }

            override fun spaceFor(resource: ResourceType, quantity: Int) = items.all { it != resource }

            override fun remove(resource: ResourceType, quantity: Int, to: ResourceNode<ItemType>?, checkIfAble: Boolean): Boolean {
                if (!isValid(resource))
                    return false
                if (checkIfAble)
                    if (!contains(resource, quantity))
                        return false
                for (i in items.indices) {
                    if (items[i] != null) {
                        if (items[i]!! == resource) {
                            items[i] = null
                            return true
                        }
                    }
                }
                throw Exception("Please use checkIfAble when calling this and not knowing if it contains adequate resources")
            }

            override fun contains(resource: ResourceType, quantity: Int) = items.any { it == resource }

            override fun clear() {
                for(i in items.indices) {
                    items[i] = null
                }
            }

            override fun copy(): ResourceContainer<ItemType> = HotbarInventory()

            override fun getQuantity(resource: ResourceType): Int {
                if(contains(resource))
                    return 1
                return 0
            }

            operator fun get(i: Int) = items[i]

            override fun toList(): ResourceList {
                val map = mutableMapOf<ResourceType, Int>()
                for(i in items) {
                    if(i != null) {
                        map.put(i, 1)
                    }
                }
                return ResourceList(map)
            }

        }

        val selectOverlay = GUITexturePane(rootChild, "Hotbar slot selected overlay", { selected * GUIItemSlot.WIDTH }, { 0 }, texture = Image.GUI.HOTBAR_SELECTED_SLOT, layer = layer + 2)

        init {
            partOfLevel = true
            InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.SLOT_1, Control.SLOT_2, Control.SLOT_3, Control.SLOT_4, Control.SLOT_5, Control.SLOT_6, Control.SLOT_7, Control.SLOT_8, Control.GIVE_TEST_ITEM)
            for (i in 0 until HOTBAR_SIZE) {
                GUIItemSlot(rootChild, "Hotbar slot $i", i * GUIItemSlot.WIDTH, 0, i, items, open = true)
            }
            Game.mainInv.listeners.add(this)
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
                    Control.GIVE_TEST_ITEM -> Game.mainInv.add(Item(ItemType.TUBE))
                }
            }
        }

        override fun onContainerAdd(container: ResourceContainer<*>, resource: ResourceType, quantity: Int) {
        }

        override fun onContainerRemove(inv: Inventory, resource: ResourceType, quantity: Int) {
        }

        override fun onContainerClear(container: ResourceContainer<*>) {
        }

        override fun onContainerChange(container: ResourceContainer<*>) {
            if(container == Game.mainInv) {
                if(selected != -1 && items[selected] != null && Game.mainInv.getQuantity(items[selected]!!) > 0)
                    Mouse.heldItemType = items[selected]
            }
        }

        private fun setSlot(slot: Int) {
            selected = slot
            Mouse.heldItemType = items[slot]
        }
    }
}
