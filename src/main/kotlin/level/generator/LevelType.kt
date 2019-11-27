package level.generator

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import level.Level

private var nextId = 0

class LevelType(initializer: LevelType.() -> Unit = {}) {

    var id = nextId++
    var widthChunks = 32
    var heightChunks = 32
    var getGenerator: (level: Level) -> LevelGenerator = { level -> SimplexLevelGenerator(level) }

    init {
        ALL.add(this)
        initializer()
    }

    companion object {

        val ALL = mutableListOf<LevelType>()

        val EMPTY = LevelType {
            widthChunks = 0
            heightChunks = 0
            getGenerator = { level -> EmptyLevelGenerator(level) }
        }

        val DEFAULT_SIMPLEX = LevelType {
        }

        val AI_ENEMY = LevelType {
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LevelType

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}

class LevelTypeSerializer : Serializer<LevelType>() {
    override fun write(kryo: Kryo, output: Output, `object`: LevelType) {
        output.writeInt(`object`.id)
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out LevelType>): LevelType {
        val id = input.readInt()
        return LevelType.ALL.first { it.id == id }.apply { kryo.reference(this) }
    }

}