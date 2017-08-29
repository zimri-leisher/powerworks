package level.block

import graphics.Images
import graphics.Texture
import level.Hitbox

private var nextID = 0

object BlockTypes {
    val ERROR = BlockType("ERROR", arrayOf(Images.ERROR))
}

open class BlockType(val name: String, private val textures: Array<Texture>, val widthTiles: Int = 1, val heightTiles: Int = 1, val hitbox: Hitbox = Hitbox.TILE, val textureXPixelOffset: Int = 0, val textureYPixelOffset: Int = 0, val requiresUpdate: Boolean = false) {

    val id = nextID++

    fun getTexture(dir: Int) = textures[Math.min(dir, textures.size)]
}