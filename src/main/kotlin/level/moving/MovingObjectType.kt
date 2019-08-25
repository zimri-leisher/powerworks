package level.moving

import level.DroppedItem
import level.Hitbox
import level.LevelObjectType
import screen.Camera

open class MovingObjectType<T : MovingObject>(initializer: MovingObjectType<T>.() -> Unit = {}) : LevelObjectType<T>() {
    var maxSpeed = 10
    var drag = 2

    init {
        initializer()
    }
    companion object {
        val DROPPED_ITEM = MovingObjectType<DroppedItem> {
            // Instead, instantiate the actual class with the given item type. TODO fix this, this is bad - what to do?
            instantiate = { _, _, _ -> throw Exception("Don't use the LevelObjectType.DROPPED_ITEM.instantiate function") }
            hitbox = Hitbox.DROPPED_ITEM
        }

        val CAMERA = MovingObjectType<Camera> {
            ghost = true
            instantiate = { xPixel, yPixel, _ -> Camera(xPixel, yPixel) }
        }
    }
}