package io

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import data.ConcurrentlyModifiableMutableMap
import data.WeakMutableList
import level.Level
import level.LevelManager
import level.LevelObject
import main.Game
import main.State
import screen.ScreenManager
import screen.elements.GUILevelView
import screen.mouse.Mouse
import java.awt.event.KeyEvent
import java.lang.ref.WeakReference

interface ControlPressHandler {
    fun handleControlPress(p: ControlPress)
}

enum class ControlPressHandlerType {
    /** Anywhere, no matter what */
    GLOBAL,
    /**
     * Only when this [LevelObject] is under the mouse
     */
    LEVEL_THIS_UNDER_MOUSE,
    /**
     * Only when this [LevelObject] is the last [LevelObject] interacted with (clicked on)
     */
    LEVEL_THIS_LAST_SELECTED,
    /**
     * Only when there is a [Level] under the mouse
     */
    LEVEL_ANY_UNDER_MOUSE,
    /**
     * Only when the [State.CURRENT_STATE] is [State.INGAME]
     */
    INGAME_ONLY,
    /**
     * Only this object on the screen
     */
    SCREEN_THIS
    // no need for SCREEN_ANY because that's the same as global
}

data class ControlPress(val control: Control, val pressType: PressType)

enum class PressType { PRESSED, REPEAT, RELEASED }

object InputManager : InputProcessor {
    var map = ControlMap.DEFAULT

    val handlers = ConcurrentlyModifiableMutableMap<
            // the handler itself and the handler type (GLOBAL, LEVEL_ANY_UNDER_MOUSE, etc.)
            Pair<
                    WeakReference<ControlPressHandler>,
                    ControlPressHandlerType>,
            // the control maps and the controls themselves
            // each pair in this map specifies the control map and the controls that the handler wants to receive when that map is active
            Map<
                    ControlMap,
                    // you've tried to remove this 'out' before. don't bother. lmao kotlin generics
                    Array<out Control>?>?>()

    var currentScreenHandlers = WeakMutableList<ControlPressHandler>()

    var currentLevelHandlers = WeakMutableList<ControlPressHandler>()
    val inputsBeingPressed = mutableSetOf<String>()

    var inputEvent = ConcurrentlyModifiableMutableMap<String, PressType>()

    /* Each control can only happen once per update */
    var queue = linkedSetOf<ControlPress>()

    val mouseMovementListeners = WeakMutableList<MouseMovementListener>()

    var mouseOutside = false

    /**
     * The characters that will be sent to the text handler
     */
    val charsTyped = mutableListOf<Char>()

    /**
     * The object that should be sent key type (the word 'type' as in related to pressing, not category) events.
     */
    var textHandler: TextHandler? = null

    private var actualMouseXPixel = 0
    private var actualMouseYPixel = 0

    /**
     * Registers a control press handler
     * @param type one of GLOBAL, LEVEL_ANY_UNDER_MOUSE, LEVEL_THIS_UNDER_MOUSE and SCREEN_THIS
     * @param controls the control group of controls to send to this
     */
    fun registerControlPressHandler(h: ControlPressHandler, type: ControlPressHandlerType, controls: Control.Group) {
        registerControlPressHandler(h, type, *controls.controls)
    }

    /**
     * Registers a control press handler
     * @param type one of GLOBAL, LEVEL_ANY_UNDER_MOUSE, LEVEL_THIS_UNDER_MOUSE and SCREEN_THIS
     * @param controls the first element is the control map that these controls are active on, the second is the list of controls to send to this
     */
    fun registerControlPressHandler(h: ControlPressHandler, type: ControlPressHandlerType, controls: Map<ControlMap, Array<out Control>>? = null) {
        handlers.put(Pair(WeakReference(h), type), controls)
    }

