package level

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
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
    var textures = LevelObjectTextures(Image.Misc.ERROR)

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
    override fun write(kryo: Kryo, output: Output, `object`: LevelObjectType<*>) {
        output.writeInt(`object`.id)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out LevelObjectType<*>>): LevelObjectType<*> {
        val id = input.readInt()
        return LevelObjectType.ALL.first { it.id == id }.apply { kryo.reference(this) }
    }
}