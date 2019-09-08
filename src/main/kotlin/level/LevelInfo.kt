package level

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag

data class LevelInfo(
        @Tag(1)
        val userId: String,
        @Tag(2)
        val name: String,
        @Tag(3)
        val dateCreated: String,
        @Tag(4)
        val settings: LevelGeneratorSettings,
        @Tag(5)
        val seed: Long)