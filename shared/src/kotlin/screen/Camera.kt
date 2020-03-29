package screen

import graphics.Renderer
import level.Hitbox
import level.LevelObject
import level.LevelObjectType
import level.moving.MovingObject
import level.moving.MovingObjectType

class Camera(xPixel: Int, yPixel: Int) : MovingObject(MovingObjectType.CAMERA, xPixel, yPixel, 0) {

    override fun render() {
        Renderer.renderEmptyRectangle(xPixel - 4, yPixel - 4, 8, 8)
    }

    override fun toString(): String {
        return "Camera at $xPixel, $yPixel (id: $id)"
    }

    override fun equals(other: Any?): Boolean {
        return other is Camera && other.id == this.id // you can have multiple identical cameras
    }

}
