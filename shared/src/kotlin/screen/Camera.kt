package screen

import graphics.Renderer
import level.moving.MovingObject
import level.moving.MovingObjectType

class Camera(x: Int, y: Int) : MovingObject(MovingObjectType.CAMERA, x, y, 0) {

    override fun render() {
        Renderer.renderEmptyRectangle(x - 4, y - 4, 8, 8)
    }

    override fun toString(): String {
        return "Camera at $x, $y (id: $id)"
    }

    override fun equals(other: Any?): Boolean {
        return other is Camera && other.id == this.id // you can have multiple identical cameras
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

}
