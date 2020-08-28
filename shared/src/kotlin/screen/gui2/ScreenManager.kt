package screen.gui2

import data.ConcurrentlyModifiableMutableList
import graphics.Renderer
import io.*
import main.DebugCode
import main.Game
import misc.Geometry
import screen.mouse.Mouse

enum class ScreenLayer {
    BACKGROUND,
    LEVEL_VIEW,
    MENU,
    WINDOWS,
    OVERLAY
    ;

    val guis = ConcurrentlyModifiableMutableList<Gui>()
}

data class Interaction(val event: ControlEvent, val xPixel: Int, val yPixel: Int, val shift: Boolean, val ctrl: Boolean, val alt: Boolean)

object ScreenManager : ControlEventHandler {

    var elementLastInteractedWith: GuiElement = Background.backgroundElement

    var guiLastInteractedWith: Gui = Background

    var elementUnderMouse: GuiElement = Background.backgroundElement

    var elementsUnderMouse = listOf(Background.backgroundElement)

    var guiUnderMouse: Gui = Background

    init {
        InputManager.register(this, Control.Group.INTERACTION)
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
            Renderer.renderEmptyRectangle(elementUnderMouse.absoluteXPixel, elementUnderMouse.absoluteYPixel, currentDimensions.widthPixels, currentDimensions.heightPixels)
        }
    }

    fun update() {
        guiUnderMouse = getOpenGuisAt(Mouse.xPixel, Mouse.yPixel).last()
        val newElementsUnderMouse = getOpenElementsAt(Mouse.xPixel, Mouse.yPixel).toList()
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
            elementUnderMouse.onDeselect?.invoke(elementUnderMouse)
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

    fun getGuisAt(xPixel: Int, yPixel: Int): Sequence<Gui> {
        val list = mutableListOf<Gui>()
        var foundGui = false
        for (layer in ScreenLayer.values()) {
            layer.guis.elements.forEach {
                val placement = it.placement
                val dimensions = it.dimensions
                if (!foundGui && Geometry.intersects(placement.xPixel, placement.yPixel, dimensions.widthPixels, dimensions.heightPixels, xPixel, yPixel, 1, 1)) {
                    list.add(it)
                    foundGui = true
                }
            }
            foundGui = false
        }

        return if (list.isEmpty()) sequenceOf(Background) else list.asSequence()
    }

    fun getOpenGuisAt(xPixel: Int, yPixel: Int): Sequence<Gui> {
        val list = mutableListOf<Gui>()
        var foundGui = false
        for (layer in ScreenLayer.values()) {
            layer.guis.elements.forEach {
                val placement = it.placement
                val dimensions = it.dimensions
                if (!foundGui && it.open && Geometry.intersects(placement.xPixel, placement.yPixel, dimensions.widthPixels, dimensions.heightPixels, xPixel, yPixel, 1, 1)) {
                    list.add(it)
                    foundGui = true
                }
            }
            foundGui = false
        }
        return if (list.isEmpty()) sequenceOf(Background) else list.asSequence()
    }

    fun getElementsAt(xPixel: Int, yPixel: Int): Sequence<GuiElement> {

        fun recursivelyGetElementsAt(element: GuiElement, xPixel: Int, yPixel: Int): Sequence<GuiElement> {
            val placement = element.gui.layout.getExactPlacement(element)
            val intersectingChildren = element.getChildrenAt(xPixel - placement.xPixel, yPixel - placement.yPixel)
            return intersectingChildren + intersectingChildren.flatMap { recursivelyGetElementsAt(it, xPixel - placement.xPixel, yPixel - placement.yPixel) }
        }

        return getGuisAt(xPixel, yPixel).flatMap { gui -> sequenceOf(gui.parentElement) + recursivelyGetElementsAt(gui.parentElement, xPixel, yPixel) }
    }

    fun getOpenElementsAt(xPixel: Int, yPixel: Int): Sequence<GuiElement> {

        fun recursivelyGetOpenElementsAt(element: GuiElement, xPixel: Int, yPixel: Int): Sequence<GuiElement> {
            val placement = element.gui.layout.getExactPlacement(element)
            val intersectingChildren = element.getChildrenAt(xPixel - placement.xPixel, yPixel - placement.yPixel).filter { it.open }
            return intersectingChildren + intersectingChildren.flatMap { recursivelyGetOpenElementsAt(it, xPixel - placement.xPixel, yPixel - placement.yPixel) }
        }

        return getOpenGuisAt(xPixel, yPixel).flatMap { gui -> (if (gui.parentElement.open) sequenceOf(gui.parentElement) + recursivelyGetOpenElementsAt(gui.parentElement, xPixel, yPixel) else sequenceOf()) }
    }

    override fun handleControlEvent(event: ControlEvent) {
        if (Control.Group.INTERACTION.contains(event.control)) {
            if (event.type == ControlEventType.PRESS) {
                elementLastInteractedWith = elementUnderMouse
                guiLastInteractedWith = guiUnderMouse
            }
            val shift = InputManager.state.isDown(Modifier.SHIFT)
            val ctrl = InputManager.state.isDown(Modifier.CTRL)
            val alt = InputManager.state.isDown(Modifier.ALT)
            val interaction = Interaction(event, Mouse.xPixel, Mouse.yPixel, shift, ctrl, alt)
            elementsUnderMouse.forEach { element ->
                element.onInteractOn(interaction)
                element.onInteractOn?.invoke(element, interaction)
            }
        }
    }
}