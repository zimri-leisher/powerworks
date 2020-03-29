package level

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.generator.LevelType
import network.User
import serialization.Id
import java.time.LocalDateTime
import java.util.*

/**
 * An object containing information describing the general characteristics and generation settings of a [Level]
 */
data class LevelInfo(
        /**
         * The owner of the [Level]
         */
        @Id(1)
        val owner: User,
        /**
         * The name of the [Level]. This is purely for identification and is usually the name of the [owner]
         */
        @Id(2)
        val name: String,
        /**
         * The date this level was created, gotten with [LocalDateTime.now]
         */
        @Id(3)
        val dateCreated: String,
        /**
         * The generationSettings specifying how the [Level] should be generated
         */
        @Id(4)
        val levelType: LevelType,
        @Id(5)
        val seed: Long) {
    private constructor() : this(User(UUID.randomUUID(), ""), "", "", LevelType.EMPTY, 0)
}