package player

import io.*
import level.Hitbox
import level.moving.MovingObject

class Player(xPixel: Int, yPixel: Int) : MovingObject(xPixel, yPixel, Hitbox.NONE), ControlPressHandler {

    init {
        InputManager.registerControlPressHandler(this, Control.UP, Control.DOWN, Control.RIGHT, Control.LEFT)
    }

    override fun render() {
        //Player has no hitbox or texture
    }

    override fun handleControlPress(p: ControlPress) {
        if (p.pressType == PressType.RELEASED)
            return
        val c = p.control
        if (c == Control.UP) {
            yVel--
        } else if (c == Control.DOWN) {
            yVel++
        } else if (c == Control.RIGHT) {
            xVel++
        } else if (c == Control.LEFT) {
            xVel--
        }
    }

}
