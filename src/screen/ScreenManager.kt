package screen

import io.*
import misc.GeometryHelper

object ScreenManager : ControlPressHandler {

    init {
        InputManager.registerControlPressHandler(this, Control.INTERACT, Control.DEBUG)
    }

    val guiElements = mutableListOf<GUIElement>()
    val openGuiElements = mutableListOf<RootGUIElement>()

    fun update() {
        updateMouseOn()
        openGuiElements.forEach { it.update() }
    }

    fun updateMouseOn() {
        openGuiElements.stream().forEach {
            if (GeometryHelper.contains(it.xPixel, it.yPixel, it.widthPixels, it.heightPixels, InputManager.mouseXPixel, InputManager.mouseYPixel, 0, 0)) {
                if (!it.mouseOn) {
                    it.mouseOn = true
                }
            } else if (it.mouseOn) {
                it.mouseOn = false
            }
        }
    }

    fun render() {
        openGuiElements.stream().sorted { o2, o1 -> o2.layer.compareTo(o1.layer) }.forEach { it.render() }
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.control == Control.INTERACT) {
            val t = p.pressType
            val x = InputManager.mouseXPixel
            val y = InputManager.mouseYPixel
            updateMouseOn()
            if (openGuiElements.size > 0) {
                openGuiElements.stream().filter { it.mouseOn }.sorted { o2, o1 -> o1.layer.compareTo(o2.layer) }.findFirst().get().onMouseActionOn(t, x, y)
                openGuiElements.stream().filter { !it.mouseOn }.forEach { it.onMouseActionOff(t, x, y) }
            }
        } else if(p.control == Control.DEBUG && p.pressType == PressType.PRESSED) {
            fun RootGUIElement.print(spaces: String = ""): String {
                var v = spaces + toString()
                for(g in children)
                    v += "\n" + g.print(spaces + "  ")
                return v
            }
            println(RootGUIElementObject.print())
        }
    }
}