package screen

import io.*
import misc.GeometryHelper

object ScreenManager : ControlPressHandler {

    val _windowGroups = mutableListOf<WindowGroup>()
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
    val windows = mutableListOf<GUIWindow>()
    val openWindows = mutableListOf<GUIWindow>()
    var elementBeingInteractedWith: RootGUIElement? = null

    var selectedElement: RootGUIElement? = null
        set(value) {
            if (field != value) {
                if (field is ControlPressHandler)
                    InputManager.currentScreenHandlers.remove(field as ControlPressHandler)
                if (value is ControlPressHandler)
                    InputManager.currentScreenHandlers.add(value) // fix the error (mouse level coords not init) TODO
                field = value
            }
        }

    var selectedWindow: GUIWindow? = null
        set(value) {
            if (field != value) {
                if (field is ControlPressHandler)
                    InputManager.currentScreenHandlers.remove(field as ControlPressHandler)
                if (value is ControlPressHandler)
                    InputManager.currentScreenHandlers.add(value)
                field = value
            }
        }

    object Groups {
        val BACKGROUND = WindowGroup(0, "Background")
        val VIEW = WindowGroup(1, "GUIViews")
        val INVENTORY = WindowGroup(2, "Inventories")
        val HOTBAR = WindowGroup(3, "Hotbar")
        val DEBUG_OVERLAY = WindowGroup(10000, "Debug overlay")
    }

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, Control.INTERACT, Control.SHIFT_INTERACT, Control.SCROLL_UP, Control.SCROLL_DOWN, Control.DEBUG)
    }

    fun render() {
        _backwardsWindowGroups.forEach {
            it.windows.forEach {
                it.openChildren.forEach { it.render() }
            }
        }
    }

    fun update() {
        updateMouseOn()
        fun recursivelyUpdate(e: RootGUIElement) {
            e.update()
            e.children.forEach { it.update() }
        }
        windows.forEach { it.update(); recursivelyUpdate(it.rootChild) }
    }

    fun updateMouseOn() {
        fun recursivelyUpdateMouseOn(e: RootGUIElement) {
            if (e.open) {
                e.mouseOn = isMouseOn(e)
                e.children.forEach { recursivelyUpdateMouseOn(it) }
            }
        }
        windows.forEach { recursivelyUpdateMouseOn(it.rootChild) }
    }

    fun isMouseOn(e: RootGUIElement): Boolean {
        return intersectsElement(Mouse.xPixel, Mouse.yPixel, e)
    }

    fun intersectsElement(xPixel: Int, yPixel: Int, e: RootGUIElement): Boolean {
        return GeometryHelper.contains(e.xPixel, e.yPixel, e.widthPixels, e.heightPixels, xPixel, yPixel, 0, 0)
    }

    fun intersectsElement(xPixel: Int, yPixel: Int, e: GUIWindow): Boolean {
        return GeometryHelper.contains(e.xPixel, e.yPixel, e.widthPixels, e.heightPixels, xPixel, yPixel, 0, 0)
    }

    /** @return the highest window, layer-wise, that intersects the given x and y coordinates and matches the predicate */
    fun getHighestWindow(xPixel: Int, yPixel: Int, predicate: (GUIWindow) -> Boolean = { true }): GUIWindow? {
        var g: GUIWindow? = null
        windowGroups.forEach {
            if (g == null) {
                g = it.windows.lastOrNull { it.open && intersectsElement(xPixel, yPixel, it) && predicate(it) }
            }
        }
        return g
    }

    /** @return the highest element, layer-wise, that intersects the given x and y coordinates and matches the predicate. */
    fun getHighestElement(window: GUIWindow, xPixel: Int, yPixel: Int, predicate: (RootGUIElement) -> Boolean): RootGUIElement? {
        return window.openChildren.stream().filter {
            intersectsElement(xPixel, yPixel, it) && predicate(it)
        }.max { o1, o2 -> o1.layer.compareTo(o2.layer) }.orElseGet { null }
    }

    fun screenSizeChange(oldWidth: Int, oldHeight: Int) {
        windows.forEach {
            if (it.adjustDimensions) {
                it.widthPixels = it.widthAlignment()
                it.heightPixels = it.heightAlignment()
            }
            it.xPixel = it.xAlignment()
            it.yPixel = it.yAlignment()
            it.onScreenSizeChange(oldWidth, oldHeight)
        }
    }

    fun updateSelected() {
        val x = Mouse.xPixel
        val y = Mouse.yPixel
        selectedWindow = getHighestWindow(x, y, { !it.transparentToInteraction })
        val window = selectedWindow
        if (window != null) {
            window.windowGroup.bringToTop(window)
            selectedElement = getHighestElement(window, x, y, { !it.transparentToInteraction })
        }
    }

    override fun handleControlPress(p: ControlPress) {
        val x = Mouse.xPixel
        val y = Mouse.yPixel
        val t = p.pressType
        val b = Mouse.button

        /* INTERACT */

        if (p.control == Control.INTERACT) {
            if (t == PressType.REPEAT) {
                elementBeingInteractedWith?.onMouseActionOn(t, x, y, b)
                return
            }
            updateSelected()
            val window = selectedWindow
            if (window != null) {
                window.windowGroup.bringToTop(window)
                if (t == PressType.PRESSED) {
                    if (selectedElement != null) {
                        selectedElement!!.onMouseActionOn(t, x, y, b)
                        elementBeingInteractedWith = selectedElement
                    }
                    // This purposely doesn't update elementBeingInteractedWith when the control press repeats,
                    // so that we are able to move the mouse quickly and not have it switch elements
                } else if (t == PressType.RELEASED) {
                    if (elementBeingInteractedWith != null) {
                        elementBeingInteractedWith!!.onMouseActionOn(t, x, y, b)
                        elementBeingInteractedWith = null
                    }
                }
            } // TODO redo this
            // These are separated because we want to do different things regarding which elements are selected
            fun recursivelyCallMouseOff(e: RootGUIElement) {
                if (e.open && e !== selectedElement) {
                    e.onMouseActionOff(t, x, y, b)
                    e.children.forEach { recursivelyCallMouseOff(it) }
                }
            }
            openWindows.stream().forEachOrdered { recursivelyCallMouseOff(it.rootChild) }

        } else if(p.control == Control.SHIFT_INTERACT) {
            updateSelected()

            /* SCROLL */

        } else if (p.control == Control.SCROLL_DOWN || p.control == Control.SCROLL_UP) {
            val dir = if (p.control == Control.SCROLL_DOWN) -1 else 1
            val highestW = getHighestWindow(x, y, { !it.transparentToInteraction })
            selectedWindow = highestW
            if (highestW != null) {
                highestW.windowGroup.bringToTop(highestW)
                val highestE = getHighestElement(highestW, x, y, { !it.transparentToInteraction })
                highestE!!.onMouseScroll(dir)
                elementBeingInteractedWith = highestE
                selectedElement = highestE
            }

            /* DEBUG */

        } else if (p.control == Control.DEBUG && p.pressType == PressType.PRESSED) {
            /*
            fun RootGUIElement.print(spaces: String = ""): String {
                var v = spaces + toString()
                for (g in children)
                    v += "\n" + g.print(spaces + "   ")
                return v
            }
            for (group in windowGroups) {
                println(group.name + ":")
                for (window in group.windows) {
                    if (window.open) {
                        println("   $window:")
                        println(window.rootChild.print("      "))
                    }
                }
            }
            */
        }
    }
}