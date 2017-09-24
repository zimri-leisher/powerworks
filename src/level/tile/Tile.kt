package level.tile

import graphics.RenderParams
import graphics.Renderer
import level.CHUNK_TILE_EXP

// Default argument for type is present here
open class Tile(type: TileType = TileTypes.GRASS, val xTile: Int, val yTile: Int) {

    val xPixel = xTile shl 4
    val yPixel = yTile shl 4
    val xChunk = xTile shr CHUNK_TILE_EXP
    val yChunk = yTile shr CHUNK_TILE_EXP

    open val type = type
    val texture = type.textures[(Math.random() * type.textures.size).toInt()]
    val rotation = (Math.random() * 4).toInt()

    fun render() {
        Renderer.renderTexture(texture, xPixel, yPixel, RenderParams(rotation = rotation * 90f))
    }
}