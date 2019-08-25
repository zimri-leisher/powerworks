package screen

import level.Hitbox
import level.LevelObject
import level.LevelObjectType
import level.moving.MovingObject
import level.moving.MovingObjectType

class Camera(xPixel: Int, yPixel: Int) : MovingObject(MovingObjectType.CAMERA, xPixel, yPixel, 0, Hitbox.NONE) {

    override fun toString(): String {
        return "Camera at $xPixel, $yPixel"
    }

}
