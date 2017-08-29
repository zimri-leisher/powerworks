package level.tile

import graphics.Renderer
import graphics.Texture
import level.LevelObject
import java.awt.Point
import java.io.DataOutputStream

open class Tile(val type: TileType, xTile: Int, yTile: Int) : LevelObject(xTile shl 4, yTile shl 4, false) {

    val texture: Texture = type.textures[(Math.random() * type.textures.size).toInt()]

    override fun render() {
        Renderer.renderTexture(texture, xPixel, yPixel)
    }

    override fun save(out: DataOutputStream) {
    }

}