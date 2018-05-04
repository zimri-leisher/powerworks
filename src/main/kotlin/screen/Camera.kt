package screen

import level.Hitbox
import level.LevelObjectType
import level.moving.MovingObject

class Camera(xPixel: Int, yPixel: Int) : MovingObject(LevelObjectType.CAMERA, xPixel, yPixel, 0, Hitbox.NONE) {

    override fun toString(): String {
        return "Camera at $xPixel, $yPixel"
    }

}
