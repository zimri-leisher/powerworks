package screen

import graphics.Image
import io.*
import item.BlockItemType
import item.Item
import item.ItemType
import main.State
import player.PlayerManager
import resource.*
import routing.InvalidFunctionCallException
import screen.elements.GUIItemSlot
import screen.elements.GUITexturePane
import screen.elements.GUIWindow
import screen.mouse.Mouse

/**
 * The heads-up-display, consisting of handy information that remains on the screen when in game
 */
object HUD {
    const val HOTBAR_SIZE = 8

    init {
        Hotbar
    }

    /**
     * A set of item slots at the bottom of the screen which allows the user to quickly switch between held items for block placement purposes
     * It does not store anything itself, even if it looks like it does. Instead, each slot, when containing an item type, displays the total amount of that item in the
     * main inventory, meaning it could potentially have a larger value than the max stack of that type.
     */
    object Hotbar : GUIWindow("In game default hotbar",
            { (IngameGUI.widthPixels - (HOTBAR_SIZE * GUIItemSlot.WIDTH)) / 2 },
            { 0 },
            { HOTBAR_SIZE * GUIItemSlot.WIDTH },
            { GUIItemSlot.HEIGHT },
            ScreenManager.Groups.HUD,
            true,
            IngameGUI.layer + 2),
            ControlPressHandler,
            ResourceContainerChangeListener {

        internal val items = HotbarInventory()

        /**
         * The selected slot
         * When it is -1, that means the currently held item isn't on the hotbar, usually indicating it is from an inventory
         */
        var selected = -1
            set(value) {
                field = value
                if (value == -1) {
                    selectOverlay.open = false
                } else {
                    selectOverlay.open = true
                    selectOverlay.alignments.updatePosition()
                    Mouse.heldItemType = items[value]
                }
            }

        internal class HotbarInventory : ResourceContainer(ResourceCategory.ITEM) {

            override val expected: ResourceList
                get() = throw InvalidFunctionCallException("Hotbar inventory cannot expect items")

            override fun expect(resources: ResourceList): Boolean {
                throw InvalidFunctionCallException("Hotbar inventory cannot expect items")
            }

            override fun cancelExpectation(resources: ResourceList): Boolean {
                throw InvalidFunctionCallException("Hotbar inventory cannot expect items")
            }

            override fun getSpaceForType(type: ResourceType): Int {
                return 0
            }

            override val totalQuantity: Int
                get() = items.sumBy { if (it != null) 1 else 0 }

            val items = arrayOfNulls<ItemType>(HOTBAR_SIZE)

            override fun add(resources: ResourceList, from: ResourceNode?, checkIfAble: Boolean): Boolean {
                if (checkIfAble)
                    if (!canAdd(resources))
                        return false
                list@ for ((resource, _) in resources) {
                    for (i in items.indices) {
                        if (items[i] == null) {
                            items[i] = resource as ItemType
                            continue@list
                        }
                    }
                }
                return true
            }

            override fun spaceFor(list: ResourceList): Boolean {
                for ((resource, quantity) in list) {
                    if (items.any { it == resource }) {
                        return false
                    }
                }
                return true
            }

            override fun remove(resources: ResourceList, to: ResourceNode?, checkIfAble: Boolean): Boolean {
                if (checkIfAble)
                    if (!canRemove(resources))
                        return false
                list@ for ((resource, _) in resources) {
                    for (i in items.indices) {
                        if (items[i] != null) {
                            if (items[i]!! == resource) {
                                items[i] = null
                                continue@list
                            }
                        }
                    }
                }
                return true
            }

            override fun contains(list: ResourceList): Boolean {
                for ((resource, _) in list) {
                    if (items.none { it == resource }) {
                        return false
                    }
                }
                return true
            }

            override fun clear() {
                for (i in items.indices) {
                    items[i] = null
                }
            }

            override fun copy(): ResourceContainer = HotbarInventory()

            override fun getQuantity(resource: ResourceType): Int {
                if (isRightType(resource))
                    if (contains(resource as ItemType))
                        return 1
                return 0
            }

            operator fun get(i: Int) = items[i]

            override fun toResourceList(): ResourceList {
                val map = mutableMapOf<ResourceType, Int>()
                for (i in items) {
                    if (i != null) {
                        map.put(i, 1)
                    }
                }
                return ResourceList(map)
            }
        }

        private val selectOverlay = GUITexturePane(this, "Hotbar slot selected overlay", { selected * GUIItemSlot.WIDTH }, { 0 }, textureRegion = Image.GUI.HOTBAR_SELECTED_SLOT, layer = layer + 2)

        init {
            InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.SLOT_1, Control.SLOT_2, Control.SLOT_3, Control.SLOT_4, Control.SLOT_5, Control.SLOT_6, Control.SLOT_7, Control.SLOT_8, Control.DESELECT_HOTBAR)
            GUITexturePane(this, "hotbar background", -1, 0, Image.GUI.GREY_FILLER, widthPixels + 2, heightPixels + 1, open = true)
            for (i in 0 until HOTBAR_SIZE) {
                GUIItemSlot(this, "Hotbar slot $i", i * GUIItemSlot.WIDTH, 0, i, items, open = true)
            }
            PlayerManager.localPlayer.brainRobot.inventory.listeners.add(this)
            selectOverlay.transparentToInteraction = true
        }

        override fun handleControlPress(p: ControlPress) {
            if (p.pressType != PressType.PRESSED)
                return
            if (State.CURRENT_STATE == State.INGAME) {
                when (p.control) {
                    Control.SLOT_1 -> selected = 0
                    Control.SLOT_2 -> selected = 1
                    Control.SLOT_3 -> selected = 2
                    Control.SLOT_4 -> selected = 3
                    Control.SLOT_5 -> selected = 4
                    Control.SLOT_6 -> selected = 5
                    Control.SLOT_7 -> selected = 6
                    Control.SLOT_8 -> selected = 7
                    Control.DESELECT_HOTBAR -> {
                        Mouse.heldItemType = null
                        selected = -1
                    }
                }
            }
        }

        override fun onContainerClear(container: ResourceContainer) {
        }

        override fun onAddToContainer(container: ResourceContainer, resources: ResourceList) {
            if (container.id == PlayerManager.localPlayer.brainRobot.inventory.id) {
                if (selected != -1 && items[selected] != null && PlayerManager.localPlayer.brainRobot.inventory.getQuantity(items[selected]!!) > 0)
                    Mouse.heldItemType = items[selected]
            }
        }

        override fun onRemoveFromContainer(container: ResourceContainer, resources: ResourceList) {
            if (container.id == PlayerManager.localPlayer.brainRobot.inventory.id) {
                if (selected != -1 && items[selected] != null && PlayerManager.localPlayer.brainRobot.inventory.getQuantity(items[selected]!!) > 0)
                    Mouse.heldItemType = items[selected]
            }
        }
    }
}
