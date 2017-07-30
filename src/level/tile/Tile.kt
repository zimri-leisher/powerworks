package level.tile

import graphics.Renderer
import graphics.Texture
import level.LevelObject
import java.io.OutputStream

class Tile(val type: TileType, xTile: Int, yTile: Int) : LevelObject(xTile shl 4, yTile shl 4) {

    val texture: Texture = type.textures[(Math.random() * type.textures.size + 1).toInt()]

    override fun render() {
        Renderer.renderTexture(texture, xPixel, yPixel)
    }

    override fun save(out: OutputStream) {
    }

}