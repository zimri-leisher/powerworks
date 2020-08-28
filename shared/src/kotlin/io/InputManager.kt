package io

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import data.WeakMutableMap
import main.Game
import screen.mouse.Mouse
import java.awt.event.KeyEvent

interface ControlEventHandler {
    fun handleControlEvent(event: ControlEvent)
}

object InputManager : InputProcessor {

    val state = InputState()

    var map = ControlMap.DEFAULT

    /**
     * The characters that will be sent to the text handler
     */
    val charsTyped = mutableListOf<Char>()

    /**
     * The object that should be sent key type (the word 'type' as in related to pressing, not category) events.
     */
    var textHandler: TextHandler? = null

    val handlers = WeakMutableMap<ControlEventHandler, Set<Control>>()

    private var actualMouseXPixel = 0
    private var actualMouseYPixel = 0

    fun register(handler: ControlEventHandler, controls: Control.Group) = register(handler, controls.controls)

    fun register(handler: ControlEventHandler, vararg controls: Control) = register(handler, controls.toSet())

    fun register(handler: ControlEventHandler, controls: Collection<Control> = setOf()) {
        handlers.put(handler, controls.toSet())
    }

    fun update() {
        Mouse.xPixel = actualMouseXPixel
        Mouse.yPixel = actualMouseYPixel
        state.updateEvents()
        val controlEvents = map.getControlEvents(state)
        println("controls: $controlEvents")
        for (controlEvent in controlEvents) {
            handlers.forEach { controlEventHandler, controls ->
                if (controls.isEmpty() || controlEvent.control in controls) {
                    controlEventHandler.handleControlEvent(controlEvent)
                }
            }
        }
    }

    fun shutdown() {
        map.save()
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        actualMouseXPixel = screenX / Game.SCALE
        actualMouseYPixel = (Game.HEIGHT - screenY / Game.SCALE)
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        state.setKeyState(getKeyName(keycode), ButtonState.DOWN)
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
        state.setKeyState(getKeyName(keycode), ButtonState.UP)
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
        state.setMouseButtonState("MOUSE_${getButtonName(button)}", ButtonState.UP)
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        Mouse.button = button
        state.setMouseButtonState("MOUSE_${getButtonName(button)}", ButtonState.DOWN)
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
