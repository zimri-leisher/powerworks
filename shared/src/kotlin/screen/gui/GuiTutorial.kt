package screen.gui

import graphics.TextureRenderParams
import io.*
import main.toColor
import screen.ScreenLayer
object GuiTutorial : Gui(ScreenLayer.HUD), ControlEventHandler {

    var currentTutorialStage = TutorialStage.values().first()

    lateinit var text: ElementText

    init {
        InputManager.register(this, Control.TOGGLE_INVENTORY)
        InputManager.register(this, Control.Group.HOTBAR_SLOTS)
        define {
            placement = Placement.Align(HorizontalAlign.LEFT, VerticalAlign.TOP).offset(20, -20)
            background {
                closeButton(Placement.Align(HorizontalAlign.RIGHT, VerticalAlign.TOP).offset(-1, -1))
                dimensions = Dimensions.FitChildren.pad(6, 6)
                renderParams = TextureRenderParams(color = toColor(r = 0.01f, g = 0.9f, b = 0.1f))
                text = text(currentTutorialStage.message(), Placement.Align.Center)
            }
        }
    }

    fun showStage(tutorialStage: TutorialStage) {
        currentTutorialStage = tutorialStage
        text.text = currentTutorialStage.message()
        layout.set()
    }

    fun showNextStage() {
        if(currentTutorialStage.ordinal + 1 == TutorialStage.values().size) {
            open = false
        } else {
            showStage(TutorialStage.values()[currentTutorialStage.ordinal + 1])
        }

    }

    override fun handleControlEvent(event: ControlEvent) {
        if (event.type == ControlEventType.PRESS) {
            if (event.control == Control.TOGGLE_INVENTORY) {
                if (currentTutorialStage == TutorialStage.OPEN_INVENTORY) {
                    showNextStage()
                }
            } else if (event.control in Control.Group.HOTBAR_SLOTS) {
                if (currentTutorialStage == TutorialStage.SELECT_HOTBAR_SLOT) {
                    showNextStage()
                }
            }
        }
    }
}


enum class TutorialStage(val message: () -> String) {
    OPEN_INVENTORY({ "Press " + io.InputManager.map.getControlString(io.Control.TOGGLE_INVENTORY) + " to open your inventory." }),
    PUT_ITEM_IN_HOTBAR({ io.InputManager.map.getControlString(io.Control.SECONDARY_INTERACT) + " on an item to put it into your hotbar." }),
    SELECT_HOTBAR_SLOT({ io.Control.Group.HOTBAR_SLOTS.controls.joinToString { io.InputManager.map.getControlString(it) } + " \nto select hotbar slots." }),
    PLACE_BLOCK({ io.InputManager.map.getControlString(io.Control.INTERACT) + " to place a block." })
}