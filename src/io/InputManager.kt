package io

import main.Game
import io.OutputManager as out
import java.awt.event.*

interface ControlPressHandler {
    fun handleControlPress(p: ControlPress)
}

data class ControlPress(val control: Control, val pressType: PressType)

enum class PressType { PRESSED, REPEAT, RELEASED }

object InputManager : KeyListener, MouseWheelListener, MouseListener, MouseMotionListener {

    val map = ControlMap.DEFAULT

    /* Print info to out */
    var print = false

    val handlers = mutableMapOf<ControlPressHandler, Map<ControlMap, Array<out Control>?>?>()

    val keysDown = BooleanArray(156)

    val mouseButtonsDown = BooleanArray(5)

    var currentModifiers = 0

    /* Each control can only happen once per update */
    val queue = linkedSetOf<ControlPress>()

    var keyPress = mutableListOf<KeyEvent>()
    var keyRelease = mutableListOf<KeyEvent>()

    var mousePress = mutableListOf<MouseEvent>()
    var mouseRelease = mutableListOf<MouseEvent>()

    var mouseWheelEvent: MouseWheelEvent? = null

    var mouseXPixel = 0
    var mouseYPixel = 0

    var mouseOutside = false

    fun registerControlPressHandler(h: ControlPressHandler, controls: Map<ControlMap, Array<out Control>>? = null) {
        handlers.put(h, controls)
    }

    fun registerControlPressHandler(h: ControlPressHandler, vararg controls: Control) {
        val m = mutableMapOf<ControlMap, Array<out Control>>()
        for(map in ControlMap.values()) {
            m.put(map, controls)
        }
        handlers.put(h, m)
    }

    fun registerControlPressHandler(h: ControlPressHandler, map: ControlMap) {
        handlers.put(h, mapOf<ControlMap, Array<Control>?>(Pair(map, null)))
    }

    fun update() {
        /* Translate */
        for (k in keyRelease) {
            val i = k.extendedKeyCode
            /*
            Check to see if it is within valid bounds and the key in question is currently held down
             */
            if (i < keysDown.size && keysDown[i]) {
                keysDown[i] = false
                currentModifiers = k.modifiers
                if (print)
                    out.println("${k.extendedKeyCode} : RELEASED")
                map.translateKey(i, currentModifiers).forEach { queue.add(ControlPress(it, PressType.RELEASED)) }
            }
        }
        keyRelease.clear()
        for (key in keysDown.indices) {
            if (keysDown[key]) {
                if (print)
                    out.println("$key : REPEAT")
                map.translateKey(key, currentModifiers).forEach { queue.add(ControlPress(it, PressType.REPEAT)) }
            }
        }
        for (k in keyPress) {
            val i = k.extendedKeyCode
            if (i < keysDown.size && !keysDown[i]) {
                keysDown[i] = true
                currentModifiers = k.modifiers
                if (print)
                    out.println("${k.extendedKeyCode} : PRESSED")
                map.translateKey(i, currentModifiers).forEach { queue.add(ControlPress(it, PressType.PRESSED)) }
            }

        }
        keyPress.clear()
        for (m in mouseRelease) {
            val i = m.button
            if (mouseButtonsDown[i - 1]) {
                mouseButtonsDown[i - 1] = false
                currentModifiers = m.modifiers
                if (print)
                    out.println("MOUSE $i : RELEASED")
                map.translateMouse(i, currentModifiers).forEach { queue.add(ControlPress(it, PressType.RELEASED)) }
            }
        }
        mouseRelease.clear()
        for (button in mouseButtonsDown.indices) {
            if (mouseButtonsDown[button]) {
                if (print)
                    out.println("MOUSE ${button + 1} : REPEAT")
                map.translateMouse(button + 1, currentModifiers).forEach { queue.add(ControlPress(it, PressType.REPEAT)) }
            }
        }
        for (m in mousePress) {
            val i = m.button
            if (!mouseButtonsDown[i - 1]) {
                mouseButtonsDown[i - 1] = true
                currentModifiers = m.modifiers
                if (print)
                    out.println("MOUSE $i : PRESSED")
                map.translateMouse(i, currentModifiers).forEach { queue.add(ControlPress(it, PressType.PRESSED)) }
            }
        }
        mousePress.clear()
        if (mouseWheelEvent != null) {
            val i: Int = mouseWheelEvent!!.scrollAmount
            if (print)
                out.println("MOUSE WHEEL " + if (i == -1) "DOWN" else "UP")
            map.translateMouseWheel(i, currentModifiers).forEach { queue.add(ControlPress(it, PressType.PRESSED)) }
            mouseWheelEvent = null
        }
        /* Execute */
        for (p in queue) {
            for ((k, v) in handlers) {
                if(v != null) {
                    if(v.containsKey(map)) {
                        val controls = v.get(map)
                        if(controls != null) {
                            if(controls.contains(p.control))
                                k.handleControlPress(p)
                        } else {
                            k.handleControlPress(p)
                        }
                    }
                } else {
                    k.handleControlPress(p)
                }
            }
        }
        queue.clear()
    }

    override fun keyTyped(e: KeyEvent) {
    }

    override fun keyPressed(e: KeyEvent) {
        keyPress.add(e)
    }

    override fun keyReleased(e: KeyEvent) {
        keyRelease.add(e)
    }

    override fun mouseWheelMoved(e: MouseWheelEvent) {
        mouseWheelEvent = e
    }

    override fun mouseReleased(e: MouseEvent) {
        mouseRelease.add(e)
    }

    override fun mouseEntered(e: MouseEvent) {
        mouseOutside = false
        Game.clearMouseIcon()
    }

    override fun mouseClicked(e: MouseEvent) {
    }

    override fun mouseExited(e: MouseEvent) {
        mouseOutside = true
        Game.resetMouseIcon()
    }

    override fun mousePressed(e: MouseEvent) {
        mousePress.add(e)
    }

    override fun mouseMoved(e: MouseEvent) {
    }

    override fun mouseDragged(e: MouseEvent) {
    }

}