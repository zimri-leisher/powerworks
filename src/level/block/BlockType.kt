package level.block

import graphics.Image
import graphics.Texture
import level.Hitbox

private var nextID = 0

sealed class BlockType(val name: String, private val textures: Array<Texture>, val widthTiles: Int = 1, val heightTiles: Int = 1, val hitbox: Hitbox = Hitbox.TILE, val textureXPixelOffset: Int = 0, val textureYPixelOffset: Int = 0, val requiresUpdate: Boolean = false) {

    object ERROR : BlockType("Error", arrayOf<Texture>(Image.ERROR))
    object TEST : BlockType("Test", arrayOf<Texture>(Image.ERROR))

    val id = nextID++

    init {
        ALL.add(this)
    }

    override fun toString() = name

    fun getTexture(dir: Int) = textures[Math.min(dir, textures.size)]

    companion object {
        val ALL = mutableListOf<BlockType>()

        fun getByID(id: Int): BlockType? {
            return ALL.firstOrNull { it.id == id }
        }

        fun getByName(name: String): BlockType? {
            return ALL.firstOrNull { it.name == name }
        }

    }

}