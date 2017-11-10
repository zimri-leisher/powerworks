package level.block

import audio.Sound
import graphics.Image
import graphics.Texture
import level.Hitbox

private var nextID = 0

sealed class BlockType(val name: String,
                       val textures: Array<Texture>,
                       val widthTiles: Int = 1, val heightTiles: Int = 1,
                       val hitbox: Hitbox = Hitbox.TILE,
                       val textureXPixelOffset: Int = 0, val textureYPixelOffset: Int = 0,
                       val requiresUpdate: Boolean = false) {

    object ERROR : BlockType("Error", arrayOf<Texture>(Image.ERROR))

    val id = nextID++

    init {
        ALL.add(this)
    }

    open operator fun invoke(xTile: Int, yTile: Int): Block {
        return Block(xTile, yTile, this)
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

sealed class MachineBlockType(name: String,
                              textures: Array<Texture>,
                              widthTiles: Int = 1, heightTiles: Int = 1,
                              hitbox: Hitbox = Hitbox.TILE,
                              textureXPixelOffset: Int = 0, textureYPixelOffset: Int = 0,
                              requiresUpdate: Boolean = false,
                              val maxWork: Int,
                              val defaultEfficiency: Float = 1.0f,
                              val defaultSpeed: Float = 1.0f,
                              val loop: Boolean = true,
                              val onSound: Sound? = null) :
        BlockType(name, textures, widthTiles, heightTiles, hitbox, textureXPixelOffset, textureYPixelOffset, requiresUpdate) {

    constructor(parentType: BlockType, maxWork: Int, defaultEfficiency: Float = 1.0f, defaultSpeed: Float = 1.0f, loop: Boolean = true) : this(
            parentType.name,
            parentType.textures,
            parentType.widthTiles, parentType.heightTiles,
            parentType.hitbox,
            parentType.textureXPixelOffset, parentType.textureYPixelOffset,
            parentType.requiresUpdate,
            maxWork,
            defaultEfficiency,
            defaultSpeed,
            loop)

    object MINER : MachineBlockType("Miner", arrayOf<Texture>(Image.MINER), 2, 2, Hitbox.TILE2X2, textureYPixelOffset = 32, requiresUpdate = true, maxWork = 500) {
        override operator fun invoke(xTile: Int, yTile: Int) = MinerBlock(xTile, yTile)
    }

}