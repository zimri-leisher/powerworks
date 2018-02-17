package io

import main.Game
import misc.ConcurrentlyModifiableMutableMap
import misc.WeakMutableList
import screen.Mouse
import screen.ScreenManager
import screen.elements.GUIView
import java.awt.event.*
import io.OutputManager as out

interface ControlPressHandler {
    fun handleControlPress(p: ControlPress)
}

enum class ControlPressHandlerType {
    /** Anywhere, no matter what */
    GLOBAL,
    /** Only this object in the level*/
    LEVEL_THIS,
    /** Anywhere in the level*/
    LEVEL_ANY,
    /** Only this object on the screen*/
    SCREEN_THIS
    /** Anywhere on the screen*/
    // no need for SCREEN_ANY because that's the same as global
}

data class ControlPress(val control: Control, val pressType: PressType)

enum class PressType { PRESSED, REPEAT, RELEASED }

enum class SpecialChar(val char: Char) {
    ESCAPE(''),
    ENTER('\n'),
    BACKSPACE('\b'),
    UP_ARROW('f'),
    DOWN_ARROW('f'),
    LEFT_ARROW('f'),
    RIGHT_ARROW('f');

    companion object {
        operator fun get(c: Char): SpecialChar? {
            return values().firstOrNull { it.char == c }
        }
    }
}

object InputManager : KeyListener, MouseWheelListener, MouseListener, MouseMotionListener {

    val map = ControlMap.DEFAULT

    val handlers = ConcurrentlyModifiableMutableMap<
            Pair<
                    ControlPressHandler,
                    ControlPressHandlerType>,
            Map<
                    ControlMap,
                    Array<out Control>?>?>()

    var currentScreenHandlers = WeakMutableList<ControlPressHandler>()
    var currentLevelHandlers = WeakMutableList<ControlPressHandler>()

    val inputsBeingPressed = mutableSetOf<String>()

    var inputEvent = ConcurrentlyModifiableMutableMap<String, PressType>()

    /* Each control can only happen once per update */
    val queue = linkedSetOf<ControlPress>()

    var mouseMoved: MouseEvent? = null

    val mouseMovementListeners = WeakMutableList<MouseMovementListener>()

    var mouseOutside = false

    /**
     * The characters that will be sent to text handler
     */
    val charsInTextQueue = mutableListOf<Char>()

    /**
     * The object that should be sent key type (as in related to pressing, not category) events.
     * If the character typed is one of the SpecialChars, it will be sent through the handleSpecialChar method,
     * otherwise it will be send through handleChar
     */
    var textHandler: TextHandler? = null

    /**
     * Registers a control press handler
     * @param type one of GLOBAL, LEVEL_ANY, LEVEL_THIS and SCREEN_THIS
     * @param controls the first element is the control map that these controls are active on, the second is the list of controls to send to this
     */
    fun registerControlPressHandler(h: ControlPressHandler, type: ControlPressHandlerType, controls: Map<ControlMap, Array<Control>>? = null) {
        handlers.put(Pair(h, type), controls)
    }

    /**
     * Registers a control press handler
     * @param type one of GLOBAL, LEVEL_ANY, LEVEL_THIS and SCREEN_THIS
     * @param controls the list of controls to send to this
     */
    fun registerControlPressHandler(h: ControlPressHandler, type: ControlPressHandlerType, vararg controls: Control) {
        val m = mutableMapOf<ControlMap, Array<out Control>>()
        for (map in ControlMap.values()) {
            m.put(map, controls)
        }
        handlers.put(Pair(h, type), m)
    }

    /**
     * Registers a control press handler
     * @param type one of GLOBAL, LEVEL_ANY, LEVEL_THIS and SCREEN_THIS
     * @param map whenever this map is active, all controls will be sent to this
     */
    fun registerControlPressHandler(h: ControlPressHandler, type: ControlPressHandlerType, map: ControlMap) {
        handlers.put(Pair(h, type), mapOf<ControlMap, Array<Control>?>(Pair(map, null)))
    }

    /**
     * Removes the entry corresponding with the handler
     */
    fun removeControlPressHandler(handler: ControlPressHandler) {
        for ((k, _) in handlers) {
            if (k.first == handler) {
                handlers.remove(k)
            }
        }
    }

