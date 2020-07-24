package level.moving

import graphics.Image
import item.EntityItemType
import item.ItemType
import level.DroppedItem
import level.Hitbox
import level.LevelObjectTextures
import level.LevelObjectType
import screen.Camera

open class MovingObjectType<T : MovingObject>(initializer: MovingObjectType<T>.() -> Unit = {}) : LevelObjectType<T>() {
    var maxSpeed = 10
    var drag = 2
    var density = 1.0
    var mass = -1.0

    init {
        requiresUpdate = true
        initializer()
        mass = hitbox.width * hitbox.height * density
    }

    companion object {

        val ERROR = MovingObjectType<DefaultMovingObject> {
            instantiate = { xPixel, yPixel, rotation -> DefaultMovingObject(this, xPixel, yPixel, rotation) }
            textures = LevelObjectTextures(Image.Misc.ERROR)
            hitbox = Hitbox.NONE
        }

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