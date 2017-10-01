package screen

import io.*
import misc.GeometryHelper

object ScreenManager : ControlPressHandler, MouseMovementListener {

    init {
        InputManager.mouseMovementListeners.add(this)
        InputManager.registerControlPressHandler(this, Control.DEBUG, Control.INTERACT, Control.SCROLL_UP, Control.SCROLL_DOWN)
    }

    val guiElements = mutableListOf<GUIElement>()
    val openGuiElements = mutableListOf<RootGUIElement>()

    fun update() {
        openGuiElements.forEach { it.update() }
    }

    fun updateMouseOn() {
        openGuiElements.forEach {
            updateMouseOn(it)
        }
    }

    fun updateMouseOn(e: RootGUIElement) {
        if (GeometryHelper.contains(e.xPixel, e.yPixel, e.widthPixels, e.heightPixels, Mouse.xPixel, Mouse.yPixel, 0, 0)) {
            if (!e.mouseOn) {
                e.mouseOn = true
            }
        } else if (e.mouseOn) {
            e.mouseOn = false
        }
    }

    fun render() {
        openGuiElements.stream().filter { it.autoRender }.sorted { o2, o1 -> o2.layer.compareTo(o1.layer) }.forEach { it.render() }
    }

    override fun onMouseMove(pXPixel: Int, pYPixel: Int) {
        updateMouseOn()
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.control == Control.INTERACT) {
            val t = p.pressType
            val x = Mouse.xPixel
            val y = Mouse.yPixel
            val b = Mouse.button
            updateMouseOn()
            if (openGuiElements.size > 0) {
                val o = openGuiElements.stream().filter { it.mouseOn }.sorted { o2, o1 -> o1.layer.compareTo(o2.layer) }.findFirst().orElseGet { null }
                o?.onMouseActionOn(t, x, y, b)
                openGuiElements.stream().filter { it != o }.forEach { it.onMouseActionOff(t, x, y, b) }
            }
        } else if (p.control == Control.SCROLL_DOWN) {
            updateMouseOn()
            if (openGuiElements.size > 0) {
                openGuiElements.stream().filter { it.mouseOn }.sorted { o2, o1 -> o1.layer.compareTo(o2.layer) }.findFirst().orElseGet { null }?.onMouseScroll(-1)
            }
        } else if (p.control == Control.SCROLL_UP) {
            updateMouseOn()
            if (openGuiElements.size > 0) {
                openGuiElements.stream().filter { it.mouseOn }.sorted { o2, o1 -> o1.layer.compareTo(o2.layer) }.findFirst().orElseGet { null }?.onMouseScroll(1)
            }
        } else if (p.control == Control.DEBUG && p.pressType == PressType.PRESSED) {
            /*
            fun RootGUIElement.print(spaces: String = ""): String {
                var v = spaces + toString()
                for (g in children)
                    v += "\n" + g.print(spaces + "  ")
                return v
            }
            println(RootGUIElementObject.print())
             */
        }
    }
}