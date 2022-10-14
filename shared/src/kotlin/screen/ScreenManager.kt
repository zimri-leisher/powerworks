package screen

import data.ConcurrentlyModifiableMutableList
import graphics.Renderer
import io.*
import main.DebugCode
import main.Game
import misc.Geometry
import resource.ResourceContainer
import screen.gui.Gui
import screen.gui.GuiBackground
import screen.gui.GuiElement
import screen.gui.GuiEscapeMenu
import screen.mouse.Mouse

enum class ScreenLayer {
    BACKGROUND,
    LEVEL_VIEW,
    MENU_0,
    MENU_1,
    MENU_2,
    MENU_3,
    HUD,
    MENU_4,
    OVERLAY
    ;

    val guis = ConcurrentlyModifiableMutableList<Gui>()

    fun bringToTop(gui: Gui) {
        if (gui !in guis) {
            return
        }
        guis.remove(gui)
        guis.add(gui)
    }
}

data class Interaction(val event: ControlEvent, val x: Int, val y: Int, val shift: Boolean, val ctrl: Boolean, val alt: Boolean)

object ScreenManager : ControlEventHandler {

    var elementLastInteractedWith: GuiElement = GuiBackground.backgroundElement

    var guiLastInteractedWith: Gui = GuiBackground

    var elementUnderMouse: GuiElement = GuiBackground.backgroundElement

    var elementsUnderMouse: List<GuiElement> = listOf(GuiBackground.backgroundElement)

    var guiUnderMouse: Gui = GuiBackground

    val resourceContainerDisplays = mutableMapOf<Gui, ResourceContainer>()

    init {
        InputManager.register(this, Control.Group.INTERACTION)
        InputManager.register(this, Control.ESCAPE)
    }

    fun render() {
        ScreenLayer.values().forEach { layer ->
            layer.guis.forEach { gui ->
                if (gui.open) {
                    gui.render()
                }
            }
        }
        if (Game.currentDebugCode == DebugCode.SCREEN_INFO) {
            val gui = elementUnderMouse.gui
            val currentDimensions = gui.layout.getExactDimensions(elementUnderMouse)
            Renderer.renderEmptyRectangle(elementUnderMouse.absoluteX, elementUnderMouse.absoluteY, currentDimensions.width, currentDimensions.height)
        }
    }

    fun update() {
        guiUnderMouse = getOpenGuisAt(Mouse.x, Mouse.y).last()
        val newElementsUnderMouse = getOpenElementsAt(Mouse.x, Mouse.y).toList()
        for (newElement in newElementsUnderMouse) {
            if (newElement !in elementsUnderMouse) {
                newElement.mouseOn = true
            }
        }
        for (oldElement in elementsUnderMouse) {
            if (oldElement !in newElementsUnderMouse) {
                oldElement.mouseOn = false
            }
        }
        elementsUnderMouse = newElementsUnderMouse
        if (newElementsUnderMouse.last() != elementUnderMouse) {
            elementUnderMouse.onDeselect()
        }
        elementUnderMouse = newElementsUnderMouse.last()
        ScreenLayer.values().forEach {
            it.guis.forEach { gui ->
                if (gui.open) {
                    gui.update()
                }
            }
        }
    }

    fun getGuisAt(x: Int, y: Int): Sequence<Gui> {
        val list = mutableListOf<Gui>()
        var foundGui = false
        for (layer in ScreenLayer.values()) {
            layer.guis.elements.asReversed().forEach {
                val placement = it.placement
                val dimensions = it.dimensions
                if (!foundGui && Geometry.intersects(placement.x, placement.y, dimensions.width, dimensions.height, x, y, 1, 1)) {
                    list.add(it)
                    foundGui = true
                }
            }
            foundGui = false
        }

        return if (list.isEmpty()) sequenceOf(GuiBackground) else list.asSequence()
    }

