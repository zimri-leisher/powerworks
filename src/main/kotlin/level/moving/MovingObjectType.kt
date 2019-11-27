package level.moving

import item.ItemType
import level.DroppedItem
import level.Hitbox
import level.LevelObjectType
import screen.Camera

open class MovingObjectType<T : MovingObject>(initializer: MovingObjectType<T>.() -> Unit = {}) : LevelObjectType<T>() {
    var maxSpeed = 10
    var drag = 2

    init {
        requiresUpdate = true
        initializer()
    }
    companion object {
        val DROPPED_ITEM = MovingObjectType<DroppedItem> {
            // Instead, instantiate the actual class with the given item type. TODO fix this, this is bad - what to do?
            instantiate = { xPixel, yPixel, rotation -> DroppedItem(xPixel, yPixel, ItemType.ERROR) }
            hitbox = Hitbox.DROPPED_ITEM
        }

        val CAMERA = MovingObjectType<Camera> {
            hitbox = Hitbox.NONE
            instantiate = { xPixel, yPixel, _ -> Camera(xPixel, yPixel) }
        }
    }
}