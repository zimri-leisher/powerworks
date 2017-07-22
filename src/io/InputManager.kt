package io

import java.awt.event.*

interface ControlPressHandler {
    fun handleControlPress(p: ControlPress)
}

data class ControlPress(val control: Control, val pressType: PressType)

enum class PressType { PRESSED, REPEAT, RELEASED }

object InputManager : KeyListener, MouseWheelListener, MouseListener, MouseMotionListener {

    val map = ControlMap.DEFAULT

    val handlers = mutableMapOf<ControlPressHandler, Map<ControlMap, Control>?>()

    val keysDown = BooleanArray(156)

    var currentModifiers = 0

    /* Each control can only happen once per update */
    val queue = linkedSetOf<ControlPress>()

    var keyPress = mutableListOf<KeyEvent>()
    var keyRelease = mutableListOf<KeyEvent>()

    var mouseXPixel = 0
    var mouseYPixel = 0

    fun registerControlPressHandler(h: ControlPressHandler, controls: Map<ControlMap, Control>? = null) {
        handlers.put(h, controls)
    }

    fun update() {
        for (k in keyRelease) {
            val i = k.extendedKeyCode
            /*
            Check to see if it is within valid bounds and the key in question is currently held down
             */
            if (i < keysDown.size && keysDown[i] == true) {
                keysDown[i] = false
                currentModifiers = k.modifiers
                map.translateKey(i, currentModifiers).forEach { queue.add(ControlPress(it, PressType.RELEASED)) }
            }
        }
        for (key in keysDown.indices) {
            if(keysDown[key])
                map.translateKey(key, currentModifiers).forEach { queue.add(ControlPress(it, PressType.REPEAT)) }
        }
        for (k in keyPress) {
            val i = k.extendedKeyCode
            /*
            Check to see if it is within valid bounds and the key in question is currently not pressed
             */
            if (i < keysDown.size && keysDown[i] == false) {
                keysDown[i] = true
                currentModifiers = k.modifiers
                map.translateKey(i, currentModifiers).forEach { queue.add(ControlPress(it, PressType.PRESSED)) }
            }

        }
        for (p in queue) {

        }
    }

    override fun keyTyped(e: KeyEvent) {
    }

    override fun keyPressed(e: KeyEvent) {
        println(e.extendedKeyCode)
        //keyPress.add(e)
    }

    override fun keyReleased(e: KeyEvent) {
        keyRelease.add(e)
    }

    override fun mouseWheelMoved(e: MouseWheelEvent) {
    }

    override fun mouseReleased(e: MouseEvent) {
    }

    override fun mouseEntered(e: MouseEvent) {
    }

    override fun mouseClicked(e: MouseEvent) {
    }

    override fun mouseExited(e: MouseEvent) {
    }

    override fun mousePressed(e: MouseEvent) {
    }

    override fun mouseMoved(e: MouseEvent) {
    }

    override fun mouseDragged(e: MouseEvent) {
    }

}