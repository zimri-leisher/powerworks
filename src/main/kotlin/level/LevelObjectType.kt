package level

import graphics.Image
import screen.Camera

private var nextId = 0

open class LevelObjectType<T : LevelObject>(initializer: LevelObjectType<T>.() -> Unit = {}) {
    val id = nextId++
    /**
     * 1: x pixel
     * 2: y pixel
     * 3: rotation
     */
    var instantiate: (xPixel: Int, yPixel: Int, rotation: Int) -> T = { _, _, _ -> throw Exception("Level object type failed to specify an adequate instantiator function") }
    var hitbox = Hitbox.NONE
    var requiresUpdate = false
    var requiresRender = false
    var textures = LevelObjectTextures(Image.Misc.ERROR)
        set(value) {
            field = value
            requiresRender = true
        }

    init {
        initializer()
    }

    companion object {
        val ALL = mutableListOf<LevelObjectType<*>>()

        val ERROR = LevelObjectType<LevelObject>()

        val DROPPED_ITEM = LevelObjectType<DroppedItem> {
            // Instead, instantiate the actual class with the given item type. TODO fix this, this is bad - what to do?
            instantiate = { _, _, _ -> throw Exception("Don't use the LevelObjectType.DROPPED_ITEM.instantiate function") }
            hitbox = Hitbox.DROPPED_ITEM
        }

        val CAMERA = LevelObjectType<Camera> {
            instantiate = { xPixel, yPixel, _ -> Camera(xPixel, yPixel) }
        }
    }
}