    fun getOpenGuisAt(x: Int, y: Int): Sequence<Gui> {
        val list = mutableListOf<Gui>()
        var foundGui = false
        for (layer in ScreenLayer.values()) {
            layer.guis.elements.asReversed().forEach {
                val placement = it.placement
                val dimensions = it.dimensions
                if (!foundGui && it.open && Geometry.intersects(placement.x, placement.y, dimensions.width, dimensions.height, x, y, 1, 1)) {
                    list.add(it)
                    foundGui = true
                }
            }
            foundGui = false
        }
        return if (list.isEmpty()) sequenceOf(GuiBackground) else list.asSequence()
    }

    fun getElementsAt(x: Int, y: Int): Sequence<GuiElement> {

        fun recursivelyGetElementsAt(element: GuiElement, x: Int, y: Int): Sequence<GuiElement> {
            val placement = element.gui.layout.getExactPlacement(element)
            val intersectingChildren = element.getChildrenAt(x - placement.x, y - placement.y)
            return intersectingChildren + intersectingChildren.flatMap { recursivelyGetElementsAt(it, x - placement.x, y - placement.y) }
        }

        return getGuisAt(x, y).flatMap { gui -> sequenceOf(gui.parentElement) + recursivelyGetElementsAt(gui.parentElement, x, y) }
    }

    fun getOpenElementsAt(x: Int, y: Int): Sequence<GuiElement> {

        fun recursivelyGetOpenElementsAt(element: GuiElement, x: Int, y: Int): Sequence<GuiElement> {
            val placement = element.gui.layout.getExactPlacement(element)
            val intersectingChildren = element.getChildrenAt(x - placement.x, y - placement.y).filter { it.open }
            return intersectingChildren + intersectingChildren.flatMap { recursivelyGetOpenElementsAt(it, x - placement.x, y - placement.y) }
        }

        return getOpenGuisAt(x, y).flatMap { gui -> (if (gui.parentElement.open) sequenceOf(gui.parentElement) + recursivelyGetOpenElementsAt(gui.parentElement, x, y) else sequenceOf()) }
    }

    /**
     * Gets the highest [ResourceContainer] that is being displayed that isn't [other] but is of the same category.
     */
    fun getSecondaryResourceContainer(other: ResourceContainer): ResourceContainer? {
        var highestContainer: ResourceContainer? = null
        var highestGui: Gui? = null
        for((gui, container) in resourceContainerDisplays) {
            if(gui.open && container.id != other.id) {
                if(highestContainer == null || gui.layer.ordinal > highestGui!!.layer.ordinal){
                    highestContainer = container
                    highestGui = gui
                } else if(gui.layer.ordinal == highestGui.layer.ordinal) {
                    if(gui.layer.guis.elements.indexOf(gui) > highestGui.layer.guis.elements.indexOf(highestGui)) {
                        highestContainer = container
                        highestGui = gui
                    }
                }
            }
        }
        return highestContainer
    }

    fun onScreenSizeChange() {
        ScreenLayer.BACKGROUND.guis.forEach { it.layout.set() }
        ScreenLayer.LEVEL_VIEW.guis.forEach { it.layout.set() }
        ScreenLayer.MENU_1.guis.forEach { it.layout.set() }
        ScreenLayer.HUD.guis.forEach { it.layout.set() }
    }

    override fun handleControlEvent(event: ControlEvent) {
        if (Control.Group.INTERACTION.contains(event.control)) {
            if (event.type == ControlEventType.PRESS) {
                elementLastInteractedWith = elementUnderMouse
                guiLastInteractedWith = guiUnderMouse
                guiUnderMouse.layer.bringToTop(guiUnderMouse)
            }
            val shift = InputManager.state.isDown(Modifier.SHIFT)
            val ctrl = InputManager.state.isDown(Modifier.CTRL)
            val alt = InputManager.state.isDown(Modifier.ALT)
            val interaction = Interaction(event, Mouse.x, Mouse.y, shift, ctrl, alt)
            elementsUnderMouse.forEach { element ->
                element.onInteractOn(interaction)
            }
        } else if (event.control == Control.ESCAPE && event.type == ControlEventType.PRESS) {
            ScreenLayer.MENU_1.guis.forEach { it.open = false }
        }
    }
}