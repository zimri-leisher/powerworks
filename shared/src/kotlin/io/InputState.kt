package io

/**
 * InputEventTypes define what happens to individual keys, as opposed to [ControlEventType], which are just about the
 * activation of controls.
 */
enum class InputEventType {
    PRESS, RELEASE, KEY_TYPE, DOUBLE_TAP
}

/**
 * A data class for storing button/event combinations, along with the time they occurred (to determine if it was a double
 * press or not)
 */
data class InputEvent(val type: InputEventType, val button: String) {
    val time = System.currentTimeMillis()
}

enum class ButtonState {
    UP, DOWN
}

/**
 * A class for storing the state of a keyboard and mouse. To ensure its state is correct, use [setButtonState] when a button
 * state changes, [type] when a char is typed and [scroll] when the mouse wheel is scrolled. Call [updateEvents] every tick to ensure
 * that any timed events, like a double press or a repeating key type, get processed correctly.
 */
class InputState {

    private val buttons = mutableMapOf<String, ButtonState>()

    private val oldEvents = mutableListOf<InputEvent>()

    private var newEvents = mutableListOf<InputEvent>()
    private var currentEvents = mutableListOf<InputEvent>()

    fun isDown(modifier: Modifier) = isDown(modifier.button)

    fun isDown(button: String) = getButtonState(button) == ButtonState.DOWN

    fun getButtonState(button: String) = buttons[button] ?: ButtonState.UP

    /**
     * @return all the [InputEvent]s this tick for the given [button].
     */
    fun getEventsFor(button: String) = currentEvents.filter { it.button == button }.map { it.type }

    /**
     * Sets the state of the [button] to the [state], and adds any events caused by that to the queue.
     */
    fun setButtonState(button: String, state: ButtonState) {
        val oldState = getButtonState(button)
        if (oldState == ButtonState.DOWN && state == ButtonState.UP) {
            newEvents.add(InputEvent(InputEventType.RELEASE, button))
        } else if (oldState == ButtonState.UP && state == ButtonState.DOWN) {
            newEvents.add(InputEvent(InputEventType.PRESS, button))
        }
        buttons[button] = state
    }

    fun type(char: Char) {
    }

    /**
     * Adds a scroll event in the given [direction] to the event queue.
     */
    fun scroll(direction: ButtonState) {
        val button = if (direction == ButtonState.UP) "SCROLL_UP" else "SCROLL_DOWN"
        newEvents.add(InputEvent(InputEventType.PRESS, button))
        newEvents.add(InputEvent(InputEventType.RELEASE, button))
    }

    fun updateEvents() {
        val currentTime = System.currentTimeMillis()
        oldEvents.removeIf { currentTime - it.time > 500 }
        oldEvents.addAll(currentEvents)
        currentEvents.clear()
        currentEvents.addAll(newEvents)
        newEvents.clear()

        val doubleClicks = mutableListOf<InputEvent>()
        // handle double clicks
        for (event in currentEvents) {
            if (event.type == InputEventType.PRESS) {
                // if this button was just pressed
                // check if it was pressed within the last 1/2 second
                if (oldEvents.any {
                            it.type == event.type
                                    && it.button == event.button
                                    && event.time - it.time < 500
                        }) {
                    doubleClicks.add(InputEvent(InputEventType.DOUBLE_TAP, event.button))
                }
            }
        }
        currentEvents.addAll(doubleClicks)
    }
}