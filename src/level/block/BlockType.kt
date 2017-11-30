package level.block

import audio.Sound
import graphics.Image
import graphics.Texture
import level.Hitbox
import level.tube.TubeBlock

private var nextID = 0

open class BlockType(val name: String,
                     val textures: Array<Texture>,
                     val widthTiles: Int = 1, val heightTiles: Int = 1,
                     val hitbox: Hitbox = Hitbox.TILE,
                     val textureXPixelOffset: Int = 0, val textureYPixelOffset: Int = 0,
                     val requiresUpdate: Boolean = false) {

    constructor(parentType: BlockType) : this(parentType.name,
            parentType.textures,
            parentType.widthTiles, parentType.heightTiles,
            parentType.hitbox,
            parentType.textureXPixelOffset, parentType.textureYPixelOffset,
            parentType.requiresUpdate)

    val id = nextID++

    init {
        ALL.add(this)
    }

    open operator fun invoke(xTile: Int, yTile: Int): Block {
        return DefaultBlock(xTile, yTile, this)
    }

    override fun toString() = name

    fun getTexture(dir: Int) = textures[Math.min(dir, textures.size)]

    companion object {
        val ALL = mutableListOf<BlockType>()

        val ERROR = BlockType("Error", arrayOf<Texture>(Image.ERROR))

        val TUBE = object : BlockType("Tube", arrayOf<Texture>(Image.Block.TUBE_2_WAY_VERTICAL)) {
            override fun invoke(xTile: Int, yTile: Int): Block {
                return TubeBlock(xTile, yTile)
            }
        }

        fun getByID(id: Int): BlockType? {
            return ALL.firstOrNull { it.id == id }
        }

        fun getByName(name: String): BlockType? {
            return ALL.firstOrNull { it.name == name }
        }
    }
}

open class ChestBlockType(name: String,
                            textures: Array<Texture>,
                            widthTiles: Int = 1, heightTiles: Int = 1,
                            hitbox: Hitbox = Hitbox.TILE,
                            textureXPixelOffset: Int = 0, textureYPixelOffset: Int = 0,
                            requiresUpdate: Boolean = false,
                            val invWidth: Int,
                            val invHeight: Int) : BlockType(name, textures, widthTiles, heightTiles, hitbox, textureXPixelOffset, textureYPixelOffset, requiresUpdate) {

    companion object {
        val CHEST_SMALL = ChestBlockType("Small Chest", arrayOf<Texture>(Image.Block.CHEST_SMALL), textureYPixelOffset = 16, invWidth = 8, invHeight = 3)
    }

    override fun invoke(xTile: Int, yTile: Int) = ChestBlock(xTile, yTile, this)
}

open class MachineBlockType(name: String,
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

    companion object {
        val MINER = object : MachineBlockType("Miner", arrayOf<Texture>(Image.Block.MINER), 2, 2, Hitbox.TILE2X2, textureYPixelOffset = 32, requiresUpdate = true, maxWork = 400) {
            override operator fun invoke(xTile: Int, yTile: Int) = MinerBlock(xTile, yTile)
        }
    }

}