    /**
     * Registers a control press handler
     * @param type one of GLOBAL, LEVEL_ANY_UNDER_MOUSE, LEVEL_THIS_UNDER_MOUSE and SCREEN_THIS
     * @param controls the list of controls to send to this
     */
    fun registerControlPressHandler(h: ControlPressHandler, type: ControlPressHandlerType, controls: List<Control>) {
        registerControlPressHandler(h, type, *controls.toTypedArray())
    }

    /**
     * Registers a control press handler
     * @param type one of GLOBAL, LEVEL_ANY_UNDER_MOUSE, LEVEL_THIS_UNDER_MOUSE and SCREEN_THIS
     * @param map the map for which controls should be sent to the handler. If the map is any other, even if the controls
     * match, this will not receive them
     * @param controls the list of controls to send to this
     */
    fun registerControlPressHandler(h: ControlPressHandler, type: ControlPressHandlerType, map: ControlMap, vararg controls: Control) {
        registerControlPressHandler(h, type, mapOf(map to controls))
    }

    /**
     * Registers a control press handler
     * @param type one of GLOBAL, LEVEL_ANY_UNDER_MOUSE, LEVEL_THIS_UNDER_MOUSE and SCREEN_THIS
     * @param controls the list of controls to send to this
     */
    fun registerControlPressHandler(h: ControlPressHandler, type: ControlPressHandlerType, vararg controls: Control) {
        val m = mutableMapOf<ControlMap, Array<out Control>>()
        for (map in ControlMap.values()) {
            m.put(map, controls)
        }
        handlers.put(Pair(WeakReference(h), type), m)
    }

    /**
     * Registers a control press handler
     * @param type one of GLOBAL, LEVEL_ANY_UNDER_MOUSE, LEVEL_THIS_UNDER_MOUSE and SCREEN_THIS
     * @param map whenever this map is active, all controls will be sent to this
     */
    fun registerControlPressHandler(h: ControlPressHandler, type: ControlPressHandlerType, map: ControlMap) {
        handlers.put(Pair(WeakReference(h), type), mapOf<ControlMap, Array<Control>?>(Pair(map, null)))
    }

    /**
     * Removes the entry corresponding with the handler
     */
    fun removeControlPressHandler(handler: ControlPressHandler) {
        handlers.forEach { k, _ ->
            if (k.first.get() == handler) {
                handlers.remove(k)
            }
        }
    }


