package level.block

import audio.Sound
import graphics.Image
import graphics.Texture
import inv.ItemType
import level.Hitbox
import level.node.*
import level.resource.ResourceType
import level.tube.TubeBlock

private var nextID = 0

open class BlockType(val name: String,
                     val textures: Array<Texture>,
                     val widthTiles: Int = 1, val heightTiles: Int = 1,
                     val hitbox: Hitbox = Hitbox.TILE,
                     val storageNodeTemplates: List<StorageNodeTemplate<*>> = listOf(),
                     val transferNodeTemplates: List<TransferNodeTemplate<*, *>> = listOf(),
                     val textureXPixelOffset: Int = 0, val textureYPixelOffset: Int = 0,
                     val requiresUpdate: Boolean = false) {

    val id = nextID++

    init {
        ALL.add(this)
    }

    open operator fun invoke(xTile: Int, yTile: Int): Block {
        return DefaultBlock(xTile, yTile, this)
    }

    override fun toString() = name

    fun getTexture(dir: Int) = textures[if (dir > textures.size) 0 else dir]

    companion object {
        val ALL = mutableListOf<BlockType>()

        val ERROR = BlockType("Error", arrayOf(Image.Misc.ERROR))

        val TUBE = object : BlockType("Tube", arrayOf(Image.Block.TUBE_2_WAY_VERTICAL)) {
            override fun invoke(xTile: Int, yTile: Int): Block {
                return TubeBlock(xTile, yTile)
            }
        }

        fun getByID(id: Int): BlockType? {
            return ALL.firstOrNull { it.id == id }
        }
    }
}

open class ChestBlockType(name: String,
                          textures: Array<Texture>,
                          widthTiles: Int = 1, heightTiles: Int = 1,
                          hitbox: Hitbox = Hitbox.TILE,
                          storageNodeTemplates: List<StorageNodeTemplate<*>> = listOf(),
                          transferNodeTemplates: List<TransferNodeTemplate<ItemType, *>> = listOf(),
                          textureXPixelOffset: Int = 0, textureYPixelOffset: Int = 0,
                          requiresUpdate: Boolean = false) : BlockType(name, textures, widthTiles, heightTiles, hitbox, storageNodeTemplates, transferNodeTemplates, textureXPixelOffset, textureYPixelOffset, requiresUpdate) {

    companion object {
        val CHEST_SMALL = ChestBlockType("Small Chest", arrayOf(Image.Block.CHEST_SMALL),
                storageNodeTemplates = listOf(
                        InventoryTemplate(8, 3)
                ),
                transferNodeTemplates = listOf<InputNodeTemplate<ItemType>>(
                        InputNodeTemplate(0, 0, 0, resourceTypeID = ResourceType.ITEM),
                        InputNodeTemplate(0, 0, 2, resourceTypeID = ResourceType.ITEM),
                        InputNodeTemplate(0, 0, 1, resourceTypeID = ResourceType.ITEM),
                        InputNodeTemplate(0, 0, 3, resourceTypeID = ResourceType.ITEM)),
                textureYPixelOffset = 16)
    }

    // TODO come up with a better way to avoid casting types inside the classes. before it was just overriding the val but then you couldn't use the type in the init method of block

    override fun invoke(xTile: Int, yTile: Int) = ChestBlock(xTile, yTile, this)
}

open class MachineBlockType(name: String,
                            textures: Array<Texture>,
                            widthTiles: Int = 1, heightTiles: Int = 1,
                            hitbox: Hitbox = Hitbox.TILE,
                            storageNodeTemplates: List<StorageNodeTemplate<*>> = listOf(),
                            transferNodeTemplates: List<TransferNodeTemplate<*, *>> = listOf(),
                            textureXPixelOffset: Int = 0, textureYPixelOffset: Int = 0,
                            requiresUpdate: Boolean = false,
                            val maxWork: Int,
                            val defaultEfficiency: Float = 1.0f,
                            val defaultSpeed: Float = 1.0f,
                            val loop: Boolean = true,
                            val onSound: Sound? = null) :
        BlockType(name, textures, widthTiles, heightTiles, hitbox, storageNodeTemplates, transferNodeTemplates, textureXPixelOffset, textureYPixelOffset, requiresUpdate) {

    companion object {
        val MINER = object : MachineBlockType("Miner", arrayOf(Image.Block.MINER),
                2, 2,
                Hitbox.TILE2X2,
                transferNodeTemplates = listOf(
                    OutputNodeTemplate<ItemType>(0, 0, 0, resourceTypeID = ResourceType.ITEM)
                ), textureYPixelOffset = 32, requiresUpdate = true, maxWork = 200) {
            override operator fun invoke(xTile: Int, yTile: Int) = MinerBlock(xTile, yTile)
        }
    }

}