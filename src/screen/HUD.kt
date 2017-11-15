package screen

import graphics.Image
import graphics.Renderer
import inv.Item
import inv.ItemType
import io.*
import level.DroppedItem
import main.Game

object HUD {
    const val HOTBAR_SIZE = 8
    val HOTBAR_SLOT_WIDTH_PIXELS = Image.GUI.HOTBAR_SLOT.widthPixels
    val HOTBAR_SLOT_HEIGHT_PIXELS = Image.GUI.HOTBAR_SLOT.heightPixels

    fun poke() {
        Hotbar.poke()
    }

    object Hotbar : GUIWindow("In game default hotbar", (
            IngameGUI.widthPixels - (HOTBAR_SIZE * HOTBAR_SLOT_WIDTH_PIXELS)) / 2,
            (IngameGUI.heightPixels - HOTBAR_SLOT_HEIGHT_PIXELS),
            HOTBAR_SIZE * HOTBAR_SLOT_WIDTH_PIXELS,
            HOTBAR_SLOT_HEIGHT_PIXELS,
            true,
            IngameGUI.layer + 2,
            // Above the background, view group and inventory group
            ScreenManager.Groups.HOTBAR),
            ControlPressHandler {

        init {
            InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.SLOT_1, Control.SLOT_2, Control.SLOT_3, Control.SLOT_4, Control.SLOT_5, Control.SLOT_6, Control.SLOT_7, Control.SLOT_8, Control.GIVE_TEST_ITEM, Control.DROP_HELD_ITEM)
            rootChild = object : GUIElement(this, name, xPixel, yPixel, widthPixels, heightPixels, open, layer) {
                override fun render() {
                    for (i in 0 until HOTBAR_SIZE) {
                        Renderer.renderTexture(if (i == selected) Image.GUI.HOTBAR_SLOT_SELECTED else Image.GUI.HOTBAR_SLOT, Hotbar.xPixel + i * HOTBAR_SLOT_WIDTH_PIXELS, Hotbar.yPixel)
                        val item = items[i]
                        if (item != null) {
                            Renderer.renderTexture(item.type.texture, Hotbar.xPixel + i * HOTBAR_SLOT_WIDTH_PIXELS, Hotbar.yPixel)
                        }
                    }
                }
            }
        }

        val items = Game.mainInv
        var selected = 0
        val currentItem
            get() = items[selected]


        override fun onScreenSizeChange(oldWidth: Int, oldHeight: Int) {
            this.xPixel = (IngameGUI.widthPixels - (HOTBAR_SIZE * HOTBAR_SLOT_WIDTH_PIXELS)) / 2
            this.yPixel = (IngameGUI.heightPixels - HOTBAR_SLOT_HEIGHT_PIXELS)
        }

        override fun handleControlPress(p: ControlPress) {
            if (p.pressType != PressType.PRESSED)
                return
            when (p.control) {
                Control.SLOT_1 -> selected = 0
                Control.SLOT_2 -> selected = 1
                Control.SLOT_3 -> selected = 2
                Control.SLOT_4 -> selected = 3
                Control.SLOT_5 -> selected = 4
                Control.SLOT_6 -> selected = 5
                Control.SLOT_7 -> selected = 6
                Control.SLOT_8 -> selected = 7
                Control.GIVE_TEST_ITEM -> items.add(Item(ItemType.MINER))
                Control.DROP_HELD_ITEM -> {
                    if (currentItem != null) {
                        val type = currentItem!!.type
                        items.remove(type, 1)
                        Game.currentLevel.add(DroppedItem(Game.currentLevel.mouseLevelXPixel, Game.currentLevel.mouseLevelYPixel, type))
                    }
                }
            }
        }

        fun poke() {}
    }
}