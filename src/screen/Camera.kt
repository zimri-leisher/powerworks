package screen

import level.Hitbox
import level.moving.MovingObject

class Camera(xPixel: Int, yPixel: Int) : MovingObject(xPixel, yPixel, Hitbox.NONE) {

    override fun toString(): String {
        return "Camera at $xPixel, $yPixel"
    }

}
