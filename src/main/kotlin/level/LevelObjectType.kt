package level

import item.ItemType
import level.block.Block
import screen.Camera

private var nextId = 0

open class LevelObjectType<T : LevelObject>(init: LevelObjectType<T>.() -> Unit = {}) {
    val id = nextId++
    /**
     * 1: x pixel
     * 2: y pixel
     * 3: rotation
     */
    var instantiate: (Int, Int, Int) -> T = { _, _, _ -> throw Exception("Level object type failed to specify an adequate instantiator function") }

    init {
        init()
    }

    companion object {
        val ALL = mutableListOf<LevelObjectType<*>>()

        val DROPPED_ITEM = LevelObjectType<DroppedItem> {
            // default to ERROR item type because this should not be used technically. Instead, instantiate the actual class with the given item type
            instantiate = { xPixel, yPixel, _ -> DroppedItem(xPixel, yPixel, ItemType.ERROR) }
        }

        val CAMERA = LevelObjectType<Camera> {
            instantiate = { xPixel, yPixel, _ -> Camera(xPixel, yPixel) }
        }
    }
}