package level.tile

import graphics.Renderer
import level.LevelObject
import java.io.DataOutputStream

// Default argument for type is present here
open class Tile(type: TileType = TileTypes.GRASS, xTile: Int, yTile: Int) : LevelObject(xTile shl 4, yTile shl 4, false) {

    open val type = type
    val texture = type.textures[(Math.random() * type.textures.size).toInt()]

    override fun render() {
        Renderer.renderTexture(texture, xPixel, yPixel)
    }

    override fun save(out: DataOutputStream) {
    }

}