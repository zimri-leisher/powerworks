package screen

import graphics.Renderer
import level.Hitbox
import level.LevelObject
import level.LevelObjectType
import level.moving.MovingObject
import level.moving.MovingObjectType

class Camera(xPixel: Int, yPixel: Int) : MovingObject(MovingObjectType.CAMERA, xPixel, yPixel, 0) {

    override fun render() {
        Renderer.renderEmptyRectangle(xPixel, yPixel, 16, 16)
    }

    override fun toString(): String {
        return "Camera at $xPixel, $yPixel"
    }

}
