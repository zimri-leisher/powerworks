package screen

import io.*
import level.CameraObject
import main.Game

object IngameDefaultGUI : GUI("In game gui", 0, 0, Game.WIDTH, Game.HEIGHT) {
    init {
        GUIView(this, "In game default view", 0, 0, Game.WIDTH, Game.HEIGHT, camera = object : CameraObject, ControlPressHandler {
            override var xPixel = 500
            override var yPixel = 500

            init {
                InputManager.registerControlPressHandler(this, Control.UP, Control.DOWN, Control.RIGHT, Control.LEFT)
            }

            override fun handleControlPress(p: ControlPress) {
                if (p.pressType == PressType.RELEASED)
                    return
                val c = p.control
                if (c == Control.UP) {
                    yPixel--
                } else if (c == Control.DOWN) {
                    yPixel++
                } else if (c == Control.RIGHT) {
                    xPixel++
                } else if (c == Control.LEFT) {
                    xPixel--
                }
            }
        })
    }
}