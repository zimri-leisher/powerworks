package screen

import data.ConcurrentlyModifiableMutableList
import data.ConcurrentlyModifiableWeakMutableList
import io.*
import main.DebugCode
import main.Game
import misc.Geometry
import screen.animations.GUIAnimation
import screen.elements.GUIElement
import screen.elements.GUIWindow
import screen.elements.RootGUIElement
import screen.mouse.Mouse

object ScreenManager : ControlHandler {

    private val _windowGroups = mutableListOf<WindowGroup>()
    val windowGroups = object : MutableList<WindowGroup> by _windowGroups {
        override fun add(element: WindowGroup): Boolean {
            val result = _windowGroups.add(element)
            _backwardsWindowGroups.add(element)
            sortByDescending { it.layer }
            _backwardsWindowGroups.sortBy { it.layer }
            return result
        }

        override fun remove(element: WindowGroup): Boolean {
            val result = _windowGroups.remove(element)
            _backwardsWindowGroups.remove(element)
            return result
        }
    }

    private val _backwardsWindowGroups = mutableListOf<WindowGroup>()
    val windows = ConcurrentlyModifiableWeakMutableList<GUIWindow>()
    val openWindows = ConcurrentlyModifiableWeakMutableList<GUIWindow>()

    val playingAnimations = ConcurrentlyModifiableMutableList<GUIAnimation<*>>()

    /**
     * The last [GUIElement] clicked on. Keep in mind this doesn't look at elements with the [GUIElement.transparentToInteraction] flag set to true
     */
    var elementLastInteractedWith: GUIElement = MainMenuGUI.logo

    /**
     * The last [GUIWindow] clicked on. Keep in mind this doesn't look at elements with the [GUIWindow.transparentToInteraction] flag set to true
     */
    var windowLastInteractedWith: GUIWindow = MainMenuGUI

    /**
     * The [GUIElement] under the mouse
     */
    var elementUnderMouse: GUIElement? = null

    /**
     * The [GUIWindow] under the mouse
     */
    var windowUnderMouse: GUIWindow? = null


