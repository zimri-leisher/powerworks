package level.tile

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import graphics.TextureRenderParams
import graphics.Renderer
import level.CHUNK_TILE_EXP
import level.Level
import level.LevelManager

// This is not a level object for performance reasons, and besides,
// why would we need it to be? It's not involved in anything except mining (for now)
open class Tile(type: TileType = TileType.GRASS,
                @Tag(1)
                val xTile: Int,
                @Tag(2)
                val yTile: Int,
                @Tag(3)
                val level: Level) {

    private constructor() : this(TileType.GRASS, 0, 0, LevelManager.EMPTY_LEVEL)

    @Tag(4)
    val xPixel = xTile shl 4
    @Tag(5)
    val yPixel = yTile shl 4
    @Tag(6)
    val xChunk = xTile shr CHUNK_TILE_EXP
    @Tag(7)
    val yChunk = yTile shr CHUNK_TILE_EXP

    @Tag(8)
    open val type = type

    @Tag(9)
    val texture = type.textures[(Math.random() * type.textures.size).toInt()]
    @Tag(10)
    var rotation = 0

    fun render() {
        Renderer.renderTexture(texture, xPixel, yPixel, TextureRenderParams(rotation = rotation * 90f))
    }

    override fun toString(): String {
        return "Tile at $xTile, $yTile, type: $type"
    }
}