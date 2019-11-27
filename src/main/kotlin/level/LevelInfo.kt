package level

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import level.generator.LevelType
import network.User
import java.time.LocalDateTime
import java.util.*

/**
 * An object containing information describing the general characteristics and generation settings of a [Level]
 */
data class LevelInfo(
        /**
         * The owner of the [Level]
         */
        @Tag(1)
        val owner: User,
        /**
         * The name of the [Level]. This is purely for identification and is usually the name of the [owner]
         */
        @Tag(2)
        val name: String,
        /**
         * The date this level was created, gotten with [LocalDateTime.now]
         */
        @Tag(3)
        val dateCreated: String,
        /**
         * The generationSettings specifying how the [Level] should be generated
         */
        @Tag(4)
        val levelType: LevelType,
        @Tag(5)
        val seed: Long) {
    private constructor() : this(User(UUID.randomUUID(), ""), "", "", LevelType.EMPTY, 0)
}