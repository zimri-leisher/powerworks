package level.tile

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag
import graphics.TextureRenderParams
import graphics.Renderer
import level.CHUNK_TILE_EXP
import level.Level
import level.LevelManager
import serialization.Id

// This is not a level object for performance reasons, and besides,
// why would we need it to be? It's not involved in anything except mining (for now)
open class Tile(type: TileType = TileType.GRASS,
                @Id(1)
                val xTile: Int,
                @Id(2)
                val yTile: Int,
                @Id(3)
                val level: Level) {

    private constructor() : this(TileType.GRASS, 0, 0, LevelManager.EMPTY_LEVEL)

    @Id(4)
    val xPixel = xTile shl 4
    @Id(5)
    val yPixel = yTile shl 4
    @Id(6)
    val xChunk = xTile shr CHUNK_TILE_EXP
    @Id(7)
    val yChunk = yTile shr CHUNK_TILE_EXP

    @Id(8)
    open val type = type

    @Id(9)
    val texture = type.textures[(Math.random() * type.textures.size).toInt()]
    @Id(10)
    var rotation = 0

    fun render() {
        Renderer.renderTexture(texture, xPixel, yPixel, TextureRenderParams(rotation = rotation * 90f))
    }

    override fun toString(): String {
        return "Tile at $xTile, $yTile, type: $type"
    }
}