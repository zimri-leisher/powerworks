package screen

import graphics.Images
import graphics.Renderer
import inv.Item
import io.*

object HUD {
    const val HOTBAR_SIZE = 8
    val HOTBAR_SLOT_WIDTH_PIXELS = Images.HOTBAR_SLOT.widthPixels
    val HOTBAR_SLOT_HEIGHT_PIXELS = Images.HOTBAR_SLOT.heightPixels

    fun poke() {
        Hotbar.poke()
    }

    object Hotbar : GUIElement(IngameDefaultGUI, "In game default hotbar", (
            IngameDefaultGUI.widthPixels - (HOTBAR_SIZE * HOTBAR_SLOT_WIDTH_PIXELS)) / 2,
            (IngameDefaultGUI.heightPixels - HOTBAR_SLOT_HEIGHT_PIXELS),
            HOTBAR_SIZE * HOTBAR_SLOT_WIDTH_PIXELS,
            HOTBAR_SIZE * HOTBAR_SLOT_HEIGHT_PIXELS), ControlPressHandler {

        init {
            InputManager.registerControlPressHandler(this, Control.SLOT_1, Control.SLOT_2, Control.SLOT_3, Control.SLOT_4, Control.SLOT_5, Control.SLOT_6, Control.SLOT_7, Control.SLOT_8)
        }

        val items = arrayOfNulls<Item>(HOTBAR_SIZE)
        var selected = 0
        val currentItem
            get() = items[selected]

        override fun render() {
            for (i in 0 until HOTBAR_SIZE) {
                Renderer.renderTexture(if (i == selected) Images.HOTBAR_SLOT_SELECTED else Images.HOTBAR_SLOT, xPixel + i * HOTBAR_SLOT_WIDTH_PIXELS, yPixel)
                val item = items[i]
                if (item != null) {
                    Renderer.renderTexture(item.type.texture, xPixel + i * HOTBAR_SLOT_WIDTH_PIXELS, yPixel)
                }
            }
        }

        override fun handleControlPress(p: ControlPress) {
            if(p.pressType != PressType.PRESSED)
                return
            when(p.control) {
                Control.SLOT_1 -> selected = 0
                Control.SLOT_2 -> selected = 1
                Control.SLOT_3 -> selected = 2
                Control.SLOT_4 -> selected = 3
                Control.SLOT_5 -> selected = 4
                Control.SLOT_6 -> selected = 5
                Control.SLOT_7 -> selected = 6
                Control.SLOT_8 -> selected = 7

            }
        }

        fun poke() {}
    }
}