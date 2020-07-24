package level

import graphics.Image
import item.ItemType
import screen.Camera
import serialization.Input
import serialization.Output
import serialization.Serializer

private var nextId = 0

open class LevelObjectType<T : LevelObject>(initializer: LevelObjectType<T>.() -> Unit = {}) {
    val id = nextId++
    var instantiate: (xPixel: Int, yPixel: Int, rotation: Int) -> T = { _, _, _ -> throw Exception("Level object type failed to specify an adequate instantiator function") }
    var hitbox = Hitbox.NONE
    var requiresUpdate = false
    var textures = LevelObjectTextures(Image.Misc.ERROR)
    var itemForm: ItemType? = null

    init {
        initializer()
        ALL.add(this)
    }

    companion object {
        val ALL = mutableListOf<LevelObjectType<*>>()
        val ERROR = LevelObjectType<LevelObject>()
    }
}

class LevelObjectTypeSerializer : Serializer<LevelObjectType<*>>() {
    override fun write(obj: Any, output: Output) {
        obj as LevelObjectType<*>
        output.writeInt(obj.id)
    }

    override fun instantiate(input: Input): LevelObjectType<*> {
        val id = input.readInt()
        return LevelObjectType.ALL.first { it.id == id }
    }
}