    fun update() {
        if (textHandler != null) {
            charsInTextQueue.forEach {
                val sp = SpecialChar[it]
                if(sp != null)
                    textHandler!!.handleSpecialKey(sp)
                else
                    textHandler!!.handleChar(it)
            }
            charsInTextQueue.clear()
        }
        for (i in inputsBeingPressed) {
            map.translate(i, inputsBeingPressed.filter { it != i }.toMutableSet()).forEach { queue.add(ControlPress(it, PressType.REPEAT)) }
        }
        for ((k, v) in inputEvent) {
            if ((v == PressType.PRESSED && !inputsBeingPressed.contains(k)) || (v == PressType.RELEASED && inputsBeingPressed.contains(k))) {
                map.translate(k, inputsBeingPressed.filter { it != k }.toMutableSet()).forEach { queue.add(ControlPress(it, v)) }
                if (v == PressType.PRESSED &&
                        /* Wheels are not able to be held down, so you shouldn't add them to the repeat */
                        !k.contains("WHEEL")) {
                    inputsBeingPressed.add(k)
                } else if (v == PressType.RELEASED) {
                    inputsBeingPressed.remove(k)
                }
            }
        }
        inputEvent.clear()
        if (mouseMoved != null) {
            val newMouseXPixel = mouseMoved!!.x / Game.SCALE
            val newMouseYPixel = mouseMoved!!.y / Game.SCALE
            if (newMouseXPixel != Mouse.xPixel || newMouseYPixel != Mouse.yPixel) {
                mouseMovementListeners.forEach { it.onMouseMove(Mouse.xPixel, Mouse.yPixel) }
                Mouse.xPixel = mouseMoved!!.x / Game.SCALE
                Mouse.yPixel = mouseMoved!!.y / Game.SCALE
            }
            mouseMoved = null
        }
        for (p in queue) {
            handlers.forEach { k, v ->
                if (k.second == ControlPressHandlerType.GLOBAL ||
                        (k.second == ControlPressHandlerType.SCREEN_THIS && currentScreenHandlers.contains(k.first)) ||
                        (k.second == ControlPressHandlerType.LEVEL_ANY && ScreenManager.selectedElement is GUIView) ||
                        (k.second == ControlPressHandlerType.LEVEL_THIS &&
                                // we want the part below because otherwise the level controls will trigger even when we
                                // press on a gui element that is higher. However, we don't care if it is a gui view because
                                // that's how we're supposed to interact
                                (ScreenManager.selectedWindow == null || ScreenManager.selectedElement is GUIView)
                                && currentLevelHandlers.contains(k.first))) {
                    sendPress(p, k.first, v)
                }
            }
        }
        queue.clear()
    }

    private fun sendPress(p: ControlPress, k: ControlPressHandler, v: Map<ControlMap, Array<out Control>?>?) {
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

    override fun keyTyped(e: KeyEvent) {
        charsInTextQueue.add(e.keyChar)
    }

    override fun keyPressed(e: KeyEvent) {
        if (textHandler == null)
            inputEvent.put(KeyEvent.getKeyText(e.extendedKeyCode).toUpperCase(), PressType.PRESSED)
    }

    override fun keyReleased(e: KeyEvent) {
        if (textHandler == null)
            inputEvent.put(KeyEvent.getKeyText(e.extendedKeyCode).toUpperCase(), PressType.RELEASED)
    }

    override fun mouseWheelMoved(e: MouseWheelEvent) {
        inputEvent.put("WHEEL_${if (e.wheelRotation == -1) "DOWN" else "UP"}", PressType.PRESSED)
    }

    override fun mouseReleased(e: MouseEvent) {
        Mouse.button = e.button
        inputEvent.put("MOUSE_${e.button}", PressType.RELEASED)
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
        for (i in 1..5)
            inputEvent.put("MOUSE_$i", PressType.RELEASED)
    }

    override fun mousePressed(e: MouseEvent) {
        Mouse.button = e.button
        inputEvent.put("MOUSE_${e.button}", PressType.PRESSED)
    }

    override fun mouseMoved(e: MouseEvent) {
        mouseMoved = e
    }

    override fun mouseDragged(e: MouseEvent) {
        mouseMoved = e
    }

}