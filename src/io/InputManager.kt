package io

import main.Game
import java.awt.event.*
import java.util.concurrent.CopyOnWriteArrayList
import io.OutputManager as out

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

    val keysDown = mutableSetOf<Int>()

    val mouseButtonsDown = BooleanArray(5)

    var currentModifiers = 0

    /* Each control can only happen once per update */
    val queue = linkedSetOf<ControlPress>()

    var keyPress = CopyOnWriteArrayList<KeyEvent>()
    var keyRelease = CopyOnWriteArrayList<KeyEvent>()

    var mousePress = CopyOnWriteArrayList<MouseEvent>()
    var mouseRelease = CopyOnWriteArrayList<MouseEvent>()
    var mouseMoved: MouseEvent? = null

    var mouseWheelEvent: MouseWheelEvent? = null

    val mouseMovementListeners = mutableListOf<MouseMovementListener>()

    var mouseXPixel = 0
    var mouseYPixel = 0

    var mouseOutside = false

    fun registerControlPressHandler(h: ControlPressHandler, controls: Map<ControlMap, Array<out Control>>? = null) {
        handlers.put(h, controls)
    }

    fun registerControlPressHandler(h: ControlPressHandler, vararg controls: Control) {
        val m = mutableMapOf<ControlMap, Array<out Control>>()
        for (map in ControlMap.values()) {
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
            Check to see if it is within valid bounds and the key in question is currently held down. Waits to clear so key presses can check if they were also released
             */
            if (keysDown.contains(i)) {
                currentModifiers = k.modifiers
                if (print)
                    out.println("${k.extendedKeyCode} : RELEASED")
                map.translateKey(i, keysDown.filter {it != i}.toMutableSet()).forEach { queue.add(ControlPress(it, PressType.RELEASED)) }
                keysDown.remove(i)
            }
        }
        for (key in keysDown) {
            if (print)
                out.println("$key : REPEAT")
            map.translateKey(key, keysDown.filter {it != key}.toMutableSet()).forEach { queue.add(ControlPress(it, PressType.REPEAT)) }
        }
        for (k in keyPress) {
            val i = k.extendedKeyCode
            if (!keysDown.contains(i)) {
                keysDown.add(i)
                currentModifiers = k.modifiers
                if (print)
                    out.println("${i} : PRESSED")
                map.translateKey(i, keysDown.filter {it != i}.toMutableSet()).forEach { queue.add(ControlPress(it, PressType.PRESSED)) }
            }
            if (keyRelease.stream().anyMatch { it.extendedKeyCode == i }) {
                if (print)
                    out.println("$i : RELEASED")
                keysDown.remove(i)
                map.translateKey(i, keysDown.filter {it != i}.toMutableSet()).forEach { queue.add(ControlPress(it, PressType.RELEASED)) }
            }
        }
        keyRelease.clear()
        keyPress.clear()
        if (mouseMoved != null) {
            val newMouseXPixel = mouseMoved!!.x / Game.SCALE
            val newMouseYPixel = mouseMoved!!.y / Game.SCALE
            if (newMouseXPixel != mouseXPixel || newMouseYPixel != mouseYPixel) {
                mouseMovementListeners.forEach { it.onMouseMove(mouseXPixel, mouseYPixel) }
                mouseXPixel = mouseMoved!!.x / Game.SCALE
                mouseYPixel = mouseMoved!!.y / Game.SCALE
            }
            mouseMoved = null
        }
        for (m in mouseRelease) {
            val i = m.button
            if (mouseButtonsDown[i - 1]) {
                mouseButtonsDown[i - 1] = false
                currentModifiers = m.modifiers
                if (print)
                    out.println("MOUSE $i : RELEASED")
                map.translateMouse(i, keysDown).forEach { queue.add(ControlPress(it, PressType.RELEASED)) }
            }
        }
        mouseRelease.clear()
        for (button in mouseButtonsDown.indices) {
            if (mouseButtonsDown[button]) {
                if (print)
                    out.println("MOUSE ${button + 1} : REPEAT")
                map.translateMouse(button + 1, keysDown).forEach { queue.add(ControlPress(it, PressType.REPEAT)) }
            }
        }
        for (m in mousePress) {
            val i = m.button
            if (!mouseButtonsDown[i - 1]) {
                mouseButtonsDown[i - 1] = true
                currentModifiers = m.modifiers
                if (print)
                    out.println("MOUSE $i : PRESSED")
                map.translateMouse(i, keysDown).forEach { queue.add(ControlPress(it, PressType.PRESSED)) }
            }
        }
        mousePress.clear()
        if (mouseWheelEvent != null) {
            val i: Int = mouseWheelEvent!!.wheelRotation
            currentModifiers = mouseWheelEvent!!.modifiers
            if (print)
                out.println("MOUSE WHEEL " + if (i == -1) "DOWN" else if (i == 1) "UP" else "???")
            map.translateMouseWheel(i, keysDown).forEach { queue.add(ControlPress(it, PressType.PRESSED)) }
            mouseWheelEvent = null
        }
        /* Execute */
        if (print)
            out.println("QUEUE: [${queue.joinToString()}]")
        for (p in queue) {
            for ((k, v) in handlers) {
                if (v != null) {
                    if (v.containsKey(map)) {
                        val controls = v.get(map)
                        if (controls != null) {
                            if (controls.contains(p.control))
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
        mouseMoved = e
    }

    override fun mouseDragged(e: MouseEvent) {
        mouseMoved = e
    }

}