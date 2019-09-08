package level

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag

data class LevelGeneratorSettings(
        @Tag(1)
        val widthTiles: Int = 256,
        @Tag(2)
        val heightTiles: Int = 256) {
    /**
     * This is only used for the default level that everything is created in
     */
    val empty get() = widthTiles == 0 && heightTiles == 0
}