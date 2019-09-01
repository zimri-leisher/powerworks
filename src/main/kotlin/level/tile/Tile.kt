package level.tile

import graphics.TextureRenderParams
import graphics.Renderer
import level.CHUNK_TILE_EXP
import level.Level

// This is not a level object for performance reasons, and besides,
// why would we need it to be? It's not involved in anything except mining (for now)
open class Tile(type: TileType = TileType.GRASS, val xTile: Int, val yTile: Int, val level: Level) {

    val xPixel = xTile shl 4
    val yPixel = yTile shl 4
    val xChunk = xTile shr CHUNK_TILE_EXP
    val yChunk = yTile shr CHUNK_TILE_EXP

    open val type = type
    val texture = type.textures[(Math.random() * type.textures.size).toInt()]
    var rotation = 0

    fun render() {
        Renderer.renderTexture(texture, xPixel, yPixel, TextureRenderParams(rotation = rotation * 90f))
    }

    override fun toString(): String {
        return "Tile at $xTile, $yTile, type: $type"
    }
}