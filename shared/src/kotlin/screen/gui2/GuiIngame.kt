package screen.gui2

import graphics.Image
import graphics.Renderer
import io.*
import item.ItemType
import level.entity.robot.BrainRobot
import level.update.LevelObjectAdd
import main.GameState
import main.heightPixels
import main.widthPixels
import player.PlayerManager
import screen.Camera
import screen.mouse.Mouse

object GuiIngame : Gui(ScreenLayer.LEVEL_VIEW), ControlEventHandler {

    lateinit var brainRobotGui: GuiBrainRobot

    lateinit var views: GroupElement

    lateinit var cameras: Array<Camera>

    fun initializeFor(brainRobot: BrainRobot) {
        InputManager.register(this, Control.TOGGLE_INVENTORY)
        cameras = arrayOf(Camera(brainRobot.xPixel, brainRobot.yPixel),
                Camera(brainRobot.xPixel, brainRobot.yPixel))
        cameras.forEach { brainRobot.level.modify(LevelObjectAdd(it), true) }
        open = false
        define {
            onOpen {
                Hotbar.open = true
            }
            onClose {
                Hotbar.open = false
            }
            views = group {
                levelView(camera = cameras[0]) {
                    dimensions = Dimensions.Fullscreen
                }
                levelView(camera = cameras[1]) {
                    dimensions = Dimensions.Fullscreen
                    open = false
                }
            }
        }
        brainRobotGui = GuiBrainRobot(brainRobot).apply {
            open = false
        }
        Hotbar.initializeFor(brainRobot)
    }

    override fun handleControlEvent(event: ControlEvent) {
        if (GameState.currentState != GameState.INGAME)
            return
        if (event.type == ControlEventType.PRESS && event.control == Control.TOGGLE_INVENTORY) {
            brainRobotGui.open = !brainRobotGui.open
        }
    }

    object Hotbar : Gui(ScreenLayer.LEVEL_VIEW), ControlEventHandler {

        val slots = arrayOfNulls<ItemType>(8)

        lateinit var slotDisplays: Array<GuiElement>

        lateinit var selectedSlotOverlay: GuiElement

        var selectedSlotIndex = -1
            set(value) {
                field = value
                if (value !in slots.indices) {
                    Mouse.heldItemType = null
                    selectedSlotOverlay.open = false
                } else {
                    selectedSlotOverlay.open = true
                    layout.recalculateExactPlacement(selectedSlotOverlay)
                    Mouse.heldItemType = slots[value]
                }
            }

        fun initializeFor(brainRobot: BrainRobot) {
            InputManager.register(this, Control.SLOT_1, Control.SLOT_2, Control.SLOT_3, Control.SLOT_4, Control.SLOT_5, Control.SLOT_6, Control.SLOT_7, Control.SLOT_8, Control.DESELECT_HOTBAR)
            open = false
            define {
                placement = Placement.Align(HorizontalAlign.CENTER, VerticalAlign.BOTTOM)
                group {
                     horizontalList(verticalAlign = VerticalAlign.BOTTOM) {
                        slotDisplays = (0..7).map { index ->
                            texture(Image.GUI.ITEM_SLOT) {
                                render { xPixel, yPixel ->
                                    if (slots[index] != null) {
                                        slots[index]!!.icon.render(xPixel, yPixel, Image.GUI.ITEM_SLOT.widthPixels, Image.GUI.ITEM_SLOT.heightPixels, true)
                                        Renderer.renderText(PlayerManager.localPlayer.brainRobot.inventory.getQuantity(slots[index]!!), xPixel, yPixel)
                                    }
                                }
                            }
                        }.toTypedArray()
                    }
                    selectedSlotOverlay = texture(Image.GUI.HOTBAR_SELECTED_SLOT) {
                        placement = Placement.Dynamic({ selectedSlotIndex * Image.GUI.ITEM_SLOT.widthPixels }, { 0 })
                        open = false
                    }
                }
            }
        }

        fun addItemType(itemType: ItemType) {
            if (slots.any { it == null }) {
                slots[slots.indexOfFirst { it == null }] = itemType
            }
        }

        override fun handleControlEvent(event: ControlEvent) {
            if (event.type != ControlEventType.PRESS)
                return
            if (GameState.currentState == GameState.INGAME) {
                when (event.control) {
                    Control.SLOT_1 -> selectedSlotIndex = 0
                    Control.SLOT_2 -> selectedSlotIndex = 1
                    Control.SLOT_3 -> selectedSlotIndex = 2
                    Control.SLOT_4 -> selectedSlotIndex = 3
                    Control.SLOT_5 -> selectedSlotIndex = 4
                    Control.SLOT_6 -> selectedSlotIndex = 5
                    Control.SLOT_7 -> selectedSlotIndex = 6
                    Control.SLOT_8 -> selectedSlotIndex = 7
                    Control.DESELECT_HOTBAR -> {
                        selectedSlotIndex = -1
                    }
                }
            }
        }
    }
}