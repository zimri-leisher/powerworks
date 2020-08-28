package io

enum class InputEventType {
    PRESS, RELEASE, KEY_TYPE, DOUBLE_TAP
}

data class InputEvent(val type: InputEventType, val button: String) {
    val time = System.currentTimeMillis()
}

enum class ButtonState {
    UP, DOWN
}

class InputState {

    private val buttons = mutableMapOf<String, ButtonState>()

    private val oldEvents = mutableListOf<InputEvent>()

    private var newEvents = mutableListOf<InputEvent>()
    private var currentEvents = mutableListOf<InputEvent>()

    fun isDown(modifier: Modifier) = isDown(modifier.button)

    fun isDown(button: String) = getButtonState(button) == ButtonState.DOWN

    fun getButtonState(button: String) = buttons[button] ?: ButtonState.UP

    fun eventOccured(inputEventType: InputEventType, button: String? = null) = currentEvents.any { (button == null || it.button == button) && it.type == inputEventType }

    fun getEventsFor(button: String) = currentEvents.filter { it.button == button }.map { it.type }

    fun setKeyState(key: String, state: ButtonState) {
        val oldState = getButtonState(key)
        if (oldState == ButtonState.DOWN && state == ButtonState.UP) {
            newEvents.add(InputEvent(InputEventType.RELEASE, key))
        } else if (oldState == ButtonState.UP && state == ButtonState.DOWN) {
            newEvents.add(InputEvent(InputEventType.PRESS, key))
        }
        buttons[key] = state
    }

    fun setMouseButtonState(button: String, state: ButtonState) {
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
        println("current events: $currentEvents")
    }

    fun getEvents() = currentEvents
}