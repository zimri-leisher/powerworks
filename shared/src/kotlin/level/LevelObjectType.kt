package level

import graphics.Image
import item.ItemType
import serialization.Input
import serialization.Output
import serialization.Serializer

private var nextId = 0

open class LevelObjectType<T : LevelObject>(initializer: LevelObjectType<T>.() -> Unit = {}) {
    val id = nextId++
    var instantiate: (xPixel: Int, yPixel: Int, rotation: Int) -> T = { _, _, _ -> throw Exception("Level object type $id failed to specify an adequate instantiator function") }
    var hitbox = Hitbox.NONE
    var requiresUpdate = false
    var textures = LevelObjectTextures(Image.Misc.ERROR)
    var itemForm: ItemType? = null
    var maxHealth = 100

    init {
        initializer()
        ALL.add(this)
    }

    companion object {
        val ALL = mutableListOf<LevelObjectType<*>>()

        val ERROR = LevelObjectType<LevelObject>()

        val DROPPED_ITEM = LevelObjectType<DroppedItem> {
            instantiate = { xPixel, yPixel, rotation -> DroppedItem(xPixel, yPixel, ItemType.ERROR) }
            hitbox = Hitbox.DROPPED_ITEM
        }
    }
}