    object Groups {
        val BACKGROUND = WindowGroup(0, "Background")
        val VIEW = WindowGroup(1, "GUIViews")
        val INVENTORY = WindowGroup(2, "Inventories")
        val PLAYER_UTIL = WindowGroup(3, "Player utilities")
        val HUD = WindowGroup(4, "Hotbar")
        val MOUSE = WindowGroup(9999, "Mouse")
    }

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.Group.INTERACTION)
    }

    fun render() {
        _backwardsWindowGroups.forEach {
            it.windows.filter { it.open }.forEach {
                it.render()
                it.openChildren.forEach {
                    if (it.autoRender)
                        it.render()
                }
            }
        }
    }

    fun update() {
        playingAnimations.forEach { it.update() }
        forEachElement(func = { it.update() })
        openWindows.forEach { it.update() }
        updateMouseOn()
        if (Game.currentDebugCode == DebugCode.SCREEN_INFO) {
            //printHierarchy()
        }
    }

    private fun updateMouseOn() {
        windows.forEach { it.mouseOn = it.open && isMouseOn(it) }
        forEachElement({ it.mouseOn = isMouseOn(it) }, { it.open })
        windowUnderMouse = getHighestWindow(Mouse.xPixel, Mouse.yPixel, { !it.transparentToInteraction })
        elementUnderMouse = getHighestElement(Mouse.xPixel, Mouse.yPixel, { !it.transparentToInteraction })
    }

    fun isMouseOn(e: RootGUIElement): Boolean {
        return intersectsElement(Mouse.xPixel, Mouse.yPixel, e)
    }

    fun intersectsElement(xPixel: Int, yPixel: Int, e: RootGUIElement): Boolean {
        return Geometry.contains(e.xPixel, e.yPixel, e.widthPixels, e.heightPixels, xPixel, yPixel, 0, 0)
    }

    /** @return the highest [GUIWindow], [layer][GUIWindow.layer]-wise, that intersects the point ([xPixel], [yPixel]) and matches the [predicate] */
    fun getHighestWindow(xPixel: Int, yPixel: Int, predicate: (GUIWindow) -> Boolean = { true }): GUIWindow? {
        var g: GUIWindow? = null
        windowGroups.forEach {
            if (g == null) {
                g = it.windows.lastOrNull { it.open && intersectsElement(xPixel, yPixel, it) && predicate(it) }
            }
        }
        return g
    }

    /** @return the highest [GUIElement], [layer][GUIElement.layer]-wise, that intersects the point ([xPixel], [yPixel]) and matches the [predicate] */
    fun getHighestElement(xPixel: Int, yPixel: Int, window: GUIWindow, predicate: (GUIElement) -> Boolean = { true }): GUIElement? {
        return window.openChildren.stream().filter {
            intersectsElement(xPixel, yPixel, it) && predicate(it)
        }?.max { o1, o2 -> o1.layer.compareTo(o2.layer) }?.orElseGet { null }
    }

    fun getHighestElement(xPixel: Int, yPixel: Int, predicate: (GUIElement) -> Boolean = { true }): GUIElement? {
        var highestElement: GUIElement? = null
        windowGroups.forEach {
            it.windows.forEachBackwards {
                if (highestElement == null) {
                    it.openChildren.forEach {
                        if (predicate(it) && intersectsElement(xPixel, yPixel, it) && it.layer > highestElement?.layer ?: Int.MIN_VALUE) {
                            highestElement = it
                        }
                    }
                }
            }
        }
        return highestElement
    }

    fun screenSizeChange() {
        windows.forEach {
            it.alignments.update()
        }
    }

    private fun updateSelected() {
        InputManager.currentScreenHandlers.clear()
        if (windowUnderMouse != null) {
            windowLastInteractedWith = windowUnderMouse!!
            windowLastInteractedWith.windowGroup.bringToTop(windowLastInteractedWith)
            if (windowLastInteractedWith is ControlHandler) {
                InputManager.currentScreenHandlers.add(windowLastInteractedWith as ControlHandler)
            }
        }
        if (elementUnderMouse != null) {
            elementLastInteractedWith = elementUnderMouse!!
            if (elementLastInteractedWith is ControlHandler) {
                InputManager.currentScreenHandlers.add(elementLastInteractedWith as ControlHandler)
            }
        }
    }

    private fun forEachElement(func: ((GUIElement) -> Unit), pred: ((GUIElement) -> Boolean)? = null) {
        windows.forEach { it.children.forEach { recursivelyCall(it, func, pred) } }
    }

    private fun recursivelyCall(e: GUIElement, l: (GUIElement) -> Unit, f: ((GUIElement) -> Boolean)? = null) {
        if (f == null || f(e)) {
            l(e)
            e.children.forEach { recursivelyCall(it, l, f) }
        }
    }

    override fun handleControl(p: ControlPress) {
        val x = Mouse.xPixel
        val y = Mouse.yPixel
        val t = p.pressType
        val b = Mouse.button
        val c = p.control
        if (Control.Group.INTERACTION.contains(c)) {
            // If it is repeating, we don't want it to change the selected element so we can move the mouse nice and fast
            // without worrying that it will click on something else
            if (t == PressType.PRESSED) {
                updateSelected()
            }
            if (Control.Group.SCROLL.contains(c)) {
                elementUnderMouse?.onScroll(if (c == Control.SCROLL_UP) 1 else -1)
            } else {
                val shift = InputManager.inputsBeingPressed.contains("SHIFT")
                val control = InputManager.inputsBeingPressed.contains("CONTROL")
                val alt = InputManager.inputsBeingPressed.contains("ALT")
                elementUnderMouse?.onInteractOn(t, x, y, b, shift, control, alt)
                windowUnderMouse?.onInteractOn(t, x, y, b, shift, control, alt)
                forEachElement({ it.onInteractOff(x, y, t, b, shift, control, alt) }, { it != elementUnderMouse && it.open })
            }
        }
    }

    private fun printHierarchy() {

        fun print(el: GUIElement, prefix: String): String {
            val s = StringBuilder()
            s.appendln(prefix + el.toString())
            s.append(el.children.joinToString(separator = "", transform = { print(it, prefix + "  ") }))
            return s.toString()
        }

        for (g in windowGroups) {
            println(g.name + ": (${g.windows.size})")
            for (window in g.windows) {
                println("  $window")
                //for(child in window.children.sortedBy { it.layer }) {
                //    println(print(child, "    "))
                //}
            }
        }

    }
}