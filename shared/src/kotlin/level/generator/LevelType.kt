package level.generator

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import level.Level
import serialization.ObjectIdentifier
import serialization.ObjectList

private var nextId = 0

class LevelType(initializer: LevelType.() -> Unit = {}) {

    @ObjectIdentifier
    var id = nextId++
    var widthChunks = 32
    var heightChunks = 32
    var getGenerator: (level: Level) -> LevelGenerator = { level -> SimplexLevelGenerator(level) }

    init {
        ALL.add(this)
        initializer()
    }

    companion object {
        @ObjectList
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

        val TEST = LevelType {
            widthChunks = 3
            heightChunks = 3
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