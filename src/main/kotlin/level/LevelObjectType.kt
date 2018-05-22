package level

import screen.Camera

private var nextId = 0

open class LevelObjectType<T : LevelObject>(initializer: LevelObjectType<T>.() -> Unit = {}) {
    val id = nextId++
    /**
     * 1: x pixel
     * 2: y pixel
     * 3: rotation
     */
    var instantiate: (Int, Int, Int) -> T = { _, _, _ -> throw Exception("Level object type failed to specify an adequate instantiator function") }
    var hitbox = Hitbox.NONE
    var requiresUpdate = false

    init {
        initializer()
    }

    companion object {
        val ALL = mutableListOf<LevelObjectType<*>>()

        val DROPPED_ITEM = LevelObjectType<DroppedItem> {
            // default to ERROR item type because this should not be used technically. Instead, instantiate the actual class with the given item type
            instantiate = { _, _, _ -> throw Exception("Don't use the LevelObjectType.DROPPED_ITEM.instantiate function") }
            hitbox = Hitbox.DROPPED_ITEM
        }

        val CAMERA = LevelObjectType<Camera> {
            instantiate = { xPixel, yPixel, _ -> Camera(xPixel, yPixel) }
        }
    }
}