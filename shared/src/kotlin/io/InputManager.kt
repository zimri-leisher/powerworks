package io

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import data.WeakMutableMap
import main.DebugCode
import main.Game
import screen.gui.GuiDebugInfo
import screen.mouse.Mouse
import java.awt.event.KeyEvent

/**
 * Classes implementing this interface should use [InputManager.register] to receive [ControlEvent]s for wanted [Control]s
 */
interface ControlEventHandler {
    fun handleControlEvent(event: ControlEvent)
}

object InputManager : InputProcessor {

    /**
     * The current [InputState] of the keyboard and mouse, as seen by the game.
     */
    val state = InputState()

    /**
     * The current [ControlMap], used for translating input into controls.
     */
    var map = ControlMap.DEFAULT

    /**
     * A queue of characters to be sent to the current [textHandler].
     */
    val charsTyped = mutableListOf<Char>()

    /**
     * The object that should be sent key type (the word 'type' as in related to pressing, not category) events.
     */
    var textHandler: TextHandler? = null

    private val handlers = WeakMutableMap<ControlEventHandler, MutableSet<Control>>()

    private var actualMouseXPixel = 0
    private var actualMouseYPixel = 0

    /**
     * Registers a [ControlEventHandler], ensuring it will receive [ControlEvent]s for the given [controls]. If the
     * handler is already registered with some other controls, it will receive events for those controls and the new ones.
     */
    fun register(handler: ControlEventHandler, controls: Control.Group) = register(handler, controls.controls)

    /**
     * Registers a [ControlEventHandler], ensuring it will receive [ControlEvent]s for the given [controls]. If the
     * handler is already registered with some other controls, it will receive events for those controls and the new ones.
     */
    fun register(handler: ControlEventHandler, vararg controls: Control) = register(handler, controls.toSet())

    /**
     * Registers a [ControlEventHandler], ensuring it will receive [ControlEvent]s for the given [controls]. If the
     * handler is already registered with some other controls, it will receive events for those controls and the new ones.
     */
    fun register(handler: ControlEventHandler, controls: Collection<Control> = setOf()) {
        if (handlers.contains(handler)) {
            handlers[handler]!!.addAll(controls)
        } else {
            handlers.put(handler, controls.toMutableSet())
        }
    }

    fun update() {
        Mouse.xPixel = actualMouseXPixel
        Mouse.yPixel = actualMouseYPixel
        state.updateEvents()
        val controlEvents = map.getControlEvents(state)
        if (Game.currentDebugCode == DebugCode.CONTROLS_INFO) {
            GuiDebugInfo.show(this, controlEvents.map { it.toString() })
        }
        for (controlEvent in controlEvents) {
            handlers.forEach { controlEventHandler, controls ->
                if (controls.isEmpty() || controlEvent.control in controls) {
                    controlEventHandler.handleControlEvent(controlEvent)
                }
            }
        }
    }

    /**
     * Shuts down the [InputManager]. Should be called on game exit.
     */
    fun shutdown() {
        map.save()
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        actualMouseXPixel = screenX / Game.SCALE
        actualMouseYPixel = (Game.HEIGHT - screenY / Game.SCALE)
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        state.setButtonState(getKeyName(keycode), ButtonState.DOWN)
        return true
    }

    override fun keyTyped(character: Char): Boolean {
        if (isPrintableChar(character)) {
            state.type(character)
        }
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
        state.setButtonState(getKeyName(keycode), ButtonState.UP)
        return true
    }

    override fun scrolled(amount: Int): Boolean {
        if (amount != 0) {
            state.scroll(if (amount <= -1) ButtonState.DOWN else ButtonState.UP)
        }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        actualMouseXPixel = screenX / Game.SCALE
        // libgdx input is reversed y axis ;-;
        actualMouseYPixel = (Game.HEIGHT - screenY / Game.SCALE)
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        Mouse.button = button
        state.setButtonState("MOUSE_${getButtonName(button)}", ButtonState.UP)
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        Mouse.button = button
        state.setButtonState("MOUSE_${getButtonName(button)}", ButtonState.DOWN)
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
