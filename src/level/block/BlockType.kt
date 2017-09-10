package level.block

import graphics.Images
import graphics.Texture
import level.Hitbox

private var nextID = 0

object BlockTypes {

    val ALL = mutableListOf<BlockType>()

    val ERROR = BlockType("ERROR", arrayOf(Images.ERROR))

    fun getByID(id: Int): BlockType? {
        return ALL.firstOrNull { it.id == id }
    }

    fun getByName(name: String): BlockType? {
        return ALL.firstOrNull { it.name == name }
    }
}

open class BlockType(val name: String, private val textures: Array<Texture>, val widthTiles: Int = 1, val heightTiles: Int = 1, val hitbox: Hitbox = Hitbox.TILE, val textureXPixelOffset: Int = 0, val textureYPixelOffset: Int = 0, val requiresUpdate: Boolean = false) {

    val id = nextID++

    init {
        BlockTypes.ALL.add(this)
    }

    fun getTexture(dir: Int) = textures[Math.min(dir, textures.size)]
}