package screen.gui

import graphics.Image
import graphics.Renderer
import graphics.TextureRenderParams
import io.*
import item.ItemType
import level.entity.robot.BrainRobot
import level.update.LevelObjectAdd
import main.GameState
import main.height
import main.width
import player.PlayerManager
import screen.Camera
import screen.ScreenLayer
import screen.element.ElementLevelView
import screen.mouse.Mouse

object GuiIngame : Gui(ScreenLayer.LEVEL_VIEW), ControlEventHandler {

    lateinit var brainRobotGui: GuiBrainRobot

    lateinit var firstView: ElementLevelView
    lateinit var secondView: ElementLevelView

    lateinit var cameras: Array<Camera>

    fun initializeFor(brainRobot: BrainRobot) {
        InputManager.register(this, Control.TOGGLE_INVENTORY, Control.SWITCH_LEVEL_VIEW)
        cameras = arrayOf(Camera(brainRobot.x, brainRobot.y),
                Camera(brainRobot.x, brainRobot.y))
        cameras.forEach { brainRobot.level.modify(LevelObjectAdd(it), true) }
        open = false
        define {
            onOpen {
                Hotbar.open = true
            }
            onClose {
                Hotbar.open = false
            }
            group {
                firstView = levelView(camera = cameras[0]) {
                    dimensions = Dimensions.Fullscreen
                }
                secondView = levelView(camera = cameras[1]) {
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
        if (event.type == ControlEventType.PRESS) {
            if (event.control == Control.TOGGLE_INVENTORY) {
                if(brainRobotGui.open.not()) {
                    brainRobotGui.open = true
                } else {
                    ScreenLayer.MENU_1.guis.forEach { it.open = false }
                }
            } else if (event.control == Control.SWITCH_LEVEL_VIEW) {
                secondView.open = firstView.open
                firstView.open = !firstView.open
            }
        }
    }

    object CenterMenuGroup : Gui(ScreenLayer.MENU_1) {
//        val menuGroups = mutableMapOf<HorizontalAlign, MutableMap<VerticalAlign, GroupElement>>()
//
//        init {
//            define {
//                for(vertical in VerticalAlign.values()) {
//                    for(horizontal in HorizontalAlign.values()) {
//                        menuGroups[horizontal]!![vertical] = list(Placement.Align(horizontal, vertical), horizontal)
//                    }
//                }
//            }
//        }

        lateinit var centerGroup: MutableGroupElement

        init {
            define {
                placement = Placement.Align.Center
                dimensions = Dimensions.FitChildren
                centerGroup = mutableList()
            }
        }
    }

    object Hotbar : Gui(ScreenLayer.HUD), ControlEventHandler {

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
                            texture(Image.Gui.ITEM_SLOT) {
                                onRender { x, y, params ->
                                    if (slots[index] != null) {
                                        slots[index]!!.icon.render(x, y, Image.Gui.ITEM_SLOT.width, Image.Gui.ITEM_SLOT.height, true, params
                                                ?: TextureRenderParams.DEFAULT)
                                        Renderer.renderText(brainRobot.inventory.getQuantity(slots[index]!!), x, y)
                                    }
                                }
                            }
                        }.toTypedArray()
                    }
                    selectedSlotOverlay = texture(Image.Gui.HOTBAR_SELECTED_SLOT) {
                        placement = Placement.Dynamic({ selectedSlotIndex * Image.Gui.ITEM_SLOT.width }, { 0 })
                        open = false
                    }
                }
            }
        }

        fun addItemType(itemType: ItemType) {
            if (slots.any { it == null } && slots.none { it == itemType }) {
                if(GuiTutorial.currentTutorialStage == TutorialStage.PUT_ITEM_IN_HOTBAR) {
                    GuiTutorial.showNextStage()
                }
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
                    else -> {}
                }
            }
        }
    }
}