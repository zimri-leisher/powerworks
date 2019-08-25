package io

object Test : ControlPressHandler {

    init {
        InputManager.registerControlPressHandler(this, ControlPressHandlerType.GLOBAL, ControlMap.DEFAULT)
    }

    override fun handleControlPress(p: ControlPress) {
    }

}