    fun update() {
        if (textHandler != null) {
            charsTyped.forEach {
                textHandler!!.handleChar(it)
            }
        }
        charsTyped.clear()

        for (i in inputsBeingPressed) {
            map.translate(i, inputsBeingPressed.filter { it != i }.toMutableSet()).forEach { queue.add(ControlPress(it, PressType.REPEAT)) }
        }

        for ((k, v) in inputEvent) {
            if ((v == PressType.PRESSED && !inputsBeingPressed.contains(k)) || (v == PressType.RELEASED && inputsBeingPressed.contains(k))) {
                map.translate(k, inputsBeingPressed.filter { it != k }.toMutableSet()).forEach { queue.add(ControlPress(it, v)); if (v == PressType.RELEASED) queue.remove(ControlPress(it, PressType.REPEAT)) }
                //println(k + ", " + inputsBeingPressed.filter { it != k }.toMutableSet())
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

        if (Mouse.xPixel != actualMouseXPixel || Mouse.yPixel != actualMouseYPixel) {
            val newMouseXPixel = actualMouseXPixel
            val newMouseYPixel = actualMouseYPixel
            if (newMouseXPixel != Mouse.xPixel || newMouseYPixel != Mouse.yPixel) {
                mouseMovementListeners.forEach { it.onMouseMove(Mouse.xPixel, Mouse.yPixel) }
                Mouse.xPixel = actualMouseXPixel
                Mouse.yPixel = actualMouseYPixel
            }
            /*
            // TODO find out how to do this
            if (Mouse.isInsideWindow() && mouseOutside) {
                mouseOutside = false
                Game.clearMouseIcon()
            } else if (!org.lwjgl.input.Mouse.isInsideWindow() && !mouseOutside) {
                mouseOutside = true
                Game.resetMouseIcon()
                for (i in 0..4)
                    inputEvent.put("MOUSE_${getButtonName(i)}", PressType.RELEASED)
            }
            */
        }

        // this is here so that interaction controls get sent first because they can occasionally determine where other controls get sent
        queue = LinkedHashSet(queue.sortedBy { if (it.control in Control.Group.INTERACTION) -1 else 0 })
        for (p in queue) {
            sendPress(p)
        }
        queue.clear()
    }

    private fun sendPress(p: ControlPress) {
        handlers.forEach { k, v ->
            if (k.first.get() == null) {
                handlers.remove(k)
            } else {
                val handler = k.first.get()!!
                if (k.second == ControlPressHandlerType.GLOBAL ||
                        (k.second == ControlPressHandlerType.SCREEN_THIS && currentScreenHandlers.contains(handler)) ||
                        (k.second == ControlPressHandlerType.LEVEL_ANY_UNDER_MOUSE && ScreenManager.elementLastInteractedWith is GUILevelView && LevelManager.levelUnderMouse != null) ||
                        (k.second == ControlPressHandlerType.LEVEL_THIS_LAST_SELECTED && LevelManager.levelObjectLastInteractedWith == handler) ||
                        (k.second == ControlPressHandlerType.LEVEL_THIS_UNDER_MOUSE && LevelManager.levelObjectUnderMouse != null && currentLevelHandlers.contains(handler)) ||
                        (k.second == ControlPressHandlerType.INGAME_ONLY && State.CURRENT_STATE == State.INGAME)) {
                    if (v != null) {
                        if (v.containsKey(map)) {
                            val controls = v.get(map)
                            if (controls != null) {
                                if (controls.contains(p.control)) {
                                    handler.handleControlPress(p)
                                }
                            } else {
                                handler.handleControlPress(p)
                            }
                        }
                    } else {
                        handler.handleControlPress(p)
                    }
                }
            }
        }
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        actualMouseXPixel = screenX / Game.SCALE
        actualMouseYPixel = (Game.HEIGHT - screenY / Game.SCALE)
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        inputEvent.put(getKeyName(keycode), PressType.PRESSED)
        return true
    }

    override fun keyTyped(character: Char): Boolean {
        if (isPrintableChar(character))
            charsTyped.add(character)
        return true
    }

    private fun isPrintableChar(c: Char): Boolean {
        val block = Character.UnicodeBlock.of(c)
        return !Character.isISOControl(c) &&
                c != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block !== Character.UnicodeBlock.SPECIALS
    }

    override fun keyUp(keycode: Int): Boolean {
        inputEvent.put(getKeyName(keycode), PressType.RELEASED)
        return true
    }

    override fun scrolled(amount: Int): Boolean {
        inputEvent.put("WHEEL_${if (amount == -1) "DOWN" else "UP"}", PressType.PRESSED)
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        actualMouseXPixel = screenX / Game.SCALE
        // ugh. fuck this :(
        actualMouseYPixel = (Game.HEIGHT - screenY / Game.SCALE)
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        Mouse.button = button
        inputEvent.put("MOUSE_${getButtonName(button)}", PressType.RELEASED)
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        Mouse.button = button
        inputEvent.put("MOUSE_${getButtonName(button)}", PressType.PRESSED)
        return false
    }

    private fun getKeyName(keycode: Int) = Input.Keys.toString(keycode).toUpperCase().replace(" ", "_").removePrefix("L-").removePrefix("R-")

    private fun getButtonName(button: Int) = when (button) {
        Input.Buttons.LEFT -> "LEFT"
        Input.Buttons.BACK -> "BACK"
        Input.Buttons.FORWARD -> "FORWARD"
        Input.Buttons.RIGHT -> "RIGHT"
        Input.Buttons.MIDDLE -> "MIDDLE"
        else -> "OTHER"
    }
}