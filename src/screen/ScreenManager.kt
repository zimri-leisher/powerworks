package screen

import io.*
import main.Game
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

    object Groups {
        val BACKGROUND = WindowGroup(0, "Background")
        val VIEW = WindowGroup(1, "GUIViews")
        val INVENTORY = WindowGroup(2, "Inventories")
        val HOTBAR = WindowGroup(3, "Hotbar")
        val DEBUG_OVERLAY = WindowGroup(10000, "Debug overlay")
    }

    init {
        InputManager.registerControlPressHandler(this, Control.DEBUG, Control.INTERACT, Control.SCROLL_UP, Control.SCROLL_DOWN)
    }

    fun render() {
        fun recursivelyRender(e: RootGUIElement) {
            if (e.open) {
                e.render()
                e.children.forEach { recursivelyRender(it) }
            }
        }
        _backwardsWindowGroups.stream().forEachOrdered {
            it.windows.stream().forEachOrdered {
                recursivelyRender(it.rootChild)
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
        windowGroups.stream().forEachOrdered {
            if (g == null) {
                g = it.windows.lastOrNull { it.open && intersectsElement(xPixel, yPixel, it) && predicate(it) }
            }
        }
        return g
    }

    /** @return the highest element, layer-wise, that intersects the given x and y coordinates and matches the predicate. */
    fun getHighestElement(window: GUIWindow, xPixel: Int, yPixel: Int, predicate: (RootGUIElement) -> Boolean): RootGUIElement? {
        var highest: RootGUIElement? = null
        fun recurse(e: RootGUIElement) {
            if (e.open) {
                if (intersectsElement(xPixel, yPixel, e) && predicate(e) && (highest == null || highest!!.layer < e.layer)) {
                    highest = e
                }
                e.children.forEach { recurse(it) }
            }
        }
        recurse(window.rootChild)
        return highest
    }

    fun screenSizeChange(oldWidth: Int, oldHeight: Int) {
        windows.forEach {
            if (it.adjustDimensions) {
                it.widthPixels = ((Game.WIDTH.toDouble() / oldWidth) * it.widthPixels).toInt()
                it.heightPixels = ((Game.HEIGHT.toDouble() / oldHeight) * it.heightPixels).toInt()
            }
            it.onScreenSizeChange(oldWidth, oldHeight)
        }
    }

    override fun handleControlPress(p: ControlPress) {
        val x = Mouse.xPixel
        val y = Mouse.yPixel
        val t = p.pressType
        val b = Mouse.button
        if (p.control == Control.INTERACT) {
            if(t == PressType.REPEAT) {
                elementBeingInteractedWith!!.onMouseActionOn(t, x, y, b)
                return
            }
            val highestW = getHighestWindow(x, y, { !it.transparentToInteraction })
            if (highestW != null) {
                val highestE = getHighestElement(highestW, x, y, { !it.transparentToInteraction })
                highestW.windowGroup.bringToTop(highestW)
                if (t == PressType.PRESSED) {
                    if (highestE != null) {
                        highestE.onMouseActionOn(t, x, y, b)
                        elementBeingInteractedWith = highestE
                    }
                } else if (t == PressType.RELEASED) {
                    if (highestE != null) {
                        highestE.onMouseActionOn(t, x, y, b)
                        elementBeingInteractedWith = null
                    }
                }
                fun recursivelyCallMouseOff(e: RootGUIElement) {
                    if (e.open && e !== highestE) {
                        e.onMouseActionOff(t, x, y, b)
                        e.children.forEach { recursivelyCallMouseOff(it) }
                    }
                }
                openWindows.stream().forEachOrdered { recursivelyCallMouseOff(it.rootChild) }
            }
        } else if (p.control == Control.SCROLL_DOWN || p.control == Control.SCROLL_UP) {
            val dir = if (p.control == Control.SCROLL_DOWN) -1 else 1
            val highestW = getHighestWindow(x, y, { !it.transparentToInteraction })
            if (highestW != null) {
                highestW.windowGroup.bringToTop(highestW)
                val highestE = getHighestElement(highestW, x, y, { !it.transparentToInteraction })
                highestE!!
                highestE.onMouseScroll(dir)
            }
        } else if (p.control == Control.DEBUG && p.pressType == PressType.PRESSED) {
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
        }
    }
}